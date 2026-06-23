"""
SQLAlchemy Database Models
Complete schema for SafeWatch application
"""
from sqlalchemy import Column, String, Float, Integer, Boolean, DateTime, Text, ForeignKey, Index
from sqlalchemy.orm import relationship
from database import Base
from datetime import datetime
import uuid
import enum

# ==========================================
# ENUM DEFINITIONS
# ==========================================

class HazardType(str, enum.Enum):
    POOR_LIGHTING = "Poor Lighting"
    SUSPICIOUS_ACTIVITY = "Suspicious Activity"
    AGGRESSIVE_CROWD = "Aggressive Crowd"
    OBSTRUCTION = "Obstruction"

class SOSEventType(str, enum.Enum):
    LAST_BREATH_PROTOCOL = "LAST_BREATH_PROTOCOL_BROADCAST"
    SAFEZONE_VIOLATION = "SAFEZONE_VIOLATION_AUTO_ALERT"
    USER_TRIGGERED = "USER_TRIGGERED_SOS"

# ==========================================
# USER MODEL
# ==========================================

class User(Base):
    __tablename__ = "users"
    
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    email = Column(String(255), unique=True, nullable=True, index=True)
    phone = Column(String(50), unique=True, nullable=True, index=True)
    password_hash = Column(String(255), nullable=False)
    name = Column(String(255), default="New Operator")
    blood_group = Column(String(10), default="O+")
    address = Column(Text, nullable=True)
    emergency_phone = Column(String(20), nullable=True)
    avatar = Column(Text, default="avatar-shield")
    
    # Verification and password reset
    is_verified = Column(Boolean, default=False)
    verification_code = Column(String(10), nullable=True)
    reset_code = Column(String(10), nullable=True)

    
    # Current location
    current_lat = Column(Float, default=0.0)
    current_lng = Column(Float, default=0.0)
    current_speed = Column(Float, default=0.0)
    current_battery = Column(Float, default=100.0)
    current_signal = Column(Integer, default=100)
    last_location_update = Column(DateTime, default=datetime.utcnow)
    
    # SOS Status
    sos_active = Column(Boolean, default=False)
    
    # FCM Push Notification Token (registered from browser/app)
    fcm_token = Column(String(512), nullable=True)
    
    # Settings
    settings_shake_to_alert = Column(Boolean, default=True)
    settings_hardware_trigger = Column(Boolean, default=True)
    settings_last_breath = Column(Boolean, default=True)
    settings_location_history = Column(Boolean, default=True)
    settings_amoled_mode = Column(Boolean, default=False)
    settings_accent_color = Column(String(50), default="neon-blue")
    
    # Timestamps
    created_at = Column(DateTime, default=datetime.utcnow, index=True)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    deleted_at = Column(DateTime, nullable=True)  # Soft delete
    
    # Relationships
    location_history = relationship("LocationHistory", back_populates="user", cascade="all, delete-orphan")
    safe_zones = relationship("SafeZone", back_populates="user", cascade="all, delete-orphan")
    safe_zone_violations = relationship("SafeZoneViolation", back_populates="user", cascade="all, delete-orphan")
    emergency_contacts = relationship("EmergencyContact", back_populates="user", cascade="all, delete-orphan")
    hazard_reports = relationship("HazardReport", back_populates="user", cascade="all, delete-orphan")
    sos_events = relationship("SOSEvent", back_populates="user", cascade="all, delete-orphan")
    emergency_circle = relationship("EmergencyCircle", back_populates="user", foreign_keys="EmergencyCircle.user_id")
    user_activities = relationship("UserActivity", back_populates="user", cascade="all, delete-orphan")

# ==========================================
# LOCATION TRACKING
# ==========================================

