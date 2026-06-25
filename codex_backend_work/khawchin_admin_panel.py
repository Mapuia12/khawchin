#!/usr/bin/env python3
"""
Small admin panel for khawchin.me.

Runs on localhost and is intended to sit behind nginx:
  ADMIN_PANEL_ENABLE=1 ADMIN_PASSWORD='strong-password' python3 khawchin_admin_panel.py

Features:
  - edit /opt/khawchin/cache/app/announcements.json
  - edit /opt/khawchin/cache/app/status.json
  - send FCM topic push through Firebase Admin SDK
"""

from __future__ import annotations

import base64
import gzip
import json
import os
import secrets
import shutil
import sys
import time
from collections import deque
from datetime import datetime, timezone
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from typing import Any
from urllib.parse import parse_qs, urlparse


APP_CONTROL_DIR = Path(os.environ.get("APP_CONTROL_DIR", "/opt/khawchin/cache/app"))
FORECAST_CACHE_DIR = Path(os.environ.get("FORECAST_CACHE_DIR", "/opt/khawchin/cache/forecast"))
ANNOUNCEMENTS_JSON_PATH = Path(os.environ.get("APP_ANNOUNCEMENTS_JSON_PATH", str(APP_CONTROL_DIR / "announcements.json")))
STATUS_JSON_PATH = Path(os.environ.get("APP_STATUS_JSON_PATH", str(APP_CONTROL_DIR / "status.json")))
ADMIN_PANEL_ENABLE = os.environ.get("ADMIN_PANEL_ENABLE", "0") == "1"
ADMIN_USER = os.environ.get("ADMIN_USER", "mapuia")
ADMIN_PASSWORD = os.environ.get("ADMIN_PASSWORD", "")


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")


def read_json(path: Path, default: dict[str, Any]) -> dict[str, Any]:
    try:
        if path.exists():
            return json.loads(path.read_text(encoding="utf-8"))
    except Exception:
        pass
    return dict(default)


def write_json(path: Path, payload: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text(text + "\n", encoding="utf-8")
    os.replace(str(tmp), str(path))

    gz_path = Path(str(path) + ".gz")
    gz_tmp = Path(str(gz_path) + ".tmp")
    with gzip.open(gz_tmp, "wt", encoding="utf-8") as fh:
        fh.write(text + "\n")
    os.replace(str(gz_tmp), str(gz_path))


def file_status(path: Path) -> dict[str, Any]:
    try:
        st = path.stat()
        return {
            "exists": True,
            "bytes": st.st_size,
            "modified_utc": datetime.fromtimestamp(st.st_mtime, timezone.utc).isoformat().replace("+00:00", "Z"),
        }
    except FileNotFoundError:
        return {"exists": False, "bytes": 0, "modified_utc": None}


def default_announcement() -> dict[str, Any]:
    return {
        "enabled": False,
        "id": "default_disabled",
        "severity": "info",
        "title_mz": "",
        "body_mz": "",
        "title_en": "",
        "body_en": "",
        "dismissible": True,
    }


def default_status() -> dict[str, Any]:
    return {
        "service_ok": True,
        "maintenance": False,
        "message_mz": "",
        "message_en": "",
        "forecast_source": "json",
        "forecast_url": "https://khawchin.me/forecast/khawchin_forecast.json",
        "current_url": "https://khawchin.me/forecast/khawchin_current.json",
        "admin_updated_at": None,
    }


def firebase_send_fcm(topic: str, title: str, body: str, action_url: str, notification_type: str) -> bool:
    try:
        import firebase_admin
        from firebase_admin import credentials, messaging
    except Exception:
        return False

    try:
        if not firebase_admin._apps:
            cred_path = (
                os.environ.get("SERVICE_ACCOUNT_PATH")
                or os.environ.get("GOOGLE_APPLICATION_CREDENTIALS")
                or "/opt/khawchin/serviceAccountKey.json"
            )
            firebase_admin.initialize_app(credentials.Certificate(cred_path))

        message = messaging.Message(
            topic=topic,
            data={
                "title": title,
                "body": body,
                "type": notification_type,
                "action_url": action_url,
                "source": "khawchin_admin",
            },
        )
        messaging.send(message)
        return True
    except Exception as exc:
        print(f"FCM send failed: {exc}", file=sys.stderr)
        return False


def parse_time(value: Any) -> datetime | None:
    if not value:
        return None
    try:
        text = str(value).replace("Z", "+00:00")
        parsed = datetime.fromisoformat(text)
        return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)
    except Exception:
        return None


