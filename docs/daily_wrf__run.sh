#!/usr/bin/env bash
set -euo pipefail

log() { printf '[%s] %s\n' "$(date -u +%Y-%m-%dT%H:%M:%SZ)" "$*"; }
die() { log "ERROR: $*"; exit 1; }
retry_sleep() {
  local attempt="$1"
  local base_delay="${EC2_UPLOAD_RETRY_DELAY:-15}"
  local delay=$((base_delay * attempt))
  log "Retrying in ${delay}s..."
  sleep "$delay"
}
retry_scp() {
  local src="$1"
  local dest="$2"
  local max="${EC2_UPLOAD_RETRIES:-4}"
  local attempt=1
  while true; do
    if scp -o ConnectTimeout=30 -o ServerAliveInterval=15 -o ServerAliveCountMax=3 -i "$EC2_KEY" "$src" "$dest"; then
      return 0
    fi
    local status=$?
    if (( attempt >= max )); then
      log "SCP failed after ${attempt} attempts: $src -> $dest"
      return "$status"
    fi
    log "SCP failed attempt ${attempt}/${max}: $src -> $dest"
    retry_sleep "$attempt"
    attempt=$((attempt + 1))
  done
}
retry_ssh_script() {
  local script_file="$1"
  local max="${EC2_UPLOAD_RETRIES:-4}"
  local attempt=1
  while true; do
    if ssh -o ConnectTimeout=30 -o ServerAliveInterval=15 -o ServerAliveCountMax=3 -i "$EC2_KEY" "$EC2_HOST" "bash -s" < "$script_file"; then
      return 0
    fi
    local status=$?
    if (( attempt >= max )); then
      log "SSH remote upload step failed after ${attempt} attempts"
      return "$status"
    fi
    log "SSH remote upload step failed attempt ${attempt}/${max}"
    retry_sleep "$attempt"
    attempt=$((attempt + 1))
  done
}
on_err() {
  local status=$?
  local line="${1:-unknown}"
  log "ERROR: command failed at line ${line} (exit=${status}). Latest log dir: ${LOG_DIR:-not-created-yet}"
  exit "$status"
}
trap 'on_err $LINENO' ERR

command -v flock >/dev/null || die "flock not found"

LOCK_FILE="${WRF_DAILY_LOCK_FILE:-$HOME/wrf/.daily_wrf.lock}"
mkdir -p "$(dirname "$LOCK_FILE")"
exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  log "Another daily WRF run is already active; exiting."
  exit 0
fi

# --------------------
# Config (edit these)
# --------------------
WRF_HOME="$HOME/wrf/WRF"
WPS_HOME="$HOME/wrf/WPS"
if [[ -z "${WPS_GEOG_PATH:-}" ]]; then
  if [[ -d "$HOME/wrf/WPS_GEOG/WPS_GEOG/topo_gmted2010_30s" ]]; then
    WPS_GEOG_PATH="$HOME/wrf/WPS_GEOG/WPS_GEOG"
  elif [[ -d "$HOME/wrf/WPS_GEOG/geog/topo_gmted2010_30s" ]]; then
    WPS_GEOG_PATH="$HOME/wrf/WPS_GEOG/geog"
  else
    WPS_GEOG_PATH="$HOME/wrf/WPS_GEOG"
  fi
fi
RUNS_DIR="$HOME/wrf/runs"
DATA_DIR="$HOME/wrf/data/gfs"
WRF_RUN_LOG_KEEP_DAYS="${WRF_RUN_LOG_KEEP_DAYS:-14}"

JSON_SCRIPT="/mnt/c/Users/Mapuia/AndroidStudioProjects/KhawchinThlirna/codex_backend_work/wrf_local_to_json.py"
DOMAIN="d01"
PATTERN="wrfout_d01_*"
WRF_PROFILE="${WRF_PROFILE:-9km}"

