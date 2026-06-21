#!/usr/bin/env python3
"""
Apply IMERG Late observations to update precipitation bias factors.

Uses forecast snapshots stored during full runs and compares to IMERG Late
precipitation (satellite). Updates BiasManager with EMA smoothing.
"""

import os
from datetime import datetime, timezone, timedelta
from typing import Optional, List, Tuple, Dict, Any
from zoneinfo import ZoneInfo

from firebase_admin import firestore
from backend_v86 import (
    CONFIG,
    init_firestore,
    BiasManager,
    logger,
    classify_precip_regime,
    get_terrain_zone_key,
)


JOB_STATE_DOC = "_imerg_bias_job_state"


def _parse_iso(ts: Optional[str]) -> Optional[datetime]:
    if ts is None:
        return None
    if isinstance(ts, datetime):
        return ts if ts.tzinfo else ts.replace(tzinfo=timezone.utc)
    if not isinstance(ts, str):
        return None
    try:
        if ts.endswith("Z"):
            ts = ts.replace("Z", "+00:00")
        dt = datetime.fromisoformat(ts)
        return dt if dt.tzinfo else dt.replace(tzinfo=timezone.utc)
    except Exception:
        return None


def _parse_iso_raw(ts: Optional[str]) -> Optional[datetime]:
    if ts is None or not isinstance(ts, str):
        return None
    try:
        if ts.endswith("Z"):
            ts = ts.replace("Z", "+00:00")
        return datetime.fromisoformat(ts)
    except Exception:
        return None


def _infer_timezone_name(lat: Optional[object], lon: Optional[object]) -> str:
    try:
        lon_value = float(lon)
    except Exception:
        lon_value = None
    if lon_value is None:
        return "Asia/Yangon"
    if lon_value >= 97.5:
        return "Asia/Bangkok"
    if lon_value >= 93.0:
        return "Asia/Yangon"
    return "Asia/Kolkata"


def _resolve_tzinfo(
    tz_name: Optional[str],
    utc_offset_seconds: Optional[object],
    lat: Optional[object],
    lon: Optional[object],
):
    if tz_name:
        try:
            return ZoneInfo(str(tz_name))
        except Exception:
            pass
    if utc_offset_seconds is not None:
        try:
            return timezone(timedelta(seconds=int(float(utc_offset_seconds))))
        except Exception:
            pass
    try:
        return ZoneInfo(_infer_timezone_name(lat, lon))
    except Exception:
        return timezone.utc


def _parse_forecast_time(
    ts: Optional[str],
    tz_name: Optional[str],
    utc_offset_seconds: Optional[object],
    lat: Optional[object],
    lon: Optional[object],
) -> Optional[datetime]:
    raw = _parse_iso_raw(ts)
    if raw is None:
        return None
    if raw.tzinfo is not None:
        return raw.astimezone(timezone.utc)
    tzinfo = _resolve_tzinfo(tz_name, utc_offset_seconds, lat, lon)
    return raw.replace(tzinfo=tzinfo).astimezone(timezone.utc)


def _pick_forecast_at_time(
    times: List[str],
    precip: List,
    target: datetime,
    max_hours: int,
    tz_name: Optional[str] = None,
    utc_offset_seconds: Optional[object] = None,
    lat: Optional[object] = None,
    lon: Optional[object] = None,
) -> Tuple[Optional[float], Optional[float], Optional[int], Optional[datetime]]:
    if not times or not precip:
        return None, None, None, None
    best_idx = None
    best_delta = None
    best_dt = None
    for i, t in enumerate(times):
        dt = _parse_forecast_time(t, tz_name, utc_offset_seconds, lat, lon)
        if dt is None:
            continue
        delta = abs((dt - target).total_seconds())
        if best_delta is None or delta < best_delta:
            best_delta = delta
            best_idx = i
            best_dt = dt
    if best_idx is None:
        return None, None, None, None
    if best_delta is not None and best_delta > max_hours * 3600:
        return None, None, None, None
    try:
        val = precip[best_idx]
        return (
            float(val) if val is not None else None,
            (best_delta / 3600.0 if best_delta is not None else None),
            best_idx,
            best_dt,
        )
    except Exception:
        return None, None, None, None


