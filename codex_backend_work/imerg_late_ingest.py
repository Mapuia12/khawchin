#!/usr/bin/env python3
"""
Ingest NASA GPM IMERG Late precipitation (satellite) and store to Firestore.

Notes:
- IMERG Late has ~12h latency, so this is for bias/verification, not real-time nowcast.
- Access requires NASA Earthdata Login credentials (.netrc + .urs_cookies + .dodsrc).
"""

import os
import sys
import math
import re
import html
import signal
import time
from datetime import datetime, timezone
from typing import Dict, Tuple, Optional, List
from urllib.parse import unquote, urlparse, parse_qs

import requests
import json
from netrc import netrc
from requests.auth import HTTPBasicAuth

import numpy as np

try:
    from netCDF4 import Dataset, num2date
except Exception as e:
    print("ERROR: netCDF4 is required for IMERG ingestion. Install with: pip install netCDF4")
    print("Details:", e)
    sys.exit(2)

from backend_v86 import (
    CONFIG,
    Endpoints,
    generate_grid,
    init_firestore,
    now_iso,
    logger,
)


def _to_datetime(dt_obj) -> datetime:
    if isinstance(dt_obj, datetime):
        return dt_obj if dt_obj.tzinfo else dt_obj.replace(tzinfo=timezone.utc)
    # cftime objects
    if hasattr(dt_obj, "year"):
        return datetime(
            dt_obj.year,
            dt_obj.month,
            dt_obj.day,
            dt_obj.hour,
            dt_obj.minute,
            dt_obj.second,
            tzinfo=timezone.utc,
        )
    # numpy datetime64
    try:
        return datetime.fromtimestamp(dt_obj.astype("datetime64[s]").astype(int), tz=timezone.utc)
    except Exception:
        return datetime.now(timezone.utc)


def _parse_iso_utc(value: Optional[str]) -> Optional[datetime]:
    if not value:
        return None
    try:
        dt = datetime.fromisoformat(str(value).replace("Z", "+00:00"))
        return dt if dt.tzinfo else dt.replace(tzinfo=timezone.utc)
    except Exception:
        return None


def _get_var(ds: Dataset, names) -> Optional[object]:
    for name in names:
        if name in ds.variables:
            return ds.variables[name]
    return None


def _nearest_index(arr: np.ndarray, value: float) -> int:
    return int(np.argmin(np.abs(arr - value)))


def _bounding_indices(arr: np.ndarray, value: float) -> Tuple[int, int, float]:
    values = np.asarray(arr, dtype=float)
    if values.size == 0:
        return 0, 0, 0.0
    if values.size == 1:
        return 0, 0, 0.0

    ascending = values[0] <= values[-1]
    work = values if ascending else values[::-1]
    hi = int(np.searchsorted(work, value, side="left"))
    if hi <= 0:
        lo_rev = hi_rev = 0
        frac = 0.0
    elif hi >= work.size:
        lo_rev = hi_rev = work.size - 1
        frac = 0.0
    else:
        lo_rev = hi - 1
        hi_rev = hi
        denom = float(work[hi_rev] - work[lo_rev])
        frac = 0.0 if denom == 0 else float((value - work[lo_rev]) / denom)

    frac = min(1.0, max(0.0, frac))
    if ascending:
        return lo_rev, hi_rev, frac
    return len(values) - 1 - lo_rev, len(values) - 1 - hi_rev, frac


def _safe_value(val, fill_value=None) -> Optional[float]:
    if val is None:
        return None
    try:
        v = float(val)
    except Exception:
        return None
    if fill_value is not None and v == float(fill_value):
        return None
    if math.isnan(v):
        return None
    return v