def age_hours_from_time(value: Any) -> float | None:
    parsed = parse_time(value)
    if not parsed:
        return None
    return round((datetime.now(timezone.utc) - parsed.astimezone(timezone.utc)).total_seconds() / 3600.0, 1)


def wrf_snapshot_status(path: Path) -> dict[str, Any]:
    info = file_status(path)
    if not info.get("exists"):
        return info
    run_time = None
    keys = (
        "run_time_utc",
        "forecast_start_utc",
        "start_time_utc",
        "generated_utc",
        "generated_at_utc",
        "generated_at",
        "created_utc",
    )
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
        for key in keys:
            if data.get(key):
                run_time = data.get(key)
                break
        info["points"] = data.get("points") or data.get("grid_points") or len(data.get("data", []) or data.get("points_data", []) or [])
        info["profile"] = data.get("profile") or data.get("wrf_profile")
    except Exception:
        pass
    info["run_time_utc"] = run_time
    info["age_hours"] = age_hours_from_time(run_time) if run_time else age_hours_from_time(info.get("modified_utc"))
    return info


def tail_lines(path: Path, lines: int = 80, level: str = "all") -> str:
    if not path.exists():
        return ""
    wanted = str(level or "all").lower()
    picked: deque[str] = deque(maxlen=max(10, min(lines, 300)))
    try:
        with path.open("r", encoding="utf-8", errors="replace") as fh:
            for line in fh:
                low = line.lower()
                if wanted == "errors" and "error" not in low and "fatal" not in low:
                    continue
                if wanted == "warnings" and "warning" not in low and "warn" not in low:
                    continue
                picked.append(line.rstrip("\n"))
    except Exception as exc:
        return f"Could not read {path}: {exc}"
    return "\n".join(picked)


def log_summary(path: Path) -> dict[str, Any]:
    status = file_status(path)
    status.update({"ok": None, "last_start": None, "last_finish": None, "last_exit": None, "warnings": 0, "errors": 0})
    if not status["exists"]:
        return status
    try:
        recent = tail_lines(path, lines=300, level="all").splitlines()
        for line in recent:
            low = line.lower()
            if "starting job" in low or "starting weather update" in low:
                status["last_start"] = line[:19]
            if "job finished with exit code" in low:
                status["last_finish"] = line[:19]
                status["last_exit"] = line.rsplit(" ", 1)[-1].strip("]")
                status["ok"] = "exit code 0" in low
            elif "update complete" in low:
                status["last_finish"] = line[:19]
                if status["ok"] is None:
                    status["ok"] = True
            if "warning" in low or " warn" in low:
                status["warnings"] += 1
            if "error" in low or "fatal" in low:
                status["errors"] += 1
                if status["ok"] is None:
                    status["ok"] = False
    except Exception:
        pass
    return status


def archive_stats(path: Path) -> dict[str, Any]:
    if not path.exists():
        return {"exists": False, "count": 0, "total_mb": 0, "oldest_utc": None, "newest_utc": None}
    files = [p for p in path.glob("*.json") if p.is_file()]
    total = sum(p.stat().st_size for p in files)
    mtimes = [p.stat().st_mtime for p in files]
    return {
        "exists": True,
        "count": len(files),
        "total_mb": round(total / 1024 / 1024, 1),
        "oldest_utc": datetime.fromtimestamp(min(mtimes), timezone.utc).isoformat().replace("+00:00", "Z") if mtimes else None,
        "newest_utc": datetime.fromtimestamp(max(mtimes), timezone.utc).isoformat().replace("+00:00", "Z") if mtimes else None,
    }


def cleanup_archive_files(days: int) -> dict[str, Any]:
    cutoff = time.time() - max(1, min(days, 365)) * 86400
    roots = [
        Path("/opt/khawchin/cache/wrf_archive"),
        Path("/opt/khawchin/cache/wrf_archive_3km"),
        Path("/opt/khawchin/cache/wrf_archive_3km_test"),
    ]
    deleted: list[str] = []
    freed = 0
    for root in roots:
        if not root.exists() or not root.is_dir():
            continue
        for path in root.glob("*.json"):
            if not path.is_file() or path.stat().st_mtime >= cutoff:
                continue
            size = path.stat().st_size
            path.unlink()
            deleted.append(str(path))
            freed += size
    return {"deleted_count": len(deleted), "freed_mb": round(freed / 1024 / 1024, 2), "deleted": deleted[:50]}