def _as_list(value: Optional[object]) -> List:
    """Firestore snapshots have evolved; normalize legacy/list-like fields."""
    if isinstance(value, list):
        return value
    if isinstance(value, tuple):
        return list(value)
    return []


def _nested_get(mapping: Optional[object], *keys: str) -> Optional[object]:
    current = mapping
    for key in keys:
        if not isinstance(current, dict):
            return None
        current = current.get(key)
    return current


def _first_list(*values: Optional[object]) -> List:
    for value in values:
        items = _as_list(value)
        if items:
            return items
    return []


def _extract_snapshot_series(snapshot: Dict[str, Any]) -> Tuple[List, List, List]:
    """
    Return hourly times, precipitation and probability from both new and legacy
    snapshot shapes.

    New full runs write top-level `times` + `precip_mm`, but older deployments
    and some export/debug payloads used nested `hourly` keys. Supporting both
    prevents IMERG samples from being marked no_match solely because EC2 has a
    mixed snapshot history.
    """
    hourly = snapshot.get("hourly") if isinstance(snapshot.get("hourly"), dict) else {}
    forecast = snapshot.get("forecast") if isinstance(snapshot.get("forecast"), dict) else {}

    times = _first_list(
        snapshot.get("times"),
        snapshot.get("time"),
        snapshot.get("valid_times"),
        _nested_get(hourly, "time"),
        _nested_get(hourly, "times"),
        _nested_get(forecast, "times"),
        _nested_get(forecast, "time"),
    )
    precip = _first_list(
        snapshot.get("precip_mm"),
        snapshot.get("precipitation_mm"),
        snapshot.get("precipitation"),
        snapshot.get("rain_mm"),
        _nested_get(hourly, "precipitation_mm"),
        _nested_get(hourly, "precipitation"),
        _nested_get(hourly, "rain_mm"),
        _nested_get(forecast, "precip_mm"),
        _nested_get(forecast, "precipitation_mm"),
    )
    probs = _first_list(
        snapshot.get("precip_prob"),
        snapshot.get("precipitation_probability"),
        snapshot.get("rain_probability"),
        _nested_get(hourly, "precipitation_probability"),
        _nested_get(hourly, "precip_prob"),
        _nested_get(forecast, "precip_prob"),
    )
    return times, precip, probs


def _coerce_dt(value: Optional[object]) -> Optional[datetime]:
    if value is None:
        return None
    if isinstance(value, datetime):
        return value if value.tzinfo else value.replace(tzinfo=timezone.utc)
    if isinstance(value, str):
        return _parse_iso(value)
    return None


def _load_last_processed_time(db) -> Optional[datetime]:
    try:
        snap = db.collection(CONFIG.bias_collection).document(JOB_STATE_DOC).get()
        if not snap.exists:
            return None
        data = snap.to_dict() or {}
        ts = data.get("last_imerg_time")
        return _coerce_dt(ts)
    except Exception as err:
        logger.debug("IMERG job state load error: %s", err)
        return None


def _query_imerg_collection(col, last_processed: Optional[datetime]):
    if last_processed is None:
        try:
            yield from col.order_by("imerg_time", direction=firestore.Query.ASCENDING).stream()
        except Exception:
            yield from col.stream()
        return

    cutoff = last_processed.isoformat()
    try:
        try:
            query = col.where(filter=firestore.FieldFilter("imerg_time", ">", cutoff))
        except Exception:
            query = col.where("imerg_time", ">", cutoff)
        yield from query.order_by("imerg_time", direction=firestore.Query.ASCENDING).stream()
    except Exception as err:
        logger.warning("IMERG incremental query failed; falling back to full scan: %s", err)
        yield from col.stream()


