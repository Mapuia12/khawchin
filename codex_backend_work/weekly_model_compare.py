#!/usr/bin/env python3
"""
Weekly precipitation verification report.

Compares:
- WRF local archive JSON vs IMERG Late
- Backend final forecast snapshot vs IMERG Late
- Raw per-model ECMWF/ICON forecast snapshots vs IMERG Late, when available

Run on EC2 after IMERG history and forecast snapshots have accumulated:

    python weekly_model_compare.py --days 7 --out-dir /opt/khawchin/reports
"""

from __future__ import annotations

import argparse
import contextlib
from collections import defaultdict
from dataclasses import dataclass, field
from datetime import datetime, timedelta, timezone
import json
import math
import os
from pathlib import Path
import queue
import signal
import threading
import time
from typing import Any, Dict, Iterable, List, Optional, Tuple
from zoneinfo import ZoneInfo

from firebase_admin import firestore
from google.cloud.firestore_v1.base_query import FieldFilter

from backend_v86 import CONFIG, get_terrain_zone_key, init_firestore, logger


UTC = timezone.utc
SOURCE_BACKEND_BLEND = "backend_blend"
SOURCE_WRF_9KM = "wrf_9km"
SOURCE_WRF_3KM = "wrf_3km"


@contextlib.contextmanager
def hard_timeout(seconds: int, label: str):
    """Bound Firestore/gRPC calls that sometimes ignore stream(timeout=...)."""
    if seconds <= 0 or not hasattr(signal, "SIGALRM") or not hasattr(signal, "setitimer"):
        yield
        return

    def _handler(_signum, _frame):
        raise TimeoutError(f"{label} exceeded {seconds}s")

    previous = signal.getsignal(signal.SIGALRM)
    signal.signal(signal.SIGALRM, _handler)
    signal.setitimer(signal.ITIMER_REAL, float(seconds))
    try:
        yield
    finally:
        signal.setitimer(signal.ITIMER_REAL, 0.0)
        signal.signal(signal.SIGALRM, previous)


def parse_dt(value: Any) -> Optional[datetime]:
    if value is None:
        return None
    if isinstance(value, datetime):
        return value.astimezone(UTC) if value.tzinfo else value.replace(tzinfo=UTC)
    if not isinstance(value, str):
        return None
    try:
        text = value.replace("Z", "+00:00")
        dt = datetime.fromisoformat(text)
        return dt.astimezone(UTC) if dt.tzinfo else dt.replace(tzinfo=UTC)
    except Exception:
        return None


def infer_tz_name(lat: Optional[float], lon: Optional[float]) -> str:
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


def forecast_time_to_utc(
    value: Any,
    tz_name: Optional[str],
    utc_offset_seconds: Optional[Any],
    lat: Optional[float],
    lon: Optional[float],
) -> Optional[datetime]:
    if value is None:
        return None
    if isinstance(value, datetime):
        return value.astimezone(UTC) if value.tzinfo else value.replace(tzinfo=UTC)
    if not isinstance(value, str):
        return None
    try:
        raw = value.replace("Z", "+00:00")
        dt = datetime.fromisoformat(raw)
    except Exception:
        return None
    if dt.tzinfo is not None:
        return dt.astimezone(UTC)

    tzinfo = None
    if tz_name:
        try:
            tzinfo = ZoneInfo(str(tz_name))
        except Exception:
            tzinfo = None
    if tzinfo is None and utc_offset_seconds is not None:
        try:
            tzinfo = timezone(timedelta(seconds=int(float(utc_offset_seconds))))
        except Exception:
            tzinfo = None
    if tzinfo is None:
        try:
            tzinfo = ZoneInfo(infer_tz_name(lat, lon))
        except Exception:
            tzinfo = UTC
    return dt.replace(tzinfo=tzinfo).astimezone(UTC)


def safe_float(value: Any) -> Optional[float]:
    try:
        out = float(value)
    except Exception:
        return None
    if math.isnan(out) or math.isinf(out):
        return None
    return out