class LocationHistory(Base):
    __tablename__ = "location_history"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    speed = Column(Float, default=0.0)
    battery = Column(Float, default=100.0)
    signal_strength = Column(Integer, default=100)
    safety_score = Column(Integer, default=98)

    created_at = Column(DateTime, default=datetime.utcnow, index=True)

    # Relationship
    user = relationship("User", back_populates="location_history")
    
    __table_args__ = (
        Index("idx_user_location_time", "user_id", "created_at"),
    )

# ==========================================
# SAFE ZONE MANAGEMENT
# ==========================================

class SafeZone(Base):
    __tablename__ = "safe_zones"
    
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    radius = Column(Float, default=500.0)
    is_active = Column(Boolean, default=False)
    
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Relationship
    user = relationship("User", back_populates="safe_zones")

class SafeZoneViolation(Base):
    __tablename__ = "safe_zone_violations"
    
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    distance = Column(Float, nullable=False)
    
    created_at = Column(DateTime, default=datetime.utcnow, index=True)
    
    # Relationship
    user = relationship("User", back_populates="safe_zone_violations")

# ==========================================
# EMERGENCY CONTACTS
# ==========================================

class EmergencyContact(Base):
    __tablename__ = "emergency_contacts"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    name = Column(String(255), nullable=False)
    phone = Column(String(20), nullable=False)
    relation_type = Column(String(50), default="Family")

    created_at = Column(DateTime, default=datetime.utcnow)

    user = relationship("User", back_populates="emergency_contacts")

# ==========================================
# EMERGENCY CIRCLE (TRUSTED CONNECTIONS)
# ==========================================

class EmergencyCircle(Base):
    __tablename__ = "emergency_circle"
    
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    connected_user_id = Column(String(36), ForeignKey("users.id"), nullable=True, index=True)
    status = Column(String(20), default="pending")  # pending, accepted, rejected
    
    created_at = Column(DateTime, default=datetime.utcnow)
    updated_at = Column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    # Relationships
    user = relationship("User", back_populates="emergency_circle", foreign_keys=[user_id])

# ==========================================
# HAZARD REPORTING (COMMUNITY THREAT MAP)
# ==========================================

class HazardReport(Base):
    __tablename__ = "hazard_reports"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    hazard_type = Column(String(50), nullable=False)

    created_at = Column(DateTime, default=datetime.utcnow, index=True)

    # Relationship
    user = relationship("User", back_populates="hazard_reports")

    __table_args__ = (
        Index("idx_hazard_location", "latitude", "longitude", "created_at"),
    )

# ==========================================
# SOS EVENTS
# ==========================================

class SOSEvent(Base):
    __tablename__ = "sos_events"
    
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    event_type = Column(String(50), nullable=False)  # LAST_BREATH, SAFEZONE_VIOLATION, USER_TRIGGERED
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    battery = Column(Float, nullable=True)
    distance = Column(Float, nullable=True)  # For SafeZone violations
    
    created_at = Column(DateTime, default=datetime.utcnow, index=True)
    
    # Relationship
    user = relationship("User", back_populates="sos_events")


# ==========================================
# SMS TRANSMISSION LOGS
# ==========================================

class SMSLog(Base):
    __tablename__ = "sms_logs"
    
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    recipient_name = Column(String(255), nullable=False)
    recipient_phone = Column(String(50), nullable=False)
    message_body = Column(Text, nullable=False)
    status = Column(String(50), default="pending")  # sent, failed, mock_sent
    error_message = Column(Text, nullable=True)
    
    created_at = Column(DateTime, default=datetime.utcnow, index=True)
    
    # Relationship
    user = relationship("User")


# ==========================================
# USER ACTIVITIES
# ==========================================

class UserActivity(Base):
    __tablename__ = "user_activities"
    
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), ForeignKey("users.id", ondelete="CASCADE"), nullable=False, index=True)
    activity_type = Column(String(100), nullable=False)
    details = Column(Text, nullable=True)
    created_at = Column(DateTime, default=datetime.utcnow, index=True)
    
    # Relationship
    user = relationship("User", back_populates="user_activities")
