#!/usr/bin/env python3
"""
Crowdsource Module - Single File Version
Khawchin Weather App tan
"""

import uuid
import logging
import math
import threading
from datetime import datetime, timezone, timedelta
from dataclasses import dataclass, field
from enum import IntEnum, Enum
from typing import Dict, List, Optional, Tuple, Any

logger = logging.getLogger("crowdsource")


# ═══════════════════════════════════════════════════════════════════════════════
# ENUMS - Rain intensity levels
# ═══════════════════════════════════════════════════════════════════════════════

class RainIntensity(IntEnum):
    """Rain intensity - user selects one."""
    NO_RAIN = 0          # Ruah sur lo
    DRIZZLE = 1          # Ruah phingphisiau
    LIGHT = 2            # Ruah tlem / Sur ser ser
    MODERATE = 3         # Ruah sur pangngai
    HEAVY = 4            # Ruah sur tam
    VERY_HEAVY = 5       # Ruah sur nasa
    EXTREME = 6          # Ruahpui vanawn


class SkyCondition(IntEnum):
    """Van eng dan."""
    CLEAR = 0            # Khaw thiang
    PARTLY_CLOUDY = 1    # Chhum tlem
    MOSTLY_CLOUDY = 2    # Chhum zing
    OVERCAST = 3         # Khaw dur
    FOG = 4              # Chhum chhah / Khaw chheng


class WindStrength(IntEnum):
    """Thli nasat dan."""
    CALM = 0             # Thli thaw lo
    LIGHT = 1            # Thli thaw heuh heuh
    MODERATE = 2         # Thli thaw vuk vuk
    STRONG = 3           # Thli na
    VERY_STRONG = 4      # Thlipui


class BadgeType(str, Enum):
    """Badge types."""
    FIRST_REPORT = "first_report"
    REPORT_10 = "report_10"
    REPORT_50 = "report_50"
    REPORT_100 = "report_100"
    STREAK_7 = "streak_7"
    STREAK_30 = "streak_30"
    ACCURATE_REPORTER = "accurate_reporter"
    STORM_CHASER = "storm_chaser"
    PHOTO_PRO = "photo_pro"
    EARLY_BIRD = "early_bird"
    NIGHT_OWL = "night_owl"


# Intensity to mm/hr mapping
INTENSITY_TO_MM = {
    RainIntensity.NO_RAIN: 0.0,
    RainIntensity.DRIZZLE: 1.0,
    RainIntensity.LIGHT: 5.0,
    RainIntensity.MODERATE: 15.0,
    RainIntensity.HEAVY: 50.0,
    RainIntensity.VERY_HEAVY: 90.0,
    RainIntensity.EXTREME: 150.0,
}

# Badge points
BADGE_POINTS = {
    BadgeType.FIRST_REPORT: 10,
    BadgeType.REPORT_10: 50,
    BadgeType.REPORT_50: 150,
    BadgeType.REPORT_100: 300,
    BadgeType.STREAK_7: 70,
    BadgeType.STREAK_30: 300,
    BadgeType.ACCURATE_REPORTER: 200,
    BadgeType.STORM_CHASER: 200,
    BadgeType.PHOTO_PRO: 250,
    BadgeType.EARLY_BIRD: 150,
    BadgeType.NIGHT_OWL: 150,
}

# Badge info (name, name_mizo, icon, description)
# Mizo names updated to be more natural and descriptive
BADGE_INFO = {
    BadgeType.FIRST_REPORT: ("First Steps", "Pen Hmasa Ber", "🌱", "Submit first report"),
    BadgeType.REPORT_10: ("Getting Started", "Bul Ṭan Ṭha", "🌤️", "Submit 10 reports"),
    BadgeType.REPORT_50: ("Weather Watcher", "Khawchin Ngaihven", "👁️", "Submit 50 reports"),
    BadgeType.REPORT_100: ("Dedicated", "Mi Taima", "🔭", "Submit 100 reports"),
    BadgeType.STREAK_7: ("Week Warrior", "Kar Tluana Thawk", "🔥", "7-day streak"),
    BadgeType.STREAK_30: ("Monthly Master", "Thla Puma Thawk", "💪", "30-day streak"),
    BadgeType.ACCURATE_REPORTER: ("Sharp Eye", "Hre Dik", "🎯", "80%+ accuracy"),
    BadgeType.STORM_CHASER:  ("Storm Chaser", "Thlipui Hunter", "⛈️", "10 heavy rain reports"),
    BadgeType.PHOTO_PRO: ("Photo Pro", "Thlalak Thiam", "📸", "50 photo reports"),
    BadgeType.EARLY_BIRD:  ("Early Bird", "Hma Taka Report", "🌅", "50 reports before 7 AM"),
    BadgeType.NIGHT_OWL: ("Night Owl", "Zan Reh Report", "🦉", "50 reports after 10 PM"),
}

# Crowd quality weighting (for nowcast reliability)
CROWD_RECENCY_HALFLIFE_MIN = 30.0
CROWD_AGREEMENT_SIGMA_MM = 6.0
CROWD_MIN_QUALITY = 0.25
CROWD_REPORT_QUERY_LIMIT = 800
CROWD_QUOTA_BACKOFF_MIN = 45
CROWD_PREFETCH_CACHE_SEC = 300