def lead_bucket(lead_hour: Optional[float]) -> str:
    if lead_hour is None:
        return "unknown"
    if lead_hour < 0:
        return "future_run"
    if lead_hour < 6:
        return "00_06h"
    if lead_hour < 12:
        return "06_12h"
    if lead_hour < 24:
        return "12_24h"
    if lead_hour < 48:
        return "24_48h"
    return "48h_plus"


@dataclass
class MetricAgg:
    heavy_threshold: float
    n: int = 0
    obs_sum: float = 0.0
    fcst_sum: float = 0.0
    abs_sum: float = 0.0
    sq_sum: float = 0.0
    bias_sum: float = 0.0
    hits: int = 0
    misses: int = 0
    false_alarms: int = 0
    correct_negatives: int = 0
    heavy_hits: int = 0
    heavy_misses: int = 0
    heavy_false_alarms: int = 0
    heavy_correct_negatives: int = 0

    def add(self, forecast: float, observed: float) -> None:
        self.n += 1
        self.obs_sum += observed
        self.fcst_sum += forecast
        diff = forecast - observed
        self.bias_sum += diff
        self.abs_sum += abs(diff)
        self.sq_sum += diff * diff

        obs_event = observed >= 0.1
        fcst_event = forecast >= 0.1
        if obs_event and fcst_event:
            self.hits += 1
        elif obs_event and not fcst_event:
            self.misses += 1
        elif not obs_event and fcst_event:
            self.false_alarms += 1
        else:
            self.correct_negatives += 1

        obs_heavy = observed >= self.heavy_threshold
        fcst_heavy = forecast >= self.heavy_threshold
        if obs_heavy and fcst_heavy:
            self.heavy_hits += 1
        elif obs_heavy and not fcst_heavy:
            self.heavy_misses += 1
        elif not obs_heavy and fcst_heavy:
            self.heavy_false_alarms += 1
        else:
            self.heavy_correct_negatives += 1

    def summary(self) -> Dict[str, Any]:
        if self.n <= 0:
            return {"samples": 0}
        event_den = self.hits + self.misses
        false_alarm_den = self.false_alarms + self.correct_negatives
        precision_den = self.hits + self.false_alarms
        csi_den = self.hits + self.misses + self.false_alarms
        heavy_event_den = self.heavy_hits + self.heavy_misses
        heavy_fa_den = self.heavy_false_alarms + self.heavy_correct_negatives
        heavy_csi_den = self.heavy_hits + self.heavy_misses + self.heavy_false_alarms
        return {
            "samples": self.n,
            "obs_mean_mm_hr": round(self.obs_sum / self.n, 3),
            "forecast_mean_mm_hr": round(self.fcst_sum / self.n, 3),
            "bias_mm_hr": round(self.bias_sum / self.n, 3),
            "mae_mm_hr": round(self.abs_sum / self.n, 3),
            "rmse_mm_hr": round(math.sqrt(self.sq_sum / self.n), 3),
            "hit_rate": round(self.hits / event_den, 3) if event_den else None,
            "false_alarm_rate": round(self.false_alarms / false_alarm_den, 3) if false_alarm_den else None,
            "precision": round(self.hits / precision_den, 3) if precision_den else None,
            "csi": round(self.hits / csi_den, 3) if csi_den else None,
            "heavy_hit_rate": round(self.heavy_hits / heavy_event_den, 3) if heavy_event_den else None,
            "heavy_false_alarm_rate": round(self.heavy_false_alarms / heavy_fa_den, 3) if heavy_fa_den else None,
            "heavy_csi": round(self.heavy_hits / heavy_csi_den, 3) if heavy_csi_den else None,
            "events": {
                "hits": self.hits,
                "misses": self.misses,
                "false_alarms": self.false_alarms,
                "correct_negatives": self.correct_negatives,
                "heavy_hits": self.heavy_hits,
                "heavy_misses": self.heavy_misses,
                "heavy_false_alarms": self.heavy_false_alarms,
                "heavy_correct_negatives": self.heavy_correct_negatives,
            },
        }


