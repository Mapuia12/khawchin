#!/usr/bin/env python3
"""
Ingest real station observations via Meteostat Python library (no API key).
Writes to Firestore collection: station_observations

Usage:
  python meteostat_station_ingest.py

Env:
  SERVICE_ACCOUNT_PATH  (default: ./serviceAccountKey.json)
  STATION_COLLECTION    (default: station_observations)
  LOOKBACK_HOURS        (default: 6)
"""

from __future__ import annotations

import os
import math
import json
import logging
from datetime import datetime, timedelta, timezone
from typing import Optional, Dict, Any, List

import requests
from meteostat import Point, Stations
try:
    from meteostat import Hourly  # Older versions
except Exception:  # pragma: no cover
    from meteostat.hourly import Hourly  # Newer versions

import firebase_admin
from firebase_admin import credentials, firestore


LOG_LEVEL = os.environ.get("LOG_LEVEL", "INFO").upper()
logging.basicConfig(level=getattr(logging, LOG_LEVEL, logging.INFO), format="%(asctime)s | %(levelname)-8s | %(message)s")
logger = logging.getLogger("meteostat_ingest")


def _resolve_service_account_path() -> str:
    env_path = os.environ.get("SERVICE_ACCOUNT_PATH")
    if env_path:
        return env_path
    if os.path.exists("./serviceAccountKey.json"):
        return "./serviceAccountKey.json"
    return "/opt/khawchin/serviceAccountKey.json"


SERVICE_ACCOUNT_PATH = _resolve_service_account_path()
STATION_COLLECTION = os.environ.get("STATION_COLLECTION", "station_observations")
LOOKBACK_HOURS = int(os.environ.get("LOOKBACK_HOURS", "6"))
MAX_OBS_AGE_MINUTES = int(os.environ.get("MAX_OBS_AGE_MINUTES", "90"))
MAX_RADIUS_KM = float(os.environ.get("MAX_RADIUS_KM", "300"))
MAX_STATIONS = int(os.environ.get("MAX_STATIONS", "10"))
OPEN_METEO_ENABLED = os.environ.get("OPEN_METEO_ENABLED", "1") != "0"
OPEN_METEO_URL = os.environ.get("OPEN_METEO_URL", "https://api.open-meteo.com/v1/forecast")

DEFAULT_LOCATIONS = [
    {"name": "Aizawl", "lat": 23.8333, "lon": 92.6167},
    {"name": "Champhai", "lat": 23.4667, "lon": 93.3167},
    {"name": "Lunglei", "lat": 22.8833, "lon": 92.7333},
    {"name": "Saiha", "lat": 22.4833, "lon": 92.9667},
    {"name": "Mamit", "lat": 23.9333, "lon": 92.4833},
    {"name": "Vairengte", "lat": 24.5000, "lon": 92.7500},
    {"name": "Saitual", "lat": 23.9667, "lon": 92.5833},
    {"name": "Khawzawl", "lat": 23.5333, "lon": 93.1833},
    {"name": "Tamu", "lat": 24.2167, "lon": 94.3000},
    {"name": "Kalaymyo", "lat": 23.1889, "lon": 94.0511},
    {"name": "Hakha", "lat": 22.6500, "lon": 93.6167},
    {"name": "Falam", "lat": 22.9167, "lon": 93.6833},
]


def _load_locations() -> List[Dict[str, Any]]:
    raw = os.environ.get("STATION_LOCATIONS_JSON")
    if not raw:
        return DEFAULT_LOCATIONS
    try:
        parsed = json.loads(raw)
        if not isinstance(parsed, list):
            raise ValueError("STATION_LOCATIONS_JSON must be a list")
        out = []
        for item in parsed:
            out.append({
                "name": str(item["name"]),
                "lat": float(item["lat"]),
                "lon": float(item["lon"]),
            })
        return out or DEFAULT_LOCATIONS
    except Exception as err:
        logger.warning("Invalid STATION_LOCATIONS_JSON; using defaults: %s", err)
        return DEFAULT_LOCATIONS