def _sample_precip_rate(
    precip_slice: np.ndarray,
    lat_vals: np.ndarray,
    lon_vals: np.ndarray,
    lat: float,
    lon: float,
) -> Tuple[Optional[float], str]:
    def _read(lat_idx: int, lon_idx: int) -> Optional[float]:
        try:
            return _safe_value(precip_slice[lat_idx, lon_idx])
        except Exception:
            return None

    nearest_lat = _nearest_index(lat_vals, lat)
    nearest_lon = _nearest_index(lon_vals, lon)
    nearest_val = _read(nearest_lat, nearest_lon)

    lat_lo, lat_hi, lat_frac = _bounding_indices(lat_vals, lat)
    lon_lo, lon_hi, lon_frac = _bounding_indices(lon_vals, lon)

    corners = [
        (lat_lo, lon_lo, (1.0 - lat_frac) * (1.0 - lon_frac)),
        (lat_lo, lon_hi, (1.0 - lat_frac) * lon_frac),
        (lat_hi, lon_lo, lat_frac * (1.0 - lon_frac)),
        (lat_hi, lon_hi, lat_frac * lon_frac),
    ]

    weighted_sum = 0.0
    total_weight = 0.0
    valid_corners = 0
    for lat_idx, lon_idx, weight in corners:
        if weight <= 0:
            continue
        val = _read(lat_idx, lon_idx)
        if val is None:
            continue
        weighted_sum += weight * val
        total_weight += weight
        valid_corners += 1

    if valid_corners >= 2 and total_weight > 0:
        return weighted_sum / total_weight, "bilinear"
    return nearest_val, "nearest"


def _extract_precip_slice(
    precip_var,
    dim_index: Dict[str, int],
    latest_idx: int,
    fill_value=None,
) -> np.ndarray:
    index = [slice(None)] * len(precip_var.dimensions)
    index[dim_index["time"]] = latest_idx
    subset = precip_var[tuple(index)]

    if np.ma.isMaskedArray(subset):
        arr = np.ma.filled(subset, np.nan)
    else:
        arr = np.asarray(subset)

    arr = np.asarray(arr, dtype=float)
    if fill_value is not None:
        arr = np.where(arr == float(fill_value), np.nan, arr)

    remaining_axes = [i for i in range(len(precip_var.dimensions)) if i != dim_index["time"]]
    lat_axis = remaining_axes.index(dim_index["lat"])
    lon_axis = remaining_axes.index(dim_index["lon"])

    if arr.ndim < 2:
        raise ValueError(f"IMERG precipitation slice has too few dimensions: shape={arr.shape}")

    if (lat_axis, lon_axis) != (0, 1):
        arr = np.moveaxis(arr, (lat_axis, lon_axis), (0, 1))

    if arr.ndim > 2:
        extra_shape = arr.shape[2:]
        if any(size > 1 for size in extra_shape):
            raise ValueError(f"IMERG precipitation slice has unsupported extra dimensions: shape={arr.shape}")
        arr = np.squeeze(arr, axis=tuple(range(2, arr.ndim)))

    return arr


def _earthdata_auth() -> Optional[HTTPBasicAuth]:
    """Load Earthdata Login credentials from ~/.netrc."""
    try:
        info = netrc().authenticators("urs.earthdata.nasa.gov")
        if not info:
            return None
        user, _, password = info
        if not user or not password:
            return None
        return HTTPBasicAuth(user, password)
    except Exception:
        return None


def _fetch_text(url: str) -> str:
    """Fetch a URL with Earthdata auth (if available)."""
    auth = _earthdata_auth()
    with requests.Session() as s:
        if auth:
            s.auth = auth
        resp = s.get(url, timeout=60)
        resp.raise_for_status()
        return resp.text


def _list_dirs(contents_html: str, pattern: str) -> List[str]:
    return sorted(set(re.findall(pattern, contents_html)))


def _fetch_any(urls: List[str]) -> Optional[str]:
    for url in urls:
        try:
            return _fetch_text(url)
        except Exception:
            continue
    return None


