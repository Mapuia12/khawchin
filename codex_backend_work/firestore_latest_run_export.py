#!/usr/bin/env python3
"""
Export latest Firestore weather docs for translation or analysis.

Modes:
  - latest-run: docs within a time window from newest generated
  - latest-doc: newest N docs by generated
  - latest-tomorrow: newest docs within window that include tomorrow in daily forecast
  - by-ids: export specific doc IDs
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from datetime import datetime, timedelta, timezone
from typing import Any, Dict, Iterable, Iterator, List, Optional, Sequence, Tuple

try:
    from zoneinfo import ZoneInfo
except Exception:
    ZoneInfo = None

import firebase_admin
from firebase_admin import credentials, firestore

from google.api_core import exceptions as gexc
from google.api_core.retry import Retry

try:
    from google.cloud.firestore_v1 import FieldFilter
except Exception:  # older package versions
    FieldFilter = None

try:
    from google.cloud.firestore_v1 import GeoPoint
except Exception:
    GeoPoint = None

RETRY = Retry(initial=1.0, maximum=20.0, multiplier=2.0, deadline=60.0)
DEFAULT_QUERY_TIMEOUT = float(os.getenv("FIRESTORE_EXPORT_QUERY_TIMEOUT", "120"))
DEFAULT_COUNT_TIMEOUT = float(os.getenv("FIRESTORE_EXPORT_COUNT_TIMEOUT", "20"))
DEFAULT_DOC_BATCH_SIZE = max(1, int(os.getenv("FIRESTORE_EXPORT_DOC_BATCH_SIZE", "40")))


def _parse_dt(value: Any) -> Optional[datetime]:
    if value is None:
        return None
    if isinstance(value, datetime):
        return value if value.tzinfo else value.replace(tzinfo=timezone.utc)
    if hasattr(value, "to_datetime"):
        try:
            dt = value.to_datetime()
            return dt if dt.tzinfo else dt.replace(tzinfo=timezone.utc)
        except Exception:
            return None
    if isinstance(value, str):
        try:
            dt = datetime.fromisoformat(value.replace("Z", "+00:00"))
            return dt if dt.tzinfo else dt.replace(tzinfo=timezone.utc)
        except Exception:
            return None
    return None


def _jsonify(value: Any) -> Any:
    if isinstance(value, datetime):
        dt = value if value.tzinfo else value.replace(tzinfo=timezone.utc)
        return dt.astimezone(timezone.utc).isoformat()
    if GeoPoint is not None and isinstance(value, GeoPoint):
        return {"lat": value.latitude, "lon": value.longitude}
    if isinstance(value, dict):
        return {k: _jsonify(v) for k, v in value.items()}
    if isinstance(value, (list, tuple)):
        return [_jsonify(v) for v in value]
    return value


def _init_firestore(cred_path: str) -> firestore.Client:
    if not firebase_admin._apps:
        cred = credentials.Certificate(cred_path)
        firebase_admin.initialize_app(cred)
    return firestore.client()


def _format_dt(dt: Optional[datetime]) -> str:
    return "(missing)" if dt is None else dt.astimezone(timezone.utc).isoformat()


def _chunked(items: Sequence[Any], size: int) -> Iterator[Sequence[Any]]:
    for i in range(0, len(items), size):
        yield items[i:i + size]


def _select_fields(query, field_paths: Optional[Sequence[str]]):
    if not field_paths:
        return query
    try:
        return query.select(list(field_paths))
    except Exception:
        return query


def _safe_get_docs(query, attempts: int = 3, timeout: float = DEFAULT_QUERY_TIMEOUT):
    last_exc = None
    for attempt in range(1, attempts + 1):
        try:
            # Use get() rather than stream() to avoid the stream retry bug
            return list(query.get(retry=RETRY, timeout=timeout))
        except (gexc.ServiceUnavailable, gexc.DeadlineExceeded, gexc.InternalServerError) as exc:
            last_exc = exc
            if attempt < attempts:
                time.sleep(2 * attempt)
                continue
        except Exception as exc:
            last_exc = exc
            break
    raise last_exc  # type: ignore[misc]


def _count_docs(col: firestore.CollectionReference, timeout: float = DEFAULT_COUNT_TIMEOUT) -> Optional[int]:
    try:
        # aggregation count is cheapest when available
        res = col.count().get(retry=RETRY, timeout=timeout)
        if res:
            return int(res[0][0].value)
    except Exception:
        return None


def _load_docs_from_query(query, field_paths: Optional[Sequence[str]] = None) -> List[Tuple[str, Optional[datetime], Dict[str, Any]]]:
    rows: List[Tuple[str, Optional[datetime], Dict[str, Any]]] = []
    for doc in _safe_get_docs(_select_fields(query, field_paths)):
        data = doc.to_dict() or {}
        dt = _parse_dt(data.get("generated"))
        rows.append((doc.id, dt, data))
    return rows


def _safe_get_all(client: firestore.Client, refs: Sequence[Any], batch_size: int, timeout: float = DEFAULT_QUERY_TIMEOUT, attempts: int = 3):
    snaps = []
    for ref_batch in _chunked(list(refs), max(1, batch_size)):
        last_exc = None
        for attempt in range(1, attempts + 1):
            try:
                snaps.extend(list(client.get_all(ref_batch, retry=RETRY, timeout=timeout)))
                last_exc = None
                break
            except (gexc.ServiceUnavailable, gexc.DeadlineExceeded, gexc.InternalServerError) as exc:
                last_exc = exc
                if attempt < attempts:
                    time.sleep(2 * attempt)
                    continue
            except Exception as exc:
                last_exc = exc
                break
        if last_exc is not None:
            raise last_exc
    return snaps


def _load_docs_by_ids(col: firestore.CollectionReference, ids: Iterable[str], batch_size: int = DEFAULT_DOC_BATCH_SIZE) -> List[Tuple[str, Optional[datetime], Dict[str, Any]]]:
    wanted = [doc_id for doc_id in ids if doc_id]
    if not wanted:
        return []
    refs = [col.document(doc_id) for doc_id in wanted]
    rows_by_id: Dict[str, Tuple[str, Optional[datetime], Dict[str, Any]]] = {}
    try:
        for snap in _safe_get_all(col._client, refs, batch_size=batch_size):
            if not snap.exists:
                continue
            data = snap.to_dict() or {}
            dt = _parse_dt(data.get("generated"))
            rows_by_id[snap.id] = (snap.id, dt, data)
    except Exception:
        rows_by_id.clear()

    if rows_by_id:
        return [rows_by_id[doc_id] for doc_id in wanted if doc_id in rows_by_id]

    rows: List[Tuple[str, Optional[datetime], Dict[str, Any]]] = []
    for doc_id in wanted:
        try:
            snap = col.document(doc_id).get(retry=RETRY, timeout=DEFAULT_QUERY_TIMEOUT)
            if not snap.exists:
                continue
            data = snap.to_dict() or {}
            dt = _parse_dt(data.get("generated"))
            rows.append((snap.id, dt, data))
        except Exception as exc:
            print(f"Failed loading document {doc_id}: {exc}")
    return rows


def _window_value_for_generated(newest_raw: Any, window_start: datetime) -> Any:
    if isinstance(newest_raw, datetime):
        return window_start
    if isinstance(newest_raw, str):
        value = window_start.astimezone(timezone.utc).isoformat()
        if newest_raw.endswith("Z"):
            return value.replace("+00:00", "Z")
        return value
    return window_start


def _load_latest_docs(col: firestore.CollectionReference, limit: int, field_paths: Optional[Sequence[str]] = None) -> List[Tuple[str, Optional[datetime], Dict[str, Any]]]:
    query = col.order_by("generated", direction=firestore.Query.DESCENDING).limit(limit)
    return _load_docs_from_query(query, field_paths=field_paths)


def _load_latest_run_doc_metadata(col: firestore.CollectionReference, window_minutes: int, field_paths: Optional[Sequence[str]] = None) -> Tuple[List[Tuple[str, Optional[datetime], Dict[str, Any]]], Optional[datetime]]:
    newest = _load_latest_docs(col, 1, field_paths=field_paths or ["generated"])
    if not newest or newest[0][1] is None:
        return ([], None)

    newest_dt = newest[0][1]
    window_start = newest_dt - timedelta(minutes=window_minutes)
    newest_raw = newest[0][2].get("generated")
    window_value = _window_value_for_generated(newest_raw, window_start)

    if FieldFilter is not None:
        query = (
            col.where(filter=FieldFilter("generated", ">=", window_value))
               .order_by("generated", direction=firestore.Query.DESCENDING)
        )
    else:
        query = col.where("generated", ">=", window_value).order_by("generated", direction=firestore.Query.DESCENDING)

    return (_load_docs_from_query(query, field_paths=field_paths), newest_dt)


def _hydrate_docs(col: firestore.CollectionReference, rows: List[Tuple[str, Optional[datetime], Dict[str, Any]]], batch_size: int) -> List[Tuple[str, Optional[datetime], Dict[str, Any]]]:
    if not rows:
        return []
    full_rows = _load_docs_by_ids(col, [doc_id for doc_id, _, _ in rows], batch_size=batch_size)
    if len(full_rows) != len(rows):
        found_ids = {doc_id for doc_id, _, _ in full_rows}
        missing = [doc_id for doc_id, _, _ in rows if doc_id not in found_ids]
        preview = ", ".join(missing[:10])
        suffix = "..." if len(missing) > 10 else ""
        raise RuntimeError(
            f"Failed to load {len(missing)} full document(s) after metadata selection: {preview}{suffix}"
        )
    return full_rows


def _tomorrow_date_str(tz_name: str, override: Optional[str]) -> str:
    if override:
        return override
    tzinfo = timezone.utc
    if ZoneInfo is not None:
        try:
            tzinfo = ZoneInfo(tz_name)
        except Exception:
            tzinfo = timezone.utc
    now = datetime.now(tzinfo)
    return (now.date() + timedelta(days=1)).isoformat()


def _tomorrow_dates(tz_names: Iterable[str], override: Optional[str]) -> List[str]:
    if override:
        return [item for item in (s.strip() for s in override.split(",")) if item]
    return [_tomorrow_date_str(tz_name, None) for tz_name in tz_names]


def _doc_has_date(data: Dict[str, Any], date_str: str) -> bool:
    daily = data.get("daily") or {}
    days = daily.get("time") or []
    for item in days:
        if isinstance(item, datetime):
            if item.date().isoformat() == date_str:
                return True
            continue
        if isinstance(item, str) and item[:10] == date_str:
            return True
    return False


def _doc_has_any_date(data: Dict[str, Any], date_strs: Iterable[str]) -> bool:
    return any(_doc_has_date(data, ds) for ds in date_strs)


def main() -> int:
    parser = argparse.ArgumentParser(description="Export latest Firestore docs from weather collection")
    parser.add_argument("--collection", default=os.getenv("WEATHER_COLLECTION", "weather_v69_grid"))
    parser.add_argument("--mode", choices=["latest-run", "latest-doc", "latest-tomorrow", "by-ids"], default="latest-run")
    parser.add_argument("--window-minutes", type=int, default=30, help="Window for latest-run mode")
    parser.add_argument("--limit", type=int, default=1, help="Number of docs for latest-doc mode")
    parser.add_argument("--ids", help="Comma-separated doc IDs for by-ids mode")
    parser.add_argument("--out", help="Output JSON file path")
    parser.add_argument("--timezone", default="Asia/Kolkata", help="Timezone for tomorrow date (latest-tomorrow)")
    parser.add_argument("--timezones", help="Comma-separated timezones for latest-tomorrow")
    parser.add_argument("--tomorrow-date", help="Override tomorrow date (YYYY-MM-DD or comma-separated)")
    parser.add_argument("--doc-batch-size", type=int, default=DEFAULT_DOC_BATCH_SIZE, help="Batch size when loading full docs by ID")
    parser.add_argument("--skip-total-count", action="store_true", help="Skip Firestore aggregation count to reduce load")
    args = parser.parse_args()

    cred_path = os.getenv("GOOGLE_APPLICATION_CREDENTIALS", "serviceAccountKey.json")
    if not os.path.exists(cred_path):
        print(f"ERROR: credentials file not found: {cred_path}")
        print("Set GOOGLE_APPLICATION_CREDENTIALS or place serviceAccountKey.json in this folder.")
        return 2

    try:
        db = _init_firestore(cred_path)
        col = db.collection(args.collection)

        total = None if args.skip_total_count else _count_docs(col)
        newest_dt: Optional[datetime] = None

        if args.mode == "latest-doc":
            selected = _load_latest_docs(col, max(1, args.limit))
            newest_dt = selected[0][1] if selected and selected[0][1] is not None else None

        elif args.mode == "latest-tomorrow":
            window = max(1, args.window_minutes)
            selected, newest_dt = _load_latest_run_doc_metadata(col, window, field_paths=["generated", "daily.time"])
            if args.timezones:
                tz_names = [item for item in (s.strip() for s in args.timezones.split(",")) if item]
            else:
                tz_names = [args.timezone]
            tomorrow_dates = _tomorrow_dates(tz_names, args.tomorrow_date)
            selected = [r for r in selected if _doc_has_any_date(r[2], tomorrow_dates)]
            selected = _hydrate_docs(col, selected, batch_size=max(1, args.doc_batch_size))

        elif args.mode == "by-ids":
            if not args.ids:
                print("ERROR: --ids is required for by-ids mode")
                return 2
            wanted = [s.strip() for s in args.ids.split(",") if s.strip()]
            selected = _load_docs_by_ids(col, wanted, batch_size=max(1, args.doc_batch_size))

        else:
            window = max(1, args.window_minutes)
            selected, newest_dt = _load_latest_run_doc_metadata(col, window, field_paths=["generated"])
            selected = _hydrate_docs(col, selected, batch_size=max(1, args.doc_batch_size))

        if not selected:
            print("No documents matched selection.")
            return 1

        out_path = args.out or (
            "latest_doc_export.json" if args.mode == "latest-doc"
            else "selected_docs_export.json" if args.mode == "by-ids"
            else "latest_run_export.json"
        )

        docs_out = [
            {"id": doc_id, "generated": _format_dt(dt), "data": _jsonify(data)}
            for doc_id, dt, data in selected
        ]
        generated_vals = [r[1] for r in selected if r[1] is not None]
        newest_sel = max(generated_vals) if generated_vals else None
        oldest_sel = min(generated_vals) if generated_vals else None

        payload: Dict[str, Any] = {
            "collection": args.collection,
            "mode": args.mode,
            "total_docs": total,
            "selected_docs": len(docs_out),
            "newest_generated": _format_dt(newest_sel),
            "oldest_generated": _format_dt(oldest_sel),
            "docs": docs_out,
        }
        if args.mode in ("latest-run", "latest-tomorrow"):
            payload["window_minutes"] = max(1, args.window_minutes)
            payload["run_newest_generated"] = _format_dt(newest_dt)
        if args.mode == "latest-tomorrow":
            if args.timezones:
                tz_names = [item for item in (s.strip() for s in args.timezones.split(",")) if item]
            else:
                tz_names = [args.timezone]
            payload["tomorrow_dates"] = _tomorrow_dates(tz_names, args.tomorrow_date)

        with open(out_path, "w", encoding="utf-8") as fh:
            json.dump(payload, fh, indent=2, ensure_ascii=False)

        print(f"Collection: {args.collection}")
        print("Total docs: (unknown)" if total is None else f"Total docs: {total}")
        print(f"Selected docs: {len(docs_out)}")
        print(f"Newest selected: {_format_dt(newest_sel)}")
        print(f"Oldest selected: {_format_dt(oldest_sel)}")
        print(f"Wrote export: {out_path}")
        return 0

    except Exception as exc:
        print(f"ERROR: {exc}")
        return 3


if __name__ == "__main__":
    sys.exit(main())