def system_status(level: str = "all", log_name: str = "full") -> dict[str, Any]:
    log_dir = Path("/opt/khawchin/logs")
    logs = {
        "full": log_dir / "full.log",
        "current": log_dir / "current.log",
        "imerg_late": log_dir / "imerg_late.log",
        "imerg_bias": log_dir / "imerg_bias.log",
        "meteostat": log_dir / "meteostat.log",
        "weekly_compare": log_dir / "weekly_compare.log",
    }
    selected_log = logs.get(log_name, logs["full"])
    disk_root = Path("/opt/khawchin/cache")
    try:
        disk = shutil.disk_usage(disk_root)
        disk_payload = {
            "used_mb": round(disk.used / 1024 / 1024),
            "free_mb": round(disk.free / 1024 / 1024),
            "total_mb": round(disk.total / 1024 / 1024),
            "used_pct": round((disk.used / disk.total) * 100, 1) if disk.total else None,
        }
    except Exception:
        disk_payload = {"used_mb": None, "free_mb": None, "total_mb": None, "used_pct": None}
    return {
        "generated_utc": now_iso(),
        "wrf_9km": wrf_snapshot_status(Path("/opt/khawchin/cache/wrf_local_latest.json")),
        "wrf_3km": wrf_snapshot_status(Path("/opt/khawchin/cache/wrf_local_3km_latest.json")),
        "disk": disk_payload,
        "archives": {
            "wrf_9km": archive_stats(Path("/opt/khawchin/cache/wrf_archive")),
            "wrf_3km": archive_stats(Path("/opt/khawchin/cache/wrf_archive_3km")),
            "wrf_3km_test": archive_stats(Path("/opt/khawchin/cache/wrf_archive_3km_test")),
        },
        "logs": {name: log_summary(path) for name, path in logs.items()},
        "selected_log": log_name if log_name in logs else "full",
        "log_tail": tail_lines(selected_log, lines=80, level=level),
    }


