# Local WRF Setup Plan For Khawchin Thlirna

Last updated: 2026-05-28

Detailed revision marker: WRF-WPS-RUNBOOK-20260528

This guide is for running WRF locally on a Windows laptop and using it as an optional high-resolution helper for the existing Khawchin backend. The goal is not to replace the current backend setup, but to add local hill/valley detail where WRF proves useful.

## 1. Final Recommendation

Use this model structure:

```text
ECMWF IFS + ICON = main forecast backbone
AIFS = medium-range guidance layer
WRF local = terrain/rain-pocket/wind-direction correction layer
IMERG Late + station/proxy observations = verification and bias learning
```

Do not let WRF replace ECMWF/ICON at first. Run WRF in shadow mode, compare it with IMERG Late, then blend it only if it proves useful.

## 2. GFS Or ERA5 For WRF?

### Use GFS For Live Forecasts

For your app's daily/operational forecast, use GFS 0.25 degree GRIB2 as WRF initial and boundary input.

Why:

- GFS provides real forecast hours into the future.
- It is free and available operationally from NOAA NOMADS.
- WRF needs future lateral boundary conditions; GFS provides those.
- ERA5 is not designed as a live forecast boundary source.

NOAA NOMADS has a GFS 0.25 degree filter service and states that the GRIB subset tool can subset by time, field, level, and region.

Official source:

- NOAA NOMADS GFS 0.25 filter: https://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl
- NOAA/NCEP GFS products: https://www.nco.ncep.noaa.gov/pmb/products/gfs/

### Use ERA5 For Historical Testing / Calibration

ERA5 is excellent for historical case studies, hindcasts, and tuning WRF physics. It is not the right live daily input.

Why:

- ERA5 is a reanalysis, not an operational future forecast.
- Copernicus says ERA5 is available on 0.25 degree grids with atmospheric data on 37 pressure levels.
- Copernicus also says ERA5 daily updates are made available about 5 days behind real time.
- It is very useful for replaying past storms and checking whether WRF can reproduce local rain/wind patterns.

Official source:

- Copernicus ERA5 overview: https://climate.copernicus.eu/climate-reanalysis
- ERA5 pressure levels: https://cds.climate.copernicus.eu/datasets/reanalysis-era5-pressure-levels

### Practical Decision

Use both, but for different jobs:

```text
Live app forecast: GFS -> WRF
Historical tuning: ERA5 -> WRF hindcasts
Backend learning/verification: WRF forecast output vs IMERG Late
```

If you only have time for one source now, choose GFS.

## 3. Hardware Reality Check

Your laptop:

```text
Windows laptop
i7 CPU
16 GB RAM
```

Recommended first WRF setup:

```text
OS layer: WSL2 Ubuntu
Outer domain d01: 9 km
Optional nest d02: 3 km
Forecast length: 24-48 hours
Run frequency: 1x/day first
MPI tasks: 4-6
Memory for WSL2: 10-12 GB
```

Avoid at first:

```text
1 km domain
72+ hour forecast
Very large Bay of Bengal domain
Multiple nested domains beyond d02
```

A 3 km nest may work, but only after 9 km is stable.

## 4. WRF Role In Your Existing Backend

WRF should not be blended as a normal equal model at first.

Better method:

```text
ECMWF/ICON decide large-scale rain amount and timing.
WRF supplies local spatial adjustment.
Backend caps WRF impact.
IMERG verifies later.
```

Example:

```text
ECMWF/ICON regional rain = 8 mm
WRF says this hill grid is 1.35x wetter than nearby area
Final local rain = 8 mm * capped WRF factor
```

Suggested cap:

```text
WRF local rain factor min = 0.65
WRF local rain factor max = 1.45
```

Initial WRF weights:

```text
0-3h: 0% for rainfall, because WRF spin-up can be noisy
3-12h: 5-10%
12-36h: 10-15%
36-48h: 5-10%
Beyond 48h: 0-5%
```

Wind direction:

```text
Use vector blending, not degree averaging.
WRF wind direction weight: 10-25% where terrain channeling is important.
```

Temperature:

```text
Use low WRF weight first: 5-10%.
Do not use WRF temperature if terrain/elevation looks unrealistic.
```

Severe weather:

```text
WRF can increase confidence.
WRF should not trigger severe alerts alone during the first month.
```

## 5. Setup WSL2 On Windows

Open PowerShell as Administrator:

```powershell
wsl --install -d Ubuntu-22.04
wsl --set-default-version 2
```

Create or edit this file:

```text
C:\Users\Mapuia\.wslconfig
```
notepad "$env:USERPROFILE\.wslconfig"
Recommended content:

```ini
[wsl2]
memory=12GB
processors=6
swap=8GB
localhostForwarding=true
```

Restart WSL:

```powershell
wsl --shutdown
```

Open Ubuntu from Start Menu.

## 6. Install Linux Build Tools

Inside Ubuntu WSL:

```bash
sudo apt update
sudo apt upgrade -y

sudo apt install -y \
  build-essential gfortran gcc g++ make m4 perl csh tcsh \
  git wget curl unzip time file \
  python3 python3-pip python3-venv \
  mpich libmpich-dev \
  libnetcdf-dev libnetcdff-dev netcdf-bin \
  zlib1g-dev libpng-dev libjpeg-dev \
  libxml2-dev libcurl4-openssl-dev
```

Create project directory:

```bash
mkdir -p ~/wrf
cd ~/wrf
```

## 7. Download WRF And WPS

Use matched WRF and WPS versions. Start with a stable pinned version rather than chasing latest.

```bash
cd ~/wrf
git clone https://github.com/wrf-model/WRF.git
git clone https://github.com/wrf-model/WPS.git

cd ~/wrf/WRF
git checkout v4.6.0

cd ~/wrf/WPS
git checkout v4.6.0
```

Official sources:

- WRF releases: https://github.com/wrf-model/WRF/releases
- WPS repository: https://github.com/wrf-model/WPS
- WRF/WPS users guide: https://www2.mmm.ucar.edu/wrf/site/documentation/users_guide/

## 8. Compile WRF

```bash
cd ~/wrf/WRF

export NETCDF=/usr
export NETCDF_classic=1
export WRFIO_NCD_LARGE_FILE_SUPPORT=1

./configure
```

When prompted, choose a Linux gfortran `dmpar` option.

Then compile:

```bash
./compile em_real -j 6 >& compile.log
```

Check success:

```bash
ls -lh main/wrf.exe main/real.exe
```

If both files exist, WRF compiled successfully.

If compile fails, inspect:

```bash
tail -80 compile.log
```

## 9. Compile WPS

WRF should be compiled before WPS.

```bash
cd ~/wrf/WPS

export NETCDF=/usr
./configure --build-grib2-libs
```

Choose a Linux gfortran option. For your small first domain, `serial` or `dmpar` can work, but `dmpar` is better if available.

Important: GFS is GRIB2. WPS 4.4+ can build internal zlib, libpng, and JasPer libraries with `--build-grib2-libs`. This is more robust on Ubuntu/WSL than trying to find old JasPer packages manually.

Compile:

```bash
./compile >& compile.log
```

Check success:

```bash
ls -lh geogrid.exe ungrib.exe metgrid.exe
```

The official WPS guide says the WPS flow is:

```text
geogrid.exe -> ungrib.exe -> metgrid.exe
```

Then WRF uses:

```text
real.exe -> wrf.exe
```

Official WPS guide:

- https://www2.mmm.ucar.edu/wrf/site/documentation/users_guide/wps.html

## 10. Download WPS Geography Data

WRF/WPS needs static terrain, land-use, soil, albedo, green fraction, and other geography data before `geogrid.exe` can run.

Official source:

- https://www2.mmm.ucar.edu/wrf/site/access_code/geog_data.html
- https://www2.mmm.ucar.edu/wrf/users/download/get_sources_wps_geog.html

UCAR says the high-resolution mandatory package is recommended for most real applications, while the low-resolution package is mainly for testing/education. For Khawchin accuracy, use high-resolution if disk space allows it.

Check disk space first:

```bash
df -h ~
```

Recommended choice:

```text
First serious Khawchin run: high-resolution mandatory geography
Only compile/test run: low-resolution mandatory geography
```

Create the geography directory:

```bash
mkdir -p ~/wrf/WPS_GEOG
cd ~/wrf/WPS_GEOG
```

High-resolution mandatory data, recommended for real Khawchin runs:

```bash
cd ~/wrf/WPS_GEOG
wget -c https://www2.mmm.ucar.edu/wrf/src/wps_files/geog_high_res_mandatory.tar.gz
tar -xzf geog_high_res_mandatory.tar.gz
```

If `wget` is too slow or keeps timing out, download the same file with your Windows browser first. Then copy it from Windows Downloads into WSL:

```bash
cd ~/wrf/WPS_GEOG

ls -lh /mnt/c/Users/Mapuia/Downloads/*geog*mandatory*.tar.gz
cp "/mnt/c/Users/Mapuia/Downloads/geog_high_res_mandatory.tar.gz" .

ls -lh geog_high_res_mandatory.tar.gz
gzip -t geog_high_res_mandatory.tar.gz
tar -xzf geog_high_res_mandatory.tar.gz
```

If the browser saved it with a suffix such as `(1)`, use the actual filename:

```bash
cp "/mnt/c/Users/Mapuia/Downloads/geog_high_res_mandatory (1).tar.gz" geog_high_res_mandatory.tar.gz
```

Do not extract the tar file directly inside `/mnt/c/...`; copy it into `~/wrf/WPS_GEOG` first. Extracting inside the Linux filesystem is much faster and avoids Windows/WSL filesystem slowdowns.

Low-resolution mandatory data, only if disk/download size is a problem:

```bash
cd ~/wrf/WPS_GEOG
wget -c https://www2.mmm.ucar.edu/wrf/src/wps_files/geog_low_res_mandatory.tar.gz
tar -xzf geog_low_res_mandatory.tar.gz
```

After extraction, check what directory structure was created:

```bash
cd ~/wrf/WPS_GEOG
find . -maxdepth 2 -type f -name index | head -20
ls -lh
```

Usually `geog_data_path` should point to:

```text
/home/mapuia/wrf/WPS_GEOG
```

If the tar file created a nested `geog` directory, use this instead:

```text
/home/mapuia/wrf/WPS_GEOG/geog
```

Set a helper variable for your shell session. This command is correct and safe, but it only lasts in the current terminal window:

```bash
if [ -d "$HOME/wrf/WPS_GEOG/geog" ]; then
  export WPS_GEOG_PATH="$HOME/wrf/WPS_GEOG/geog"
else
  export WPS_GEOG_PATH="$HOME/wrf/WPS_GEOG"
fi

echo "$WPS_GEOG_PATH"
```

Expected output is one of these:

```text
/home/mapuia/wrf/WPS_GEOG
/home/mapuia/wrf/WPS_GEOG/geog
```

If you want this variable to be available every time you open Ubuntu, add it to `~/.bashrc`:

```bash
cat >> ~/.bashrc <<'EOF'

# WRF/WPS geography data path
if [ -d "$HOME/wrf/WPS_GEOG/geog" ]; then
  export WPS_GEOG_PATH="$HOME/wrf/WPS_GEOG/geog"
else
  export WPS_GEOG_PATH="$HOME/wrf/WPS_GEOG"
fi
EOF

source ~/.bashrc
echo "$WPS_GEOG_PATH"
```

Important low-resolution note:

```text
If using low-resolution geog data, do not use geog_data_res='default'.
Use geog_data_res='10m' for the first test.
```

For high-resolution geog data, `geog_data_res='default'` is fine.

## 11. First Domain Plan

This step is a planning/decision step, not a command step. You do not need to run anything here. The actual commands begin in Step 12.

Start with one simple domain. Do not start with a nest. The first goal is to prove the complete WPS -> WRF pipeline works.

Recommended first domain:

```text
Domain: d01 only
Resolution: 9 km
Forecast length: 24 hours first
Center latitude: 23.30
Center longitude: 93.30
Grid size: 120 x 110
Approximate coverage: Mizoram, Chin Hills, Kabaw Valley, nearby Bangladesh/Myanmar side, and moisture inflow corridor
```

Why this setup:

```text
9 km is light enough for a laptop.
120 x 110 is large enough to include upstream weather movement.
24h first run avoids wasting hours if namelist/domain settings are wrong.
```

For the first run, use one domain:

```text
max_dom = 1
dx = 9000
dy = 9000
e_we = 120
e_sn = 110
```

Later, after the 9 km run works, add a 3 km nested domain:

```text
d01 = 9 km outer domain
d02 = 3 km Khawchin focus domain
parent_grid_ratio = 3
```

Do not use the 3 km nest yet. It will be slower and harder to debug.

Suggested first physics:

```text
Microphysics: Thompson, mp_physics = 8
Longwave radiation: RRTMG, ra_lw_physics = 4
Shortwave radiation: RRTMG, ra_sw_physics = 4
Surface layer: revised MM5, sf_sfclay_physics = 1
Land surface: Noah LSM, sf_surface_physics = 2
PBL: YSU, bl_pbl_physics = 1
Cumulus: Kain-Fritsch for 9 km, cu_physics = 1
```

For future 3 km nest:

```text
d01 cu_physics = 1
d02 cu_physics = 0
```

Reason: cumulus parameterization is usually kept on for 9 km, but turned off for cloud-resolving 3 km nests.

What you should do now:

```text
Use the d01-only 9 km plan exactly as written.
Do not create d02 yet.
Copy the Step 12 namelist.wps template.
Then continue to Step 13 for matching GFS files.
```

## 12. Create First Run Directory And namelist.wps

Use a separate run directory for each GFS cycle. This keeps outputs clean.

The date in this guide is only an example. For a real run, choose one GFS cycle and use the same cycle everywhere:

```text
RUN_DATE = YYYYMMDD, for example 20260529
RUN_HH   = 00, 06, 12, or 18 UTC
RUN_ID   = YYYYMMDDHH, for example 2026052900
```

For the first manual test, choose the latest cycle that is definitely available. GFS cycles are in UTC and often appear a few hours after the cycle time. If unsure, use `00` UTC.

Set run variables. Change these when you run a different day/cycle:

```bash
export RUN_DATE=20260529
export RUN_HH=00
export RUN_ID="${RUN_DATE}${RUN_HH}"

export START_YMD="${RUN_DATE:0:4}-${RUN_DATE:4:2}-${RUN_DATE:6:2}"
export START_EPOCH=$(date -u -d "${START_YMD} ${RUN_HH}:00:00 UTC" +%s)
export END_EPOCH=$((START_EPOCH + 24 * 3600))
export START_WPS=$(date -u -d "@${START_EPOCH}" +%Y-%m-%d_%H:00:00)
export END_WPS=$(date -u -d "@${END_EPOCH}" +%Y-%m-%d_%H:00:00)

echo "RUN_ID=$RUN_ID"
echo "START_WPS=$START_WPS"
echo "END_WPS=$END_WPS"
```

Important: verify that `END_WPS` is later than `START_WPS`. For `RUN_DATE=20260529` and `RUN_HH=00`, expected output is:

```text
START_WPS=2026-05-29_00:00:00
END_WPS=2026-05-30_00:00:00
```

Create the run folder:

```bash
mkdir -p ~/wrf/runs/khawchin_gfs_${RUN_ID}
cd ~/wrf/runs/khawchin_gfs_${RUN_ID}
```

Link WPS executables and support folders:

```bash
ln -sf ~/wrf/WPS/geogrid.exe .
ln -sf ~/wrf/WPS/ungrib.exe .
ln -sf ~/wrf/WPS/metgrid.exe .
ln -sf ~/wrf/WPS/link_grib.csh .
ln -sfn ~/wrf/WPS/geogrid geogrid
ln -sfn ~/wrf/WPS/ungrib ungrib
ln -sfn ~/wrf/WPS/metgrid metgrid
```

Create `namelist.wps` for the first 24h run:

```bash
cat > namelist.wps <<'EOF'
&share
 wrf_core = 'ARW',
 max_dom = 1,
 start_date = 'START_WPS_PLACEHOLDER',
 end_date   = 'END_WPS_PLACEHOLDER',
 interval_seconds = 10800,
 io_form_geogrid = 2,
/

&geogrid
 parent_id         = 1,
 parent_grid_ratio = 1,
 i_parent_start    = 1,
 j_parent_start    = 1,
 e_we              = 120,
 e_sn              = 110,
 geog_data_res     = 'default',
 dx = 9000,
 dy = 9000,
 map_proj = 'mercator',
 ref_lat   = 23.30,
 ref_lon   = 93.30,
 truelat1  = 23.30,
 truelat2  = 0.0,
 stand_lon = 93.30,
 geog_data_path = '/home/mapuia/wrf/WPS_GEOG',
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

# Fill the dates from RUN_DATE/RUN_HH variables
sed -i "s/START_WPS_PLACEHOLDER/${START_WPS}/" namelist.wps
sed -i "s/END_WPS_PLACEHOLDER/${END_WPS}/" namelist.wps
```

If your geography data extracted into `/home/mapuia/wrf/WPS_GEOG/geog`, edit this line:

```bash
sed -i "s|geog_data_path = '/home/mapuia/wrf/WPS_GEOG'|geog_data_path = '/home/mapuia/wrf/WPS_GEOG/geog'|" namelist.wps
```

If you used low-resolution geography data, edit this line:

```bash
sed -i "s/geog_data_res     = 'default'/geog_data_res     = '10m'/" namelist.wps
```

Check the namelist:

```bash
cat namelist.wps
```

Common mistakes to avoid:

```text
start_date and end_date must match available GFS files.
interval_seconds must match GFS interval, usually 10800 for 3-hourly.
geog_data_path must be an absolute path, not ~/wrf/WPS_GEOG.
Do not use max_dom=2 until d01 works.
```

## 13. Download GFS Data

Use GFS 0.25 degree GRIB2 from NOAA NOMADS.

Official source:

- https://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl
- https://www.nco.ncep.noaa.gov/pmb/products/gfs/

For the first 24h run, download 3-hourly files from f000 to f024.

Use the same `RUN_DATE`, `RUN_HH`, and `RUN_ID` from Step 12. If you closed the terminal, set them again before downloading.

Important: a GFS cycle is not available immediately at the cycle time. For example, at `18:38 UTC`, the `18z` cycle may still return `404 Not Found`. In that case, use the previous cycle such as `12z`, or wait a few hours.

Before downloading all files, test whether the cycle exists.

Use a wide GFS subset for the first WRF runs. A tight subset such as `leftlon=88,rightlon=97,toplat=27,bottomlat=18` can look valid in `ungrib.exe` but fail later in `metgrid.exe` because WPS needs extra buffer around the WRF domain.

```bash
test_url="https://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?dir=%2Fgfs.${RUN_DATE}%2F${RUN_HH}%2Fatmos&file=gfs.t${RUN_HH}z.pgrb2.0p25.f000&all_lev=on&all_var=on&subregion=&leftlon=80&rightlon=106&toplat=35&bottomlat=5"
wget --spider "$test_url"
```

If it says `404 Not Found`, choose an older cycle:

```bash
export RUN_DATE=20260528
export RUN_HH=12
export RUN_ID="${RUN_DATE}${RUN_HH}"
```

Then recompute `START_WPS` and `END_WPS`, create a matching run folder, and use the matching GFS folder. The WPS namelist dates and the GFS cycle must match.

Create data folder:

```bash
mkdir -p ~/wrf/data/gfs/${RUN_ID}
cd ~/wrf/data/gfs/${RUN_ID}
```

Download regional GFS subset with a safe buffer:

```bash
for FFF in 000 003 006 009 012 015 018 021 024; do
  url="https://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?dir=%2Fgfs.${RUN_DATE}%2F${RUN_HH}%2Fatmos&file=gfs.t${RUN_HH}z.pgrb2.0p25.f${FFF}&all_lev=on&all_var=on&subregion=&leftlon=80&rightlon=106&toplat=35&bottomlat=5"
  wget -c -O "gfs.t${RUN_HH}z.pgrb2.0p25.f${FFF}.grib2" "$url"
done
```

Yes, for every new WRF forecast run you need new GFS files for that GFS cycle. For example, if you run WRF once per day, download one new GFS cycle per day. Later this can be automated with a script or Windows Task Scheduler/cron.

Check that files downloaded correctly:

```bash
ls -lh *.grib2
file *.grib2 | head
```

If any file is HTML instead of GRIB, the download failed:

```bash
grep -i "html\|error\|not found" *.grib2
```

If HTML/error appears, delete that file and download again.

If `metgrid.exe` later fails with this message:

```text
ERROR: Missing values encountered in interpolated fields. Stopping.
```

and the warnings mention grid points near the edge such as `(i,j)=(1,103)`, the GFS subset was too small. Download the same cycle again using the wider bounding box above, then rerun `link_grib.csh`, `ungrib.exe`, and `metgrid.exe`.

For a 48h run later, use:

```text
000 003 006 009 012 015 018 021 024 027 030 033 036 039 042 045 048
```

For current live date, change only:

```text
RUN_DATE
RUN_HH
RUN_ID-derived folders
namelist start_date/end_date
```

The GFS date is UTC, not Myanmar/India local time. Example: May 29 morning in Myanmar may still use the May 28 18 UTC or May 29 00 UTC cycle depending on availability.

## 14. Run WPS

Before running WPS, make sure the run folder and GFS folder use the same cycle.
For the current manual test, they should both be `2026052812`.

```bash
echo "RUN_ID=$RUN_ID"
pwd
grep -n "start_date\|end_date\|geog_data_path" namelist.wps
ls -lh ~/wrf/data/gfs/${RUN_ID}/gfs*.grib2
```

For the current `2026052812` test, expected key lines are:

```text
start_date = '2026-05-28_12:00:00'
end_date   = '2026-05-29_12:00:00'
geog_data_path = '/home/mapuia/wrf/WPS_GEOG'
```

If the dates do not match the GFS cycle, go back to Step 12 and recreate `namelist.wps`.

Go to the run folder:

```bash
cd ~/wrf/runs/khawchin_gfs_${RUN_ID}
```

Run `geogrid.exe`:

```bash
./geogrid.exe >& geogrid.log
```

Check geogrid success:

```bash
tail -40 geogrid.log
ls -lh geo_em.d01.nc
```

If `geo_em.d01.nc` does not exist, inspect errors:

```bash
grep -n "ERROR\|Error\|error\|Could not\|not found" geogrid.log | tail -80
```

Common geogrid fixes:

```text
If geography data missing: check geog_data_path.
If low-res data fails with default: use geog_data_res='10m'.
If path has ~ symbol: replace with absolute /home/mapuia/...
```

If geogrid says something like this:

```text
ERROR: Could not open /home/mapuia/wrf/WPS_GEOG/topo_gmted2010_30s/index
```

then WPS geography data is not extracted correctly, or `geog_data_path` points to the wrong folder.

