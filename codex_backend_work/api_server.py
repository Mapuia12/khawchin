#!/usr/bin/env python3
"""
REST API Server for Android App

Run:  uvicorn api_server:app --host 0.0.0.0 --port 8080
Or:   python api_server.py
"""

import os
import sys
import time
import logging
import threading
from datetime import datetime, timezone
from typing import Optional, List, Dict

# Check FastAPI
try:
    from fastapi import FastAPI, HTTPException, Query, Body, Request
    from fastapi.middleware.cors import CORSMiddleware
    from pydantic import BaseModel, Field
    import uvicorn
except ImportError:
    print("ERROR: FastAPI not installed!")
    print("Run: pip install fastapi uvicorn")
    sys.exit(1)

# Import our modules
from backend_v86 import init_firestore
from crowdsource import CrowdsourceManager, BADGE_INFO, BADGE_POINTS, BadgeType

try:
    from firebase_admin import auth as fb_auth
    FIREBASE_AUTH_AVAILABLE = True
except Exception:
    fb_auth = None
    FIREBASE_AUTH_AVAILABLE = False

logger = logging.getLogger("api_server")
logging.basicConfig(level=logging.INFO, format="%(asctime)s | %(levelname)-8s | %(message)s")



# ═══════════════════════════════════════════════════════════════════════════════
# SETUP
# ═══════════════════════════════════════════════════════════════════════════════

app = FastAPI(
    title="Khawchin Weather API",
    description="Weather & Crowdsourcing API for Mizoram",
    version="2.0.0"
)

_cors_env = os.environ.get("CORS_ALLOWED_ORIGINS", "*")
_cors_origins = [o.strip() for o in _cors_env.split(",") if o.strip()] or ["*"]
_allow_credentials = "*" not in _cors_origins
app.add_middleware(
    CORSMiddleware,
    allow_origins=_cors_origins,
    allow_credentials=_allow_credentials,
    allow_methods=["*"],
    allow_headers=["*"]
)

# Initialize
print("Initializing Firestore...")
db = init_firestore()
crowd_mgr = CrowdsourceManager(db)
API_REQUIRE_FIRESTORE = os.environ.get("API_REQUIRE_FIRESTORE", "1") == "1"
if db is None and API_REQUIRE_FIRESTORE:
    logger.error("Firestore is not initialized; write/read API endpoints will return 503")
print("Ready!")

# ═══════════════════════════════════════════════════════════════════════════════
# SECURITY & RATE LIMITING
# ═══════════════════════════════════════════════════════════════════════════════

AUTH_REQUIRED = os.environ.get("AUTH_REQUIRED", "1") == "1"
ALLOW_PUBLIC_PROFILE = os.environ.get("ALLOW_PUBLIC_PROFILE", "0") == "1"

RATE_LIMIT_UID_PER_MIN = int(os.environ.get("RATE_LIMIT_UID_PER_MIN", "60"))
RATE_LIMIT_IP_PER_MIN = int(os.environ.get("RATE_LIMIT_IP_PER_MIN", "120"))
READ_RATE_LIMIT_IP_PER_MIN = int(os.environ.get("READ_RATE_LIMIT_IP_PER_MIN", "180"))
RATE_LIMIT_WINDOW_SEC = int(os.environ.get("RATE_LIMIT_WINDOW_SEC", "60"))


_rate_lock = threading.Lock()
_rate_buckets: Dict[str, List[float]] = {}


def _enforce_rate_limit(key: str, limit: int, window_sec: int) -> None:
    now = time.time()
    cutoff = now - window_sec
    with _rate_lock:
        bucket = _rate_buckets.get(key, [])
        bucket = [ts for ts in bucket if ts >= cutoff]
        if len(bucket) >= limit:
            raise HTTPException(status_code=429, detail="Rate limit exceeded")
        bucket.append(now)
        _rate_buckets[key] = bucket


def _get_bearer_token(request: Request) -> Optional[str]:
    auth_header = request.headers.get("Authorization", "")
    if auth_header.startswith("Bearer "):
        return auth_header.split(" ", 1)[1].strip()
    return None