case "$WRF_PROFILE" in
  9km)
    WRF_DX="${WRF_DX:-9000}"
    WRF_DY="${WRF_DY:-9000}"
    WRF_E_WE="${WRF_E_WE:-120}"
    WRF_E_SN="${WRF_E_SN:-110}"
    WRF_TIME_STEP="${WRF_TIME_STEP:-54}"
    WRF_RADT="${WRF_RADT:-9}"
    WRF_DEFAULT_NP="${WRF_DEFAULT_NP:-4}"
    SOURCE="${SOURCE:-wrf_gfs_local_d01_9km}"
    OUTPUT_DIR="${WRF_OUTPUT_DIR:-$HOME/wrf/output/json}"
    RUN_DIR_PREFIX="khawchin_gfs"
    DEFAULT_EC2_DEST_TMP="/home/ubuntu/wrf_local_latest.json.tmp"
    DEFAULT_EC2_DEST_FINAL="/opt/khawchin/cache/wrf_local_latest.json"
    DEFAULT_EC2_ARCHIVE_DIR="/opt/khawchin/cache/wrf_archive"
    ;;
  3km)
    WRF_DX="${WRF_DX:-3000}"
    WRF_DY="${WRF_DY:-3000}"
    WRF_E_WE="${WRF_E_WE:-221}"
    WRF_E_SN="${WRF_E_SN:-201}"
    WRF_TIME_STEP="${WRF_TIME_STEP:-18}"
    WRF_RADT="${WRF_RADT:-3}"
    WRF_DEFAULT_NP="${WRF_DEFAULT_NP:-6}"
    SOURCE="${SOURCE:-wrf_gfs_local_d01_3km}"
    OUTPUT_DIR="${WRF_OUTPUT_DIR:-$HOME/wrf/output/json_3km}"
    RUN_DIR_PREFIX="khawchin_gfs_3km"
    DEFAULT_EC2_DEST_TMP="/home/ubuntu/wrf_local_3km_latest.json.tmp"
    DEFAULT_EC2_DEST_FINAL="/opt/khawchin/cache/wrf_local_3km_latest.json"
    DEFAULT_EC2_ARCHIVE_DIR="/opt/khawchin/cache/wrf_archive_3km"
    ;;
  *)
    die "Unsupported WRF_PROFILE=$WRF_PROFILE (use 9km or 3km)"
    ;;
esac

ARCHIVE_DIR="${WRF_JSON_ARCHIVE_DIR:-$OUTPUT_DIR/archive}"

RUN_HOURS="${RUN_HOURS:-24}"

EC2_HOST="${EC2_HOST:-ubuntu@13.207.47.235}"
EC2_KEY="${EC2_KEY:-$HOME/.ssh/khawchin-key.pem}"
EC2_DEST_TMP="${EC2_DEST_TMP:-$DEFAULT_EC2_DEST_TMP}"
EC2_DEST_FINAL="${EC2_DEST_FINAL:-$DEFAULT_EC2_DEST_FINAL}"
EC2_ARCHIVE_DIR="${EC2_ARCHIVE_DIR:-$DEFAULT_EC2_ARCHIVE_DIR}"
WRF_ARCHIVE_KEEP_DAYS="${WRF_ARCHIVE_KEEP_DAYS:-21}"
# --------------------

command -v wget >/dev/null || die "wget not found"
command -v mpirun >/dev/null || die "mpirun not found"
command -v python3 >/dev/null || die "python3 not found"

[[ -x "$WRF_HOME/run/real.exe" ]] || die "real.exe not found in $WRF_HOME/run"
[[ -x "$WRF_HOME/run/wrf.exe" ]] || die "wrf.exe not found in $WRF_HOME/run"
[[ -x "$WPS_HOME/geogrid.exe" ]] || die "geogrid.exe not found in $WPS_HOME"
[[ -x "$WPS_HOME/ungrib.exe" ]] || die "ungrib.exe not found in $WPS_HOME"
[[ -x "$WPS_HOME/metgrid.exe" ]] || die "metgrid.exe not found in $WPS_HOME"
[[ -f "$JSON_SCRIPT" ]] || die "JSON script not found: $JSON_SCRIPT"