@dataclass
class ReportAgg:
    heavy_threshold: float
    overall: Dict[str, MetricAgg] = field(default_factory=dict)
    by_zone: Dict[str, Dict[str, MetricAgg]] = field(default_factory=lambda: defaultdict(dict))
    by_lead: Dict[str, Dict[str, MetricAgg]] = field(default_factory=lambda: defaultdict(dict))

    def _agg(self, bucket: Dict[str, MetricAgg], source: str) -> MetricAgg:
        if source not in bucket:
            bucket[source] = MetricAgg(self.heavy_threshold)
        return bucket[source]

    def add(self, source: str, forecast: float, observed: float, zone: str, lead_h: Optional[float]) -> None:
        self._agg(self.overall, source).add(forecast, observed)
        self._agg(self.by_zone[zone], source).add(forecast, observed)
        self._agg(self.by_lead[lead_bucket(lead_h)], source).add(forecast, observed)

    def summary(self) -> Dict[str, Any]:
        return {
            "overall": {source: agg.summary() for source, agg in sorted(self.overall.items())},
            "by_zone": {
                zone: {source: agg.summary() for source, agg in sorted(items.items())}
                for zone, items in sorted(self.by_zone.items())
            },
            "by_lead": {
                bucket: {source: agg.summary() for source, agg in sorted(items.items())}
                for bucket, items in sorted(self.by_lead.items())
            },
        }


def observed_imerg_mm_hr(data: Dict[str, Any]) -> Optional[float]:
    value = data.get("precip_rate_mm_hr")
    if value is None and data.get("precip_30min_mm") is not None:
        half_hour = safe_float(data.get("precip_30min_mm"))
        value = half_hour * 2.0 if half_hour is not None else None
    return safe_float(value)


def query_imerg_docs(db, start: datetime, end: datetime) -> List[Dict[str, Any]]:
    col_name = getattr(CONFIG, "imerg_history_collection", "imerg_late_history")
    col = db.collection(col_name)
    start_s = start.isoformat()
    end_s = end.isoformat()
    docs: List[Dict[str, Any]] = []
    try:
        query = (
            col.where(filter=FieldFilter("imerg_time", ">=", start_s))
            .where(filter=FieldFilter("imerg_time", "<", end_s))
        )
        stream = list(query.stream(timeout=30))
    except Exception as err:
        logger.warning("IMERG range query failed; falling back to stream filter: %s", err)
        with hard_timeout(90, "IMERG fallback stream"):
            stream = list(col.stream(timeout=60))

    for snap in stream:
        data = snap.to_dict() or {}
        imerg_time = parse_dt(data.get("imerg_time"))
        if imerg_time is None or imerg_time < start or imerg_time >= end:
            continue
        observed = observed_imerg_mm_hr(data)
        if observed is None:
            continue
        data["_doc_id"] = snap.id
        data["_imerg_time_dt"] = imerg_time
        data["_observed_mm_hr"] = observed
        docs.append(data)
    return docs


def load_snapshot_runs(db, gid: str, run_limit: int, timeout_seconds: int = 20) -> List[Dict[str, Any]]:
    try:
        query = (
            db.collection(CONFIG.forecast_snapshot_collection)
            .document(gid)
            .collection("runs")
            .order_by("run_time", direction=firestore.Query.DESCENDING)
            .limit(run_limit)
        )
        runs = []
        for doc in query.stream(timeout=timeout_seconds):
            data = doc.to_dict() or {}
            data["_doc_id"] = doc.id
            runs.append(data)
        return runs
    except TimeoutError as err:
        logger.warning("Snapshot run load timed out for %s: %s", gid, err)
        return []
    except Exception as err:
        logger.debug("Snapshot run load failed for %s: %s", gid, err)
        return []


def load_snapshot_runs_bounded(gid: str, run_limit: int, timeout_seconds: int) -> Tuple[List[Dict[str, Any]], bool]:
    """Run one Firestore gid query in a daemon thread so a stuck gRPC call cannot freeze the report."""
    result_q: "queue.Queue[Tuple[str, Any]]" = queue.Queue(maxsize=1)

    def _worker():
        try:
            local_db = firestore.client()
            runs = load_snapshot_runs(local_db, gid, run_limit, timeout_seconds=timeout_seconds)
            result_q.put(("ok", runs))
        except Exception as err:
            result_q.put(("error", err))

    thread = threading.Thread(target=_worker, name=f"weekly-compare-{gid}", daemon=True)
    thread.start()
    thread.join(max(1.0, float(timeout_seconds) + 2.0))
    if thread.is_alive():
        logger.warning("Snapshot run load hard-timeout for %s after %ss; skipping gid", gid, timeout_seconds)
        return [], True

    try:
        status, value = result_q.get_nowait()
    except queue.Empty:
        return [], False
    if status == "ok":
        return value or [], False

    logger.debug("Snapshot run load worker failed for %s: %s", gid, value)
    return [], False