ADMIN_HTML = r"""<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1" />
  <title>Khawchin Admin</title>
  <style>
    :root{color-scheme:dark;--bg:#07111f;--card:#102039;--muted:#9fb4ce;--text:#edf6ff;--cyan:#2ee6d6;--bad:#ff6b7a;--good:#52e39b}
    *{box-sizing:border-box} body{margin:0;font-family:system-ui,-apple-system,Segoe UI,sans-serif;background:radial-gradient(circle at top left,#163762,#07111f 58%);color:var(--text)}
    header{position:sticky;top:0;z-index:5;padding:24px 16px;background:rgba(7,17,31,.78);backdrop-filter:blur(14px);border-bottom:1px solid rgba(255,255,255,.12)}
    h1{margin:0;font-size:clamp(30px,5vw,46px);letter-spacing:-.04em} header p{margin:6px 0 0;color:var(--muted)}
    main{width:min(1120px,100%);margin:auto;padding:20px 14px 60px}.grid{display:grid;grid-template-columns:repeat(12,1fr);gap:16px}
    .card{grid-column:span 12;background:linear-gradient(180deg,rgba(22,44,75,.95),rgba(11,23,42,.96));border:1px solid rgba(112,184,255,.22);border-radius:24px;padding:18px;box-shadow:0 18px 60px rgba(0,0,0,.25)}
    @media(min-width:860px){.half{grid-column:span 6}.third{grid-column:span 4}.wide{grid-column:span 12}} h2{margin:0 0 12px}
    label{display:block;margin:12px 0 6px;color:var(--muted);font-weight:800;font-size:13px} input,textarea,select{width:100%;border:1px solid rgba(255,255,255,.15);border-radius:14px;padding:12px;background:#071426;color:var(--text);font:inherit}
    input[type=checkbox]{width:auto;transform:scale(1.2);margin-right:8px} textarea{min-height:92px;resize:vertical}.row{display:grid;grid-template-columns:1fr;gap:10px}@media(min-width:680px){.two{grid-template-columns:1fr 1fr}.three{grid-template-columns:1fr 1fr 1fr}}
    button{border:0;border-radius:999px;padding:12px 18px;margin:10px 8px 0 0;background:linear-gradient(135deg,var(--cyan),#9ffcff);color:#06111f;font-weight:900;cursor:pointer}.secondary{background:#263b5c;color:var(--text);border:1px solid rgba(255,255,255,.18)}.danger{background:linear-gradient(135deg,#ff8a8a,#ffd06b)}
    pre{white-space:pre-wrap;background:#06101d;border:1px solid rgba(255,255,255,.1);border-radius:16px;padding:14px;overflow:auto;max-height:460px}.pill{display:inline-flex;padding:7px 10px;border:1px solid rgba(255,255,255,.15);border-radius:999px;margin:4px 4px;color:var(--muted)}.ok{color:var(--good)}.bad{color:var(--bad)}.hint{color:var(--muted);font-size:13px;line-height:1.55}.metric{display:grid;gap:8px;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));margin:8px 0}.metric div{background:#071426;border:1px solid rgba(255,255,255,.1);border-radius:16px;padding:12px}.metric b{display:block;font-size:20px}
  </style>
</head>
<body>
<header><h1>Khawchin Control</h1><p>Announcements, app/server status, and FCM push for khawchin.me.</p></header>
<main>
  <section class="grid">
    <div class="card third"><h2>Files</h2><div id="files">Loading...</div></div>
    <div class="card third"><h2>Announcement</h2><pre id="annPreview">Loading...</pre></div>
    <div class="card third"><h2>Status</h2><pre id="statusPreview">Loading...</pre></div>
  </section>
  <section class="grid">
    <div class="card wide">
      <h2>Operations Dashboard</h2>
      <div class="metric" id="opsMetrics">Loading...</div>
      <div id="cronStatus" class="hint"></div>
      <p>
        <button class="secondary" onclick="loadSystem('all')">Show all log</button>
        <button class="secondary" onclick="loadSystem('warnings')">Warnings only</button>
        <button class="secondary" onclick="loadSystem('errors')">Errors only</button>
        <select id="logName" onchange="loadSystem(currentLogLevel)"><option value="full">full</option><option value="current">current</option><option value="imerg_late">imerg_late</option><option value="imerg_bias">imerg_bias</option><option value="meteostat">meteostat</option><option value="weekly_compare">weekly_compare</option></select>
      </p>
      <pre id="logTail">Loading log...</pre>
      <button class="secondary" onclick="quickAction('maintenance-on')">Maintenance ON</button>
      <button class="secondary" onclick="quickAction('maintenance-off')">Maintenance OFF</button>
      <button class="secondary" onclick="quickAction('clear-announcement')">Clear announcement</button>
      <button class="danger" onclick="cleanupArchives()">Delete WRF archives older than 21 days</button>
    </div>
  </section>
  <section class="grid">
    <form class="card half" id="announcementForm">
      <h2>Publish Announcement</h2>
      <div class="row three"><label><input id="annEnabled" type="checkbox"> Enabled</label><div><label>Severity</label><select id="annSeverity"><option>info</option><option>warning</option><option>success</option><option>critical</option></select></div><div><label>ID</label><input id="annId" value="manual_notice"></div></div>
      <div class="row two"><div><label>Title Mizo</label><input id="annTitleMz"></div><div><label>Title English</label><input id="annTitleEn"></div></div>
      <div class="row two"><div><label>Body Mizo</label><textarea id="annBodyMz"></textarea></div><div><label>Body English</label><textarea id="annBodyEn"></textarea></div></div>
      <div class="row two"><div><label>Action Label Mizo</label><input id="annActionMz" value="Play Store-ah kal"></div><div><label>Action Label English</label><input id="annActionEn" value="Open Play Store"></div></div>
      <label>Action URL</label><input id="annActionUrl" value="https://play.google.com/store/apps/details?id=com.mapuia.khawchinthlirna">
      <div class="row three"><label><input id="annDismissible" type="checkbox" checked> Dismissible</label><div><label>Start at ISO optional</label><input id="annStartAt" placeholder="2026-06-24T08:00:00+06:30"></div><div><label>End at ISO optional</label><input id="annEndAt" placeholder="2026-07-07T23:59:00+06:30"></div></div>
      <button type="submit">Publish banner</button><button type="button" class="secondary" onclick="loadState()">Reload</button><button type="button" class="danger" onclick="disableAnnouncement()">Disable banner</button>
    </form>
    <form class="card half" id="fcmForm">
      <h2>Send FCM Push</h2><p class="hint">Use topic <b>weather_alerts</b> for all app users. Do not send duplicates unless urgent.</p>
      <label>Topic</label><input id="fcmTopic" value="weather_alerts"><label>Title</label><input id="fcmTitle" value="Khawchin Thlirna update"><label>Body</label><textarea id="fcmBody">Forecast thar hmuh nan Play Store atangin app update rawh.</textarea><label>Action URL</label><input id="fcmActionUrl" value="https://play.google.com/store/apps/details?id=com.mapuia.khawchinthlirna"><button type="submit">Send push</button>
    </form>
  </section>
  <section class="grid">
    <form class="card half" id="statusForm">
      <h2>App / Server Status</h2><div class="row two"><label><input id="statusServiceOk" type="checkbox" checked> Service OK</label><label><input id="statusMaintenance" type="checkbox"> Maintenance mode</label></div><label>Message Mizo</label><textarea id="statusMsgMz"></textarea><label>Message English</label><textarea id="statusMsgEn"></textarea><div class="row three"><div><label>Forecast source</label><input id="statusSource" value="json"></div><div><label>Latest version code</label><input id="statusLatestVersion" type="number"></div><div><label>Min supported version</label><input id="statusMinVersion" type="number"></div></div><button type="submit">Save status</button>
    </form>
    <div class="card half"><h2>Quick Update Notice</h2><p class="hint">Publishes the standard non-dismissible Play Store update banner. Tick FCM only once.</p><label><input id="quickFcm" type="checkbox"> Also send FCM to weather_alerts</label><button onclick="quickUpdateNotice()">Publish update notice</button></div>
  </section>
</main>
<script>
const j=id=>document.getElementById(id);
let currentLogLevel='all';
async function api(path,opts={}){const r=await fetch(path,{headers:{'Content-Type':'application/json'},...opts}); if(!r.ok) throw new Error(await r.text()); return await r.json()}
const pretty=x=>JSON.stringify(x,null,2);
const pill=(txt,cls='')=>`<span class="pill ${cls}">${txt}</span>`;
async function loadState(){const s=await api('/admin/api/state');j('annPreview').textContent=pretty(s.announcement);j('statusPreview').textContent=pretty(s.status);j('files').innerHTML=Object.entries(s.files).map(([k,v])=>pill(`${k}: ${v.exists?Math.round(v.bytes/1024)+' KB':'missing'}`,v.exists?'ok':'bad')).join('');const a=s.announcement||{};j('annEnabled').checked=!!a.enabled;j('annSeverity').value=a.severity||'info';j('annId').value=a.id||'manual_notice';j('annTitleMz').value=a.title_mz||'';j('annTitleEn').value=a.title_en||'';j('annBodyMz').value=a.body_mz||'';j('annBodyEn').value=a.body_en||'';j('annActionMz').value=a.action_label_mz||'';j('annActionEn').value=a.action_label_en||'';j('annActionUrl').value=a.action_url||'';j('annDismissible').checked=a.dismissible!==false;j('annStartAt').value=a.start_at||'';j('annEndAt').value=a.end_at||'';const st=s.status||{};j('statusServiceOk').checked=st.service_ok!==false;j('statusMaintenance').checked=!!st.maintenance;j('statusMsgMz').value=st.message_mz||'';j('statusMsgEn').value=st.message_en||'';j('statusSource').value=st.forecast_source||'json';j('statusLatestVersion').value=st.latest_version_code||'';j('statusMinVersion').value=st.min_supported_version_code||''}
async function loadSystem(level='all'){currentLogLevel=level;const log=j('logName').value||'full';const s=await api(`/admin/api/system?level=${encodeURIComponent(level)}&log=${encodeURIComponent(log)}`);const w9=s.wrf_9km||{},w3=s.wrf_3km||{},d=s.disk||{},a=s.archives||{};j('opsMetrics').innerHTML=[`<div><span>WRF 9km age</span><b>${w9.age_hours??'--'}h</b><small>${w9.exists?'ready':'missing'}</small></div>`,`<div><span>WRF 3km age</span><b>${w3.age_hours??'--'}h</b><small>${w3.exists?'ready':'missing'}</small></div>`,`<div><span>Disk used</span><b>${d.used_pct??'--'}%</b><small>${d.free_mb??'--'} MB free</small></div>`,`<div><span>WRF archives</span><b>${(a.wrf_9km?.count||0)+(a.wrf_3km?.count||0)}</b><small>${(a.wrf_9km?.total_mb||0)+(a.wrf_3km?.total_mb||0)} MB</small></div>`].join('');j('cronStatus').innerHTML=Object.entries(s.logs||{}).map(([name,v])=>pill(`${name}: ${v.ok===true?'OK':v.ok===false?'CHECK':'?'} ${v.last_finish||''} warn=${v.warnings} err=${v.errors}`,v.ok===false?'bad':'ok')).join('');j('logTail').textContent=s.log_tail||'(no matching log lines)'}
j('announcementForm').addEventListener('submit',async e=>{e.preventDefault();const p={enabled:j('annEnabled').checked,severity:j('annSeverity').value,id:j('annId').value,title_mz:j('annTitleMz').value,body_mz:j('annBodyMz').value,title_en:j('annTitleEn').value,body_en:j('annBodyEn').value,dismissible:j('annDismissible').checked,action_label_mz:j('annActionMz').value,action_label_en:j('annActionEn').value,action_url:j('annActionUrl').value,start_at:j('annStartAt').value||null,end_at:j('annEndAt').value||null};await api('/admin/api/announcement',{method:'POST',body:JSON.stringify(p)});alert('Announcement saved');loadState();loadSystem(currentLogLevel)});
j('statusForm').addEventListener('submit',async e=>{e.preventDefault();const p={service_ok:j('statusServiceOk').checked,maintenance:j('statusMaintenance').checked,message_mz:j('statusMsgMz').value,message_en:j('statusMsgEn').value,forecast_source:j('statusSource').value,latest_version_code:j('statusLatestVersion').value?Number(j('statusLatestVersion').value):null,min_supported_version_code:j('statusMinVersion').value?Number(j('statusMinVersion').value):null,forecast_url:'https://khawchin.me/forecast/khawchin_forecast.json',current_url:'https://khawchin.me/forecast/khawchin_current.json'};await api('/admin/api/status',{method:'POST',body:JSON.stringify(p)});alert('Status saved');loadState();loadSystem(currentLogLevel)});
j('fcmForm').addEventListener('submit',async e=>{e.preventDefault();if(!confirm('Send FCM push now?'))return;const p={topic:j('fcmTopic').value,title:j('fcmTitle').value,body:j('fcmBody').value,action_url:j('fcmActionUrl').value};const out=await api('/admin/api/fcm',{method:'POST',body:JSON.stringify(p)});alert(out.success?'FCM sent':'FCM failed')});
async function quickAction(name){await api(`/admin/api/quick/${name}`,{method:'POST',body:'{}'});alert('Done');loadState();loadSystem(currentLogLevel)}
async function disableAnnouncement(){await quickAction('clear-announcement')}
async function quickUpdateNotice(){const out=await api('/admin/api/quick/update-notice',{method:'POST',body:JSON.stringify({send_fcm:j('quickFcm').checked})});alert(out.success?'Update notice published':'Failed');loadState();loadSystem(currentLogLevel)}
async function cleanupArchives(){if(!confirm('Delete WRF archive JSON files older than 21 days?'))return;const out=await api('/admin/api/archive/cleanup',{method:'POST',body:JSON.stringify({days:21,confirm:'DELETE_OLD_WRF_ARCHIVES'})});alert(`Deleted ${out.deleted_count} files, freed ${out.freed_mb} MB`);loadSystem(currentLogLevel)}
Promise.all([loadState(),loadSystem()]).catch(e=>alert(e.message));
</script></body></html>"""


