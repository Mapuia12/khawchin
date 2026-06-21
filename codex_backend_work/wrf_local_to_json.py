#!/usr/bin/env python3
import argparse
from datetime import datetime, timedelta
import json
import math
from pathlib import Path

import numpy as np
from netCDF4 import Dataset


# Grid defaults aligned to backend_v86.py
GRID_LAT_MIN = 22.00
GRID_LAT_MAX = 24.60
GRID_LON_MIN = 92.15
GRID_LON_MAX = 94.35

PRIORITY_ZONES = {
    "serchhip_champhai_corridor": {
        "lat_min": 23.20,
        "lat_max": 23.60,
        "lon_min": 92.75,
        "lon_max": 93.40,
        "step": 0.10,
        "radius_km": 10,
    },
    "lunglei_lawngtlai_farming": {
        "lat_min": 22.45,
        "lat_max": 23.15,
        "lon_min": 92.60,
        "lon_max": 93.10,
        "step": 0.10,
        "radius_km": 10,
    },
    "tlabung_west_lunglei": {
        "lat_min": 22.80,
        "lat_max": 23.05,
        "lon_min": 92.42,
        "lon_max": 92.62,
        "step": 0.10,
        "radius_km": 8,
    },
    "aizawl_reiek_hills": {
        "lat_min": 23.55,
        "lat_max": 23.80,
        "lon_min": 92.55,
        "lon_max": 92.80,
        "step": 0.10,
        "radius_km": 8,
    },
    "kolasib_mamit_north": {
        "lat_min": 23.85,
        "lat_max": 24.35,
        "lon_min": 92.30,
        "lon_max": 92.85,
        "step": 0.10,
        "radius_km": 8,
    },
    "saitual_ngopa_northeast": {
        "lat_min": 23.65,
        "lat_max": 24.05,
        "lon_min": 92.85,
        "lon_max": 93.25,
        "step": 0.10,
        "radius_km": 8,
    },
    "kabaw_valley": {
        "lat_min": 23.15,
        "lat_max": 23.90,
        "lon_min": 94.00,
        "lon_max": 94.25,
        "step": 0.10,
        "radius_km": 10,
    },
    "tamu_area": {
        "lat_min": 24.00,
        "lat_max": 24.30,
        "lon_min": 94.15,
        "lon_max": 94.35,
        "step": 0.10,
        "radius_km": 8,
    },
}


LOCATIONS = [
    (23.73, 92.72),
    (22.88, 92.73),
    (23.47, 93.33),
    (23.30, 92.85),
    (24.22, 92.68),
    (22.53, 92.90),
    (23.92, 92.49),
    (23.69, 92.96),
    (22.97, 92.93),
    (22.49, 92.98),
    (23.53, 93.18),
    (24.49, 92.76),
    (24.18, 92.54),
    (24.12, 92.34),
    (24.01, 92.92),
    (23.68, 92.60),
    (23.32, 92.75),
    (22.90, 92.49),
    (22.62, 92.64),
    (23.13, 93.06),
    (23.89, 93.21),
    (23.38, 93.13),
    (22.72, 93.03),
    (22.31, 93.03),
    (23.312, 93.389),
    (23.36, 93.39),
    (23.18, 93.42),
    (23.39, 93.38),
    (23.38, 93.41),
    (23.45, 92.98),
    (23.12, 92.95),
    (23.052394, 92.771748),  # Haulawng
    (23.256719, 93.349852),  # Lianpui
    (22.15, 92.93),
    (23.19, 94.05),
    (23.202, 94.016),
    (23.331, 94.025),
    (23.671, 94.138),
    (23.805, 94.146),
    (24.063, 94.264),
    (24.22, 94.30),
]