def process_gid_compare(
    gid: str,
    docs: List[Dict[str, Any]],
    wrf_payloads_by_source: Dict[str, List[Dict[str, Any]]],
    model_run_limit: int,
    snapshot_timeout_seconds: int,
    match_max_hours: float,
    include_future_runs: bool,
) -> Dict[str, Any]:
    local_db = firestore.client()
    runs = load_snapshot_runs(local_db, gid, model_run_limit, timeout_seconds=snapshot_timeout_seconds)
    model_sources = set()
    for run in runs:
        model_sources.update((run.get("model_precip_mm") or {}).keys())

    events: List[Tuple[str, float, float, str, Optional[float]]] = []
    processed = 0
    for obs_doc in docs:
        processed += 1
        target = obs_doc["_imerg_time_dt"]
        observed = obs_doc["_observed_mm_hr"]
        lat = safe_float(obs_doc.get("lat"))
        lon = safe_float(obs_doc.get("lon"))
        zone = get_terrain_zone_key(lat, lon)

        backend_value, _, backend_run_time = best_snapshot_forecast(
            runs, target, SOURCE_BACKEND_BLEND, match_max_hours, lat, lon
        )
        if backend_value is not None:
            lead_h = None
            if backend_run_time is not None:
                lead_h = (target - backend_run_time).total_seconds() / 3600.0
            if lead_h is not None and lead_h < 0 and not include_future_runs:
                backend_value = None
        if backend_value is not None:
            events.append((SOURCE_BACKEND_BLEND, backend_value, observed, zone, lead_h))

        for model_source in sorted(model_sources):
            model_value, _, model_run_time = best_snapshot_forecast(
                runs, target, model_source, match_max_hours, lat, lon
            )
            if model_value is None:
                continue
            lead_h = None
            if model_run_time is not None:
                lead_h = (target - model_run_time).total_seconds() / 3600.0
            if lead_h is not None and lead_h < 0 and not include_future_runs:
                continue
            events.append((model_source, model_value, observed, zone, lead_h))

        for wrf_source, wrf_payloads in sorted((wrf_payloads_by_source or {}).items()):
            wrf_value, _, wrf_run_time = best_wrf_forecast(
                wrf_payloads, gid, target, match_max_hours
            )
            if wrf_value is not None:
                lead_h = None
                if wrf_run_time is not None:
                    lead_h = (target - wrf_run_time).total_seconds() / 3600.0
                if lead_h is not None and lead_h < 0 and not include_future_runs:
                    wrf_value = None
            if wrf_value is not None:
                events.append((wrf_source, wrf_value, observed, zone, lead_h))

    return {
        "processed_samples": processed,
        "events": events,
        "model_sources": sorted(model_sources),
        "runs_count": len(runs),
    }


def process_gid_compare_bounded(
    gid: str,
    docs: List[Dict[str, Any]],
    wrf_payloads_by_source: Dict[str, List[Dict[str, Any]]],
    model_run_limit: int,
    snapshot_timeout_seconds: int,
    match_max_hours: float,
    gid_timeout_seconds: int,
    include_future_runs: bool,
) -> Tuple[Optional[Dict[str, Any]], bool]:
    result_q: "queue.Queue[Tuple[str, Any]]" = queue.Queue(maxsize=1)

    def _worker():
        try:
            result_q.put((
                "ok",
                process_gid_compare(
                    gid,
                    docs,
                    wrf_payloads_by_source,
                    model_run_limit,
                    snapshot_timeout_seconds,
                    match_max_hours,
                    include_future_runs,
                ),
            ))
        except Exception as err:
            result_q.put(("error", err))

    thread = threading.Thread(target=_worker, name=f"weekly-compare-process-{gid}", daemon=True)
    thread.start()
    thread.join(max(1.0, float(gid_timeout_seconds)))
    if thread.is_alive():
        logger.warning("Weekly compare hard-timeout for gid=%s after %ss; skipping gid", gid, gid_timeout_seconds)
        return None, True

    try:
        status, value = result_q.get_nowait()
    except queue.Empty:
        return None, False
    if status == "ok":
        return value, False

    logger.debug("Weekly compare gid worker failed for %s: %s", gid, value)
    return None, False


