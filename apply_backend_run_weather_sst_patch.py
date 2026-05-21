from pathlib import Path
import shutil


backend_path = Path(r"C:\Users\Mapuia\Desktop\App developement\Khawchin app\backend\backend_v86.py")
backup_path = backend_path.with_name("backend_v86.py.20260325_codex_weather_sst.bak")
if not backup_path.exists():
    shutil.copy2(backend_path, backup_path)

text = backend_path.read_text(encoding="utf-8")

# 1) Add marine endpoint for observed BoB SST
old = '    ELEVATION = f"{OPEN_METEO_BASE}/v1/elevation"\n'
new = (
    '    ELEVATION = f"{OPEN_METEO_BASE}/v1/elevation"\n'
    '    MARINE = "https://marine-api.open-meteo.com/v1/marine"\n'
)
if old in text and "MARINE = " not in text:
    text = text.replace(old, new, 1)

# 2) Add BoB SST sampling constants
marker = "# Cyclone filtering & freshness (tunable)\n"
insert = (
    "\n"
    "# Bay of Bengal SST sampling points and monthly climatology (degC)\n"
    "BOB_SST_SAMPLE_POINTS = [\n"
    "    (13.0, 85.0),\n"
    "    (15.0, 88.0),\n"
    "    (18.0, 91.0),\n"
    "    (20.0, 93.0),\n"
    "]\n"
    "BOB_SST_CLIMATOLOGY_C = {\n"
    "    1: 27.0, 2: 27.4, 3: 28.3, 4: 29.4,\n"
    "    5: 30.0, 6: 29.8, 7: 29.2, 8: 28.8,\n"
    "    9: 28.8, 10: 28.8, 11: 28.3, 12: 27.5,\n"
    "}\n"
)
if marker in text and "BOB_SST_SAMPLE_POINTS" not in text:
    text = text.replace(marker, insert + marker, 1)

# 3) Add observed BoB SST helper
helper_marker = "def fetch_live_climate_indices() -> Dict[str, Any]:\n"
helper = (
    "def _fetch_observed_bob_sst(reference_month: int) -> Optional[Dict[str, float]]:\n"
    "    \"\"\"Fetch observed BoB SST from marine API and convert to anomaly vs monthly climatology.\"\"\"\n"
    "    clim = BOB_SST_CLIMATOLOGY_C.get(reference_month)\n"
    "    if clim is None:\n"
    "        return None\n"
    "\n"
    "    samples: List[float] = []\n"
    "    for lat, lon in BOB_SST_SAMPLE_POINTS:\n"
    "        payload = http.get_json(\n"
    "            Endpoints.MARINE,\n"
    "            params={\n"
    "                \"latitude\": lat,\n"
    "                \"longitude\": lon,\n"
    "                \"hourly\": \"sea_surface_temperature\",\n"
    "                \"forecast_days\": 1,\n"
    "                \"timezone\": \"UTC\",\n"
    "            },\n"
    "            use_budget=False,\n"
    "        )\n"
    "        if not payload:\n"
    "            continue\n"
    "\n"
    "        vals = []\n"
    "        for v in ((payload.get(\"hourly\") or {}).get(\"sea_surface_temperature\") or []):\n"
    "            fv = safe_float(v)\n"
    "            if fv is not None:\n"
    "                vals.append(fv)\n"
    "        if vals:\n"
    "            samples.append(vals[0])\n"
    "\n"
    "    if not samples:\n"
    "        return None\n"
    "\n"
    "    mean_sst = round(sum(samples) / len(samples), 2)\n"
    "    raw_anom = round(mean_sst - clim, 2)\n"
    "    anom = _bounded_climate_value(raw_anom, BOB_SST_VALID_RANGE)\n"
    "    if anom is None:\n"
    "        return None\n"
    "\n"
    "    return {\n"
    "        \"sst_c\": mean_sst,\n"
    "        \"anomaly_c\": anom,\n"
    "        \"sample_count\": float(len(samples)),\n"
    "    }\n"
    "\n"
    "\n"
)
if helper_marker in text and "_fetch_observed_bob_sst(" not in text:
    text = text.replace(helper_marker, helper + helper_marker, 1)

# 4) Update climate result schema
old = (
    '    result = {\n'
    '        "nino34": None, "nino34_state": "NEUTRAL",\n'
    '        "iod_dmi": None, "iod_state": "NEUTRAL",\n'
    '        "bob_sst_anomaly": None,\n'
    '        "source": "NOAA PSL / heuristic",\n'
    '        "fetched_at": now_iso(),\n'
    '    }\n'
)
new = (
    '    result = {\n'
    '        "nino34": None, "nino34_state": "NEUTRAL",\n'
    '        "iod_dmi": None, "iod_state": "NEUTRAL",\n'
    '        "bob_sst_anomaly": None,\n'
    '        "bob_sst_c": None,\n'
    '        "bob_sst_source": "none",\n'
    '        "source": "NOAA PSL + Open-Meteo Marine",\n'
    '        "fetched_at": now_iso(),\n'
    '    }\n'
)
if old in text:
    text = text.replace(old, new, 1)