def _normalize_opendap_url(url: str) -> Optional[str]:
    if not url:
        return None
    raw = html.unescape(url)
    raw = unquote(raw)
    lower = raw.lower()
    if "viewers/viewers" in lower or "dapservice=" in lower or "datasetid=" in lower:
        qs = parse_qs(urlparse(raw).query, keep_blank_values=True)
        dsid = (qs.get("datasetID") or qs.get("datasetid") or [None])[0]
        if not dsid:
            match = re.search(r"(?:^|[?&])(?:amp;)?datasetid=([^&]+)", raw, flags=re.IGNORECASE)
            if match:
                dsid = match.group(1)
        if dsid:
            dsid = html.unescape(unquote(dsid)).strip()
            if not dsid.startswith("/"):
                dsid = "/" + dsid
            return f"https://gpm1.gesdisc.eosdis.nasa.gov/opendap{dsid}"
    return raw


def _resolve_opendap_href(root: str, year: str, doy: str, href: str) -> str:
    if href.startswith("http://") or href.startswith("https://"):
        raw = href
    elif href.startswith("/"):
        raw = f"https://gpm1.gesdisc.eosdis.nasa.gov{href}"
    else:
        raw = f"{root}/{year}/{doy}/{href}"
    return _normalize_opendap_url(raw) or raw


def _is_dataset_url(url: Optional[str]) -> bool:
    if not url:
        return False
    lower = url.lower()
    if "viewers/viewers" in lower or "dapservice=" in lower or "datasetid=" in lower:
        return False
    return lower.endswith(".hdf5.nc4") or lower.endswith(".nc4") or lower.endswith(".hdf5")


def _build_dataset_candidates(url: str) -> List[str]:
    normalized = _normalize_opendap_url(url) or url
    candidates: List[str] = []

    def _add(candidate: Optional[str]) -> None:
        if not candidate:
            return
        if candidate not in candidates:
            candidates.append(candidate)

    lower = normalized.lower()
    if lower.endswith(".hdf5") and not lower.endswith(".hdf5.nc4"):
        _add(normalized + ".nc4")
    _add(normalized)
    if lower.endswith(".hdf5.nc4"):
        _add(normalized[:-4])
    return candidates


def _probe_dataset_url(url: str, timeout_sec: int = 20) -> bool:
    auth = _earthdata_auth()
    probe_paths = (".dds", ".das", ".dmr.xml")
    with requests.Session() as session:
        if auth:
            session.auth = auth
        for suffix in probe_paths:
            probe_url = f"{url}{suffix}"
            try:
                resp = session.get(probe_url, timeout=timeout_sec)
                resp.raise_for_status()
                preview = (resp.text or "")[:500].lower()
                if "earthdata login" in preview or "urs.earthdata.nasa.gov" in preview:
                    continue
                if "<html" in preview and "viewers/viewers" in preview:
                    continue
                return True
            except Exception:
                continue
    return False


def _open_dataset_with_timeout(url: str, timeout_sec: int) -> Dataset:
    sigalrm = getattr(signal, "SIGALRM", None)
    if os.name == "nt" or sigalrm is None:
        return Dataset(url)

    def _raise_timeout(signum, frame):
        raise TimeoutError(f"Timed out opening remote IMERG dataset after {timeout_sec}s")

    old_handler = signal.getsignal(sigalrm)
    try:
        signal.signal(sigalrm, _raise_timeout)
        signal.alarm(timeout_sec)
        return Dataset(url)
    finally:
        signal.alarm(0)
        signal.signal(sigalrm, old_handler)