class AdminHandler(BaseHTTPRequestHandler):
    server_version = "KhawchinAdmin/1.0"

    def _send(self, status: int, body: bytes, content_type: str = "application/json") -> None:
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(body)

    def _send_json(self, status: int, payload: dict[str, Any]) -> None:
        self._send(status, json.dumps(payload, ensure_ascii=False).encode("utf-8"))

    def _auth_ok(self) -> bool:
        if not ADMIN_PANEL_ENABLE or not ADMIN_PASSWORD:
            self._send_json(HTTPStatus.SERVICE_UNAVAILABLE, {"error": "admin_disabled_or_password_missing"})
            return False
        header = self.headers.get("Authorization", "")
        if not header.startswith("Basic "):
            self.send_response(HTTPStatus.UNAUTHORIZED)
            self.send_header("WWW-Authenticate", 'Basic realm="Khawchin Admin"')
            self.end_headers()
            return False
        try:
            raw = base64.b64decode(header.split(" ", 1)[1]).decode("utf-8")
            username, password = raw.split(":", 1)
        except Exception:
            self.send_response(HTTPStatus.UNAUTHORIZED)
            self.send_header("WWW-Authenticate", 'Basic realm="Khawchin Admin"')
            self.end_headers()
            return False
        if not (secrets.compare_digest(username, ADMIN_USER) and secrets.compare_digest(password, ADMIN_PASSWORD)):
            self.send_response(HTTPStatus.UNAUTHORIZED)
            self.send_header("WWW-Authenticate", 'Basic realm="Khawchin Admin"')
            self.end_headers()
            return False
        return True

    def _body_json(self) -> dict[str, Any]:
        length = min(int(self.headers.get("Content-Length", "0") or 0), 64_000)
        if length <= 0:
            return {}
        return json.loads(self.rfile.read(length).decode("utf-8"))

    def do_HEAD(self) -> None:
        if not self._auth_ok():
            return
        self.send_response(HTTPStatus.OK)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.end_headers()

    def do_GET(self) -> None:
        if not self._auth_ok():
            return
        parsed = urlparse(self.path)
        path = parsed.path
        query = parse_qs(parsed.query)
        if path in ("/admin", "/admin/"):
            self._send(HTTPStatus.OK, ADMIN_HTML.encode("utf-8"), "text/html; charset=utf-8")
            return
        if path == "/admin/api/state":
            self._send_json(HTTPStatus.OK, {
                "announcement": read_json(ANNOUNCEMENTS_JSON_PATH, default_announcement()),
                "status": read_json(STATUS_JSON_PATH, default_status()),
                "files": {
                    "announcement": file_status(ANNOUNCEMENTS_JSON_PATH),
                    "status": file_status(STATUS_JSON_PATH),
                    "forecast": file_status(FORECAST_CACHE_DIR / "khawchin_forecast.json"),
                    "current": file_status(FORECAST_CACHE_DIR / "khawchin_current.json"),
                    "forecast_backup": file_status(FORECAST_CACHE_DIR / "khawchin_forecast_backup.json"),
                    "current_backup": file_status(FORECAST_CACHE_DIR / "khawchin_current_backup.json"),
                },
            })
            return
        if path == "/admin/api/system":
            level = (query.get("level") or ["all"])[0]
            log_name = (query.get("log") or ["full"])[0]
            self._send_json(HTTPStatus.OK, system_status(level=level, log_name=log_name))
            return
        self._send_json(HTTPStatus.NOT_FOUND, {"error": "not_found"})

    def do_POST(self) -> None:
        if not self._auth_ok():
            return
        try:
            body = self._body_json()
            if self.path == "/admin/api/announcement":
                payload = {
                    "enabled": bool(body.get("enabled", True)),
                    "id": str(body.get("id") or "manual_notice")[:80],
                    "severity": str(body.get("severity") or "info")[:20],
                    "title_mz": str(body.get("title_mz") or "")[:160],
                    "body_mz": str(body.get("body_mz") or "")[:1200],
                    "title_en": str(body.get("title_en") or "")[:160],
                    "body_en": str(body.get("body_en") or "")[:1200],
                    "dismissible": bool(body.get("dismissible", True)),
                    "action_label_mz": str(body.get("action_label_mz") or "")[:80],
                    "action_label_en": str(body.get("action_label_en") or "")[:80],
                    "action_url": str(body.get("action_url") or "")[:500],
                    "start_at": body.get("start_at"),
                    "end_at": body.get("end_at"),
                    "min_version_code": body.get("min_version_code"),
                    "max_version_code": body.get("max_version_code"),
                    "admin_updated_at": now_iso(),
                }
                write_json(ANNOUNCEMENTS_JSON_PATH, payload)
                self._send_json(HTTPStatus.OK, {"success": True, "announcement": payload})
                return
            if self.path == "/admin/api/status":
                existing = read_json(STATUS_JSON_PATH, default_status())
                payload = {
                    **existing,
                    "service_ok": bool(body.get("service_ok", True)),
                    "maintenance": bool(body.get("maintenance", False)),
                    "message_mz": str(body.get("message_mz") or "")[:800],
                    "message_en": str(body.get("message_en") or "")[:800],
                    "forecast_source": str(body.get("forecast_source") or "json")[:80],
                    "latest_version_code": body.get("latest_version_code"),
                    "min_supported_version_code": body.get("min_supported_version_code"),
                    "forecast_url": str(body.get("forecast_url") or "https://khawchin.me/forecast/khawchin_forecast.json")[:500],
                    "current_url": str(body.get("current_url") or "https://khawchin.me/forecast/khawchin_current.json")[:500],
                    "admin_updated_at": now_iso(),
                }
                write_json(STATUS_JSON_PATH, payload)
                self._send_json(HTTPStatus.OK, {"success": True, "status": payload})
                return
            if self.path == "/admin/api/fcm":
                success = firebase_send_fcm(
                    topic=str(body.get("topic") or "weather_alerts")[:120],
                    title=str(body.get("title") or "")[:160],
                    body=str(body.get("body") or "")[:1200],
                    action_url=str(body.get("action_url") or "https://play.google.com/store/apps/details?id=com.mapuia.khawchinthlirna")[:500],
                    notification_type=str(body.get("notification_type") or "admin_announcement")[:80],
                )
                self._send_json(HTTPStatus.OK, {"success": bool(success)})
                return
            if self.path == "/admin/api/quick/clear-announcement":
                payload = {**default_announcement(), "admin_updated_at": now_iso()}
                write_json(ANNOUNCEMENTS_JSON_PATH, payload)
                self._send_json(HTTPStatus.OK, {"success": True, "announcement": payload})
                return
            if self.path == "/admin/api/quick/maintenance-on":
                existing = read_json(STATUS_JSON_PATH, default_status())
                existing.update({
                    "maintenance": True,
                    "service_ok": False,
                    "admin_updated_at": now_iso(),
                })
                write_json(STATUS_JSON_PATH, existing)
                self._send_json(HTTPStatus.OK, {"success": True, "status": existing})
                return
            if self.path == "/admin/api/quick/maintenance-off":
                existing = read_json(STATUS_JSON_PATH, default_status())
                existing.update({
                    "maintenance": False,
                    "service_ok": True,
                    "message_mz": "",
                    "message_en": "",
                    "admin_updated_at": now_iso(),
                })
                write_json(STATUS_JSON_PATH, existing)
                self._send_json(HTTPStatus.OK, {"success": True, "status": existing})
                return
            if self.path == "/admin/api/archive/cleanup":
                confirm = str(body.get("confirm") or "")
                days = int(body.get("days") or 21)
                if confirm != "DELETE_OLD_WRF_ARCHIVES":
                    self._send_json(HTTPStatus.BAD_REQUEST, {"error": "confirmation_required"})
                    return
                self._send_json(HTTPStatus.OK, {"success": True, **cleanup_archive_files(days)})
                return
            if self.path == "/admin/api/quick/update-notice":
                announcement = {
                    "enabled": True,
                    "id": "update_required_20260623",
                    "severity": "warning",
                    "title_mz": "App update pawimawh",
                    "body_mz": "Forecast thar leh data chhiar zat tlem zawk hmuh nan Play Store atangin Khawchin Thlirna update rawh. Update button a lang loh chuan uninstall/reinstall rawh.",
                    "title_en": "Important app update",
                    "body_en": "Please update Khawchin Thlirna from Play Store for the latest forecast and lighter data usage. If update is not shown, uninstall and reinstall.",
                    "dismissible": False,
                    "action_label_mz": "Play Store-ah kal",
                    "action_label_en": "Open Play Store",
                    "action_url": "https://play.google.com/store/apps/details?id=com.mapuia.khawchinthlirna",
                    "admin_updated_at": now_iso(),
                }
                write_json(ANNOUNCEMENTS_JSON_PATH, announcement)
                fcm_sent = False
                if bool(body.get("send_fcm")):
                    fcm_sent = firebase_send_fcm(
                        topic="weather_alerts",
                        title="Khawchin Thlirna update",
                        body="Forecast thar hmuh nan Play Store atangin app update rawh.",
                        action_url="https://play.google.com/store/apps/details?id=com.mapuia.khawchinthlirna",
                        notification_type="update_required",
                    )
                self._send_json(HTTPStatus.OK, {"success": True, "fcm_sent": bool(fcm_sent), "announcement": announcement})
                return
            self._send_json(HTTPStatus.NOT_FOUND, {"error": "not_found"})
        except Exception as exc:
            self._send_json(HTTPStatus.BAD_REQUEST, {"error": str(exc)})


def main() -> None:
    host = os.environ.get("ADMIN_HOST", "127.0.0.1")
    port = int(os.environ.get("ADMIN_PORT", "8091"))
    server = ThreadingHTTPServer((host, port), AdminHandler)
    print(f"Khawchin admin panel listening on http://{host}:{port}/admin/")
    server.serve_forever()


if __name__ == "__main__":
    main()