LOCATIONS = _load_locations()


def _init_firestore():
    if not firebase_admin._apps:
        cred = credentials.Certificate(SERVICE_ACCOUNT_PATH)
        firebase_admin.initialize_app(cred)
    return firestore.client()


def _obs_iso_from_index(df) -> Optional[str]:
    try:
        idx = df.index[-1]
    except Exception:
        return None
    try:
        if hasattr(idx, "to_pydatetime"):
            dt = idx.to_pydatetime()
        else:
            dt = idx
        if getattr(dt, "tzinfo", None) is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt.astimezone(timezone.utc).isoformat()
    except Exception:
        return None


def _parse_iso_utc(value: Optional[str]) -> Optional[datetime]:
    if not value:
        return None
    try:
        dt = datetime.fromisoformat(value.replace("Z", "+00:00"))
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt.astimezone(timezone.utc)
    except Exception:
        return None


def _safe_float(value: Any, default: Optional[float] = None) -> Optional[float]:
    try:
        if value is None:
            return default
        result = float(value)
        if math.isnan(result) or math.isinf(result):
            return default
        return result
    except Exception:
        return default


def _age_minutes(observed_dt: Optional[datetime]) -> Optional[float]:
    if observed_dt is None:
        return None
    return max(0.0, (datetime.now(timezone.utc) - observed_dt).total_seconds() / 60.0)


def _station_doc_id(station_id: str) -> str:
    # Keep one document per station so Firestore does not grow duplicate docs every cron run.
    return str(station_id or "unknown").replace("/", "_").replace(" ", "_")


def _station_confidence(source_detail: str, rain_missing: bool, age_min: Optional[float]) -> float:
    if source_detail == "meteostat_station":
        confidence = 0.82
    elif source_detail == "meteostat_point":
        confidence = 0.70
    elif source_detail == "open_meteo_proxy":
        confidence = 0.35
    else:
        confidence = 0.50
    if rain_missing:
        confidence = min(confidence, 0.45)
    if age_min is None:
        confidence *= 0.85
    else:
        age_factor = max(0.45, 1.0 - (age_min / max(1, MAX_OBS_AGE_MINUTES)) * 0.35)
        confidence *= age_factor
    return round(max(0.2, min(confidence, 0.9)), 2)


def _candidate_score(obs: Dict[str, Any], distance_m: Optional[float] = None) -> float:
    observed_dt = _parse_iso_utc(obs.get("_time"))
    age_min = _age_minutes(observed_dt)
    rain_raw = obs.get("prcp")
    if rain_raw is None:
        rain_raw = obs.get("rain")
    rain_missing = rain_raw is None
    source_detail = obs.get("_source_detail") or "meteostat_station"
    score = _station_confidence(source_detail, rain_missing, age_min)
    if distance_m is not None:
        score -= min(0.18, max(0.0, float(distance_m)) / 1000.0 / 600.0)
    return round(score, 3)


def _fetch_latest_hour(lat: float, lon: float) -> Optional[Dict[str, Any]]:
    # Meteostat expects naive UTC datetimes
    now_utc = datetime.now(timezone.utc).replace(minute=0, second=0, microsecond=0)
    now_naive = now_utc.replace(tzinfo=None)

    for delta_hours in range(0, LOOKBACK_HOURS + 1):
        start = now_naive - timedelta(hours=delta_hours)
        end = start
        point = Point(lat, lon)

        try:
            df = Hourly(point, start, end).fetch()
        except Exception as e:
            logger.warning("Meteostat fetch error: %s", e)
            continue

        if df is None or df.empty:
            continue

        row = df.iloc[-1].to_dict()
        row["_time"] = _obs_iso_from_index(df) or start.replace(tzinfo=timezone.utc).isoformat()
        row["_source_detail"] = "meteostat_point"
        row["_station_id"] = f"point_{lat:.4f}_{lon:.4f}"
        return row

    return None