def _open_imerg_dataset(file_urls) -> Tuple[str, Dataset]:
    probe_timeout = int(os.environ.get("IMERG_PROBE_TIMEOUT", "20"))
    open_timeout = int(os.environ.get("IMERG_DATASET_OPEN_TIMEOUT", "120"))
    last_error: Optional[Exception] = None
    if isinstance(file_urls, str):
        base_urls = [file_urls]
    else:
        base_urls = [u for u in file_urls if u]

    candidate_urls: List[str] = []
    for base_url in base_urls:
        for candidate in _build_dataset_candidates(base_url):
            if candidate not in candidate_urls:
                candidate_urls.append(candidate)

    logger.info("IMERG dataset candidates: %s", ", ".join(candidate_urls))
    for candidate in candidate_urls:
        logger.info("Probing IMERG dataset candidate: %s", candidate)
        if not _probe_dataset_url(candidate, timeout_sec=probe_timeout):
            logger.warning("IMERG dataset probe failed for %s", candidate)
            continue
        try:
            logger.info("Opening IMERG file: %s", candidate)
            return candidate, _open_dataset_with_timeout(candidate, timeout_sec=open_timeout)
        except Exception as e:
            last_error = e
            logger.warning("IMERG dataset open failed for %s: %s", candidate, e)
    raise RuntimeError(f"Could not open IMERG dataset from {len(candidate_urls)} candidate(s): {last_error}")


def _pick_recent_imerg_files_opendap(root: str) -> Optional[List[str]]:
    """Locate several recent IMERG Late half-hourly files via OPeNDAP contents listing."""
    root = root.rstrip("/")
    lookback_days = max(1, int(os.environ.get("IMERG_LOOKBACK_DAYS", "3")))
    per_day_limit = max(1, int(os.environ.get("IMERG_FILES_PER_DAY", "6")))
    # 1) List years
    years_html = _fetch_any([
        f"{root}/contents.html",
        f"{root}/catalog.html",
        f"{root}/",
    ])
    if not years_html:
        return None
    if "Earthdata Login" in years_html or "urs.earthdata.nasa.gov" in years_html:
        logger.error("Earthdata auth failed while listing IMERG root.")
        return None
    years = _list_dirs(years_html, r'href="(\d{4})/?')
    if not years:
        logger.error("Could not find year directories in IMERG listing.")
        return []
    year = years[-1]

    # 2) List day-of-year directories
    doys_html = _fetch_any([
        f"{root}/{year}/contents.html",
        f"{root}/{year}/catalog.html",
        f"{root}/{year}/",
    ])
    if not doys_html:
        return []
    doys = _list_dirs(doys_html, r'href="(\d{3})/?')
    if not doys:
        logger.error("Could not find day-of-year directories in IMERG listing.")
        return []

    recent_doys = list(reversed(doys[-lookback_days:]))
    recent_urls: List[str] = []
    for doy in recent_doys:
        day_html = _fetch_any([
            f"{root}/{year}/{doy}/contents.html",
            f"{root}/{year}/{doy}/catalog.html",
            f"{root}/{year}/{doy}/",
        ])
        if not day_html:
            continue
        hrefs = re.findall(r'href="([^"]+)"', day_html, flags=re.IGNORECASE)
        dataset_urls: List[str] = []
        for href in hrefs:
            resolved = _resolve_opendap_href(root, year, doy, href)
            if _is_dataset_url(resolved) and resolved not in dataset_urls:
                dataset_urls.append(resolved)
        dataset_urls = sorted(dataset_urls, reverse=True)
        for resolved in dataset_urls[:per_day_limit]:
            if resolved not in recent_urls:
                recent_urls.append(resolved)
    return recent_urls