Check the geography folder:

```bash
cd ~/wrf/WPS_GEOG
ls -lh | head -50
find . -maxdepth 3 -type f -name index | head -30
find . -maxdepth 3 -type d -name 'topo_gmted2010_30s' -o -name 'topo*' | head -30
```

You should see folders like:

```text
topo_gmted2010_30s
landuse_30s
soiltype_top_30s
soiltype_bot_30s
```

If these folders are inside a nested folder such as `~/wrf/WPS_GEOG/geog`, use that nested path in `namelist.wps`:

```bash
sed -i "s|geog_data_path = '.*'|geog_data_path = '/home/mapuia/wrf/WPS_GEOG/geog'|" namelist.wps
```

If the tarball extracted into `~/wrf/WPS_GEOG/WPS_GEOG`, which is common with this archive, use this path instead:

```bash
sed -i "s|geog_data_path = '.*'|geog_data_path = '/home/mapuia/wrf/WPS_GEOG/WPS_GEOG'|" namelist.wps
```

For permanent use, update `~/.bashrc` so future namelists use the correct nested path:

```bash
cat >> ~/.bashrc <<'EOF'

# WRF/WPS geography data path override
if [ -d "$HOME/wrf/WPS_GEOG/WPS_GEOG/topo_gmted2010_30s" ]; then
  export WPS_GEOG_PATH="$HOME/wrf/WPS_GEOG/WPS_GEOG"
elif [ -d "$HOME/wrf/WPS_GEOG/geog/topo_gmted2010_30s" ]; then
  export WPS_GEOG_PATH="$HOME/wrf/WPS_GEOG/geog"
else
  export WPS_GEOG_PATH="$HOME/wrf/WPS_GEOG"
fi
EOF

source ~/.bashrc
echo "$WPS_GEOG_PATH"
```

If these folders are missing, extract the geography tarball again:

```bash
cd ~/wrf/WPS_GEOG
tar -tzf geog_high_res_mandatory.tar.gz | head
tar -xzf geog_high_res_mandatory.tar.gz
```

If the tarball was downloaded in Windows Downloads by browser, copy or extract it from WSL like this:

```bash
cd ~/wrf/WPS_GEOG
cp /mnt/c/Users/Mapuia/Downloads/geog_high_res_mandatory.tar.gz .
tar -xzf geog_high_res_mandatory.tar.gz
```

Link GFS files:

```bash
./link_grib.csh ~/wrf/data/gfs/${RUN_ID}/gfs*.grib2
ls -lh GRIBFILE.* | head
```

If you used a separate retry folder such as `${RUN_ID}_wide`, link that folder instead:

```bash
./link_grib.csh ~/wrf/data/gfs/${RUN_ID}_wide/gfs*.grib2
```

Link the GFS variable table:

```bash
ln -sf ~/wrf/WPS/ungrib/Variable_Tables/Vtable.GFS Vtable
ls -lh Vtable
```

Run `ungrib.exe`:

```bash
./ungrib.exe >& ungrib.log
```

Check ungrib success:

```bash
tail -40 ungrib.log
ls -lh FILE:* | head
```

If `FILE:*` does not exist, inspect errors:

```bash
grep -n "ERROR\|Error\|error\|Unknown Data\|not found" ungrib.log | tail -80
```

Run `metgrid.exe`:

```bash
./metgrid.exe >& metgrid.log
```

Check metgrid success:

```bash
tail -40 metgrid.log
ls -lh met_em.d01.*.nc | head
ls -lh met_em.d01.*.nc | tail
```

Expected output for 24h 3-hourly run:

```text
9 met_em files:
00, 03, 06, 09, 12, 15, 18, 21, 24 hours
```

Count them:

```bash
ls met_em.d01.*.nc | wc -l
```

Check land category value for `namelist.input` later:

```bash
ncdump -h met_em.d01.${START_WPS}.nc | grep -i NUM_LAND_CAT
```

Write down the value. It is often 21 for MODIS land-use.

## 15. Run WRF

Use the WRF run directory created during compilation:

```bash
cd ~/wrf/WRF/run
```

Clean old run artifacts, but do not delete model executables or static tables:

```bash
rm -f met_em.d0*.nc wrfinput_d0* wrfbdy_d01 wrfout_d0* rsl.out.* rsl.error.*
```

Link metgrid output from WPS:

```bash
ln -sf ~/wrf/runs/khawchin_gfs_${RUN_ID}/met_em.d01.*.nc .
ls -lh met_em.d01.*.nc | head
```

Create a simple 24h `namelist.input` for d01 only.

Use the same `RUN_DATE` and `RUN_HH` as WPS. For the current manual test:

```bash
export RUN_DATE=20260528
export RUN_HH=12
export RUN_ID="${RUN_DATE}${RUN_HH}"

export START_YMD="${RUN_DATE:0:4}-${RUN_DATE:4:2}-${RUN_DATE:6:2}"
export START_EPOCH=$(date -u -d "${START_YMD} ${RUN_HH}:00:00 UTC" +%s)
export END_EPOCH=$((START_EPOCH + 24 * 3600))

export START_YEAR=$(date -u -d "@${START_EPOCH}" +%Y)
export START_MONTH=$(date -u -d "@${START_EPOCH}" +%m)
export START_DAY=$(date -u -d "@${START_EPOCH}" +%d)
export START_HOUR=$(date -u -d "@${START_EPOCH}" +%H)

export END_YEAR=$(date -u -d "@${END_EPOCH}" +%Y)
export END_MONTH=$(date -u -d "@${END_EPOCH}" +%m)
export END_DAY=$(date -u -d "@${END_EPOCH}" +%d)
export END_HOUR=$(date -u -d "@${END_EPOCH}" +%H)

cp namelist.input namelist.input.backup.$(date +%Y%m%d_%H%M%S) 2>/dev/null || true

cat > namelist.input <<EOF
&time_control
 run_days                            = 0,
 run_hours                           = 24,
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
 time_step                           = 54,
 time_step_fract_num                 = 0,
 time_step_fract_den                 = 1,
 max_dom                             = 1,
 e_we                                = 120,
 e_sn                                = 110,
 e_vert                              = 40,
 p_top_requested                     = 5000,
 num_metgrid_levels                  = 34,
 num_metgrid_soil_levels             = 4,
 dx                                  = 9000,
 dy                                  = 9000,
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
 radt                                = 9,
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
 num_land_cat                        = 21,
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
```

Verify that WRF dates match the metgrid files:

```bash
grep -n "start_year\|start_month\|start_day\|start_hour\|end_year\|end_month\|end_day\|end_hour" namelist.input
ls -lh met_em.d01.*.nc | head
ls -lh met_em.d01.*.nc | tail
```

If `ncdump` showed `NUM_LAND_CAT = 24`, change this line:

```bash
sed -i 's/num_land_cat                        = 21/num_land_cat                        = 24/' namelist.input
```

Choose MPI process count. On a 16 GB laptop, start with 4 processes. If WSL exposes fewer cores, use that number.

```bash
nproc
export WRF_NP=4
printf "localhost slots=%s\n" "$(nproc)" > ~/wrf/mpi_hostfile
```

If `nproc` returns `2`, use:

```bash
export WRF_NP=2
printf "localhost slots=%s\n" "$(nproc)" > ~/wrf/mpi_hostfile
```

Quick MPI slot test:

```bash
mpirun --hostfile ~/wrf/mpi_hostfile -np ${WRF_NP} hostname
```

Run `real.exe` first:

```bash
mpirun --hostfile ~/wrf/mpi_hostfile -np ${WRF_NP} ./real.exe >& real.log
```

Check `real.exe` success:

```bash
tail -50 real.log
ls -lh wrfinput_d01 wrfbdy_d01
```