# 5) Replace Bob SST heuristic-only block with observed+fallback
old = (
    "    nino = result[\"nino34\"]\n"
    "    iod = result[\"iod_dmi\"]\n"
    "    if nino is not None or iod is not None:\n"
    "        bob_sst_est = round(0.3 * (nino or 0.0) - 0.4 * (iod or 0.0) + 0.1, 2)\n"
    "        bounded_bob = _bounded_climate_value(bob_sst_est, BOB_SST_VALID_RANGE)\n"
    "        if bounded_bob is not None:\n"
    "            result[\"bob_sst_anomaly\"] = bounded_bob\n"
    "            logger.info(\"BoB SST anomaly (estimated): %s?C\", bounded_bob)\n"
    "        else:\n"
    "            logger.warning(\"Discarded implausible BoB SST anomaly estimate: %s?C\", bob_sst_est)\n"
)
new = (
    "    observed_bob = _fetch_observed_bob_sst(reference_month=now_utc().month)\n"
    "    if observed_bob is not None:\n"
    "        result[\"bob_sst_c\"] = observed_bob.get(\"sst_c\")\n"
    "        result[\"bob_sst_anomaly\"] = observed_bob.get(\"anomaly_c\")\n"
    "        result[\"bob_sst_source\"] = \"marine_observed\"\n"
    "        logger.info(\n"
    "            \"BoB SST observed: %s degC (anomaly: %s degC, samples=%d)\",\n"
    "            result[\"bob_sst_c\"],\n"
    "            result[\"bob_sst_anomaly\"],\n"
    "            int(observed_bob.get(\"sample_count\", 0)),\n"
    "        )\n"
    "    else:\n"
    "        nino = result[\"nino34\"]\n"
    "        iod = result[\"iod_dmi\"]\n"
    "        if nino is not None or iod is not None:\n"
    "            bob_sst_est = round(0.3 * (nino or 0.0) - 0.4 * (iod or 0.0) + 0.1, 2)\n"
    "            bounded_bob = _bounded_climate_value(bob_sst_est, BOB_SST_VALID_RANGE)\n"
    "            if bounded_bob is not None:\n"
    "                result[\"bob_sst_anomaly\"] = bounded_bob\n"
    "                result[\"bob_sst_source\"] = \"enso_iod_fallback\"\n"
    "                logger.info(\"BoB SST anomaly (fallback estimate): %s degC\", bounded_bob)\n"
    "            else:\n"
    "                logger.warning(\"Discarded implausible BoB SST fallback estimate: %s degC\", bob_sst_est)\n"
)
if old in text:
    text = text.replace(old, new, 1)

# 6) process_cell signature add weather_systems_snapshot
old = (
    "def process_cell(\n"
    "    gid: str,\n"
    "    lat: float,\n"
    "    lon: float,\n"
    "    model_map: Dict[str, Dict],\n"
    "    elevation:  float,\n"
    "    run_id_str: str,\n"
    "    run_time: datetime,\n"
    "    bias_mgr: BiasManager,\n"
    "    stations: List[Dict],\n"
    "    db,\n"
    "    enable_verify: bool,\n"
    "    dry_run: bool,\n"
    "    crowd_mgr=None,\n"
    "    crowd_reports_pool: Optional[List[Dict]] = None,\n"
    "    skill_reporter: Optional[SkillReportAggregator] = None,\n"
    ") -> bool:\n"
)
new = (
    "def process_cell(\n"
    "    gid: str,\n"
    "    lat: float,\n"
    "    lon: float,\n"
    "    model_map: Dict[str, Dict],\n"
    "    elevation:  float,\n"
    "    run_id_str: str,\n"
    "    run_time: datetime,\n"
    "    bias_mgr: BiasManager,\n"
    "    stations: List[Dict],\n"
    "    db,\n"
    "    enable_verify: bool,\n"
    "    dry_run: bool,\n"
    "    crowd_mgr=None,\n"
    "    crowd_reports_pool: Optional[List[Dict]] = None,\n"
    "    weather_systems_snapshot: Optional[Dict[str, Any]] = None,\n"
    "    skill_reporter: Optional[SkillReportAggregator] = None,\n"
    ") -> bool:\n"
)
if old in text:
    text = text.replace(old, new, 1)