def _pick_recent_imerg_files_cmr() -> List[str]:
    """
    Fallback: use NASA CMR to find recent granules and their direct OPeNDAP/data URLs.
    Reject HTML viewer links.
    """
    base = "https://cmr.earthdata.nasa.gov/search/granules.json"
    cmr_limit = max(1, int(os.environ.get("IMERG_CMR_LIMIT", "5")))
    params = {
        "short_name": "GPM_3IMERGHHL",
        "version": "07",
        "provider": "GES_DISC",
        "page_size": cmr_limit,
        "sort_key": "-start_date",
    }
    try:
        resp = requests.get(base, params=params, timeout=60)
        resp.raise_for_status()
        data = resp.json()
        entries = (data.get("feed") or {}).get("entry") or []
        if not entries:
            return []

        recent_urls: List[str] = []
        for entry in entries:
            links = entry.get("links") or []
            for link in links:
                href = _normalize_opendap_url(link.get("href") or "") or ""
                rel = (link.get("rel") or "").lower()
                typ = (link.get("type") or "").lower()

                # Skip CMR collection or API links that are not OPeNDAP datasets
                if "earthdata.nasa.gov/collections/" in href:
                    continue

                # Direct data links
                if href.endswith(".HDF5.nc4") or href.endswith(".nc4") or href.endswith(".HDF5"):
                    if href not in recent_urls:
                        recent_urls.append(href)
                    continue

                # Some CMR results label the real file as data
                if ".HDF5" in href and ("data#" in rel or "application/x-netcdf" in typ or "application/netcdf" in typ):
                    if href not in recent_urls:
                        recent_urls.append(href)
            if len(recent_urls) >= cmr_limit:
                break
        return recent_urls
    except Exception:
        return []


def _pick_recent_imerg_files(root: str) -> List[str]:
    """Locate several recent IMERG Late files via OPeNDAP listing, else via CMR."""
    file_urls = _pick_recent_imerg_files_opendap(root)
    if file_urls:
        return file_urls
    logger.warning("OPeNDAP listing failed; trying CMR lookup...")
    return _pick_recent_imerg_files_cmr()