Also check MPI logs:

```bash
grep -n "SUCCESS COMPLETE REAL_EM INIT" rsl.out.0000 rsl.error.0000 2>/dev/null
```

If `real.exe` fails:

```bash
grep -n "FATAL\|ERROR\|Error\|error" real.log rsl.error.0000 rsl.out.0000 | tail -120
```

If `real.exe` only shows `MPI_ABORT`, the real error is usually in the `rsl.*` files. Inspect them:

```bash
ls -lh rsl.error.* rsl.out.* | head
tail -120 rsl.error.0000
tail -120 rsl.out.0000
grep -Rni "fatal\|error\|not found\|mismatch\|namelist\|input_from_file" rsl.error.* rsl.out.* real.log | tail -160
```

For this first test, the most common causes are:

```text
namelist.input dates do not match met_em files.
num_land_cat does not match NUM_LAND_CAT in met_em.
num_metgrid_levels does not match met_em vertical levels.
```

If the log says it is trying to open an old sample file, for example:

```text
error opening met_em.d01.2019-09-04_12:00:00.nc
```

then `namelist.input` was not overwritten with the Khawchin run dates. Re-run the `namelist.input` creation block in Step 15 and confirm `start_year=2026`, `start_hour=12`, `max_dom=1`, `dx=9000`, and `dy=9000`.

Run `wrf.exe` only after `wrfinput_d01` and `wrfbdy_d01` exist:

```bash
mpirun --hostfile ~/wrf/mpi_hostfile -np ${WRF_NP} ./wrf.exe >& wrf.log
```

WRF runtime expectation on an i7 laptop with 16 GB RAM:

```text
9 km d01, 120 x 110, 24-hour forecast:
Usually around 30 minutes to 2 hours.
On a busy/thermal-throttled laptop it can take longer.
```

Do not judge failure just because the terminal waits silently. `wrf.exe` usually writes progress to `rsl.out.0000`, not to the terminal.

Monitor progress from another terminal:

```bash
cd ~/wrf/WRF/run
tail -f rsl.out.0000
```

Check whether output has started:

```bash
ls -lh wrfout_d01_*
```

For this guide, `history_interval = 60`, so WRF should write one output file per simulated hour. If no new log lines appear for 15-20 minutes, inspect errors:

```bash
tail -120 rsl.error.0000
grep -Rni "fatal\|error\|cfl\|segmentation\|killed" rsl.error.* rsl.out.* wrf.log | tail -160
```

Check WRF success:

```bash
tail -60 wrf.log
ls -lh wrfout_d01_*
grep -n "SUCCESS COMPLETE WRF" rsl.out.0000 rsl.error.0000 2>/dev/null
```

Expected first output:

```text
wrfout_d01_2026-05-28_12:00:00
wrfout_d01_2026-05-28_13:00:00
...
```

If WRF is too slow, stop after confirming it starts and writes the first output. Later reduce `run_hours` or domain size.

For a quick startup test only, reduce `run_hours` to 3:

```bash
sed -i 's/run_hours                           = 24/run_hours                           = 3/' namelist.input
```

Then rerun `real.exe` and `wrf.exe`. For real forecasts, set it back to 24.

Common first-run fixes:

```text
If CFL errors appear: reduce time_step from 54 to 36.
If missing met_em files: check namelist start/end times and GFS hours.
If num_land_cat error appears: set num_land_cat to the NUM_LAND_CAT from met_em.
If MPI complains about root: do not run as root; use normal WSL user.
If Open MPI says not enough slots: create `~/wrf/mpi_hostfile` with `localhost slots=$(nproc)` and use `--hostfile ~/wrf/mpi_hostfile`.
If memory pressure is high: use `WRF_NP=2` or `WRF_NP=4`.
Avoid `--oversubscribe` for normal WRF runs unless hostfile still fails and you are only testing startup.
```

## 16. Postprocess WRF To Backend JSON

Do not upload full WRF NetCDF files to EC2. They are too large.

Extract only your forecast grid cells.

Variables to extract:

```text
T2 = 2m temperature in K
U10/V10 = 10m wind components
RAINC + RAINNC = accumulated precipitation
PSFC = surface pressure
Q2 = 2m water vapor mixing ratio, if needed
XLAT/XLONG = grid coordinates
```

Convert:

```text
temperature_c = T2 - 273.15
rain_hourly_mm = diff(RAINC + RAINNC)
wind_speed_kmh = sqrt(U10^2 + V10^2) * 3.6
wind_dir_deg = meteorological direction from U/V
pressure_hpa = PSFC / 100
```

Target JSON:

```json
{
  "source": "wrf_gfs_local_d02_3km",
  "run_id": "wrf_20260527_00",
  "run_time_utc": "2026-05-27T00:00:00Z",
  "domain": "d02",
  "grid": {
    "23.50_93.20": {
      "lat": 23.5,
      "lon": 93.2,
      "times": ["2026-05-27T06:00:00Z"],
      "temp_2m_c": [24.1],
      "precip_mm": [2.4],
      "wind_kmh": [12.5],
      "wind_dir_deg": [210],
      "pressure_hpa": [1006.2],
      "confidence": 0.35
    }
  }
}
```

Recommended Step 16 commands (WSL):

```bash
sudo apt update
sudo apt install -y python3-numpy python3-netcdf4

python3 /mnt/c/Users/Mapuia/AndroidStudioProjects/KhawchinThlirna/codex_backend_work/wrf_local_to_json.py \
  --wrf-dir ~/wrf/WRF/run \
  --pattern 'wrfout_d01_*' \
  --domain d01 \
  --source wrf_gfs_local_d01_9km \
  --run-id wrf_YYYYMMDD_HH \
  --output ~/wrf/output/json/archive/wrf_local_YYYYMMDDHH.json

cp ~/wrf/output/json/archive/wrf_local_YYYYMMDDHH.json ~/wrf/output/json/wrf_local_latest.json
```

If you later run a d02 nest, change the pattern, domain, and source:

```text
pattern = wrfout_d02_*
domain  = d02
source  = wrf_gfs_local_d02_3km
```

## 17. Upload JSON To EC2

From Windows PowerShell or WSL:

```bash
scp ~/wrf/output/json/wrf_local_latest.json ubuntu@YOUR_EC2_IP:/opt/khawchin/cache/wrf_local_latest.json.tmp
ssh ubuntu@YOUR_EC2_IP "mv /opt/khawchin/cache/wrf_local_latest.json.tmp /opt/khawchin/cache/wrf_local_latest.json"
```

If you want to use FileZilla instead of scp:

1. Copy the JSON from WSL to Windows so FileZilla can see it:

```bash
cp ~/wrf/output/json/wrf_local_latest.json /mnt/c/Users/Mapuia/Downloads/
```

2. In FileZilla, connect with SFTP and upload `wrf_local_latest.json` to your EC2 home folder (for example `/home/ubuntu/`).

3. Move it into the backend cache path with SSH:

```bash
ssh ubuntu@YOUR_EC2_IP
sudo mv /home/ubuntu/wrf_local_latest.json /opt/khawchin/cache/wrf_local_latest.json
sudo chmod 644 /opt/khawchin/cache/wrf_local_latest.json
```

Backend should read:

```text
/opt/khawchin/cache/wrf_local_latest.json
```

Keep a history archive too. Weekly verification cannot compare WRF vs IMERG if only `wrf_local_latest.json` is kept, because the latest file is overwritten every day.

Recommended EC2 paths:

```text
/opt/khawchin/cache/wrf_local_latest.json
/opt/khawchin/cache/wrf_archive/wrf_local_2026052812.json
/opt/khawchin/cache/wrf_archive/wrf_local_2026052912.json
```