def bearing_deg(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    lat1_rad = math.radians(lat1)
    lat2_rad = math.radians(lat2)
    dlon_rad = math.radians(lon2 - lon1)
    y = math.sin(dlon_rad) * math.cos(lat2_rad)
    x = (
        math.cos(lat1_rad) * math.sin(lat2_rad)
        - math.sin(lat1_rad) * math.cos(lat2_rad) * math.cos(dlon_rad)
    )
    return (math.degrees(math.atan2(y, x)) + 360.0) % 360.0


def angular_diff_deg(a: Optional[float], b: Optional[float]) -> float:
    if a is None or b is None:
        return 180.0
    diff = abs(float(a) - float(b)) % 360.0
    return diff if diff <= 180.0 else 360.0 - diff


def upwind_weight_factor(
    target_lat: float,
    target_lon: float,
    source_lat: float,
    source_lon: float,
    wind_dir_deg: Optional[float],
) -> float:
    if wind_dir_deg is None:
        return 1.0
    try:
        src_bearing = bearing_deg(target_lat, target_lon, source_lat, source_lon)
        diff = angular_diff_deg(src_bearing, wind_dir_deg)
        align = max(0.0, math.cos(math.radians(diff)))
        return 0.8 + 0.7 * align
    except Exception:
        return 1.0


def intensity_to_mm(intensity: RainIntensity) -> float:
    """Convert intensity to mm/hr."""
    return INTENSITY_TO_MM.get(intensity, 0.0)


def mm_to_intensity(mm: float) -> RainIntensity:
    """Convert mm/hr to intensity."""
    if mm <= 0:
        return RainIntensity.NO_RAIN
    elif mm < 2.5:
        return RainIntensity.DRIZZLE
    elif mm < 7.5:
        return RainIntensity.LIGHT
    elif mm < 35: 
        return RainIntensity.MODERATE
    elif mm < 65:
        return RainIntensity.HEAVY
    elif mm < 125:
        return RainIntensity.VERY_HEAVY
    return RainIntensity.EXTREME


def report_rain_mm_from_payload(report: Dict[str, Any], default: float = 0.0) -> float:
    """Return report rain rate, including legacy docs that only stored intensity."""
    rain_mm = safe_float(report.get("rain_mm"), None)
    if rain_mm is not None:
        return max(0.0, rain_mm)

    intensity = safe_float(report.get("rain_intensity"), None)
    if intensity is None:
        return default
    try:
        return intensity_to_mm(RainIntensity(int(round(intensity))))
    except Exception:
        return default


def _local_offset_hours_for_lon(lon: float) -> float:
    """Approximate local civil time for the app region from longitude."""
    try:
        lon_value = float(lon)
    except Exception:
        lon_value = 92.7
    return 6.5 if lon_value >= 93.0 else 5.5


def local_report_datetime(ts: datetime, lon: float) -> datetime:
    if ts.tzinfo is None:
        ts = ts.replace(tzinfo=timezone.utc)
    return ts.astimezone(timezone.utc) + timedelta(hours=_local_offset_hours_for_lon(lon))


def haversine_km(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Distance in km."""
    R = 6371.0
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlam = math.radians(lon2 - lon1)
    a = math.sin(dphi/2)**2 + math.cos(phi1)*math.cos(phi2)*math.sin(dlam/2)**2
    return 2 * R * math.asin(min(1.0, math.sqrt(a)))


def safe_float(value: Any, default: Optional[float] = None) -> Optional[float]:
    try:
        if value is None:
            return default
        result = float(value)
        if math.isnan(result) or math.isinf(result):
            return default
        return result
    except Exception:
        return default


def parse_report_dt(value: Any) -> Optional[datetime]:
    if value is None:
        return None
    if isinstance(value, datetime):
        return value if value.tzinfo else value.replace(tzinfo=timezone.utc)
    if isinstance(value, str):
        try:
            dt = datetime.fromisoformat(value.replace("Z", "+00:00"))
            return dt if dt.tzinfo else dt.replace(tzinfo=timezone.utc)
        except Exception:
            return None
    return None


# ═══════════════════════════════════════════════════════════════════════════════
# DATA CLASSES
# ═══════════════════════════════════════════════════════════════════════════════

@dataclass
class WeatherReport:
    """User weather report."""
    report_id: str
    user_id: str
    lat: float
    lon: float
    rain_intensity:  RainIntensity
    timestamp: datetime
    
    sky_condition: Optional[SkyCondition] = None
    wind_strength: Optional[WindStrength] = None
    photo_urls: List[str] = field(default_factory=list)
    notes: Optional[str] = None
    location_name: Optional[str] = None
    device_id: Optional[str] = None
    received_at: Optional[datetime] = None
    reporter_reputation: Optional[float] = None
    reporter_trust_level: Optional[int] = None
    
    @property
    def rain_mm(self) -> float:
        return intensity_to_mm(self.rain_intensity)
    
    @property
    def has_photo(self) -> bool:
        return len(self.photo_urls) > 0
    
    def to_dict(self) -> Dict: 
        return {
            "report_id": self.report_id,
            "user_id": self.user_id,
            "lat": self.lat,
            "lon": self.lon,
            "rain_intensity": self.rain_intensity.value,
            "timestamp": self.timestamp.isoformat(),
            "sky_condition": self.sky_condition.value if self.sky_condition else None,
            "wind_strength": self.wind_strength.value if self.wind_strength else None,
            "photo_urls": self.photo_urls,
            "notes": self.notes,
            "location_name": self.location_name,
            "device_id": self.device_id,
            "received_at": (
                self.received_at.isoformat()
                if isinstance(self.received_at, datetime)
                else None
            ),
            "reporter_reputation": round(self.reporter_reputation, 3) if self.reporter_reputation is not None else None,
            "reporter_trust_level": self.reporter_trust_level,
            "rain_mm": self.rain_mm,
        }
    
    @classmethod
    def from_dict(cls, d: Dict) -> 'WeatherReport':
        ts = d.get("timestamp")
        if isinstance(ts, datetime):
            ts = ts if ts.tzinfo else ts.replace(tzinfo=timezone.utc)
        elif isinstance(ts, str):
            ts = datetime.fromisoformat(ts.replace("Z", "+00:00"))
        else:
            ts = datetime.now(timezone.utc)
        
        return cls(
            report_id=d.get("report_id", str(uuid.uuid4())),
            user_id=d["user_id"],
            lat=float(d["lat"]),
            lon=float(d["lon"]),
            rain_intensity=RainIntensity(int(d.get("rain_intensity", 0))),
            timestamp=ts,
            sky_condition=SkyCondition(d["sky_condition"]) if d.get("sky_condition") is not None else None,
            wind_strength=WindStrength(d["wind_strength"]) if d.get("wind_strength") is not None else None,
            photo_urls=d.get("photo_urls", []),
            notes=d.get("notes"),
            location_name=d.get("location_name"),
            device_id=d.get("device_id"),
            received_at=parse_report_dt(d.get("received_at")),
            reporter_reputation=safe_float(d.get("reporter_reputation")),
            reporter_trust_level=int(d["reporter_trust_level"]) if d.get("reporter_trust_level") is not None else None,
        )


@dataclass
class UserProfile:
    """User profile."""
    user_id: str
    display_name: Optional[str] = None
    reputation:  float = 0.5
    trust_level: int = 1
    total_reports: int = 0
    accurate_reports: int = 0
    photo_reports: int = 0
    heavy_rain_reports: int = 0
    early_reports: int = 0
    night_reports: int = 0
    current_streak: int = 0
    longest_streak: int = 0
    last_report_date: Optional[str] = None
    badges: List[str] = field(default_factory=list)
    total_points: int = 0
    warning_count: int = 0
    is_banned: bool = False
    
    @property
    def accuracy_rate(self) -> float:
        if self.total_reports == 0:
            return 0.0
        return self.accurate_reports / self.total_reports
    
    def to_dict(self) -> Dict:
        return {
            "user_id": self.user_id,
            "display_name": self.display_name,
            "reputation": self.reputation,
            "trust_level": self.trust_level,
            "total_reports": self.total_reports,
            "accurate_reports": self.accurate_reports,
            "photo_reports": self.photo_reports,
            "heavy_rain_reports": self.heavy_rain_reports,
            "early_reports": self.early_reports,
            "night_reports": self.night_reports,
            "current_streak": self.current_streak,
            "longest_streak": self.longest_streak,
            "last_report_date": self.last_report_date,
            "badges": self.badges,
            "total_points": self.total_points,
            "warning_count": self.warning_count,
            "is_banned": self.is_banned,
            "accuracy_rate": self.accuracy_rate,
        }
    
    @classmethod
    def from_dict(cls, d: Dict) -> 'UserProfile':
        # Support legacy / alternate field names for backwards compatibility
        user_id = d.get("user_id") or d.get("uid") or d.get("id") or ""
        display_name = d.get("display_name") or d.get("name")
        reputation = float(d.get("reputation", 0.5))
        trust_level = int(d.get("trust_level", d.get("trust", 1)))

        # Accept either 'total_points' or legacy 'points'
        total_points = int(d.get("total_points", d.get("points", 0)))

        return cls(
            user_id=user_id,
            display_name=display_name,
            reputation=reputation,
            trust_level=trust_level,
            total_reports=int(d.get("total_reports", d.get("reports", 0))),
            accurate_reports=int(d.get("accurate_reports", 0)),
            photo_reports=int(d.get("photo_reports", 0)),
            heavy_rain_reports=int(d.get("heavy_rain_reports", 0)),
            early_reports=int(d.get("early_reports", 0)),
            night_reports=int(d.get("night_reports", 0)),
            current_streak=int(d.get("current_streak", 0)),
            longest_streak=int(d.get("longest_streak", 0)),
            last_report_date=d.get("last_report_date"),
            badges=d.get("badges", []),
            total_points=total_points,
            warning_count=int(d.get("warning_count", 0)),
            is_banned=bool(d.get("is_banned", False)),
        )



# ═══════════════════════════════════════════════════════════════════════════════
# VALIDATION
# ═══════════════════════════════════════════════════════════════════════════════

def validate_report(
    user_id: str,
    lat: float,
    lon: float,
    rain_intensity: int,
    timestamp: datetime
) -> Tuple[bool, str]:
    """Validate report.  Returns (is_valid, error_message)."""
    
    if not user_id or len(user_id) < 5:
        return False, "Invalid user ID"

    try:
        coords_ok = all(math.isfinite(float(v)) for v in (lat, lon))
    except Exception:
        coords_ok = False
    if not coords_ok:
        return False, "Invalid coordinates"
    
    # Widened to match Android app bounds (covers full Mizoram + Myanmar border regions)
    if not (21.0 <= lat <= 26.0):
        return False, "Latitude outside valid region (21.0-26.0)"
    
    if not (91.0 <= lon <= 96.0):
        return False, "Longitude outside valid region (91.0-96.0)"
    
    if not (0 <= rain_intensity <= 6):
        return False, "Invalid rain intensity"
    
    now = datetime.now(timezone.utc)
    if timestamp.tzinfo is None:
        timestamp = timestamp.replace(tzinfo=timezone.utc)
    
    age_min = (now - timestamp).total_seconds() / 60
    if age_min > 60:
        return False, "Report too old (>60 minutes)"
    if age_min < -5:
        return False, "Future timestamp"
    
    return True, "OK"


# ═══════════════════════════════════════════════════════════════════════════════
# FRAUD DETECTION (Firestore-backed for persistence across restarts)
# ═══════════════════════════════════════════════════════════════════════════════

class FraudDetector:
    """
    Fraud detection with Firestore persistence.
    Rate limits survive server restarts.
    """
    
    def __init__(self, db=None):
        self._db = db
        self._local_cache: Dict[str, List[datetime]] = {}  # In-memory fallback
        self._lock = threading.Lock()
        self.RATE_LIMITS_COLLECTION = "rate_limits"
        self.MAX_REPORTS_PER_HOUR = 10
    
    def check(self, user_id: str, lat: float, lon: float, timestamp: datetime) -> Tuple[bool, str]:
        """Check for fraud. Returns (is_ok, reason)."""
        cutoff = datetime.now(timezone.utc) - timedelta(hours=1)
        
        # Try Firestore-backed check first
        if self._db:
            try:
                return self._check_firestore(user_id, timestamp, cutoff)
            except Exception as e:
                logger.warning("Firestore fraud check failed, using local: %s", e)
        
        # Fallback to in-memory
        return self._check_local(user_id, timestamp, cutoff)
    
    def _check_firestore(self, user_id: str, timestamp: datetime, cutoff: datetime) -> Tuple[bool, str]:
        """Firestore-backed rate limit check."""
        doc_ref = self._db.collection(self.RATE_LIMITS_COLLECTION).document(user_id)
        doc = doc_ref.get()
        
        recent_timestamps = []
        if doc.exists:
            data = doc.to_dict()
            stored_times = data.get("recent_reports", [])
            # Filter to only recent timestamps
            for ts in stored_times:
                try:
                    if isinstance(ts, str):
                        dt = datetime.fromisoformat(ts.replace("Z", "+00:00"))
                    else:
                        dt = ts
                    if dt.tzinfo is None:
                        dt = dt.replace(tzinfo=timezone.utc)
                    if dt > cutoff:
                        recent_timestamps.append(dt)
                except:
                    continue
        
        # Check rate limit
        if len(recent_timestamps) >= self.MAX_REPORTS_PER_HOUR:
            return False, f"Too many reports in 1 hour (max {self.MAX_REPORTS_PER_HOUR})"
        
        # Record this report
        recent_timestamps.append(timestamp)
        doc_ref.set({
            "user_id": user_id,
            "recent_reports": [t.isoformat() for t in recent_timestamps],
            "last_updated": datetime.now(timezone.utc).isoformat(),
        })
        
        return True, "OK"
    
    def _check_local(self, user_id: str, timestamp: datetime, cutoff: datetime) -> Tuple[bool, str]:
        """In-memory fallback rate limit check."""
        with self._lock:
            # Clean old entries
            if user_id in self._local_cache:
                self._local_cache[user_id] = [t for t in self._local_cache[user_id] if t > cutoff]
            
            # Check rapid fire
            recent_count = len(self._local_cache.get(user_id, []))
            if recent_count >= self.MAX_REPORTS_PER_HOUR:
                return False, f"Too many reports in 1 hour (max {self.MAX_REPORTS_PER_HOUR})"
            
            # Record this report
            if user_id not in self._local_cache:
                self._local_cache[user_id] = []
            self._local_cache[user_id].append(timestamp)
        
        return True, "OK"


# ═══════════════════════════════════════════════════════════════════════════════
# REPUTATION MANAGER
# ═══════════════════════════════════════════════════════════════════════════════

class CrowdsourceManager:
    """Main manager for crowdsourcing."""
    
    def __init__(self, db=None):
        self._db = db
        self._user_cache: Dict[str, UserProfile] = {}
        self._fraud = FraudDetector(db)  # Pass db for Firestore-backed fraud detection
        self._lock = threading.Lock()
        self._recent_reports_cache: Dict[int, Dict[str, Any]] = {}
        self._report_query_backoff_until: Optional[datetime] = None
        
        self.REPORTS_COLLECTION = "crowd_reports"
        self.USERS_COLLECTION = "users"
    
    # === User Profile ===
    
    def get_user(self, user_id:  str) -> UserProfile:
        """Get or create user profile."""
        with self._lock:
            if user_id in self._user_cache:
                return self._user_cache[user_id]
        
        if self._db:
            try:
                doc = self._db.collection(self.USERS_COLLECTION).document(user_id).get()
                if doc.exists:
                    profile = UserProfile.from_dict(doc.to_dict())
                    with self._lock:
                        self._user_cache[user_id] = profile
                    return profile
            except Exception as e:
                logger.warning("Get user error: %s", e)
        
        # Create new
        profile = UserProfile(user_id=user_id)
        self._save_user(profile)
        return profile
    
    def _save_user(self, profile: UserProfile):
        """Save user profile (writes both modern and legacy keys for compatibility)."""
        with self._lock:
            self._user_cache[profile.user_id] = profile

        if not self._db:
            logger.info("No Firestore DB instance available; skipping persistent save for user %s", profile.user_id)
            return

        # Prepare payload with both new and legacy keys
        payload = profile.to_dict()
        # Add legacy keys for older clients
        payload_legacy = {
            "uid": profile.user_id,
            "points": payload.get("total_points", 0),
            "reports": payload.get("total_reports", 0),
            "name": payload.get("display_name"),
        }
        # Merge so persistent doc contains both sets
        payload.update(payload_legacy)

        try:
            # Single write (users collection configured in USERS_COLLECTION)
            self._db.collection(self.USERS_COLLECTION).document(profile.user_id).set(payload)
            logger.info("Saved user %s: reports=%d points=%d", profile.user_id, profile.total_reports, profile.total_points)
        except Exception as e:
            logger.warning("Save user error for %s: %s", profile.user_id, e)


    
    def update_display_name(self, user_id: str, name: str) -> UserProfile:
        """Update user display name."""
        profile = self.get_user(user_id)
        profile.display_name = name[: 50]  # Limit length
        self._save_user(profile)
        return profile
    
    # === Report Submission ===
    
    def submit_report(
        self,
        user_id: str,
        lat: float,
        lon: float,
        rain_intensity: int,
        observed_at: Optional[datetime] = None,
        sky_condition: Optional[int] = None,
        wind_strength: Optional[int] = None,
        photo_urls: List[str] = None,
        notes: Optional[str] = None,
        location_name: Optional[str] = None,
        device_id: Optional[str] = None,
    ) -> Tuple[bool, str, Optional[str], List[str]]:
        """
        Submit a weather report.
        
        Returns: (success, message, report_id, new_badges)
        """
        received_at = datetime.now(timezone.utc)
        timestamp = observed_at if isinstance(observed_at, datetime) else received_at
        if timestamp.tzinfo is None:
            timestamp = timestamp.replace(tzinfo=timezone.utc)
        else:
            timestamp = timestamp.astimezone(timezone.utc)
        
        # Validate
        is_valid, error = validate_report(user_id, lat, lon, rain_intensity, timestamp)
        if not is_valid:
            return False, error, None, []
        
        # Check user status
        profile = self.get_user(user_id)
        if profile.is_banned:
            return False, "User is banned", None, []
        
        # Fraud check
        is_ok, reason = self._fraud.check(user_id, lat, lon, received_at)
        if not is_ok:
            profile.warning_count += 1
            if profile.warning_count >= 5:
                profile.is_banned = True
            self._save_user(profile)
            return False, reason, None, []
        
        # Create report
        report_id = str(uuid.uuid4())
        report = WeatherReport(
            report_id=report_id,
            user_id=user_id,
            lat=lat,
            lon=lon,
            rain_intensity=RainIntensity(rain_intensity),
            timestamp=timestamp,
            sky_condition=SkyCondition(sky_condition) if sky_condition is not None else None,
            wind_strength=WindStrength(wind_strength) if wind_strength is not None else None,
            photo_urls=photo_urls or [],
            notes=notes[: 500] if notes else None,
            location_name=location_name,
            device_id=device_id,
            received_at=received_at,
            reporter_reputation=profile.reputation,
            reporter_trust_level=profile.trust_level,
        )
        
        # Save report
        if self._db:
            try:
                self._db.collection(self.REPORTS_COLLECTION).document(report_id).set(report.to_dict())
                with self._lock:
                    self._recent_reports_cache.clear()
                    self._report_query_backoff_until = None
            except Exception as e:
                logger.warning("Save report error: %s", e)
                return False, "Database error", None, []
        
        # Update user stats and check badges
        new_badges = self._update_user_stats(profile, report)
        logger.info("After submit: user=%s total_reports=%d total_points=%d", profile.user_id, profile.total_reports, profile.total_points)
        
        return True, "Report submitted", report_id, new_badges
    
    def _update_user_stats(self, profile: UserProfile, report: WeatherReport) -> List[str]:
        """Update user stats after report.  Returns new badges."""
        new_badges = []
        
        # Update counts
        profile.total_reports += 1
        
        if report.has_photo:
            profile.photo_reports += 1
        
        if report.rain_intensity.value >= RainIntensity.HEAVY.value:
            profile.heavy_rain_reports += 1
        
        # Time-based counts use local civil time. Mizoram is UTC+5:30,
        # western Myanmar/Kabaw Valley is UTC+6:30.
        local_dt = local_report_datetime(report.timestamp, report.lon)
        local_hour = local_dt.hour
        if local_hour < 7:
            profile.early_reports += 1
        elif local_hour >= 22:
            profile.night_reports += 1
        
        # Streak update
        today = local_dt.strftime("%Y-%m-%d")
        if profile.last_report_date: 
            yesterday = (local_dt - timedelta(days=1)).strftime("%Y-%m-%d")
            if profile.last_report_date == yesterday:
                profile.current_streak += 1
            elif profile.last_report_date != today:
                profile.current_streak = 1
        else:
            profile.current_streak = 1
        
        profile.last_report_date = today
        profile.longest_streak = max(profile.longest_streak, profile.current_streak)
        
        # Small reputation bonus for photos
        if report.has_photo:
            profile.reputation = min(0.99, profile.reputation + 0.01)
        
        # Check badges
        def award(badge: BadgeType):
            if badge.value not in profile.badges:
                profile.badges.append(badge.value)
                profile.total_points += BADGE_POINTS.get(badge, 0)
                new_badges.append(badge.value)
        
        if profile.total_reports >= 1:
            award(BadgeType.FIRST_REPORT)
        if profile.total_reports >= 10:
            award(BadgeType.REPORT_10)
        if profile.total_reports >= 50:
            award(BadgeType.REPORT_50)
        if profile.total_reports >= 100:
            award(BadgeType.REPORT_100)
        
        if profile.current_streak >= 7:
            award(BadgeType.STREAK_7)
        if profile.current_streak >= 30:
            award(BadgeType.STREAK_30)
        
        if profile.photo_reports >= 50:
            award(BadgeType.PHOTO_PRO)
        if profile.heavy_rain_reports >= 10:
            award(BadgeType.STORM_CHASER)
        if profile.early_reports >= 50:
            award(BadgeType.EARLY_BIRD)
        if profile.night_reports >= 50:
            award(BadgeType.NIGHT_OWL)
        
        # Update trust level
        if profile.reputation >= 0.8:
            profile.trust_level = 4
        elif profile.reputation >= 0.6:
            profile.trust_level = 3
        elif profile.reputation >= 0.4:
            profile.trust_level = 2
        else:
            profile.trust_level = 1
        
        self._save_user(profile)
        return new_badges
    
    # === Fetch Reports ===
    
    def preload_recent_reports(
        self,
        minutes: int = 120,
        limit: int = CROWD_REPORT_QUERY_LIMIT,
        force: bool = False,
    ) -> List[Dict]:
        """Fetch recent reports once and cache them for reuse during a backend run."""
        if not self._db:
            return []

        now_ts = datetime.now(timezone.utc)
        with self._lock:
            cached = self._recent_reports_cache.get(minutes)
            backoff_until = self._report_query_backoff_until

        if cached and not force:
            age_sec = (now_ts - cached["fetched_at"]).total_seconds()
            if age_sec <= CROWD_PREFETCH_CACHE_SEC:
                return [dict(r) for r in cached.get("reports", [])]

        if backoff_until and now_ts < backoff_until:
            remaining = int((backoff_until - now_ts).total_seconds())
            if cached:
                logger.warning(
                    "Crowdsource query backoff active for %ds; reusing %d cached reports",
                    remaining,
                    len(cached.get("reports", [])),
                )
                return [dict(r) for r in cached.get("reports", [])]
            logger.warning("Crowdsource query backoff active for %ds; skipping live fetch", remaining)
            return []

        try:
            cutoff = (now_ts - timedelta(minutes=minutes)).isoformat()
            from google.cloud.firestore_v1.base_query import FieldFilter
            from google.cloud import firestore as gcloud_firestore

            snaps = (
                self._db.collection(self.REPORTS_COLLECTION)
                .where(filter=FieldFilter("timestamp", ">=", cutoff))
                .order_by("timestamp", direction=gcloud_firestore.Query.DESCENDING)
                .limit(limit)
                .get(timeout=25)
            )

            reports: List[Dict] = []
            for snap in snaps:
                payload = snap.to_dict() or {}
                payload.setdefault("report_id", getattr(snap, "id", None))
                reports.append(payload)

            with self._lock:
                self._recent_reports_cache[minutes] = {
                    "fetched_at": now_ts,
                    "reports": list(reports),
                }
                self._report_query_backoff_until = None
            return [dict(r) for r in reports]

        except Exception as e:
            msg = str(e).lower()
            if "429" in msg or "quota exceeded" in msg:
                backoff_until = now_ts + timedelta(minutes=CROWD_QUOTA_BACKOFF_MIN)
                with self._lock:
                    self._report_query_backoff_until = backoff_until
                logger.warning(
                    "Crowdsource query quota exceeded; disabling live fetches until %s",
                    backoff_until.isoformat(),
                )
            else:
                logger.warning("Get reports error: %s", e)

            with self._lock:
                cached = self._recent_reports_cache.get(minutes)
            return [dict(r) for r in cached.get("reports", [])] if cached else []

    def _filter_reports_for_location(
        self,
        reports: List[Dict],
        lat: float,
        lon: float,
        radius_km: float,
        minutes: int,
    ) -> List[Dict]:
        if not reports:
            return []

        cutoff_dt = datetime.now(timezone.utc) - timedelta(minutes=minutes)
        filtered: List[Dict] = []
        for raw in reports:
            try:
                d = dict(raw)
                ts = d.get("timestamp")
                if isinstance(ts, str):
                    ts = datetime.fromisoformat(ts.replace("Z", "+00:00"))
                if isinstance(ts, datetime):
                    if ts.tzinfo is None:
                        ts = ts.replace(tzinfo=timezone.utc)
                    if ts < cutoff_dt:
                        continue

                r_lat = float(d.get("lat", 0))
                r_lon = float(d.get("lon", 0))
                dist = haversine_km(lat, lon, r_lat, r_lon)
                if dist <= radius_km:
                    d["distance_km"] = round(dist, 2)
                    filtered.append(d)
            except Exception:
                continue

        filtered.sort(key=lambda x: x.get("distance_km", 999))
        return filtered

    def get_recent_reports(
        self,
        lat: float,
        lon: float,
        radius_km: float = 15.0,
        minutes: int = 60,
        preloaded_reports: Optional[List[Dict]] = None,
    ) -> List[Dict]:
        """Get recent reports near a location with cached prefetch support."""
        if preloaded_reports is None:
            base_reports = self.preload_recent_reports(minutes=minutes)
        else:
            base_reports = [dict(r) for r in preloaded_reports]
        return self._filter_reports_for_location(base_reports, lat, lon, radius_km, minutes)

    def _report_reputation(self, report: Dict[str, Any]) -> float:
        rep = safe_float(report.get("reporter_reputation"))
        if rep is None:
            user_id = str(report.get("user_id") or "").strip()
            if user_id:
                try:
                    rep = self.get_user(user_id).reputation
                except Exception:
                    rep = None
        trust = report.get("reporter_trust_level")
        try:
            trust_level = int(trust) if trust is not None else None
        except Exception:
            trust_level = None
        rep = max(0.1, min(rep if rep is not None else 0.5, 1.0))
        if trust_level is not None:
            rep *= max(0.85, min(1.10, 0.85 + 0.07 * max(0, trust_level - 1)))
        return min(rep, 1.2)

    def _collapse_reports_for_aggregation(self, reports: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
        """
        Keep only the freshest report per reporter/device so one user cannot
        over-dominate the crowd estimate with repeated submissions.
        """
        latest_by_reporter: Dict[str, Dict[str, Any]] = {}
        anonymous_reports: List[Dict[str, Any]] = []
        for raw in reports:
            report = dict(raw)
            reporter_key = str(report.get("user_id") or report.get("device_id") or "").strip()
            ts = parse_report_dt(report.get("timestamp")) or parse_report_dt(report.get("received_at"))
            report["_parsed_timestamp"] = ts
            if not reporter_key:
                anonymous_reports.append(report)
                continue
            previous = latest_by_reporter.get(reporter_key)
            prev_ts = previous.get("_parsed_timestamp") if previous else None
            if previous is None or (ts is not None and (prev_ts is None or ts > prev_ts)):
                latest_by_reporter[reporter_key] = report
        return list(latest_by_reporter.values()) + anonymous_reports
    
    # === Aggregation ===
    
    def aggregate_rainfall(
        self,
        lat: float,
        lon: float,
        reports: List[Dict],
        reference_mm: Optional[float] = None,
        return_quality: bool = False,
    ) -> Optional[float]:
        """
        Aggregate reports into estimated rainfall (mm/hr).
        Uses reputation-weighted IDW with recency + agreement scoring.
        """
        reports = self._collapse_reports_for_aggregation(reports)
        if len(reports) < 2:
            return (None, 0.0) if return_quality else None
        
        total_weight = 0.0
        weighted_sum = 0.0
        valid_count = 0
        now_ts = datetime.now(timezone.utc)
        
        for r in reports:
            try:
                r_lat = float(r["lat"])
                r_lon = float(r["lon"])
                rain_mm = report_rain_mm_from_payload(r, 0.0)
                
                dist = haversine_km(lat, lon, r_lat, r_lon)
                if dist > 15:
                    continue
                
                # Prefer the reputation snapshot stored on the report itself so
                # bulk aggregation does not need one Firestore user read per report.
                rep = self._report_reputation(r)
                
                # Recency weight (newer is stronger)
                age_min = 60.0
                ts = r.get("_parsed_timestamp") or parse_report_dt(r.get("timestamp")) or parse_report_dt(r.get("received_at"))
                if isinstance(ts, datetime):
                    age_min = max(0.0, (now_ts - ts).total_seconds() / 60.0)
                recency_weight = 0.5 ** (age_min / CROWD_RECENCY_HALFLIFE_MIN)
                
                # Agreement weight vs reference (station/model)
                agree_weight = 1.0
                if reference_mm is not None:
                    diff = abs(rain_mm - reference_mm)
                    agree_weight = math.exp(-diff / max(1.0, CROWD_AGREEMENT_SIGMA_MM))
                
                # Weight = reputation * distance * recency * agreement
                dist_weight = 1.0 / (1.0 + dist / 5.0)
                weight = rep * dist_weight * recency_weight * agree_weight
                
                weighted_sum += weight * rain_mm
                total_weight += weight
                valid_count += 1
            
            except Exception:
                continue
        
        if total_weight < 0.01 or valid_count == 0:
            return (None, 0.0) if return_quality else None
        
        value = round(weighted_sum / total_weight, 2)
        quality = min(1.0, total_weight / max(1.0, valid_count))
        
        if return_quality:
            return value, quality
        
        return value
    
    # === Leaderboard ===
    
    def get_leaderboard(self, limit: int = 20) -> List[Dict]:
        """Get top users by points."""
        if not self._db:
            return []
        
        try:
            from google.cloud import firestore as gcloud_firestore
            snaps = (
                self._db.collection(self.USERS_COLLECTION)
                .order_by("total_points", direction=gcloud_firestore.Query.DESCENDING)
                .limit(limit)
                .get()
            )
            
            result = []
            for i, snap in enumerate(snaps, 1):
                d = snap.to_dict()
                result.append({
                    "rank": i,
                    "user_id": d.get("user_id"),
                    "display_name": d.get("display_name") or f"User_{d.get('user_id', '')[:6]}",
                    "total_points": d.get("total_points", 0),
                    "total_reports": d.get("total_reports", 0),
                    "badges_count": len(d.get("badges", [])),
                })
            
            return result
        
        except Exception as e: 
            logger.warning("Leaderboard error: %s", e)
            return []
    
    # === Badges Info ===
    
    def get_all_badges(self, user_id: Optional[str] = None) -> List[Dict]:
        """Get all badge definitions with earned status."""
        earned = []
        if user_id:
            profile = self.get_user(user_id)
            earned = profile.badges
        
        badges = []
        for badge_type in BadgeType:
            info = BADGE_INFO.get(badge_type, ("Unknown", "Unknown", "❓", ""))
            badges.append({
                "badge_type": badge_type.value,
                "name": info[0],
                "name_mizo": info[1],
                "icon": info[2],
                "description": info[3],
                "points": BADGE_POINTS.get(badge_type, 0),
                "earned": badge_type.value in earned,
            })
        
        return badges


# ═══════════════════════════════════════════════════════════════════════════════
# INTEGRATION WITH MAIN BACKEND
# ═══════════════════════════════════════════════════════════════════════════════

def apply_crowdsource_nowcast(
    blended_precip: List,
    lat: float,
    lon: float,
    station_data: List[Dict],
    crowd_manager: CrowdsourceManager,
    wind_dir_deg: Optional[float] = None,
    prefetched_reports: Optional[List[Dict]] = None,
) -> List:
    """
    Enhanced nowcast using crowdsource data.
    Call this from main backend's process_cell function.
    """
    out = list(blended_precip)
    
    # Get crowdsource reports
    crowd_reports = crowd_manager.get_recent_reports(
        lat,
        lon,
        radius_km=15,
        minutes=60,
        preloaded_reports=prefetched_reports,
    )
    
    # Get station value (existing)
    station_val = None
    if station_data: 
        total_w = 0.0
        weighted_sum = 0.0
        for s in station_data:
            try:
                s_lat = float(s["lat"])
                s_lon = float(s["lon"])
                rain = float(s.get("rain_mm", 0))
                d = haversine_km(lat, lon, s_lat, s_lon)
                flow_w = upwind_weight_factor(lat, lon, s_lat, s_lon, wind_dir_deg)
                w = flow_w / (d**2 + 0.001)
                weighted_sum += w * rain
                total_w += w
            except: 
                continue
        if total_w > 0:
            station_val = weighted_sum / total_w
    
    # Get crowdsource value
    model_now = out[0] if out else 0.0
    ref_mm = station_val if station_val is not None else model_now
    crowd_val, crowd_q = crowd_manager.aggregate_rainfall(
        lat,
        lon,
        crowd_reports,
        reference_mm=ref_mm,
        return_quality=True,
    )
    if crowd_q < CROWD_MIN_QUALITY:
        crowd_val = None
    
    # Combine (station takes priority)
    if station_val is not None:
        now_val = station_val
        if crowd_val is not None:
            # Quality-scaled crowd influence
            crowd_w = min(0.5, 0.2 + 0.6 * crowd_q)
            now_val = (1.0 - crowd_w) * station_val + crowd_w * crowd_val
    elif crowd_val is not None:
        now_val = crowd_val
    else:
        now_val = None
    
    # Apply nowcast blending (first 4 hours)
    if now_val is not None:
        base = out[0] if out else 0
        for i in range(min(len(out), 4)):
            decay = 1.0 / (1 + i * 0.6)
            obs_w = 0.8 * decay
            model_w = 1.0 - obs_w
            model_val = out[i] if out[i] is not None else base
            out[i] = round(obs_w * now_val + model_w * model_val, 2)
    
    return out