def _imerg_already_current(db, grid: List[object], latest_time: datetime) -> bool:
    if os.environ.get("IMERG_FORCE_REWRITE", "0") == "1":
        logger.info("IMERG_FORCE_REWRITE=1; bypassing up-to-date check")
        return False
    if not grid:
        return False

    sample_indices = sorted(set([0, len(grid) // 2, len(grid) - 1]))
    matched = 0
    for idx in sample_indices:
        try:
            sample_doc = db.collection(CONFIG.imerg_collection).document(grid[idx].id).get()
            if not sample_doc.exists:
                return False
            sample_time = _parse_iso_utc((sample_doc.to_dict() or {}).get("imerg_time"))
            if sample_time is not None and abs((sample_time - latest_time).total_seconds()) < 60:
                matched += 1
            else:
                return False
        except Exception as e:
            logger.debug("IMERG state check failed for sample %d: %s", idx, e)
            return False

    if matched == len(sample_indices):
        logger.info("IMERG Late already up to date for %s; skipping rewrite", latest_time.isoformat())
        return True
    return False


def _commit_batch_with_retry(batch, ops: int, label: str) -> bool:
    attempts = max(1, int(os.environ.get("IMERG_FIRESTORE_COMMIT_RETRIES", "3")))
    for attempt in range(1, attempts + 1):
        try:
            batch.commit()
            return True
        except Exception as err:
            if attempt >= attempts:
                logger.error("IMERG %s commit failed after %d attempt(s): %s", label, attempt, err)
                return False
            delay = min(10.0, 1.5 * attempt)
            logger.warning("IMERG %s commit failed (%s); retrying in %.1fs", label, err, delay)
            time.sleep(delay)
    return False


def ingest_imerg_late() -> int:
    logger.info("=" * 60)
    logger.info("Starting IMERG Late ingestion")
    logger.info("Dataset: %s", Endpoints.GPM_IMERG_LATE)
    logger.info("=" * 60)

    # Firestore
    db = init_firestore()
    if db is None:
        logger.error("Firestore not initialized. Aborting IMERG ingestion.")
        return 0

    try:
        file_urls = _pick_recent_imerg_files(Endpoints.GPM_IMERG_LATE)
        if not file_urls:
            logger.error("Could not locate recent IMERG Late files via OPeNDAP/CMR.")
            return 0
        logger.info("IMERG recent file candidates discovered: %d", len(file_urls))
        resolved_url, ds = _open_imerg_dataset(file_urls)
    except Exception as e:
        logger.error("Failed to open IMERG dataset (auth required). Error: %s", e)
        return 0
    try:
        lat_var = _get_var(ds, ["lat", "latitude"])
        lon_var = _get_var(ds, ["lon", "longitude"])
        time_var = _get_var(ds, ["time"])
        precip_var = _get_var(ds, ["precipitationCal", "precipitation"])

        if not (lat_var and lon_var and time_var and precip_var):
            logger.error("IMERG variables missing (lat/lon/time/precip). Aborting.")
            return 0

        lat_vals = np.array(lat_var[:])
        lon_vals = np.array(lon_var[:])

        time_vals = num2date(time_var[:], getattr(time_var, "units", None))
        if len(time_vals) == 0:
            logger.error("IMERG time axis is empty.")
            return 0

        latest_idx = len(time_vals) - 1
        latest_time = _to_datetime(time_vals[latest_idx])

        logger.info("Latest IMERG time: %s (index=%d)", latest_time.isoformat(), latest_idx)

        # Determine dimension order for precipitation
        dims = list(getattr(precip_var, "dimensions", []))
        dim_index = {name: i for i, name in enumerate(dims)}
        time_dim = "time" if "time" in dim_index else None
        lat_dim = "lat" if "lat" in dim_index else ("latitude" if "latitude" in dim_index else None)
        lon_dim = "lon" if "lon" in dim_index else ("longitude" if "longitude" in dim_index else None)

        if not (time_dim and lat_dim and lon_dim):
            logger.error("IMERG precipitation dims not recognized: %s", dims)
            return 0

        fill_value = getattr(precip_var, "_FillValue", None)
        logger.info("Loading latest IMERG precipitation slice into memory for fast local sampling...")
        precip_slice = _extract_precip_slice(
            precip_var,
            {"time": dim_index[time_dim], "lat": dim_index[lat_dim], "lon": dim_index[lon_dim]},
            latest_idx,
            fill_value=fill_value,
        )
        logger.info("IMERG slice loaded: shape=%s", tuple(int(x) for x in precip_slice.shape))

        grid = list(generate_grid())
        logger.info(
            "IMERG ingestion will sample %d grid cells. The API estimate above comes from shared grid metadata and does not mean this job is calling forecast model APIs.",
            len(grid),
        )
        if _imerg_already_current(db, grid, latest_time):
            return 0
        written = 0
        queued = 0
        batch = db.batch()
        ops = 0

        for p in grid:
            rate, sampling_method = _sample_precip_rate(
                precip_slice,
                lat_vals,
                lon_vals,
                p.lat,
                p.lon,
            )

            payload = {
                "grid_id": p.id,
                "lat": p.lat,
                "lon": p.lon,
                "imerg_time": latest_time.isoformat(),
                "precip_rate_mm_hr": round(rate, 3) if rate is not None else None,
                "precip_30min_mm": round(rate * 0.5, 3) if rate is not None else None,
                "source": "GPM IMERG Late",
                "sampling_method": sampling_method,
                "generated": now_iso(),
            }

            try:
                doc_ref = db.collection(CONFIG.imerg_collection).document(p.id)
                batch.set(doc_ref, payload, merge=True)
                ops += 1
                queued += 1
                if queued % 50 == 0:
                    logger.info("IMERG ingestion progress: %d/%d cells queued", queued, len(grid))
                if ops >= 400:
                    logger.info("IMERG ingestion committing batch with %d queued writes", ops)
                    if _commit_batch_with_retry(batch, ops, "batch"):
                        written += ops
                    batch = db.batch()
                    ops = 0
            except Exception as e:
                logger.debug("IMERG write failed for %s: %s", p.id, e)

        if ops:
            logger.info("IMERG ingestion committing final batch with %d queued writes", ops)
            if _commit_batch_with_retry(batch, ops, "final batch"):
                written += ops

        logger.info("IMERG Late ingestion complete: %d/%d written (%d queued)", written, len(grid), queued)
        return written

    finally:
        try:
            ds.close()
        except Exception:
            pass


if __name__ == "__main__":
    ingest_imerg_late()