The daily script `C:\Users\Mapuia\AndroidStudioProjects\KhawchinThlirna\docs\daily_wrf__run.sh` now uploads both:

```text
latest file  -> /opt/khawchin/cache/wrf_local_latest.json
archive file -> /opt/khawchin/cache/wrf_archive/wrf_local_${RUN_ID}.json
```

Recommended backend env later:

```bash
WRF_LOCAL_ENABLE=1
WRF_LOCAL_FILE=/opt/khawchin/cache/wrf_local_latest.json
WRF_LOCAL_MAX_AGE_HOURS=26
WRF_LOCAL_MIN_COVERAGE=0.75
WRF_LOCAL_BASE_WEIGHT=0.12
WRF_LOCAL_SHADOW_MODE=1
```

WRF_LOCAL_ENABLE=1 \
WRF_LOCAL_FILE=/opt/khawchin/cache/wrf_local_latest.json \
WRF_LOCAL_MAX_AGE_HOURS=26 \
WRF_LOCAL_MIN_COVERAGE=0.75 \
WRF_LOCAL_BASE_WEIGHT=0.12 \
WRF_LOCAL_SHADOW_MODE=1 \
/opt/khawchin/venv/bin/python /opt/khawchin/backend_v86.py --dry-run --limit 1

Start with shadow mode:

```text
WRF_LOCAL_SHADOW_MODE=1
```

This means backend reads WRF and stores diagnostics, but public forecast is not changed yet.

## 18. Verification Plan

Run WRF in shadow mode for at least 2-4 weeks.

Compare:

```text
WRF vs IMERG Late
ECMWF/ICON vs IMERG Late
WRF spatial rain pocket vs satellite nowcast
WRF wind direction vs app/station/proxy/crowd reports if available
```

Metrics:

```text
Rain MAE
Heavy-rain hit rate
False alarm rate
Timing error
Wind direction error
Regional bias by terrain zone
```

Only increase WRF weight if it improves at least one important metric without making false alarms worse.

### Weekly Compare Job

Goal:

```text
WRF raw rain vs IMERG Late
ECMWF IFS raw rain vs IMERG Late
ICON raw rain vs IMERG Late
Backend final blend vs IMERG Late
```

Important detail:

```text
IMERG time, WRF time, ECMWF/ICON time must all be matched in UTC.
WRF rain is an accumulated-difference value, so compare the WRF valid hour with the matching IMERG hour/rate.
For decision-making, 3-hour and 24-hour totals are more stable than one exact convective hour.
```

The backend has been updated so new forecast snapshots can store raw model rain:

```text
forecast_snapshots/{grid_id}/runs/{run_id}
  precip_mm                      = backend final/blended rain
  model_precip_mm.ecmwf_ifs      = raw ECMWF IFS rain
  model_precip_mm.icon_seamless  = raw ICON rain
```

Old snapshots will not have `model_precip_mm`; raw ECMWF/ICON rows begin appearing after the updated backend is deployed and a few full runs complete.

Copy the weekly compare script to EC2:

```bash
scp /mnt/c/Users/Mapuia/AndroidStudioProjects/KhawchinThlirna/codex_backend_work/weekly_model_compare.py \
  ubuntu@YOUR_EC2_IP:/home/ubuntu/weekly_model_compare.py

ssh ubuntu@YOUR_EC2_IP
sudo mv /home/ubuntu/weekly_model_compare.py /opt/khawchin/weekly_model_compare.py
sudo chmod 755 /opt/khawchin/weekly_model_compare.py
sudo mkdir -p /opt/khawchin/reports
```

Manual weekly report test:

```bash
cd /opt/khawchin
SERVICE_ACCOUNT_PATH=/opt/khawchin/serviceAccountKey.json \
GOOGLE_APPLICATION_CREDENTIALS=/opt/khawchin/serviceAccountKey.json \
/opt/khawchin/venv/bin/python /opt/khawchin/weekly_model_compare.py \
  --days 7 \
  --wrf-archive-dir /opt/khawchin/cache/wrf_archive \
  --out-dir /opt/khawchin/reports
```

Expected output:

```text
/opt/khawchin/reports/weekly_model_compare_latest.json
/opt/khawchin/reports/weekly_model_compare_latest.md
```

Recommended weekly cron on EC2:

```cron
# Weekly WRF/model verification against IMERG Late
30 4 * * 1 cd /opt/khawchin && SERVICE_ACCOUNT_PATH=/opt/khawchin/serviceAccountKey.json GOOGLE_APPLICATION_CREDENTIALS=/opt/khawchin/serviceAccountKey.json /opt/khawchin/venv/bin/python /opt/khawchin/weekly_model_compare.py --days 7 --wrf-archive-dir /opt/khawchin/cache/wrf_archive --out-dir /opt/khawchin/reports >> /opt/khawchin/logs/weekly_compare.log 2>&1
```

Use the weekly report like this:

```text
Lower MAE/RMSE = better rain amount.
Bias > 0 = forecast too wet.
Bias < 0 = forecast too dry.
Higher CSI = better event detection.
Heavy CSI matters more than ordinary rain CSI for alerts.
If WRF improves heavy CSI but adds many false alarms, keep shadow mode.
If WRF improves heavy CSI and false alarms stay stable, try WRF_LOCAL_SHADOW_MODE=0 with WRF_LOCAL_BASE_WEIGHT=0.05 first.
```

## 19. Backend Blend Logic Later

Recommended backend logic:

```text
If WRF file is missing/stale -> ignore WRF.
If WRF coverage < 75% -> ignore WRF.
If WRF run age > 26h -> ignore WRF for a once-daily 24h WRF cycle.
If WRF lead hour < 3h -> do not use WRF rain.
If WRF is shadow mode -> store diagnostics only.
```

Why `WRF_LOCAL_MAX_AGE_HOURS=26` is acceptable for this setup:

```text
The laptop currently produces one 24-hour WRF forecast per day.
The backend full jobs run multiple times across the day.
At the last full job before the next WRF cycle, the WRF run can be 22-24h old.
26h gives a small operational buffer for upload delay and clock/schedule drift.
In shadow mode it cannot change public forecasts.
When shadow mode is off, WRF still only adjusts hours whose timestamps exactly match the WRF JSON.
```

If WRF is later run twice daily or the backend starts using WRF live, reduce this toward `18` hours or add a forecast-end-time freshness check.

Rain blend:

```text
core_rain = ECMWF/ICON blended rain
wrf_local_factor = WRF cell rain / WRF nearby-domain mean rain
wrf_local_factor = clamp(wrf_local_factor, 0.65, 1.45)
final_rain = core_rain * (1 + WRF_WEIGHT * (wrf_local_factor - 1))
```

Wind blend:

```text
Blend U/V vectors, not degrees.
```

Temperature blend:

```text
Small additive correction only.
Cap correction at +/- 2 C first.
```

Severe alert:

```text
WRF can boost confidence if ECMWF/ICON already show risk.
WRF alone should not trigger ORANGE/RED during testing.
```

## 20. When To Add ERA5

Add ERA5 later for historical experiments.

Use cases:

```text
Replay known severe rain/thunderstorm days.
Tune WRF physics.
Estimate which terrain zones benefit from WRF.
Create skill weights before using WRF live.
```

Do not use ERA5 as the operational live boundary source for tomorrow's forecast.

## 21. Troubleshooting Checklist

If WRF compile fails:

```bash
tail -100 ~/wrf/WRF/compile.log
tail -100 ~/wrf/WPS/compile.log
```

If WPS fails:

```bash
cat geogrid.log | tail -80
cat ungrib.log | tail -80
cat metgrid.log | tail -80
```

If `real.exe` fails:

```bash
tail -100 rsl.error.0000
```

If `wrf.exe` fails:

```bash
tail -100 rsl.error.0000
```