# 7) process_cell weather systems block uses snapshot (fallback only if missing)
old = (
    "        # Weather system status (BoB cyclones, Western Disturbance, Nor'westers)\n"
    "        # Now checked for ALL locations since results are cached for 30 minutes\n"
    "        # This ensures cyclone/safety warnings reach all users regardless of location\n"
    "        weather_systems = None\n"
    "        try:\n"
    "            weather_systems = check_all_weather_sources()\n"
    "        except Exception as e:\n"
    "            logger.warning(\"Weather systems check failed for (%.3f, %.3f): %s\", lat, lon, e)\n"
    "            weather_systems = None\n"
)
new = (
    "        # Weather systems are prepared once per run and reused across cells.\n"
    "        weather_systems = weather_systems_snapshot.copy() if isinstance(weather_systems_snapshot, dict) else None\n"
    "        if weather_systems is None:\n"
    "            try:\n"
    "                weather_systems = check_all_weather_sources()\n"
    "            except Exception as e:\n"
    "                logger.warning(\"Weather systems check failed for (%.3f, %.3f): %s\", lat, lon, e)\n"
    "                weather_systems = None\n"
)
if old in text:
    text = text.replace(old, new, 1)

# 8) run_update creates one weather systems snapshot for reuse
old = (
    "    # Skill report aggregator (only if verification enabled)\n"
    "    skill_reporter = SkillReportAggregator() if enable_verify else None\n"
)
new = (
    "    weather_systems_snapshot: Optional[Dict[str, Any]] = None\n"
    "    if not dry_run:\n"
    "        try:\n"
    "            weather_systems_snapshot = check_all_weather_sources()\n"
    "            if weather_systems_snapshot:\n"
    "                logger.info(\"Weather systems snapshot prepared for run reuse\")\n"
    "        except Exception as e:\n"
    "            logger.warning(\"Weather systems snapshot fetch failed: %s\", e)\n"
    "            weather_systems_snapshot = None\n"
    "\n"
    "    # Skill report aggregator (only if verification enabled)\n"
    "    skill_reporter = SkillReportAggregator() if enable_verify else None\n"
)
if old in text:
    text = text.replace(old, new, 1)

# 9) pass weather systems snapshot to process_cell
old = (
    "        success = process_cell(\n"
    "            gid, lat, lon, model_map, elev,\n"
    "            run_id_str, run_time,\n"
    "            bias_mgr, stations, db,\n"
    "            enable_verify, dry_run,\n"
    "            crowd_mgr,\n"
    "            crowd_reports_pool,\n"
    "            skill_reporter\n"
    "        )\n"
)
new = (
    "        success = process_cell(\n"
    "            gid, lat, lon, model_map, elev,\n"
    "            run_id_str, run_time,\n"
    "            bias_mgr, stations, db,\n"
    "            enable_verify, dry_run,\n"
    "            crowd_mgr,\n"
    "            crowd_reports_pool,\n"
    "            weather_systems_snapshot,\n"
    "            skill_reporter\n"
    "        )\n"
)
if old in text:
    text = text.replace(old, new, 1)

# 10) reuse snapshot for severe alert send at end
old = (
    "    alerts_sent = []\n"
    "    if not dry_run:\n"
    "        try:\n"
    "            # Get latest weather systems status (should be cached from cell processing)\n"
    "            weather_systems = check_all_weather_sources()\n"
    "            if weather_systems:\n"
    "                alerts_sent = check_and_send_severe_weather_alerts(weather_systems)\n"
    "                if alerts_sent:\n"
    "                    logger.info(\"Sent %d severe weather alerts: %s\", len(alerts_sent), alerts_sent)\n"
    "        except Exception as e:\n"
    "            logger.warning(\"Failed to check/send weather alerts: %s\", e)\n"
)
new = (
    "    alerts_sent = []\n"
    "    if not dry_run:\n"
    "        try:\n"
    "            weather_systems = weather_systems_snapshot\n"
    "            if not weather_systems:\n"
    "                weather_systems = check_all_weather_sources()\n"
    "            if weather_systems:\n"
    "                alerts_sent = check_and_send_severe_weather_alerts(weather_systems)\n"
    "                if alerts_sent:\n"
    "                    logger.info(\"Sent %d severe weather alerts: %s\", len(alerts_sent), alerts_sent)\n"
    "        except Exception as e:\n"
    "            logger.warning(\"Failed to check/send weather alerts: %s\", e)\n"
)
if old in text:
    text = text.replace(old, new, 1)

# 11) update stale comment text
text = text.replace("Results are cached for 30 minutes to avoid repeated slow API calls.", "Results are cached for 90 minutes to avoid repeated slow API calls.")

# Keep function spacing tidy
text = text.replace("    return result\ndef predict_cyclone_season()", "    return result\n\n\ndef predict_cyclone_season()")

backend_path.write_text(text, encoding="utf-8")
print("Patched backend_v86.py")