def resolve_report_out_dir(requested: str) -> Path:
    """Return a writable report directory, falling back instead of crashing."""
    candidates = [
        Path(requested),
        Path.home() / "khawchin_reports",
        Path("/tmp/khawchin_reports"),
    ]
    last_error: Optional[Exception] = None
    for out_dir in candidates:
        try:
            out_dir.mkdir(parents=True, exist_ok=True)
            probe = out_dir / ".write_test"
            probe.write_text("ok\n", encoding="utf-8")
            probe.unlink(missing_ok=True)
            if str(out_dir) != requested:
                logger.warning("Report output dir not writable; using fallback: %s", out_dir)
            return out_dir
        except Exception as err:
            last_error = err
            logger.warning("Report output dir unavailable (%s): %s", out_dir, err)
    raise RuntimeError(f"No writable report output directory found: {last_error}")


def pick_forecast_value(
    times: List[Any],
    values: List[Any],
    target: datetime,
    max_delta_hours: float,
    tz_name: Optional[str],
    utc_offset_seconds: Optional[Any],
    lat: Optional[float],
    lon: Optional[float],
) -> Tuple[Optional[float], Optional[float], Optional[datetime]]:
    best_value = None
    best_delta = None
    best_valid = None
    for idx, raw_time in enumerate(times or []):
        if idx >= len(values):
            break
        valid_time = forecast_time_to_utc(raw_time, tz_name, utc_offset_seconds, lat, lon)
        if valid_time is None:
            continue
        delta_h = abs((valid_time - target).total_seconds()) / 3600.0
        if delta_h > max_delta_hours:
            continue
        value = safe_float(values[idx])
        if value is None:
            continue
        if best_delta is None or delta_h < best_delta:
            best_value = value
            best_delta = delta_h
            best_valid = valid_time
    return best_value, best_delta, best_valid


def best_snapshot_forecast(
    runs: Iterable[Dict[str, Any]],
    target: datetime,
    source: str,
    max_delta_hours: float,
    lat: Optional[float],
    lon: Optional[float],
) -> Tuple[Optional[float], Optional[float], Optional[datetime]]:
    best_value = None
    best_delta = None
    best_run_time = None
    future_cutoff = target + timedelta(minutes=5)

    for run in runs:
        run_time = parse_dt(run.get("run_time"))
        if run_time is not None and run_time > future_cutoff:
            continue

        if source == SOURCE_BACKEND_BLEND:
            values = run.get("precip_mm") or []
        else:
            model_precip = run.get("model_precip_mm") or {}
            values = model_precip.get(source) or []
        if not values:
            continue

        value, delta_h, valid_time = pick_forecast_value(
            run.get("times") or [],
            values,
            target,
            max_delta_hours,
            run.get("timezone"),
            run.get("utc_offset_seconds"),
            lat,
            lon,
        )
        if value is None or delta_h is None:
            continue
        if best_delta is None or delta_h < best_delta:
            best_value = value
            best_delta = delta_h
            best_run_time = run_time
            if valid_time is not None and run_time is not None:
                # Keep the closest valid-time match, but report lead from the chosen run.
                best_run_time = run_time

    if best_value is None:
        return None, None, None
    return best_value, best_delta, best_run_time