def _parse_open_meteo_time(value: Optional[str]) -> Optional[datetime]:
    if not value:
        return None
    try:
        dt = datetime.fromisoformat(value)
        if dt.tzinfo is None:
            dt = dt.replace(tzinfo=timezone.utc)
        return dt.astimezone(timezone.utc)
    except Exception:
        return None


def _open_meteo_obs(lat: float, lon: float, time_str: str, hourly: Dict[str, Any], idx: int) -> Dict[str, Any]:
    dt = _parse_open_meteo_time(time_str)
    return {
        "temp": hourly.get("temperature_2m", [None])[idx],
        "rhum": hourly.get("relative_humidity_2m", [None])[idx],
        "prcp": hourly.get("precipitation", [None])[idx],
        "wspd": hourly.get("wind_speed_10m", [None])[idx],
        "wpgt": hourly.get("wind_gusts_10m", [None])[idx],
        "pres": hourly.get("pressure_msl", [None])[idx],
        "_time": dt.isoformat() if dt else None,
        "_source_detail": "open_meteo_proxy",
        "_station_id": f"open_meteo_{lat:.4f}_{lon:.4f}",
    }


def _fetch_latest_hour_open_meteo(lat: float, lon: float) -> Optional[Dict[str, Any]]:
    if not OPEN_METEO_ENABLED:
        return None

    params = {
        "latitude": lat,
        "longitude": lon,
        "hourly": "temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,wind_gusts_10m,pressure_msl",
        "timezone": "UTC",
        "past_days": 1,
        "forecast_days": 1,
    }

    try:
        resp = requests.get(OPEN_METEO_URL, params=params, timeout=20)
        resp.raise_for_status()
        payload = resp.json()
    except Exception as e:
        logger.warning("Open-Meteo fetch error: %s", e)
        return None

    hourly = payload.get("hourly") or {}
    times = hourly.get("time") or []
    if not times:
        return None

    now_utc = datetime.now(timezone.utc)
    max_age_minutes = LOOKBACK_HOURS * 60

    for idx in range(len(times) - 1, -1, -1):
        dt = _parse_open_meteo_time(times[idx])
        if dt is None:
            continue
        age_minutes = (now_utc - dt).total_seconds() / 60.0
        if 0 <= age_minutes <= max_age_minutes:
            return _open_meteo_obs(lat, lon, times[idx], hourly, idx)

    return None


def _fetch_latest_hour_for_station(station_id: str) -> Optional[Dict[str, Any]]:
    now_utc = datetime.now(timezone.utc).replace(minute=0, second=0, microsecond=0)
    now_naive = now_utc.replace(tzinfo=None)

    for delta_hours in range(0, LOOKBACK_HOURS + 1):
        start = now_naive - timedelta(hours=delta_hours)
        end = start
        try:
            df = Hourly(station_id, start, end).fetch()
        except Exception as e:
            logger.warning("Meteostat station fetch error (%s): %s", station_id, e)
            continue

        if df is None or df.empty:
            continue

        row = df.iloc[-1].to_dict()
        row["_time"] = _obs_iso_from_index(df) or start.replace(tzinfo=timezone.utc).isoformat()
        row["_source_detail"] = "meteostat_station"
        row["_station_id"] = f"meteostat_{station_id}"
        return row

    return None


def _find_nearby_stations(lat: float, lon: float) -> List[Dict[str, Any]]:
    try:
        stations = Stations().nearby(lat, lon)
        df = stations.fetch()
    except Exception as e:
        logger.warning("Meteostat station search error: %s", e)
        return []

    if df is None or df.empty:
        return []

    results = []
    for idx, row in df.iterrows():
        dist = row.get("distance")
        if dist is not None and dist > MAX_RADIUS_KM * 1000:
            continue
        results.append({
            "id": idx,
            "name": row.get("name"),
            "distance": dist,
        })
    results.sort(key=lambda r: r.get("distance") or 1e12)
    return results[:MAX_STATIONS]