COVERAGE_POLYGON = [
    (24.60, 92.15),
    (24.60, 92.75),
    (24.40, 92.85),
    (24.30, 93.10),
    (24.50, 93.30),
    (24.60, 93.50),
    (24.60, 94.20),
    (24.25, 94.35),
    (23.70, 94.25),
    (23.20, 94.20),
    (23.00, 93.95),
    (22.70, 93.75),
    (22.40, 93.65),
    (22.10, 93.50),
    (21.85, 93.30),
    (21.85, 92.90),
    (22.05, 92.65),
    (22.30, 92.55),
    (22.70, 92.50),
    (22.85, 92.43),
    (23.05, 92.45),
    (23.40, 92.45),
    (23.70, 92.40),
    (24.00, 92.25),
    (24.30, 92.20),
    (24.60, 92.15),
]


def haversine_km(lat1, lon1, lat2, lon2):
    r = 6371.0
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlambda = math.radians(lon2 - lon1)
    a = math.sin(dphi / 2.0) ** 2 + math.cos(phi1) * math.cos(phi2) * math.sin(dlambda / 2.0) ** 2
    return 2.0 * r * math.atan2(math.sqrt(a), math.sqrt(1.0 - a))


def point_in_polygon(lat, lon, polygon):
    inside = False
    j = len(polygon) - 1
    for i in range(len(polygon)):
        yi, xi = polygon[i]
        yj, xj = polygon[j]
        if ((yi > lat) != (yj > lat)) and (lon < (xj - xi) * (lat - yi) / (yj - yi) + xi):
            inside = not inside
        j = i
    return inside


def generate_grid_points(coarse_step, refine_step, refine_radius_km):
    points = set()

    lat = GRID_LAT_MIN
    while lat <= GRID_LAT_MAX + 1e-9:
        lon = GRID_LON_MIN
        while lon <= GRID_LON_MAX + 1e-9:
            points.add((round(lat, 2), round(lon, 2)))
            lon = round(lon + coarse_step, 6)
        lat = round(lat + coarse_step, 6)

    for zone in PRIORITY_ZONES.values():
        zone_step = zone.get("step", refine_step)
        zone_lat_min = max(zone["lat_min"], GRID_LAT_MIN)
        zone_lat_max = min(zone["lat_max"], GRID_LAT_MAX)
        zone_lon_min = max(zone["lon_min"], GRID_LON_MIN)
        zone_lon_max = min(zone["lon_max"], GRID_LON_MAX)

        lat = zone_lat_min
        while lat <= zone_lat_max + 1e-9:
            lon = zone_lon_min
            while lon <= zone_lon_max + 1e-9:
                points.add((round(lat, 2), round(lon, 2)))
                lon = round(lon + zone_step, 6)
            lat = round(lat + zone_step, 6)

    radius_deg = refine_radius_km / 111.0
    for clat, clon in LOCATIONS:
        points.add((round(clat, 2), round(clon, 2)))
        lat_min = max(GRID_LAT_MIN, clat - radius_deg)
        lat_max = min(GRID_LAT_MAX, clat + radius_deg)
        lon_min = max(GRID_LON_MIN, clon - radius_deg)
        lon_max = min(GRID_LON_MAX, clon + radius_deg)

        lat = lat_min
        while lat <= lat_max + 1e-9:
            lon = lon_min
            while lon <= lon_max + 1e-9:
                if haversine_km(lat, lon, clat, clon) <= refine_radius_km:
                    points.add((round(lat, 2), round(lon, 2)))
                lon = round(lon + refine_step, 6)
            lat = round(lat + refine_step, 6)

    points = [p for p in points if point_in_polygon(p[0], p[1], COVERAGE_POLYGON)]
    return sorted(points, key=lambda p: (p[0], p[1]))


def decode_times(ds):
    if "Times" not in ds.variables:
        raise RuntimeError("WRF file missing Times variable")
    times_raw = ds.variables["Times"][:]
    out = []
    for row in times_raw:
        t = row.tobytes().decode("ascii").strip()
        out.append(t)
    return out


def iso_time(t):
    dt = datetime.strptime(t, "%Y-%m-%d_%H:%M:%S")
    # Snap to the nearest hour; WRF output can drift slightly around the hour.
    if dt.minute > 30 or (dt.minute == 30 and dt.second >= 0):
        dt = dt + timedelta(hours=1)
    return dt.replace(minute=0, second=0, microsecond=0).strftime("%Y-%m-%dT%H:00:00Z")