def _iter_imerg_documents(db, last_processed: Optional[datetime]):
    """Yield new IMERG docs, preferring immutable history over latest-only docs."""
    use_history = os.environ.get("IMERG_BIAS_USE_HISTORY", "1") == "1"
    fallback_latest = os.environ.get("IMERG_BIAS_FALLBACK_LATEST", "1") == "1"

    if use_history:
        history_name = getattr(CONFIG, "imerg_history_collection", "imerg_late_history")
        yielded = False
        try:
            for doc in _query_imerg_collection(db.collection(history_name), last_processed):
                yielded = True
                yield doc
            if yielded:
                return
        except Exception as err:
            logger.warning("IMERG history query failed; falling back to latest collection: %s", err)

    if fallback_latest:
        yield from _query_imerg_collection(db.collection(CONFIG.imerg_collection), last_processed)


def _load_candidate_run_snapshots(runs_ref, target: datetime, per_side_limit: int = 3) -> List[dict]:
    """Load a few candidate run snapshots around target time."""
    docs_out: List[dict] = []
    seen_ids = set()

    def _append_from_query(query_builder, ascending: bool) -> None:
        try:
            docs = list(
                query_builder.order_by(
                    "run_time",
                    direction=firestore.Query.ASCENDING if ascending else firestore.Query.DESCENDING,
                ).limit(per_side_limit).stream()
            )
        except Exception:
            return
        for doc in docs:
            if doc.id in seen_ids:
                continue
            payload = doc.to_dict() or {}
            payload["run_id"] = payload.get("run_id") or doc.id
            docs_out.append(payload)
            seen_ids.add(doc.id)

    def _append_recent_scan(limit: int) -> None:
        """Fallback for mixed Firestore timestamp/string run_time schemas."""
        try:
            docs = list(
                runs_ref.order_by(
                    "run_time",
                    direction=firestore.Query.DESCENDING,
                ).limit(limit).stream()
            )
        except Exception as err:
            logger.debug("IMERG bias run snapshot fallback scan failed: %s", err)
            return
        for doc in docs:
            if doc.id in seen_ids:
                continue
            payload = doc.to_dict() or {}
            payload["run_id"] = payload.get("run_id") or doc.id
            docs_out.append(payload)
            seen_ids.add(doc.id)

    try:
        try:
            before_query = runs_ref.where(filter=firestore.FieldFilter("run_time", "<=", target))
        except Exception:
            before_query = runs_ref.where("run_time", "<=", target)
        _append_from_query(before_query, ascending=False)
    except Exception:
        pass

    try:
        try:
            after_query = runs_ref.where(filter=firestore.FieldFilter("run_time", ">=", target))
        except Exception:
            after_query = runs_ref.where("run_time", ">=", target)
        _append_from_query(after_query, ascending=True)
    except Exception:
        pass

    if not docs_out:
        scan_limit = max(6, int(os.environ.get("IMERG_BIAS_RUN_SCAN_LIMIT", "18")))
        _append_recent_scan(scan_limit)

    return docs_out