If laptop becomes too slow:

```text
Reduce domain size.
Run 24h instead of 48h.
Use only d01 9 km.
Use fewer output variables.
Use 3-hourly GFS instead of hourly.
```

## 22. Recommended Phases

### Phase 1 - Build And Run

```text
Compile WRF/WPS.
Run 9 km d01 for 24h.
Do not connect to backend yet.
```

### Phase 2 - Local JSON Export

```text
Extract 303 grid cells.
Generate wrf_local_latest.json.
Validate units and timestamps.
```

### Phase 3 - Backend Shadow Mode

```text
Upload JSON to EC2.
Backend reads WRF but public forecast unchanged.
Store WRF diagnostics.
```

### Phase 4 - Skill Review

```text
Compare WRF to IMERG for 2-4 weeks.
Check false alarms.
Check hill/valley signal.
```

### Phase 5 - Conservative Blend

```text
Enable WRF low weight.
Keep caps.
Keep fallback.
Monitor logs.
```

## 23. Final Decision Summary

For your Khawchin app:

```text
Best live WRF input: GFS 0.25 degree
Best historical/tuning input: ERA5
Best backend use: WRF as local correction, not main model
Best first resolution: 9 km, then 3 km nest
Best first mode: shadow mode
```

This is the safest path to improve accuracy without damaging the already-working ECMWF/ICON backend.

## 24. Daily Operational Workflow

After the first manual test works, daily runs should follow this repeatable pattern. This section is the short operational guide to revisit later.

### Daily Rule

```text
Everything is UTC.
Do not use Myanmar/India local clock for RUN_DATE/RUN_HH.
RUN_ID = RUN_DATE + RUN_HH.
GFS folder, WPS namelist, met_em files, and WRF namelist must all use the same cycle.
```

For a reliable daily run, choose a GFS cycle at least 6 hours older than current UTC:

```text
UTC 00-05: use previous day's 18z, or previous day's 12z if 18z is unavailable.
UTC 06-11: use current day's 00z.
UTC 12-17: use current day's 06z.
UTC 18-23: use current day's 12z. Use 18z only after checking it exists.
```

Example from the first successful test:

```text
RUN_DATE=20260528
RUN_HH=12
RUN_ID=2026052812
START_WPS=2026-05-28_12:00:00
END_WPS=2026-05-29_12:00:00
Expected met_em files:
met_em.d01.2026-05-28_12:00:00.nc
...
met_em.d01.2026-05-29_12:00:00.nc
WRF namelist.input must also start at 2026-05-28 12 and end at 2026-05-29 12.
```

Automatic safe-cycle chooser:

```bash
# Pick a GFS cycle that is at least 6 hours older than current UTC.
SAFE_EPOCH=$(($(date -u +%s) - 6 * 3600))
SAFE_HOUR=$(date -u -d "@${SAFE_EPOCH}" +%H)
CYCLE_HOUR=$((10#${SAFE_HOUR} / 6 * 6))

export RUN_DATE=$(date -u -d "@${SAFE_EPOCH}" +%Y%m%d)
printf -v RUN_HH "%02d" "${CYCLE_HOUR}"
export RUN_ID="${RUN_DATE}${RUN_HH}"

export START_YMD="${RUN_DATE:0:4}-${RUN_DATE:4:2}-${RUN_DATE:6:2}"
export START_EPOCH=$(date -u -d "${START_YMD} ${RUN_HH}:00:00 UTC" +%s)
export END_EPOCH=$((START_EPOCH + 24 * 3600))
export START_WPS=$(date -u -d "@${START_EPOCH}" +%Y-%m-%d_%H:00:00)
export END_WPS=$(date -u -d "@${END_EPOCH}" +%Y-%m-%d_%H:00:00)

echo "RUN_ID=$RUN_ID"
echo "START_WPS=$START_WPS"
echo "END_WPS=$END_WPS"
```

If this selected cycle still gives `404 Not Found`, manually move back one cycle:

```bash
# Example fallback only. Change RUN_DATE too if crossing midnight UTC.
export RUN_HH=12
export RUN_ID="${RUN_DATE}${RUN_HH}"
```

After changing `RUN_DATE` or `RUN_HH`, rerun the `START_YMD` through `END_WPS` date-calculation block above before recreating `namelist.wps`.

### Daily Step 1 - Create Run Folder

```bash
mkdir -p ~/wrf/runs/khawchin_gfs_${RUN_ID}
cd ~/wrf/runs/khawchin_gfs_${RUN_ID}

ln -sf ~/wrf/WPS/geogrid.exe .
ln -sf ~/wrf/WPS/ungrib.exe .
ln -sf ~/wrf/WPS/metgrid.exe .
ln -sf ~/wrf/WPS/link_grib.csh .
ln -sfn ~/wrf/WPS/geogrid geogrid
ln -sfn ~/wrf/WPS/ungrib ungrib
ln -sfn ~/wrf/WPS/metgrid metgrid
```

### Daily Step 2 - Create `namelist.wps`

Recreate `namelist.wps` every run, because the dates change:

```bash
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
 e_we              = 120,
 e_sn              = 110,
 geog_data_res     = 'default',
 dx = 9000,
 dy = 9000,
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
```

Check time matching:

```bash
grep -n "start_date\|end_date\|geog_data_path" namelist.wps
echo "RUN_ID=$RUN_ID"
echo "START_WPS=$START_WPS"
echo "END_WPS=$END_WPS"
```

### Daily Step 3 - Download GFS

Use the wide box from Step 13. For a daily 24-hour forecast, download `f000` to `f024`.

```bash
mkdir -p ~/wrf/data/gfs/${RUN_ID}
cd ~/wrf/data/gfs/${RUN_ID}

for FFF in 000 003 006 009 012 015 018 021 024; do
  url="https://nomads.ncep.noaa.gov/cgi-bin/filter_gfs_0p25.pl?dir=%2Fgfs.${RUN_DATE}%2F${RUN_HH}%2Fatmos&file=gfs.t${RUN_HH}z.pgrb2.0p25.f${FFF}&all_lev=on&all_var=on&subregion=&leftlon=80&rightlon=106&toplat=35&bottomlat=5"
  wget -c -O "gfs.t${RUN_HH}z.pgrb2.0p25.f${FFF}.grib2" "$url"
done

ls -lh *.grib2
file *.grib2 | head
grep -i "html\|error\|not found" *.grib2
```

If `grep` returns anything, the cycle or download failed. Choose an older cycle or rerun the failed file.

### Daily Step 4 - Run WPS

```bash
cd ~/wrf/runs/khawchin_gfs_${RUN_ID}

rm -f GRIBFILE.* FILE:* PFILE:* met_em.d01.*.nc

./geogrid.exe >& geogrid.log
tail -30 geogrid.log
ls -lh geo_em.d01.nc

./link_grib.csh ~/wrf/data/gfs/${RUN_ID}/gfs*.grib2
ln -sf ~/wrf/WPS/ungrib/Variable_Tables/Vtable.GFS Vtable

./ungrib.exe >& ungrib.log
tail -30 ungrib.log
ls -lh FILE:* | head

./metgrid.exe >& metgrid.log
tail -40 metgrid.log
ls met_em.d01.*.nc | wc -l
```

Expected for a 24-hour, 3-hourly run:

```text
9
```

### Daily Step 5 - Time Match Check

Before running WRF, confirm the WPS output starts and ends at the correct UTC times:

```bash
cd ~/wrf/runs/khawchin_gfs_${RUN_ID}
ls met_em.d01.*.nc | head -1
ls met_em.d01.*.nc | tail -1
```

The first file must match `START_WPS`; the last file must match `END_WPS`.

### Daily Step 6 - Run `real.exe`