def _verify_request_user(request: Request, expected_user_id: Optional[str] = None) -> Optional[str]:
    if not AUTH_REQUIRED:
        return expected_user_id

    token = _get_bearer_token(request)
    if not token:
        raise HTTPException(status_code=401, detail="Missing Authorization token")

    if not FIREBASE_AUTH_AVAILABLE:
        raise HTTPException(status_code=500, detail="Firebase auth not available")

    try:
        decoded = fb_auth.verify_id_token(token)
    except Exception as e:
        logger.warning("Token verification failed: %s", e)
        raise HTTPException(status_code=401, detail="Invalid token")

    uid = decoded.get("uid")
    if not uid:
        raise HTTPException(status_code=401, detail="Invalid token payload")

    if expected_user_id and expected_user_id != uid:
        raise HTTPException(status_code=403, detail="User ID mismatch")

    return uid


def _require_firestore() -> None:
    if API_REQUIRE_FIRESTORE and db is None:
        raise HTTPException(status_code=503, detail="Firestore unavailable")


# ═══════════════════════════════════════════════════════════════════════════════
# PYDANTIC MODELS (Request/Response)
# ═══════════════════════════════════════════════════════════════════════════════

class ReportRequest(BaseModel):
    """Report submission request."""
    user_id: str = Field(..., min_length=5, description="Firebase Auth UID")
    lat: float = Field(..., ge=21.0, le=26.0)
    lon: float = Field(..., ge=91.0, le=96.0)
    rain_intensity: int = Field(..., ge=0, le=6, description="0=No rain, 6=Extreme")
    observed_at: Optional[datetime] = Field(None, description="Optional client-observed UTC timestamp")
    sky_condition: Optional[int] = Field(None, ge=0, le=4)
    wind_strength: Optional[int] = Field(None, ge=0, le=4)
    photo_urls: List[str] = Field(default_factory=list)
    notes: Optional[str] = Field(None, max_length=500)
    location_name: Optional[str] = Field(None, max_length=100)
    device_id: Optional[str] = None


class ReportResponse(BaseModel):
    """Report submission response."""
    success: bool
    message: str
    report_id: Optional[str] = None
    new_badges: List[str] = Field(default_factory=list)
    stats: Optional[dict] = None


class UserResponse(BaseModel):
    """User profile response."""
    user_id: str
    display_name: Optional[str]
    reputation: float
    trust_level: int
    total_reports: int
    accuracy_rate: float
    current_streak: int
    longest_streak: int
    badges:  List[str]
    total_points: int


# ═══════════════════════════════════════════════════════════════════════════════
# API ENDPOINTS
# ═══════════════════════════════════════════════════════════════════════════════

@app.get("/")
async def root():
    """Health check."""
    return {
        "status": "healthy",
        "service": "Khawchin Weather API",
        "version": "2.0.0",
        "timestamp": datetime.now(timezone.utc).isoformat()
    }


@app.get("/health")
async def health():
    """Health check endpoint."""
    return {"status": "ok"}


# === REPORTS ===

@app.post("/api/v1/reports", response_model=ReportResponse)
async def submit_report(req: ReportRequest, request: Request):
    """
    Submit a weather report.
    
    Rain Intensity: 
    - 0: No rain (Ruah a sur lo)
    - 1: Drizzle (Ruah Phingphisiau)
    - 2: Light (Ruah Tlem)
    - 3: Moderate (Ruah Sur Pangngai)
    - 4: Heavy (Ruah Nasa)
    - 5: Very Heavy (Ruah nasa tak)
    - 6: Extreme (Ruahpui/Hlauhawm)
    """
    _require_firestore()
    uid = _verify_request_user(request, expected_user_id=req.user_id)
    _enforce_rate_limit(f"uid:{uid}", RATE_LIMIT_UID_PER_MIN, RATE_LIMIT_WINDOW_SEC)
    client_ip = request.client.host if request.client else "unknown"
    _enforce_rate_limit(f"ip:{client_ip}", RATE_LIMIT_IP_PER_MIN, RATE_LIMIT_WINDOW_SEC)

    success, message, report_id, new_badges = crowd_mgr.submit_report(
        user_id=req.user_id,
        lat=req.lat,
        lon=req.lon,
        rain_intensity=req.rain_intensity,
        observed_at=req.observed_at,
        sky_condition=req.sky_condition,
        wind_strength=req.wind_strength,
        photo_urls=req.photo_urls,
        notes=req.notes,
        location_name=req.location_name,
        device_id=req.device_id,
    )
    
    if not success:
        raise HTTPException(status_code=400, detail=message)
    
    # Get updated stats
    profile = crowd_mgr.get_user(req.user_id)
    stats = {
        "total_reports": profile.total_reports,
        "current_streak": profile.current_streak,
        "total_points": profile.total_points,
        "reputation": profile.reputation,
    }
    
    return ReportResponse(
        success=True,
        message=message,
        report_id=report_id,
        new_badges=new_badges,
        stats=stats,
    )