def run_id_from_time(t):
    date_part, time_part = t.split("_")
    yyyymmdd = date_part.replace("-", "")
    hh = time_part.split(":")[0]
    return f"wrf_{yyyymmdd}_{hh}"


def load_grid_points(grid_json_path, coarse_step, refine_step, refine_radius_km):
    if grid_json_path:
        data = json.loads(Path(grid_json_path).read_text(encoding="utf-8"))
        points = []
        for item in data:
            lat = float(item["lat"])
            lon = float(item["lon"])
            points.append((round(lat, 2), round(lon, 2)))
        return sorted(set(points), key=lambda p: (p[0], p[1]))
    return generate_grid_points(coarse_step, refine_step, refine_radius_km)


def nearest_indices(lat2d, lon2d, points):
    flat_lat = lat2d.ravel()
    flat_lon = lon2d.ravel()
    indices = []
    for lat, lon in points:
        d2 = (flat_lat - lat) ** 2 + (flat_lon - lon) ** 2
        idx = int(d2.argmin())
        i, j = np.unravel_index(idx, lat2d.shape)
        indices.append((i, j))
    return indices


def to_float_array(arr):
    arr = np.asarray(arr)
    if np.ma.isMaskedArray(arr):
        arr = arr.filled(np.nan)
    return arr.astype(float)


def extract_points(ds, var_name, idx_flat, grid_shape):
    var = ds.variables[var_name]
    data = np.asarray(var[:])
    if data.ndim != 3:
        raise RuntimeError(f"{var_name} has unexpected shape {data.shape}")
    flat = data.reshape(data.shape[0], grid_shape[0] * grid_shape[1])
    return flat[:, idx_flat]