```bash
cd ~/wrf/WRF/run

rm -f met_em.d0*.nc wrfinput_d0* wrfbdy_d01 wrfout_d0* rsl.out.* rsl.error.* real.log wrf.log
ln -sf ~/wrf/runs/khawchin_gfs_${RUN_ID}/met_em.d01.*.nc .

export START_YEAR=$(date -u -d "@${START_EPOCH}" +%Y)
export START_MONTH=$(date -u -d "@${START_EPOCH}" +%m)
export START_DAY=$(date -u -d "@${START_EPOCH}" +%d)
export START_HOUR=$(date -u -d "@${START_EPOCH}" +%H)
export END_YEAR=$(date -u -d "@${END_EPOCH}" +%Y)
export END_MONTH=$(date -u -d "@${END_EPOCH}" +%m)
export END_DAY=$(date -u -d "@${END_EPOCH}" +%d)
export END_HOUR=$(date -u -d "@${END_EPOCH}" +%H)
```

Create `namelist.input` using Step 15. Then check:

```bash
grep -n "start_year\|start_month\|start_day\|start_hour\|end_year\|end_month\|end_day\|end_hour\|max_dom\|dx\|dy" namelist.input
ls met_em.d01.*.nc | head -1
ls met_em.d01.*.nc | tail -1
```

Run `real.exe`:

```bash
export WRF_NP=4
printf "localhost slots=%s\n" "$(nproc)" > ~/wrf/mpi_hostfile
mpirun --hostfile ~/wrf/mpi_hostfile -np ${WRF_NP} ./real.exe >& real.log

tail -50 real.log
ls -lh wrfinput_d01 wrfbdy_d01
grep -n "SUCCESS COMPLETE REAL_EM INIT" rsl.out.0000 rsl.error.0000 2>/dev/null
```

### Daily Step 7 - Run `wrf.exe`

```bash
rm -f rsl.out.* rsl.error.* wrf.log wrfout_d01_*

mpirun --hostfile ~/wrf/mpi_hostfile -np ${WRF_NP} ./wrf.exe >& wrf.log
```

Expected wait time:

```text
For the current 9 km 24-hour d01 setup: usually 30 minutes to 2 hours.
If the laptop is hot, busy, or power-saving, it can take longer.
```

Monitor progress:

```bash
tail -f rsl.out.0000
```

Success check:

```bash
tail -60 wrf.log
ls -lh wrfout_d01_*
grep -n "SUCCESS COMPLETE WRF" rsl.out.0000 rsl.error.0000 2>/dev/null
```

### Daily Checklist Summary

```text
1. Pick UTC GFS cycle.
2. Compute START_WPS and END_WPS.
3. Create run folder and namelist.wps.
4. Download wide-box GFS.
5. Run geogrid, ungrib, metgrid.
6. Confirm first/last met_em times.
7. Link met_em into WRF/run.
8. Create namelist.input with exactly matching dates.
9. Run real.exe.
10. Run wrf.exe and wait.
```

The helper script `docs/daily_wrf__run.sh` automates these steps. It now includes:

```text
single-run lock with flock
automatic WPS_GEOG_PATH detection
wide-box GFS download
metgrid file-count check
EC2_HOST/EC2_KEY override through environment variables
```

Example manual run:

```bash
bash /mnt/c/Users/Mapuia/AndroidStudioProjects/KhawchinThlirna/docs/daily_wrf__run.sh
```

Example fixed-cycle rerun:

```bash
RUN_DATE=20260528 RUN_HH=12 bash /mnt/c/Users/Mapuia/AndroidStudioProjects/KhawchinThlirna/docs/daily_wrf__run.sh
```

What changes each day:

```text
RUN_DATE
RUN_HH
RUN_ID
namelist.wps start_date/end_date
GFS input folder
WRF run folder links
```

What does not change each day:

```text
WRF/WPS compiled executables
WPS geography data
Domain center/resolution
Most physics settings
```
### WRF laptap atanga manual runna (auto in a in run ang)
bash ~/wrf/scripts/wrf_local_to_json.sh
WRF_PROFILE=9km RUN_HOURS=24 WRF_NP=6 bash ~/wrf/scripts/daily_wrf_run.sh

### 3km run dan
WRF_PROFILE=3km RUN_HOURS=24 WRF_NP=6 bash ~/wrf/scripts/daily_wrf_run.sh

nano ~/wrf/scripts/daily_wrf_run.sh

### Neated Run a auto shutdown dan
WRF_PROFILE=nested RUN_HOURS=24 WRF_NP=6 bash ~/wrf/scripts/daily_wrf_run.sh && powershell.exe -Command "Stop-Computer -Force"

RUN_DATE=$(date -u +%Y%m%d) RUN_HH=12 WRF_PROFILE=nested RUN_HOURS=36 WRF_NP=10 bash ~/wrf/scripts/daily_wrf_run.sh && cmd.exe /c shutdown /s /t 120

-d Ubuntu -- bash -lc "source ~/.bashrc; RUN_DATE=$(date -u +%Y%m%d) RUN_HH=12 WRF_PROFILE=nested RUN_HOURS=36 WRF_NP=10 bash ~/wrf/scripts/daily_wrf_run.sh && cmd.exe /c shutdown /s /t 120

### check lehna
LATEST_LOG_DIR=$(ls -td ~/wrf/runs/khawchin_gfs_*/logs/* | head -1)
echo "$LATEST_LOG_DIR"
tail -80 "$LATEST_LOG_DIR/daily_wrf_run.log" 2>/dev/null || ls -lh "$LATEST_LOG_DIR"

### wrf log enna
ls -lh wrf_run/real_rsl | head
ls -lh wrf_run/wrf_rsl | head
tail -120 wrf_run/wrf_rsl/rsl.error.0000

tail -f ~/wrf/WRF/run/rsl.out.0000

### log enna
cd ~/wrf/runs/khawchin_gfs_2026060200/latest_logs
tail -120 daily_wrf_run.log
tail -120 geogrid.log
tail -120 ungrib.log
tail -120 metgrid.log
tail -120 wrf_run/real.log
tail -120 wrf_run/wrf.log

sudo systemctl daemon-reload
sudo systemctl restart khawchin-api.service
sudo systemctl status khawchin-api.service --no-pager

sudo systemctl edit khawchin-api.service

### WRF weekly compare run dan

cd /opt/khawchin
/opt/khawchin/venv/bin/python weekly_model_compare.py \
  --days 7 \
  --wrf-archive-dir /opt/khawchin/cache/wrf_archive \
  --wrf-3km-archive-dir /opt/khawchin/cache/wrf_archive_3km \
  --out-dir /opt/khawchin/reports \
  --model-run-limit 32 \
  --snapshot-timeout-seconds 8 \
  --gid-timeout-seconds 20 \
  --max-runtime-seconds 900 \
  --progress-every 10


  ### Direct printna (weekly compare result)
  cat /opt/khawchin/reports/weekly_model_compare_20260604.md


  cd /opt/khawchin

/opt/khawchin/venv/bin/python - <<'PY'
import json
p="/opt/khawchin/reports/weekly_model_compare_20260617.json"
r=json.load(open(p))
print("IMERG samples:", r.get("imerg_samples"))
print("Grid IDs:", r.get("grid_ids"))
print("WRF archive files:", r.get("wrf_archive_files"))
print()

for src, m in sorted((r.get("overall") or {}).items()):
    print(src)
    for k in ["n","mae","rmse","bias","heavy_csi","heavy_pod","heavy_far"]:
        if k in m:
            print(f"  {k}: {m[k]}")
    print()
PY

### Download fail hnua file clean na
wsl -d Ubuntu-22.04 -u mapuia -- bash -lc "rm -rf ~/wrf/data/gfs/2026062312 ~/wrf/runs/khawchin_gfs_nested_2026062312"