def load_wrf_archives(archive_dir: Path, start: datetime, end: datetime) -> List[Dict[str, Any]]:
    if not archive_dir.exists():
        logger.warning("WRF archive directory does not exist: %s", archive_dir)
        return []
    payloads = []
    for path in sorted(archive_dir.glob("wrf_local_*.json")):
        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except Exception as err:
            logger.warning("Skipping unreadable WRF archive %s: %s", path, err)
            continue
        run_time = parse_dt(data.get("run_time_utc"))
        forecast_start = parse_dt(data.get("forecast_start_utc") or data.get("run_time_utc"))
        forecast_end = parse_dt(data.get("forecast_end_utc"))
        if forecast_end is not None and forecast_end < start:
            continue
        if forecast_start is not None and forecast_start > end:
            continue
        if run_time is not None and run_time > end:
            continue
        data["_path"] = str(path)
        data["_run_time_dt"] = run_time
        payloads.append(data)
    return payloads


def best_wrf_forecast(
    wrf_payloads: Iterable[Dict[str, Any]],
    gid: str,
    target: datetime,
    max_delta_hours: float,
) -> Tuple[Optional[float], Optional[float], Optional[datetime]]:
    best_value = None
    best_delta = None
    best_run_time = None
    future_cutoff = target + timedelta(minutes=5)

    for payload in wrf_payloads:
        run_time = payload.get("_run_time_dt") or parse_dt(payload.get("run_time_utc"))
        if run_time is not None and run_time > future_cutoff:
            continue
        cell = (payload.get("grid") or {}).get(gid)
        if not isinstance(cell, dict):
            continue
        value, delta_h, _ = pick_forecast_value(
            cell.get("times") or [],
            cell.get("precip_mm") or [],
            target,
            max_delta_hours,
            None,
            None,
            cell.get("lat"),
            cell.get("lon"),
        )
        if value is None or delta_h is None:
            continue
        if best_delta is None or delta_h < best_delta:
            best_value = value
            best_delta = delta_h
            best_run_time = run_time
    if best_value is None:
        return None, None, None
    return best_value, best_delta, best_run_time