RUN_DATE="${RUN_DATE:-}"
RUN_HH="${RUN_HH:-}"
if [[ -z "$RUN_DATE" || -z "$RUN_HH" ]]; then
  SAFE_EPOCH=$(( $(date -u +%s) - 6 * 3600 ))
  SAFE_HOUR=$(date -u -d "@${SAFE_EPOCH}" +%H)
  CYCLE_HOUR=$((10#${SAFE_HOUR} / 6 * 6))
  RUN_DATE=$(date -u -d "@${SAFE_EPOCH}" +%Y%m%d)
  printf -v RUN_HH "%02d" "$CYCLE_HOUR"
fi

RUN_ID="${RUN_DATE}${RUN_HH}"
START_YMD="${RUN_DATE:0:4}-${RUN_DATE:4:2}-${RUN_DATE:6:2}"
START_EPOCH=$(date -u -d "${START_YMD} ${RUN_HH}:00:00 UTC" +%s)
END_EPOCH=$((START_EPOCH + RUN_HOURS * 3600))
START_WPS=$(date -u -d "@${START_EPOCH}" +%Y-%m-%d_%H:00:00)
END_WPS=$(date -u -d "@${END_EPOCH}" +%Y-%m-%d_%H:00:00)

RUN_DIR="${RUNS_DIR}/${RUN_DIR_PREFIX}_${RUN_ID}"
GFS_DIR="${DATA_DIR}/${RUN_ID}"
mkdir -p "$RUN_DIR" "$GFS_DIR" "$OUTPUT_DIR" "$ARCHIVE_DIR"
ATTEMPT_ID="${WRF_PROFILE}_${RUN_ID}_$(date -u +%Y%m%dT%H%M%SZ)"
LOG_DIR="${RUN_DIR}/logs/${ATTEMPT_ID}"
mkdir -p "$LOG_DIR"
ln -sfn "$LOG_DIR" "${RUN_DIR}/latest_logs"
find "${RUN_DIR}/logs" -mindepth 1 -maxdepth 1 -type d -mtime +"$WRF_RUN_LOG_KEEP_DAYS" -exec rm -rf {} + 2>/dev/null || true

# Keep every rerun's terminal output and component logs. This makes the script
# safe to run multiple times per day, including reruns of the same GFS cycle.
exec > >(tee -a "$LOG_DIR/daily_wrf_run.log") 2>&1

log "RUN_ID=$RUN_ID"
log "ATTEMPT_ID=$ATTEMPT_ID"
log "WRF_PROFILE=$WRF_PROFILE"
log "START_WPS=$START_WPS"
log "END_WPS=$END_WPS"
log "GRID=${WRF_E_WE}x${WRF_E_SN} DX=${WRF_DX} DY=${WRF_DY} TIME_STEP=${WRF_TIME_STEP}"
log "WPS_GEOG_PATH=$WPS_GEOG_PATH"
log "LOG_DIR=$LOG_DIR"

log "Prepare WPS run directory"
cd "$RUN_DIR"
ln -sf "$WPS_HOME/geogrid.exe" .
ln -sf "$WPS_HOME/ungrib.exe" .
ln -sf "$WPS_HOME/metgrid.exe" .
ln -sf "$WPS_HOME/link_grib.csh" .
ln -sfn "$WPS_HOME/geogrid" geogrid
ln -sfn "$WPS_HOME/ungrib" ungrib
ln -sfn "$WPS_HOME/metgrid" metgrid

cat > namelist.wps <<EOF
&share
 wrf_core = 'ARW',
 max_dom = 1,
 start_date = '${START_WPS}',
 end_date   = '${END_WPS}',
 interval_seconds = 10800,
 io_form_geogrid = 2,
/

&geogrid
 parent_id         = 1,
 parent_grid_ratio = 1,
 i_parent_start    = 1,
 j_parent_start    = 1,
 e_we              = ${WRF_E_WE},
 e_sn              = ${WRF_E_SN},
 geog_data_res     = 'default',
 dx = ${WRF_DX},
 dy = ${WRF_DY},
 map_proj = 'mercator',
 ref_lat   = 23.30,
 ref_lon   = 93.30,
 truelat1  = 23.30,
 truelat2  = 0.0,
 stand_lon = 93.30,
 geog_data_path = '${WPS_GEOG_PATH}',
/

&ungrib
 out_format = 'WPS',
 prefix = 'FILE',
/

&metgrid
 fg_name = 'FILE',
 io_form_metgrid = 2,
/
EOF

log "Download GFS data"
cd "$GFS_DIR"
for hour in $(seq 0 3 "$RUN_HOURS"); do
  printf -v FFF "%03d" "$hour"
  out="gfs.t${RUN_HH}z.pgrb2.0p25.f${FFF}.grib2"
  url="https://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?dir=%2Fgfs.${RUN_DATE}%2F${RUN_HH}%2Fatmos&file=gfs.t${RUN_HH}z.pgrb2.0p25.f${FFF}&all_lev=on&all_var=on&subregion=&leftlon=80&rightlon=106&toplat=35&bottomlat=5"
  if [[ ! -s "$out" ]]; then
    wget -q -O "$out" "$url"
  fi
  if head -c 4096 "$out" | grep -Eqi "<html|not found|error"; then
    die "GFS download returned HTML for $out (cycle not ready)"
  fi
  if [[ "$(stat -c%s "$out")" -lt 500000 ]]; then
    die "GFS download looks too small for $out"
  fi
done

log "Run WPS"
cd "$RUN_DIR"
rm -f GRIBFILE.* FILE:* PFILE:* met_em.d01.*.nc
ln -sf "$LOG_DIR/geogrid.log" geogrid.log
./geogrid.exe > "$LOG_DIR/geogrid.log" 2>&1
[[ -f geo_em.d01.nc ]] || die "geogrid failed"
./link_grib.csh "$GFS_DIR"/gfs*.grib2
ln -sf "$WPS_HOME/ungrib/Variable_Tables/Vtable.GFS" Vtable
ln -sf "$LOG_DIR/ungrib.log" ungrib.log
./ungrib.exe > "$LOG_DIR/ungrib.log" 2>&1
ls FILE:* >/dev/null 2>&1 || die "ungrib failed"
ln -sf "$LOG_DIR/metgrid.log" metgrid.log
./metgrid.exe > "$LOG_DIR/metgrid.log" 2>&1
ls met_em.d01.*.nc >/dev/null 2>&1 || die "metgrid failed"
expected_met_count=$((RUN_HOURS / 3 + 1))
actual_met_count=$(ls met_em.d01.*.nc | wc -l)
if [[ "$actual_met_count" -ne "$expected_met_count" ]]; then
  die "metgrid file count mismatch: got $actual_met_count expected $expected_met_count"
fi

log "Run WRF"
WRF_WORK_LOG_DIR="$LOG_DIR/wrf_run"
mkdir -p "$WRF_WORK_LOG_DIR/real_rsl" "$WRF_WORK_LOG_DIR/wrf_rsl"
ln -sfn "$WRF_WORK_LOG_DIR" "${RUN_DIR}/latest_wrf_logs"
cd "$WRF_HOME/run"
rm -f met_em.d0*.nc wrfinput_d0* wrfbdy_d01 wrfout_d0* rsl.out.* rsl.error.* real.log wrf.log
ln -sf "$WRF_WORK_LOG_DIR/real.log" real.log
ln -sf "$WRF_WORK_LOG_DIR/wrf.log" wrf.log
if ! compgen -G "${RUN_DIR}/met_em.d01.*.nc" >/dev/null; then
  die "met_em files missing in $RUN_DIR"
fi
ln -sf "${RUN_DIR}"/met_em.d01.*.nc .

START_YEAR=$(date -u -d "@${START_EPOCH}" +%Y)
START_MONTH=$(date -u -d "@${START_EPOCH}" +%m)
START_DAY=$(date -u -d "@${START_EPOCH}" +%d)
START_HOUR=$(date -u -d "@${START_EPOCH}" +%H)
END_YEAR=$(date -u -d "@${END_EPOCH}" +%Y)
END_MONTH=$(date -u -d "@${END_EPOCH}" +%m)
END_DAY=$(date -u -d "@${END_EPOCH}" +%d)
END_HOUR=$(date -u -d "@${END_EPOCH}" +%H)

NUM_LAND_CAT=21
if command -v ncdump >/dev/null; then
  MET_FILES=("${RUN_DIR}"/met_em.d01.*.nc)
  FIRST_MET="${MET_FILES[0]}"
  # Do not exit awk early here: with pipefail, early pipe close can make ncdump
  # report SIGPIPE (141), even though the met_em file is fine.
  NLC=$(ncdump -h "$FIRST_MET" | awk '/NUM_LAND_CAT/ {gsub(";", "", $3); value=$3} END {print value}' || true)
  if [[ -n "$NLC" ]]; then
    NUM_LAND_CAT="$NLC"
  fi
fi

cat > namelist.input <<EOF
&time_control
 run_days                            = 0,
 run_hours                           = ${RUN_HOURS},
 run_minutes                         = 0,
 run_seconds                         = 0,
 start_year                          = ${START_YEAR},
 start_month                         = ${START_MONTH},
 start_day                           = ${START_DAY},
 start_hour                          = ${START_HOUR},
 end_year                            = ${END_YEAR},
 end_month                           = ${END_MONTH},
 end_day                             = ${END_DAY},
 end_hour                            = ${END_HOUR},
 interval_seconds                    = 10800,
 input_from_file                     = .true.,
 history_interval                    = 60,
 frames_per_outfile                  = 24,
 restart                             = .false.,
 restart_interval                    = 7200,
 io_form_history                     = 2,
 io_form_restart                     = 2,
 io_form_input                       = 2,
 io_form_boundary                    = 2,
/

&domains
 time_step                           = ${WRF_TIME_STEP},
 time_step_fract_num                 = 0,
 time_step_fract_den                 = 1,
 max_dom                             = 1,
 e_we                                = ${WRF_E_WE},
 e_sn                                = ${WRF_E_SN},
 e_vert                              = 40,
 p_top_requested                     = 5000,
 num_metgrid_levels                  = 34,
 num_metgrid_soil_levels             = 4,
 dx                                  = ${WRF_DX},
 dy                                  = ${WRF_DY},
 grid_id                             = 1,
 parent_id                           = 0,
 i_parent_start                      = 1,
 j_parent_start                      = 1,
 parent_grid_ratio                   = 1,
 parent_time_step_ratio              = 1,
 feedback                            = 1,
 smooth_option                       = 0,
/

&physics
 physics_suite                       = 'CONUS',
 mp_physics                          = 8,
 ra_lw_physics                       = 4,
 ra_sw_physics                       = 4,
 radt                                = ${WRF_RADT},
 sf_sfclay_physics                   = 1,
 sf_surface_physics                  = 2,
 bl_pbl_physics                      = 1,
 bldt                                = 0,
 cu_physics                          = 1,
 cudt                                = 5,
 isfflx                              = 1,
 ifsnow                              = 0,
 icloud                              = 1,
 surface_input_source                = 1,
 num_land_cat                        = ${NUM_LAND_CAT},
 sf_urban_physics                    = 0,
/

&fdda
/

&dynamics
 hybrid_opt                          = 2,
 w_damping                           = 0,
 diff_opt                            = 1,
 km_opt                              = 4,
 diff_6th_opt                        = 0,
 diff_6th_factor                     = 0.12,
 base_temp                           = 290.,
 damp_opt                            = 3,
 zdamp                               = 5000.,
 dampcoef                            = 0.2,
 khdif                               = 0,
 kvdif                               = 0,
 non_hydrostatic                     = .true.,
 moist_adv_opt                       = 1,
 scalar_adv_opt                      = 1,
 gwd_opt                             = 0,
/

&bdy_control
 spec_bdy_width                      = 5,
 specified                           = .true.,
 nested                              = .false.,
/

&grib2
/

&namelist_quilt
 nio_tasks_per_group = 0,
 nio_groups = 1,
/
EOF

NPROC=$(nproc)
WRF_NP="${WRF_NP:-$WRF_DEFAULT_NP}"
if (( WRF_NP > NPROC )); then
  WRF_NP="$NPROC"
fi
printf "localhost slots=%s\n" "$NPROC" > "$HOME/wrf/mpi_hostfile"

log "Run real.exe with WRF_NP=$WRF_NP"
if ! mpirun --hostfile "$HOME/wrf/mpi_hostfile" -np "$WRF_NP" ./real.exe > real.log 2>&1; then
  cp -f rsl.out.* rsl.error.* "$WRF_WORK_LOG_DIR/real_rsl/" 2>/dev/null || true
  die "real.exe failed; see $WRF_WORK_LOG_DIR/real.log"
fi
cp -f rsl.out.* rsl.error.* "$WRF_WORK_LOG_DIR/real_rsl/" 2>/dev/null || true
grep -q "SUCCESS COMPLETE REAL_EM INIT" rsl.out.0000 rsl.error.0000 2>/dev/null || die "real.exe did not report success; see $WRF_WORK_LOG_DIR/real.log"

rm -f rsl.out.* rsl.error.*
log "Run wrf.exe with WRF_NP=$WRF_NP"
if ! mpirun --hostfile "$HOME/wrf/mpi_hostfile" -np "$WRF_NP" ./wrf.exe > wrf.log 2>&1; then
  cp -f rsl.out.* rsl.error.* "$WRF_WORK_LOG_DIR/wrf_rsl/" 2>/dev/null || true
  die "wrf.exe failed; see $WRF_WORK_LOG_DIR/wrf.log"
fi
cp -f rsl.out.* rsl.error.* "$WRF_WORK_LOG_DIR/wrf_rsl/" 2>/dev/null || true
grep -q "SUCCESS COMPLETE WRF" rsl.out.0000 rsl.error.0000 2>/dev/null || die "wrf.exe did not report success; see $WRF_WORK_LOG_DIR/wrf.log"

log "Generate JSON"
ARCHIVE_JSON="${ARCHIVE_DIR}/wrf_local_${ATTEMPT_ID}.json"
OUTPUT_JSON="${OUTPUT_DIR}/wrf_local_latest.json"
python3 "$JSON_SCRIPT" \
  --wrf-dir "$WRF_HOME/run" \
  --pattern "$PATTERN" \
  --domain "$DOMAIN" \
  --source "$SOURCE" \
  --run-id "wrf_${RUN_DATE}_${RUN_HH}" \
  --output "$ARCHIVE_JSON"
cp -f "$ARCHIVE_JSON" "$OUTPUT_JSON"

find "$ARCHIVE_DIR" -type f -name 'wrf_local_*.json' -mtime +"$WRF_ARCHIVE_KEEP_DAYS" -delete 2>/dev/null || true

log "Upload latest and archive JSON to EC2"
[[ -f "$EC2_KEY" ]] || die "EC2 key not found: $EC2_KEY"
retry_scp "$OUTPUT_JSON" "${EC2_HOST}:${EC2_DEST_TMP}"
EC2_ARCHIVE_TMP="/home/ubuntu/wrf_local_${ATTEMPT_ID}.json.tmp"
EC2_ARCHIVE_FINAL="${EC2_ARCHIVE_DIR}/wrf_local_${ATTEMPT_ID}.json"
retry_scp "$ARCHIVE_JSON" "${EC2_HOST}:${EC2_ARCHIVE_TMP}"
remote_upload_script=$(cat <<EOF
set -euo pipefail
archive_dir="${EC2_ARCHIVE_DIR}"
dest_tmp="${EC2_DEST_TMP}"
dest_final="${EC2_DEST_FINAL}"
archive_tmp="${EC2_ARCHIVE_TMP}"
archive_final="${EC2_ARCHIVE_FINAL}"
keep_days="${WRF_ARCHIVE_KEEP_DAYS}"

if [[ -z "\$archive_dir" || -z "\$dest_tmp" || -z "\$dest_final" || -z "\$archive_tmp" || -z "\$archive_final" || -z "\$keep_days" ]]; then
  echo "Remote upload variable missing" >&2
  exit 2
fi
if ! [[ "\$keep_days" =~ ^[0-9]+$ ]]; then
  echo "WRF archive keep days must be numeric: \$keep_days" >&2
  exit 2
fi

sudo mkdir -p "\$archive_dir" "\$(dirname "\$dest_final")"
sudo mv "\$dest_tmp" "\$dest_final"
sudo chmod 644 "\$dest_final"
sudo mv "\$archive_tmp" "\$archive_final"
sudo chmod 644 "\$archive_final"
sudo find "\$archive_dir" -type f -name 'wrf_local_*.json' -mtime +"\$keep_days" -delete
sudo ls -lh "\$dest_final" "\$archive_final"
EOF
)
REMOTE_UPLOAD_SCRIPT_FILE="$LOG_DIR/remote_upload.sh"
printf '%s\n' "$remote_upload_script" > "$REMOTE_UPLOAD_SCRIPT_FILE"
retry_ssh_script "$REMOTE_UPLOAD_SCRIPT_FILE"

log "Done"