def main():
    parser = argparse.ArgumentParser(description="Extract WRF output to compact JSON grid")
    parser.add_argument("--wrf-dir", default=".", help="Directory with wrfout files")
    parser.add_argument("--pattern", default="wrfout_d01_*", help="wrfout glob pattern")
    parser.add_argument("--domain", default="d01", help="WRF domain string")
    parser.add_argument("--output", default="wrf_local_latest.json", help="Output JSON path")
    parser.add_argument("--source", default="wrf_gfs_local_d01_9km", help="Source label")
    parser.add_argument("--run-id", default="", help="Override run_id (e.g. wrf_20260528_12)")
    parser.add_argument("--confidence", default=0.35, type=float, help="Confidence value per grid cell")
    parser.add_argument("--grid-json", default="", help="Optional JSON list of {lat, lon}")
    parser.add_argument("--coarse-step", default=0.20, type=float, help="Coarse grid step in degrees")
    parser.add_argument("--refine-step", default=0.10, type=float, help="Refine grid step in degrees")
    parser.add_argument("--refine-radius-km", default=10.0, type=float, help="Refine radius around POIs")
    args = parser.parse_args()

    wrf_dir = Path(args.wrf_dir).expanduser()
    files = sorted(wrf_dir.glob(args.pattern))
    if not files:
        raise SystemExit(f"No WRF files found in {wrf_dir} with pattern {args.pattern}")

    with Dataset(files[0]) as ds0:
        lat_var = ds0.variables.get("XLAT") or ds0.variables.get("XLAT_M")
        lon_var = ds0.variables.get("XLONG") or ds0.variables.get("XLONG_M")
        if lat_var is None or lon_var is None:
            raise SystemExit("Missing XLAT/XLONG in WRF file")
        lat2d = np.asarray(lat_var[0, :, :])
        lon2d = np.asarray(lon_var[0, :, :])

    points = load_grid_points(args.grid_json, args.coarse_step, args.refine_step, args.refine_radius_km)
    indices = nearest_indices(lat2d, lon2d, points)
    idx_i = np.array([i for i, _ in indices], dtype=int)
    idx_j = np.array([j for _, j in indices], dtype=int)
    idx_flat = np.ravel_multi_index((idx_i, idx_j), lat2d.shape)

    times = []
    t2_list = []
    u10_list = []
    v10_list = []
    rainc_list = []
    rainnc_list = []
    psfc_list = []
    seen = set()

    for path in files:
        with Dataset(path) as ds:
            time_strs = decode_times(ds)
            t2 = extract_points(ds, "T2", idx_flat, lat2d.shape)
            u10 = extract_points(ds, "U10", idx_flat, lat2d.shape)
            v10 = extract_points(ds, "V10", idx_flat, lat2d.shape)
            rainc = extract_points(ds, "RAINC", idx_flat, lat2d.shape)
            rainnc = extract_points(ds, "RAINNC", idx_flat, lat2d.shape)
            psfc = extract_points(ds, "PSFC", idx_flat, lat2d.shape)

            for ti, tstr in enumerate(time_strs):
                if tstr in seen:
                    continue
                seen.add(tstr)
                times.append(tstr)
                t2_list.append(to_float_array(t2[ti, :]))
                u10_list.append(to_float_array(u10[ti, :]))
                v10_list.append(to_float_array(v10[ti, :]))
                rainc_list.append(to_float_array(rainc[ti, :]))
                rainnc_list.append(to_float_array(rainnc[ti, :]))
                psfc_list.append(to_float_array(psfc[ti, :]))

    t2_arr = np.stack(t2_list, axis=0)
    u10_arr = np.stack(u10_list, axis=0)
    v10_arr = np.stack(v10_list, axis=0)
    rain_accum = np.stack(rainc_list, axis=0) + np.stack(rainnc_list, axis=0)
    psfc_arr = np.stack(psfc_list, axis=0)

    temp_c = t2_arr - 273.15
    wind_kmh = np.sqrt(u10_arr ** 2 + v10_arr ** 2) * 3.6
    wind_dir = (270.0 - np.degrees(np.arctan2(v10_arr, u10_arr))) % 360.0
    pressure_hpa = psfc_arr / 100.0

    rain_hourly = np.diff(rain_accum, axis=0, prepend=rain_accum[:1, :])
    rain_hourly[0, :] = 0.0

    times_iso = [iso_time(t) for t in times]
    run_time_utc = times_iso[0] if times_iso else None
    run_id = args.run_id or (run_id_from_time(times[0]) if times else "wrf_unknown")

    grid = {}
    for idx, (lat, lon) in enumerate(points):
        key = f"{lat:.2f}_{lon:.2f}"
        grid[key] = {
            "lat": float(lat),
            "lon": float(lon),
            "times": times_iso,
            "temp_2m_c": [float(x) for x in temp_c[:, idx]],
            "precip_mm": [float(x) for x in rain_hourly[:, idx]],
            "wind_kmh": [float(x) for x in wind_kmh[:, idx]],
            "wind_dir_deg": [float(x) for x in wind_dir[:, idx]],
            "pressure_hpa": [float(x) for x in pressure_hpa[:, idx]],
            "confidence": float(args.confidence),
        }

    payload = {
        "source": args.source,
        "run_id": run_id,
        "run_time_utc": run_time_utc,
        "forecast_start_utc": times_iso[0] if times_iso else None,
        "forecast_end_utc": times_iso[-1] if times_iso else None,
        "created_at_utc": datetime.utcnow().replace(microsecond=0).strftime("%Y-%m-%dT%H:%M:%SZ"),
        "domain": args.domain,
        "grid_count": len(grid),
        "time_count": len(times_iso),
        "grid": grid,
    }

    out_path = Path(args.output).expanduser()
    out_path.parent.mkdir(parents=True, exist_ok=True)
    out_path.write_text(json.dumps(payload, ensure_ascii=True), encoding="utf-8")
    print(f"Wrote {out_path} with {len(grid)} points and {len(times_iso)} times")


if __name__ == "__main__":
    main()