def _upload_observation(db, name: str, lat: float, lon: float, obs: Dict[str, Any]) -> bool:
    rain_raw = obs.get("prcp")
    if rain_raw is None:
        rain_raw = obs.get("rain")
    rain_missing = rain_raw is None
    rain_mm = _safe_float(rain_raw, 0.0)

    observed_time = obs.get("_time")
    observed_dt = _parse_iso_utc(observed_time)
    age_min = _age_minutes(observed_dt)
    if age_min is not None and age_min > MAX_OBS_AGE_MINUTES:
        logger.info(
            "Skipping stale observation for %s (age %.0f min > %d min)",
            name,
            age_min,
            MAX_OBS_AGE_MINUTES,
        )
        return False

    source_detail = obs.get("_source_detail") or "meteostat_station"
    station_id = obs.get("_station_id") or name
    source_name = "open-meteo" if source_detail.startswith("open_meteo") else "meteostat"
    doc = {
        "station_id": station_id,
        "station_name": name,
        "location": firestore.GeoPoint(lat, lon),
        "lat": lat,
        "lon": lon,
        "temperature_c": _safe_float(obs.get("temp")),
        "humidity": _safe_float(obs.get("rhum")),
        "wind_speed": _safe_float(obs.get("wspd")),
        "wind_gust": _safe_float(obs.get("wpgt")),
        "pressure": _safe_float(obs.get("pres")),
        "rain_mm": rain_mm,
        "timestamp": observed_dt or firestore.SERVER_TIMESTAMP,
        "ingested_at": firestore.SERVER_TIMESTAMP,
        "source": source_name,
        "source_detail": source_detail,
        "verification_role": "proxy" if source_detail.startswith("open_meteo") else "independent",
        "bias_learning_allowed": not source_detail.startswith("open_meteo"),
        "observed_time": observed_time,
        "avg_age_min": round(age_min, 1) if age_min is not None else None,
        "confidence": _station_confidence(source_detail, rain_missing, age_min),
        "rain_missing_assumed_zero": rain_missing,
    }
    db.collection(STATION_COLLECTION).document(_station_doc_id(station_id)).set(doc, merge=True)
    return True


def main():
    db = _init_firestore()
    uploaded = 0
    logger.info("Starting station ingest for %d representative locations", len(LOCATIONS))

    for loc in LOCATIONS:
        candidates: List[Dict[str, Any]] = []

        # Prefer true station observations first because they are the most
        # valuable for nowcast correction. Then consider interpolated Meteostat
        # point data and finally Open-Meteo fallback if needed.
        nearby = _find_nearby_stations(loc["lat"], loc["lon"])
        for s in nearby:
            obs = _fetch_latest_hour_for_station(s["id"])
            if not obs:
                continue
            candidates.append({
                "name": f"{loc['name']} (near {s.get('name') or s['id']})",
                "obs": obs,
                "score": _candidate_score(obs, s.get("distance")),
            })

        point_obs = _fetch_latest_hour(loc["lat"], loc["lon"])
        if point_obs:
            candidates.append({
                "name": loc["name"],
                "obs": point_obs,
                "score": _candidate_score(point_obs, 0.0),
            })

        open_meteo_obs = _fetch_latest_hour_open_meteo(loc["lat"], loc["lon"])
        if open_meteo_obs:
            candidates.append({
                "name": loc["name"],
                "obs": open_meteo_obs,
                "score": _candidate_score(open_meteo_obs, 0.0),
            })

        if not candidates:
            logger.info("No data for %s (within %d hours, radius %.0fkm)", loc["name"], LOOKBACK_HOURS, MAX_RADIUS_KM)
            continue

        best = max(candidates, key=lambda item: item.get("score", 0.0))
        station_name = best["name"]
        obs = best["obs"]

        if _upload_observation(db, station_name, loc["lat"], loc["lon"], obs):
            uploaded += 1
            logger.info(
                "Uploaded observation for %s via %s (score=%.2f)",
                station_name,
                obs.get("_source_detail"),
                best.get("score", 0.0),
            )

    logger.info("Done. Uploaded %d documents.", uploaded)


if __name__ == "__main__":
    main()