@app.get("/api/v1/reports/nearby")
async def get_nearby_reports(
    lat: float = Query(..., ge=21.0, le=26.0),
    lon: float = Query(..., ge=91.0, le=96.0),
    radius_km: float = Query(15.0, ge=1.0, le=50.0),
    minutes: int = Query(60, ge=10, le=180),
    request: Request = None,
):
    """Get recent reports near a location."""
    _require_firestore()
    client_ip = request.client.host if request and request.client else "unknown"
    _enforce_rate_limit(f"read-ip:{client_ip}", READ_RATE_LIMIT_IP_PER_MIN, RATE_LIMIT_WINDOW_SEC)
    reports = crowd_mgr.get_recent_reports(lat, lon, radius_km, minutes)
    
    return {
        "reports": reports,
        "count": len(reports),
        "center": {"lat": lat, "lon": lon},
        "radius_km": radius_km,
    }


# === USERS ===

@app.get("/api/v1/users/{user_id}", response_model=UserResponse)
async def get_user_profile(user_id: str, request: Request):
    """Get user profile."""
    _require_firestore()
    if not ALLOW_PUBLIC_PROFILE:
        _verify_request_user(request, expected_user_id=user_id)
    profile = crowd_mgr.get_user(user_id)
    
    return UserResponse(
        user_id=profile.user_id,
        display_name=profile.display_name,
        reputation=profile.reputation,
        trust_level=profile.trust_level,
        total_reports=profile.total_reports,
        accuracy_rate=profile.accuracy_rate,
        current_streak=profile.current_streak,
        longest_streak=profile.longest_streak,
        badges=profile.badges,
        total_points=profile.total_points,
    )


@app.put("/api/v1/users/{user_id}/name")
async def update_user_name(user_id: str, name: str = Body(..., embed=True, min_length=1, max_length=50), request: Request = None):
    """Update user display name."""
    _require_firestore()
    _verify_request_user(request, expected_user_id=user_id)
    profile = crowd_mgr.update_display_name(user_id, name)
    return {"success": True, "display_name": profile.display_name}


# === LEADERBOARD ===

@app.get("/api/v1/leaderboard")
async def get_leaderboard(limit: int = Query(20, ge=1, le=100)):
    """Get top users by points."""
    _require_firestore()
    entries = crowd_mgr.get_leaderboard(limit)
    return {
        "entries": entries,
        "count": len(entries),
        "timestamp": datetime.now(timezone.utc).isoformat()
    }


# === BADGES ===

@app.get("/api/v1/badges")
async def get_badges(user_id: Optional[str] = None, request: Request = None):
    """Get all badges with earned status."""
    _require_firestore()
    if user_id and not ALLOW_PUBLIC_PROFILE:
        _verify_request_user(request, expected_user_id=user_id)
    badges = crowd_mgr.get_all_badges(user_id)
    return {"badges": badges}


# ═══════════════════════════════════════════════════════════════════════════════
# RUN SERVER
# ═══════════════════════════════════════════════════════════════════════════════

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 8080))
    print(f"Starting server on port {port}...")
    uvicorn.run(app, host="0.0.0.0", port=port)