def render_markdown(report: Dict[str, Any]) -> str:
    lines = [
        "# Weekly WRF / Model vs IMERG Report",
        "",
        f"Generated UTC: `{report['generated_utc']}`",
        f"Period UTC: `{report['period_start_utc']}` to `{report['period_end_utc']}`",
        f"IMERG samples scanned: `{report['imerg_samples']}`",
        f"WRF archive files: `9km={report.get('wrf_archive_files', 0)}`, `3km={report.get('wrf_3km_archive_files', 0)}`",
        f"Heavy threshold: `{report['heavy_threshold_mm_hr']} mm/hr`",
        "",
        "## Overall",
        "",
        "| Source | Samples | Bias | MAE | RMSE | Hit | False Alarm | CSI | Heavy Hit | Heavy CSI |",
        "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|",
    ]
    for source, item in sorted((report.get("overall") or {}).items()):
        lines.append(
            "| {source} | {samples} | {bias} | {mae} | {rmse} | {hit} | {fa} | {csi} | {hh} | {hcsi} |".format(
                source=source,
                samples=item.get("samples", 0),
                bias=item.get("bias_mm_hr"),
                mae=item.get("mae_mm_hr"),
                rmse=item.get("rmse_mm_hr"),
                hit=item.get("hit_rate"),
                fa=item.get("false_alarm_rate"),
                csi=item.get("csi"),
                hh=item.get("heavy_hit_rate"),
                hcsi=item.get("heavy_csi"),
            )
        )

    lines.extend(["", "## Lead Buckets", ""])
    for bucket, items in sorted((report.get("by_lead") or {}).items()):
        lines.append(f"### {bucket}")
        lines.append("")
        lines.append("| Source | Samples | Bias | MAE | RMSE | CSI |")
        lines.append("|---|---:|---:|---:|---:|---:|")
        for source, item in sorted(items.items()):
            lines.append(
                f"| {source} | {item.get('samples', 0)} | {item.get('bias_mm_hr')} | "
                f"{item.get('mae_mm_hr')} | {item.get('rmse_mm_hr')} | {item.get('csi')} |"
            )
        lines.append("")

    lines.extend(["## Terrain Zones", ""])
    for zone, items in sorted((report.get("by_zone") or {}).items()):
        lines.append(f"### {zone}")
        lines.append("")
        lines.append("| Source | Samples | Bias | MAE | RMSE | CSI | Heavy CSI |")
        lines.append("|---|---:|---:|---:|---:|---:|---:|")
        for source, item in sorted(items.items()):
            lines.append(
                f"| {source} | {item.get('samples', 0)} | {item.get('bias_mm_hr')} | "
                f"{item.get('mae_mm_hr')} | {item.get('rmse_mm_hr')} | {item.get('csi')} | {item.get('heavy_csi')} |"
            )
        lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description="Weekly WRF/ECMWF/ICON/backend vs IMERG comparison")
    parser.add_argument("--days", type=int, default=7, help="Number of days ending now UTC")
    parser.add_argument("--end-utc", default="", help="Optional ISO end time UTC")
    parser.add_argument("--wrf-archive-dir", default="/opt/khawchin/cache/wrf_archive", help="9km WRF archive directory")
    parser.add_argument("--wrf-3km-archive-dir", default="/opt/khawchin/cache/wrf_archive_3km", help="3km WRF archive directory")
    parser.add_argument("--out-dir", default="/opt/khawchin/reports")
    parser.add_argument("--model-run-limit", type=int, default=32, help="Recent forecast runs to load per grid")
    parser.add_argument("--match-max-hours", type=float, default=1.6, help="Max time delta for forecast/IMERG match")
    parser.add_argument("--heavy-threshold-mm-hr", type=float, default=5.0)
    parser.add_argument("--write-firestore", action="store_true", help="Write report to weekly_model_compare collection")
    parser.add_argument("--progress-every", type=int, default=10, help="Log progress every N grid IDs")
    parser.add_argument("--max-gids", type=int, default=0, help="Optional cap for debugging; 0 means all grid IDs")
    parser.add_argument("--snapshot-timeout-seconds", type=int, default=8, help="Hard timeout per Firestore grid snapshot load")
    parser.add_argument("--gid-timeout-seconds", type=int, default=20, help="Hard timeout for one full grid ID compare step")
    parser.add_argument("--max-runtime-seconds", type=int, default=900, help="Stop gracefully after this many seconds; 0 disables")
    parser.add_argument("--include-future-runs", action="store_true", help="Include forecasts whose run_time is after the IMERG target time")
    args = parser.parse_args()

    end = parse_dt(args.end_utc) if args.end_utc else datetime.now(UTC)
    if end is None:
        raise SystemExit("--end-utc could not be parsed")
    start = end - timedelta(days=max(1, args.days))

    db = init_firestore()
    if db is None:
        raise SystemExit("Firestore initialization failed")

    imerg_docs = query_imerg_docs(db, start, end)
    by_gid: Dict[str, List[Dict[str, Any]]] = defaultdict(list)
    for doc in imerg_docs:
        gid = str(doc.get("grid_id") or doc.get("gid") or doc.get("_doc_id", "").split("_")[0])
        by_gid[gid].append(doc)

    logger.info("Weekly compare: IMERG docs=%d gids=%d", len(imerg_docs), len(by_gid))
    wrf_9km_payloads = load_wrf_archives(Path(args.wrf_archive_dir), start, end)
    wrf_3km_payloads = load_wrf_archives(Path(args.wrf_3km_archive_dir), start, end)
    wrf_payloads_by_source = {
        SOURCE_WRF_9KM: wrf_9km_payloads,
        SOURCE_WRF_3KM: wrf_3km_payloads,
    }
    logger.info(
        "Weekly compare: WRF archive files 9km=%d 3km=%d",
        len(wrf_9km_payloads),
        len(wrf_3km_payloads),
    )

    agg = ReportAgg(args.heavy_threshold_mm_hr)
    model_sources_seen = set()

    gid_items = sorted(by_gid.items())
    if args.max_gids > 0:
        gid_items = gid_items[:args.max_gids]

    started = time.monotonic()
    processed_samples = 0
    snapshot_slow_or_empty = 0
    gid_timeouts = 0
    progress_every = max(1, args.progress_every)
    logger.info(
        "Weekly compare: processing gids=%d model_run_limit=%d snapshot_timeout=%ss gid_timeout=%ss max_runtime=%ss",
        len(gid_items),
        args.model_run_limit,
        args.snapshot_timeout_seconds,
        args.gid_timeout_seconds,
        args.max_runtime_seconds,
    )

    for idx, (gid, docs) in enumerate(gid_items, start=1):
        if idx == 1 or idx % progress_every == 0:
            logger.info("Weekly compare loading gid=%d/%d id=%s imerg_docs=%d", idx, len(gid_items), gid, len(docs))

        gid_result, gid_timed_out = process_gid_compare_bounded(
            gid,
            docs,
            wrf_payloads_by_source,
            args.model_run_limit,
            args.snapshot_timeout_seconds,
            args.match_max_hours,
            args.gid_timeout_seconds,
            args.include_future_runs,
        )

        if gid_timed_out:
            gid_timeouts += 1
            snapshot_slow_or_empty += 1
            gid_result = None

        if gid_result:
            processed_samples += int(gid_result.get("processed_samples") or 0)
            model_sources_seen.update(gid_result.get("model_sources") or [])
            if int(gid_result.get("runs_count") or 0) == 0:
                snapshot_slow_or_empty += 1
            for source, pred, observed, zone, lead_h in gid_result.get("events") or []:
                agg.add(source, pred, observed, zone, lead_h)

        if idx == 1 or idx % progress_every == 0 or idx == len(gid_items):
            elapsed = time.monotonic() - started
            logger.info(
                "Weekly compare progress: gids=%d/%d samples=%d elapsed=%.1fs slow_empty_loads=%d gid_timeouts=%d",
                idx,
                len(gid_items),
                processed_samples,
                elapsed,
                snapshot_slow_or_empty,
                gid_timeouts,
            )

        if args.max_runtime_seconds > 0 and (time.monotonic() - started) >= args.max_runtime_seconds:
            logger.warning(
                "Weekly compare max runtime reached at gid=%d/%d; writing partial report",
                idx,
                len(gid_items),
            )
            break

    summary = agg.summary()
    report = {
        "generated_utc": datetime.now(UTC).replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "period_start_utc": start.replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "period_end_utc": end.replace(microsecond=0).isoformat().replace("+00:00", "Z"),
        "imerg_samples": len(imerg_docs),
        "grid_ids": len(gid_items),
        "wrf_archive_files": len(wrf_9km_payloads),
        "wrf_3km_archive_files": len(wrf_3km_payloads),
        "match_max_hours": args.match_max_hours,
        "heavy_threshold_mm_hr": args.heavy_threshold_mm_hr,
        "snapshot_slow_or_empty_loads": snapshot_slow_or_empty,
        "gid_timeouts": gid_timeouts,
        "include_future_runs": bool(args.include_future_runs),
        "notes": [
            "IMERG value is treated as mm/hr rate because imerg_late_ingest stores precip_rate_mm_hr.",
            "WRF first output hour often has 0.0 rain from accumulated-difference initialization.",
            "Raw ECMWF/ICON rows appear only for forecast snapshots written after model_precip_mm support was enabled.",
            "Future-run matches are excluded by default because they are not valid forecast verification.",
        ],
        **summary,
    }

    out_dir = resolve_report_out_dir(args.out_dir)
    stamp = end.strftime("%Y%m%d")
    json_path = out_dir / f"weekly_model_compare_{stamp}.json"
    md_path = out_dir / f"weekly_model_compare_{stamp}.md"
    latest_json = out_dir / "weekly_model_compare_latest.json"
    latest_md = out_dir / "weekly_model_compare_latest.md"
    json_text = json.dumps(report, ensure_ascii=True, indent=2, sort_keys=True)
    json_path.write_text(json_text + "\n", encoding="utf-8")
    latest_json.write_text(json_text + "\n", encoding="utf-8")
    md_text = render_markdown(report)
    md_path.write_text(md_text, encoding="utf-8")
    latest_md.write_text(md_text, encoding="utf-8")

    if args.write_firestore:
        doc_id = f"weekly_model_compare_{stamp}"
        db.collection("weekly_model_compare").document(doc_id).set(report, merge=True)
        db.collection("weekly_model_compare").document("latest").set(report, merge=True)

    logger.info("Weekly compare written: %s", json_path)
    logger.info("Weekly compare markdown: %s", md_path)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