def run_imerg_bias_job() -> int:
    logger.info("=" * 60)
    logger.info("Starting IMERG bias update job")
    logger.info("=" * 60)

    db = init_firestore()
    if db is None:
        logger.error("Firestore not initialized. Aborting IMERG bias job.")
        return 0

    bias_mgr = BiasManager(db)
    updated = 0
    checked = 0
    total_docs = 0
    matched_runs = 0
    used_legacy_snapshots = 0
    no_snapshot = 0
    no_forecast_match = 0
    stale_match = 0
    stale_min_delta_h: Optional[float] = None
    stale_max_delta_h: Optional[float] = None
    skipped_by_watermark = 0
    skipped_tiny_pair = 0
    snapshot_schema_miss = 0
    future_run_skipped = 0
    future_only_docs = 0
    flushed_bias_docs = 0
    zone_stats: Dict[str, Dict[str, int]] = {}

    last_processed = _load_last_processed_time(db)
    if os.environ.get("IMERG_BIAS_IGNORE_WATERMARK", "0") == "1":
        if last_processed is not None:
            logger.info("IMERG bias watermark ignored for this run via IMERG_BIAS_IGNORE_WATERMARK=1")
        last_processed = None
    advance_watermark_on_scan = os.environ.get("IMERG_BIAS_ADVANCE_WATERMARK_ON_SCAN", "0") == "1"
    allow_future_runs = os.environ.get("IMERG_BIAS_ALLOW_FUTURE_RUNS", "0") == "1"
    advance_future_only = os.environ.get("IMERG_BIAS_ADVANCE_FUTURE_ONLY", "1") == "1"
    newest_seen_imerg_time: Optional[datetime] = last_processed
    newest_updated_imerg_time: Optional[datetime] = None
    newest_terminal_skipped_imerg_time: Optional[datetime] = None
    if last_processed is not None:
        logger.info("IMERG bias job watermark: skipping samples at/before %s", last_processed.isoformat())

    for doc in _iter_imerg_documents(db, last_processed):
        total_docs += 1
        data = doc.to_dict() or {}
        gid = str(data.get("grid_id") or data.get("gid") or doc.id)

        imerg_time = _parse_iso(data.get("imerg_time"))
        if imerg_time is None:
            continue
        if last_processed is not None and imerg_time <= last_processed:
            skipped_by_watermark += 1
            continue
        if newest_seen_imerg_time is None or imerg_time > newest_seen_imerg_time:
            newest_seen_imerg_time = imerg_time

        observed = data.get("precip_rate_mm_hr")
        if observed is None and data.get("precip_30min_mm") is not None:
            observed = data.get("precip_30min_mm") * 2.0

        try:
            observed = float(observed) if observed is not None else None
        except Exception:
            observed = None

        if observed is None:
            continue

        chosen_snapshot = None
        chosen_is_legacy = False
        chosen_probs: List = []
        best_tuple: Tuple[Optional[float], Optional[float], Optional[int], Optional[datetime]] = (None, None, None, None)
        future_skips_for_doc = 0
        non_future_candidate_seen = False
        future_cutoff = imerg_time + timedelta(minutes=5)

        # Prefer per-run snapshots, but choose the run whose forecast valid time best matches IMERG time.
        try:
            runs_ref = db.collection(CONFIG.forecast_snapshot_collection).document(gid).collection("runs")
            candidates = _load_candidate_run_snapshots(runs_ref, imerg_time)
        except Exception:
            candidates = []

        best_delta_any = None
        for candidate in candidates:
            candidate_run_time = _coerce_dt(candidate.get("run_time"))
            if candidate_run_time is None or candidate_run_time <= future_cutoff:
                non_future_candidate_seen = True
            if (
                not allow_future_runs
                and candidate_run_time is not None
                and candidate_run_time > future_cutoff
            ):
                future_run_skipped += 1
                future_skips_for_doc += 1
                continue
            times, precip, probs = _extract_snapshot_series(candidate)
            if not times or not precip:
                snapshot_schema_miss += 1
                continue
            lat = data.get("lat") if data.get("lat") is not None else candidate.get("lat")
            lon = data.get("lon") if data.get("lon") is not None else candidate.get("lon")
            snapshot_tz_name = candidate.get("timezone")
            snapshot_utc_offset = candidate.get("utc_offset_seconds")
            forecast_tuple = _pick_forecast_at_time(
                times,
                precip,
                imerg_time,
                CONFIG.forecast_snapshot_hours,
                tz_name=snapshot_tz_name,
                utc_offset_seconds=snapshot_utc_offset,
                lat=lat,
                lon=lon,
            )
            _, delta_h, _, _ = forecast_tuple
            if delta_h is None:
                continue
            if best_delta_any is None or delta_h < best_delta_any:
                best_delta_any = delta_h
                best_tuple = forecast_tuple
                chosen_snapshot = candidate
                chosen_probs = probs

        if chosen_snapshot is not None:
            matched_runs += 1

        # Fallback to latest snapshot (legacy mode) only if no run candidate produced a usable match.
        if chosen_snapshot is None:
            snap = db.collection(CONFIG.forecast_snapshot_collection).document(gid).get()
            if not snap.exists:
                if advance_future_only and future_skips_for_doc > 0 and not non_future_candidate_seen:
                    future_only_docs += 1
                    if (
                        newest_terminal_skipped_imerg_time is None
                        or imerg_time > newest_terminal_skipped_imerg_time
                    ):
                        newest_terminal_skipped_imerg_time = imerg_time
                no_snapshot += 1
                continue
            chosen_snapshot = snap.to_dict() or {}
            chosen_is_legacy = True
            legacy_run_time = _coerce_dt(chosen_snapshot.get("run_time"))
            if legacy_run_time is None or legacy_run_time <= future_cutoff:
                non_future_candidate_seen = True
            if (
                not allow_future_runs
                and legacy_run_time is not None
                and legacy_run_time > future_cutoff
            ):
                future_run_skipped += 1
                future_skips_for_doc += 1
                if advance_future_only and future_skips_for_doc > 0 and not non_future_candidate_seen:
                    future_only_docs += 1
                    if (
                        newest_terminal_skipped_imerg_time is None
                        or imerg_time > newest_terminal_skipped_imerg_time
                    ):
                        newest_terminal_skipped_imerg_time = imerg_time
                no_forecast_match += 1
                continue
            lat = data.get("lat") if data.get("lat") is not None else chosen_snapshot.get("lat")
            lon = data.get("lon") if data.get("lon") is not None else chosen_snapshot.get("lon")
            snapshot_tz_name = chosen_snapshot.get("timezone")
            snapshot_utc_offset = chosen_snapshot.get("utc_offset_seconds")
            times, precip, chosen_probs = _extract_snapshot_series(chosen_snapshot)
            if not times or not precip:
                snapshot_schema_miss += 1
            best_tuple = _pick_forecast_at_time(
                times,
                precip,
                imerg_time,
                CONFIG.forecast_snapshot_hours,
                tz_name=snapshot_tz_name,
                utc_offset_seconds=snapshot_utc_offset,
                lat=lat,
                lon=lon,
            )
            used_legacy_snapshots += 1

        snap_data = chosen_snapshot
        forecast, match_delta_h, match_idx, matched_valid_time = best_tuple
        if forecast is None:
            no_forecast_match += 1
            continue
        if match_delta_h is not None and match_delta_h > CONFIG.imerg_match_max_hours:
            stale_match += 1
            stale_min_delta_h = match_delta_h if stale_min_delta_h is None else min(stale_min_delta_h, match_delta_h)
            stale_max_delta_h = match_delta_h if stale_max_delta_h is None else max(stale_max_delta_h, match_delta_h)
            # Avoid bias drift from temporally stale IMERG/forecast pairings.
            continue

        lat = data.get("lat") if data.get("lat") is not None else snap_data.get("lat")
        lon = data.get("lon") if data.get("lon") is not None else snap_data.get("lon")

        forecast_prob = None
        if match_idx is not None and match_idx < len(chosen_probs):
            raw_prob = chosen_probs[match_idx]
            try:
                prob_val = float(raw_prob) if raw_prob is not None else None
            except Exception:
                prob_val = None
            if prob_val is not None:
                forecast_prob = prob_val if prob_val <= 1.0 else (prob_val / 100.0)

        run_time = _coerce_dt(snap_data.get("run_time"))
        lead_hour = None
        if run_time is not None and matched_valid_time is not None:
            lead_hour = max(0.0, (matched_valid_time - run_time).total_seconds() / 3600.0)

        # Skip tiny values to avoid noise
        if observed < 0.1 and forecast < 0.1:
            skipped_tiny_pair += 1
            continue

        if newest_updated_imerg_time is None or imerg_time > newest_updated_imerg_time:
            newest_updated_imerg_time = imerg_time

        regime = classify_precip_regime(
            precip_mm=max(observed, forecast),
            prob_pct=(forecast_prob * 100.0) if forecast_prob is not None else (100.0 if max(observed, forecast) >= 0.2 else 0.0),
            month=imerg_time.month,
        )

        checked += 1
        zone_key = get_terrain_zone_key(lat, lon)
        zone_entry = zone_stats.setdefault(zone_key, {"checked": 0, "updated": 0})
        zone_entry["checked"] += 1

        bias_mgr.update(
            gid,
            observed=observed,
            forecast=forecast,
            forecast_prob=forecast_prob,
            when=imerg_time,
            lat=lat,
            lon=lon,
            regime=regime,
            lead_hour=lead_hour,
        )
        updated += 1
        zone_entry["updated"] += 1

    coverage_pct = round((updated / checked) * 100.0, 1) if checked else 0.0
    try:
        flushed_bias_docs = bias_mgr.flush()
    except Exception as err:
        logger.warning("IMERG bias flush failed: %s", err)
        flushed_bias_docs = 0
    sparsest_zones = sorted(zone_stats.items(), key=lambda kv: kv[1].get("updated", 0))[:3]
    sparse_summary = [
        {
            "zone": z,
            "checked": stats.get("checked", 0),
            "updated": stats.get("updated", 0),
        }
        for z, stats in sparsest_zones
    ]

    state_payload: Dict[str, Any] = {
        "updated_at": datetime.now(timezone.utc).isoformat(),
        "total_docs": total_docs,
        "checked": checked,
        "updated": updated,
        "coverage_pct": coverage_pct,
        "flushed_bias_docs": flushed_bias_docs,
        "matched_runs": matched_runs,
        "used_legacy_snapshots": used_legacy_snapshots,
        "no_snapshot": no_snapshot,
        "no_forecast_match": no_forecast_match,
        "stale_match": stale_match,
        "stale_min_delta_h": round(stale_min_delta_h, 2) if stale_min_delta_h is not None else None,
        "stale_max_delta_h": round(stale_max_delta_h, 2) if stale_max_delta_h is not None else None,
        "skipped_by_watermark": skipped_by_watermark,
        "skipped_tiny_pair": skipped_tiny_pair,
        "snapshot_schema_miss": snapshot_schema_miss,
        "future_run_skipped": future_run_skipped,
        "future_only_docs": future_only_docs,
        "advance_watermark_on_scan": advance_watermark_on_scan,
        "advance_future_only": advance_future_only,
        "allow_future_runs": allow_future_runs,
        "zones": zone_stats,
        "sparse_zones": sparse_summary,
    }
    if advance_watermark_on_scan:
        watermark_time = newest_seen_imerg_time
    else:
        watermark_candidates = [
            ts for ts in (newest_updated_imerg_time, newest_terminal_skipped_imerg_time) if ts is not None
        ]
        watermark_time = max(watermark_candidates) if watermark_candidates else None
    if watermark_time is not None:
        state_payload["last_imerg_time"] = watermark_time.isoformat()
    if newest_updated_imerg_time is not None:
        state_payload["last_updated_imerg_time"] = newest_updated_imerg_time.isoformat()
    if newest_terminal_skipped_imerg_time is not None:
        state_payload["last_future_only_imerg_time"] = newest_terminal_skipped_imerg_time.isoformat()

    try:
        db.collection(CONFIG.bias_collection).document(JOB_STATE_DOC).set(state_payload, merge=True)
    except Exception as err:
        logger.debug("IMERG bias state write failed: %s", err)

    logger.info(
        "IMERG bias update complete: updated=%d checked=%d coverage=%.1f%% flushed=%d stale=%d no_snapshot=%d no_match=%d skipped_old=%d tiny=%d schema_miss=%d future_run=%d future_only=%d stale_delta_h=%s..%s matched_runs=%d legacy=%d",
        updated,
        checked,
        coverage_pct,
        flushed_bias_docs,
        stale_match,
        no_snapshot,
        no_forecast_match,
        skipped_by_watermark,
        skipped_tiny_pair,
        snapshot_schema_miss,
        future_run_skipped,
        future_only_docs,
        round(stale_min_delta_h, 2) if stale_min_delta_h is not None else None,
        round(stale_max_delta_h, 2) if stale_max_delta_h is not None else None,
        matched_runs,
        used_legacy_snapshots,
    )
    return updated


if __name__ == "__main__":
    run_imerg_bias_job()
