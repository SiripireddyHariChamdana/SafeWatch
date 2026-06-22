"""
SafeWatch Backend - PostgreSQL + JWT Authentication
Production-grade API with database persistence
"""
import os
import re
from datetime import datetime, timedelta
from typing import Optional, Dict, Any, List
from fastapi import FastAPI, HTTPException, Depends, Header, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from fastapi.responses import RedirectResponse
from pydantic import BaseModel, EmailStr
from sqlalchemy.orm import Session
from sqlalchemy import desc, text
import jwt
import bcrypt
import math
import random
import string
import pathlib
from dotenv import load_dotenv


# Load environment variables from backend/.env
_BACKEND_DIR = os.path.dirname(os.path.abspath(__file__))
_ENV_PATH = os.path.join(_BACKEND_DIR, ".env")
load_dotenv(_ENV_PATH)

# Firebase Admin SDK for FCM Push Notifications
try:
    import firebase_admin
    from firebase_admin import credentials as fb_credentials, messaging as fb_messaging
    FIREBASE_AVAILABLE = True
except ImportError:
    FIREBASE_AVAILABLE = False
    print("[!] firebase-admin not installed. Run: pip install firebase-admin")

from database import engine, SessionLocal, init_db, Base, get_db, SECRET_KEY
from models import (
    User, LocationHistory, SafeZone, SafeZoneViolation,
    EmergencyContact, SOSEvent, EmergencyCircle, SMSLog
)

# Import HazardReport model with alias to avoid name clash
from models import HazardReport as HazardReportModel

# ==========================================
# FASTAPI INITIALIZATION
# ==========================================

app = FastAPI(
    title="SAFEWATCH Professional API",
    description="Production-grade personal safety monitoring backend with PostgreSQL"
)

# CORS Configuration
ALLOWED_ORIGINS = os.getenv("ALLOWED_ORIGINS", "http://localhost:3000,http://localhost:8000").split(",")
app.add_middleware(
    CORSMiddleware,
    allow_origins=[origin.strip() for origin in ALLOWED_ORIGINS],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount frontend static files (served from ../frontend relative to backend/)
_FRONTEND_DIR = pathlib.Path(__file__).parent.parent / "frontend"
if _FRONTEND_DIR.exists():
    app.mount("/static", StaticFiles(directory=str(_FRONTEND_DIR), html=True), name="static")
else:
    print(f"[!] Frontend directory not found at {_FRONTEND_DIR}")

# Initialize database tables
@app.on_event("startup")
def startup():
    """Initialize database and Firebase on application startup"""
    try:
        Base.metadata.create_all(bind=engine)
        print("[+] Database tables initialized successfully")
        
        # Check and alter users table to add settings_last_breath column if it doesn't exist
        db = SessionLocal()
        try:
            db.execute(text("SELECT settings_last_breath FROM users LIMIT 1"))
        except Exception:
            db.rollback()
            print("[*] Adding settings_last_breath column to users table...")
            db.execute(text("ALTER TABLE users ADD COLUMN settings_last_breath BOOLEAN DEFAULT 1"))
            db.commit()
            print("[+] settings_last_breath column added successfully")
        finally:
            db.close()

        # Check and alter users table to add settings_location_history column if it doesn't exist
        db = SessionLocal()
        try:
            db.execute(text("SELECT settings_location_history FROM users LIMIT 1"))
        except Exception:
            db.rollback()
            print("[*] Adding settings_location_history column to users table...")
            db.execute(text("ALTER TABLE users ADD COLUMN settings_location_history BOOLEAN DEFAULT 1"))
            db.commit()
            print("[+] settings_location_history column added successfully")
        finally:
            db.close()
    except Exception as e:
        print(f"[!] Failed to initialize database tables: {e}")
        raise
    
    # Initialize Firebase Admin SDK for FCM
    if FIREBASE_AVAILABLE:
        sa_path = os.getenv("FIREBASE_SERVICE_ACCOUNT_PATH", os.path.join(_BACKEND_DIR, "ServiceAccountKey.json"))
        if os.path.exists(sa_path) and not firebase_admin._apps:
            try:
                cred = fb_credentials.Certificate(sa_path)
                firebase_admin.initialize_app(cred)
                print(f"[+] Firebase Admin SDK initialized from {sa_path}")
            except Exception as e:
                print(f"[!] Failed to initialize Firebase: {e}")
        else:
            if not os.path.exists(sa_path):
                print(f"[!] Firebase service account not found at {sa_path}")
            else:
                print("[!] Firebase already initialized")

# ==========================================
# ROOT INDEX REDIRECT
# ==========================================

@app.get("/")
async def root_redirect():
    """Redirect root access to static frontend index.html"""
    return RedirectResponse(url="/static/index.html")

# ==========================================
# HEALTH CHECK ENDPOINT
# ==========================================

@app.get("/api/health")
async def health_check():
    """Health check endpoint for CI/CD pipelines and monitoring"""
    return {
        "status": "healthy",
        "service": "SafeWatch API",
        "version": "1.0.0",
        "timestamp": datetime.utcnow().isoformat()
    }

# ==========================================
# SECURITY & JWT CONFIGURATION
# ==========================================

ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_MINUTES = int(os.getenv("TOKEN_EXPIRE_MINUTES", "1440"))  # 24 hours

# ==========================================
# PASSWORD HASHING
# ==========================================

def hash_password(password: str) -> str:
    """Hash password using bcrypt (more secure than SHA256)"""
    salt = bcrypt.gensalt()
    return bcrypt.hashpw(password.encode(), salt).decode()

def verify_password(plain_password: str, hashed_password: str) -> bool:
    """Verify password against hash"""
    try:
        return bcrypt.checkpw(plain_password.encode(), hashed_password.encode())
    except (ValueError, TypeError):
        # Handle invalid hash format
        return False

# ==========================================
# JWT TOKEN HANDLING
# ==========================================

def create_access_token(data: Dict[str, Any], expires_delta: Optional[timedelta] = None) -> str:
    """Create JWT access token"""
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=15)
    
    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt

def verify_token(token: str, db: Session) -> User:
    """Verify JWT token and return user (email or phone)"""
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        sub: str = payload.get("sub")
        if sub is None:
            raise HTTPException(status_code=401, detail="Invalid token")
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token expired")
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Invalid token")
    
    user = db.query(User).filter(
        ((User.email == sub) | (User.phone == sub)),
        User.deleted_at.is_(None)
    ).first()
    if user is None:
        raise HTTPException(status_code=401, detail="User not found")
    return user

# ==========================================
# DEPENDENCY: GET CURRENT USER FROM TOKEN
# ==========================================

def get_current_user(authorization: Optional[str] = Header(None), db: Session = Depends(get_db)) -> User:
    """Extract user from Authorization header"""
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Authorization header missing or invalid")
    
    token = authorization.split(" ", 1)[1]
    return verify_token(token, db)

# ==========================================
# PYDANTIC SCHEMAS
# ==========================================

class AuthRequest(BaseModel):
    action: str  # "login" or "signup"
    email: Optional[str] = None
    phone: Optional[str] = None
    password: str
    name: Optional[str] = ""
    blood_group: Optional[str] = "O+"
    address: Optional[str] = ""
    emergency_phone: Optional[str] = ""
    avatar: Optional[str] = "avatar-shield"

class UserResponse(BaseModel):
    id: str
    email: Optional[str] = None
    phone: Optional[str] = None
    name: str
    blood_group: str
    address: Optional[str]
    emergency_phone: Optional[str]
    avatar: str
    sos_active: bool
    is_verified: bool
    created_at: datetime
    settings_shake_to_alert: Optional[bool] = True
    settings_hardware_trigger: Optional[bool] = True
    settings_last_breath: Optional[bool] = True
    settings_amoled_mode: Optional[bool] = False
    settings_accent_color: Optional[str] = "accent-blue"
    
    class Config:
        from_attributes = True

class LoginResponse(BaseModel):
    status: str
    message: str
    user: Optional[UserResponse] = None
    access_token: str
    token_type: str = "bearer"

class ProfileUpdate(BaseModel):
    name: str
    emergency_phone: str
    blood_group: str
    address: str
    avatar: str

class TelemetrySync(BaseModel):
    lat: float
    lng: float
    speed: float
    battery: float
    signal: int

class SafeZoneConfig(BaseModel):
    lat: float
    lng: float
    radius: float
    active: bool

class HazardReportSchema(BaseModel):
    lat: float
    lng: float
    type: str

class DirectContactSchema(BaseModel):
    name: str
    relationship: str
    phone: str

class ContactsRequest(BaseModel):
    name: Optional[str] = None
    relationship: Optional[str] = None
    phone: Optional[str] = None
    index: Optional[int] = None

class InviteRequest(BaseModel):
    phone: str

class RespondInviteRequest(BaseModel):
    sender_phone: str
    accept: bool

class VerifyRequest(BaseModel):
    identifier: str
    code: str

class ForgotPasswordRequest(BaseModel):
    identifier: str

class ResetPasswordRequest(BaseModel):
    identifier: str
    code: str
    new_password: str

class VerifyPasswordRequest(BaseModel):
    password: str

# ==========================================
# HELPER FUNCTIONS - VALIDATION
# ==========================================

EMAIL_REGEX = re.compile(r"^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\.[a-zA-Z0-9-.]+$")
PHONE_REGEX = re.compile(r"^\+?[0-9\s\-()]{3,20}$")

def is_valid_email(email: str) -> bool:
    """Validate email format"""
    return bool(EMAIL_REGEX.match(email))

def is_valid_phone(phone: str) -> bool:
    """Validate phone format"""
    return bool(PHONE_REGEX.match(phone))

def normalize_phone_number(phone: str) -> str:
    """Normalize phone number to standard format"""
    if not phone:
        return ""
    phone = str(phone).strip()  # Ensure string type
    prefix = "+" if phone.startswith("+") else ""
    digits = "".join(c for c in phone if c.isdigit())
    return prefix + digits if digits else ""

# ==========================================
# HELPER FUNCTIONS - FCM & DISTANCE
# ==========================================

def send_fcm_notification(token: str, title: str, body: str, data: Dict[str, str] = None):
    """Send FCM push notification to a device token"""
    if not FIREBASE_AVAILABLE or not firebase_admin._apps:
        print("[FCM] Firebase not initialized, skipping push notification")
        return False
    if not token:
        return False
    if token.startswith("mock_"):
        print(f"[FCM] [SIMULATOR] Mock notification dispatched to token: {token} - Title: {title}")
        return True
    try:
        message = fb_messaging.Message(
            notification=fb_messaging.Notification(title=title, body=body),
            data=data or {},
            token=token,
            android=fb_messaging.AndroidConfig(priority="high"),
            webpush=fb_messaging.WebpushConfig(
                headers={"Urgency": "high"},
                notification=fb_messaging.WebpushNotification(
                    title=title,
                    body=body,
                    icon="/static/icon.png"
                )
            )
        )
        response = fb_messaging.send(message)
        print(f"[FCM] Push sent successfully: {response}")
        return True
    except Exception as e:
        print(f"[FCM] Push failed: {e}")
        return False

def send_resend_email(to_email: str, subject: str, html_content: str) -> bool:
    """Send an email using Resend API via standard library urllib (no dependencies needed)"""
    import urllib.request
    import json
    
    resend_api_key = os.getenv("RESEND_API_KEY", "re_4LkDf6ij_2Xv92GBi5568UbM6j8dDusid")
    if not resend_api_key:
        print("[Resend] API key not found in environment")
        return False
        
    url = "https://api.resend.com/emails"
    headers = {
        "Authorization": f"Bearer {resend_api_key}",
        "Content-Type": "application/json",
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
    payload = {
        "from": "SafeWatch <onboarding@resend.dev>",
        "to": [to_email],
        "subject": subject,
        "html": html_content
    }
    
    try:
        req = urllib.request.Request(
            url,
            data=json.dumps(payload).encode("utf-8"),
            headers=headers,
            method="POST"
        )
        with urllib.request.urlopen(req) as response:
            res_body = response.read().decode("utf-8")
            print(f"[Resend] Email sent successfully: {res_body}")
            return True
    except Exception as e:
        print(f"[Resend Error] Failed to send email: {e}")
        return False

def calculate_haversine_distance(lat1: float, lng1: float, lat2: float, lng2: float) -> float:
    """Calculate distance between two coordinates in meters"""
    R = 6371000  # meters
    phi1 = math.radians(lat1)
    phi2 = math.radians(lat2)
    dphi = math.radians(lat2 - lat1)
    dlng = math.radians(lng2 - lng1)
    
    a = math.sin(dphi/2)**2 + math.cos(phi1)*math.cos(phi2)*math.sin(dlng/2)**2
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
    return R * c

def calculate_safety_score(user: User, hazards: list) -> int:
    """Calculate safety score based on proximity to hazards"""
    safety_score = 98
    for hazard in hazards:
        distance = calculate_haversine_distance(user.current_lat, user.current_lng, hazard.latitude, hazard.longitude)
        if distance < 150:  # within 150 meters
            safety_score = max(50, safety_score - 15)
    return safety_score

# ==========================================
# API ENDPOINTS: AUTHENTICATION
# ==========================================

@app.post("/api/auth", response_model=LoginResponse)
async def api_auth(req: AuthRequest, db: Session = Depends(get_db)):
    """Authentication endpoint - login or signup"""
    email = req.email.lower().strip() if req.email else None
    phone = normalize_phone_number(req.phone) if req.phone else None
    
    if req.action == "signup":
        if not email and not phone:
            raise HTTPException(status_code=400, detail="Either email or phone is required for signup")
            
        if email and not is_valid_email(email):
            raise HTTPException(status_code=400, detail="Invalid email format")
            
        if phone and not is_valid_phone(phone):
            raise HTTPException(status_code=400, detail="Invalid phone format")
            
        if req.emergency_phone:
            if not is_valid_phone(req.emergency_phone):
                raise HTTPException(status_code=400, detail="Invalid emergency phone format")
            
        # Check if user already exists
        if email:
            existing = db.query(User).filter(User.email == email).first()
            if existing:
                raise HTTPException(status_code=400, detail="Email already registered")
        if phone:
            existing = db.query(User).filter(User.phone == phone).first()
            if existing:
                raise HTTPException(status_code=400, detail="Phone number already registered")
                
        # Determine if pre-verified (e.g. test accounts)
        is_test = False
        if email and ("@example.com" in email or "test" in email):
            is_test = True
        if phone and ("555" in phone or "12345" in phone):
            is_test = True
            
        code = f"{random.randint(100000, 999999)}"
        new_user = User(
            email=email,
            phone=phone,
            password_hash=hash_password(req.password),
            name=req.name or "New Operator",
            blood_group=req.blood_group or "O+",
            address=req.address or "",
            emergency_phone=normalize_phone_number(req.emergency_phone) if req.emergency_phone else "",
            avatar=req.avatar or "avatar-shield",
            is_verified=is_test,
            verification_code=None if is_test else code
        )
        
        db.add(new_user)
        db.commit()
        db.refresh(new_user)
        
        # Send verification code if not pre-verified
        if not is_test:
            print(f"[VERIFICATION CODE] User: {email or phone} | Code: {code}")
            # Try to send email
            if email:
                resend_success = send_resend_email(
                    email,
                    "SafeWatch Verification Code",
                    f"<p>Your SafeWatch signup verification code is: <strong>{code}</strong></p>"
                )
                if not resend_success:
                    gmail_user = os.getenv("GMAIL_USER")
                    gmail_pass = os.getenv("GMAIL_APP_PASSWORD")
                    if gmail_user and gmail_pass:
                        try:
                            import smtplib
                            from email.mime.multipart import MIMEMultipart
                            from email.mime.text import MIMEText
                            msg = MIMEMultipart("alternative")
                            msg["Subject"] = "SafeWatch Verification Code"
                            msg["From"] = gmail_user
                            msg["To"] = email
                            msg.attach(MIMEText(f"Your SafeWatch signup verification code is: {code}", "plain"))
                            with smtplib.SMTP_SSL("smtp.gmail.com", 465) as server:
                                server.login(gmail_user, gmail_pass)
                                server.sendmail(gmail_user, [email], msg.as_string())
                        except Exception as e:
                            print(f"[SMTP ERROR] Verification email failed: {e}")
            # Try to send SMS/WhatsApp
            if phone:
                twilio_sid = os.getenv("TWILIO_ACCOUNT_SID")
                twilio_token = os.getenv("TWILIO_AUTH_TOKEN")
                twilio_from = os.getenv("TWILIO_FROM_NUMBER")
                if twilio_sid and twilio_token and twilio_from:
                    try:
                        from twilio.rest import Client as TwilioClient
                        twilio = TwilioClient(twilio_sid, twilio_token)
                        # Try WhatsApp first
                        try:
                            twilio.messages.create(
                                body=f"Your SafeWatch signup verification code is: {code}",
                                from_=f"whatsapp:{twilio_from}",
                                to=f"whatsapp:{phone}"
                            )
                        except Exception:
                            # Fallback to SMS
                            twilio.messages.create(
                                body=f"Your SafeWatch signup verification code is: {code}",
                                from_=twilio_from,
                                to=phone
                            )
                    except Exception as e:
                        print(f"[TWILIO ERROR] Verification SMS failed: {e}")
                        
        return LoginResponse(
            status="success",
            message="Signup successful. Verification code sent." if not is_test else "Signup successful.",
            user=UserResponse.from_orm(new_user),
            access_token=""
        )
        
    elif req.action == "login":
        identifier = email if email else phone
        if not identifier:
            raise HTTPException(status_code=400, detail="Email or phone number is required")
            
        clean_identifier = identifier
            
        user = db.query(User).filter(
            ((User.email == clean_identifier) | (User.phone == clean_identifier)),
            User.deleted_at.is_(None)
        ).first()
        
        if not user or not verify_password(req.password, user.password_hash):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid credentials"
            )
            
        if not user.is_verified:
            # Resend verification code
            code = f"{random.randint(100000, 999999)}"
            user.verification_code = code
            db.commit()
            print(f"[VERIFICATION CODE RESEND] User: {clean_identifier} | Code: {code}")
            
            # Send code logic
            if user.email:
                resend_success = send_resend_email(
                    user.email,
                    "SafeWatch Verification Code",
                    f"<p>Your SafeWatch signup verification code is: <strong>{code}</strong></p>"
                )
                if not resend_success:
                    gmail_user = os.getenv("GMAIL_USER")
                    gmail_pass = os.getenv("GMAIL_APP_PASSWORD")
                    if gmail_user and gmail_pass:
                        try:
                            import smtplib
                            from email.mime.multipart import MIMEMultipart
                            from email.mime.text import MIMEText
                            msg = MIMEMultipart("alternative")
                            msg["Subject"] = "SafeWatch Verification Code"
                            msg["From"] = gmail_user
                            msg["To"] = user.email
                            msg.attach(MIMEText(f"Your SafeWatch signup verification code is: {code}", "plain"))
                            with smtplib.SMTP_SSL("smtp.gmail.com", 465) as server:
                                server.login(gmail_user, gmail_pass)
                                server.sendmail(gmail_user, [user.email], msg.as_string())
                        except Exception as e:
                            print(f"[SMTP ERROR] Verification email failed: {e}")
            if user.phone:
                twilio_sid = os.getenv("TWILIO_ACCOUNT_SID")
                twilio_token = os.getenv("TWILIO_AUTH_TOKEN")
                twilio_from = os.getenv("TWILIO_FROM_NUMBER")
                if twilio_sid and twilio_token and twilio_from:
                    try:
                        from twilio.rest import Client as TwilioClient
                        twilio = TwilioClient(twilio_sid, twilio_token)
                        try:
                            twilio.messages.create(
                                body=f"Your SafeWatch signup verification code is: {code}",
                                from_=f"whatsapp:{twilio_from}",
                                to=f"whatsapp:{user.phone}"
                            )
                        except Exception:
                            twilio.messages.create(
                                body=f"Your SafeWatch signup verification code is: {code}",
                                from_=twilio_from,
                                to=user.phone
                            )
                    except Exception as e:
                        print(f"[TWILIO ERROR] Verification SMS failed: {e}")
                        
            raise HTTPException(
                status_code=400,
                detail="Account not verified. Verification code has been sent."
            )
            
        access_token = create_access_token(
            data={"sub": user.email if user.email else user.phone},
            expires_delta=timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
        )
        
        return LoginResponse(
            status="success",
            message="Login successful",
            user=UserResponse.from_orm(user),
            access_token=access_token
        )
    else:
        raise HTTPException(status_code=400, detail="Invalid action")

@app.post("/api/auth/verify")
async def verify_code(req: VerifyRequest, db: Session = Depends(get_db)):
    """Verify account with verification code"""
    identifier = req.identifier.strip()
    if "@" not in identifier:
        identifier = normalize_phone_number(identifier)
    else:
        identifier = identifier.lower()
    user = db.query(User).filter(
        ((User.email == identifier) | (User.phone == identifier)),
        User.deleted_at.is_(None)
    ).first()
    
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    if user.verification_code == req.code:
        user.is_verified = True
        user.verification_code = None
        db.commit()
        
        access_token = create_access_token(
            data={"sub": user.email if user.email else user.phone},
            expires_delta=timedelta(minutes=ACCESS_TOKEN_EXPIRE_MINUTES)
        )
        return {
            "status": "success",
            "message": "Account successfully verified.",
            "user": UserResponse.from_orm(user),
            "access_token": access_token
        }
    else:
        raise HTTPException(status_code=400, detail="Invalid verification code")

@app.post("/api/auth/forgot-password")
async def forgot_password(req: ForgotPasswordRequest, db: Session = Depends(get_db)):
    """Request password reset code"""
    identifier = req.identifier.strip()
    if "@" not in identifier:
        identifier = normalize_phone_number(identifier)
    else:
        identifier = identifier.lower()
    user = db.query(User).filter(
        ((User.email == identifier) | (User.phone == identifier)),
        User.deleted_at.is_(None)
    ).first()
    
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    code = f"{random.randint(100000, 999999)}"
    user.reset_code = code
    db.commit()
    
    print(f"[RESET CODE] User: {identifier} | Code: {code}")
    
    # Send code logic (SMTP for email, Twilio for phone)
    if user.email:
        resend_success = send_resend_email(
            user.email,
            "SafeWatch Password Reset Code",
            f"<p>Your password reset code is: <strong>{code}</strong></p>"
        )
        if not resend_success:
            gmail_user = os.getenv("GMAIL_USER")
            gmail_pass = os.getenv("GMAIL_APP_PASSWORD")
            if gmail_user and gmail_pass:
                try:
                    import smtplib
                    from email.mime.multipart import MIMEMultipart
                    from email.mime.text import MIMEText
                    msg = MIMEMultipart("alternative")
                    msg["Subject"] = "SafeWatch Password Reset Code"
                    msg["From"] = gmail_user
                    msg["To"] = user.email
                    msg.attach(MIMEText(f"Your password reset code is: {code}", "plain"))
                    with smtplib.SMTP_SSL("smtp.gmail.com", 465) as server:
                        server.login(gmail_user, gmail_pass)
                        server.sendmail(gmail_user, [user.email], msg.as_string())
                except Exception as e:
                    print(f"[SMTP ERROR] Forgot email failed: {e}")
                
    if user.phone:
        twilio_sid = os.getenv("TWILIO_ACCOUNT_SID")
        twilio_token = os.getenv("TWILIO_AUTH_TOKEN")
        twilio_from = os.getenv("TWILIO_FROM_NUMBER")
        if twilio_sid and twilio_token and twilio_from:
            try:
                from twilio.rest import Client as TwilioClient
                twilio = TwilioClient(twilio_sid, twilio_token)
                clean_phone = normalize_phone_number(user.phone)
                try:
                    twilio.messages.create(
                        body=f"Your SafeWatch password reset code is: {code}",
                        from_=f"whatsapp:{twilio_from}",
                        to=f"whatsapp:{clean_phone}"
                    )
                except Exception:
                    twilio.messages.create(
                        body=f"Your SafeWatch password reset code is: {code}",
                        from_=twilio_from,
                        to=clean_phone
                    )
            except Exception as e:
                print(f"[TWILIO ERROR] Forgot SMS failed: {e}")
                
    return {"status": "success", "message": "Password reset code sent."}

@app.post("/api/auth/reset-password")
async def reset_password(req: ResetPasswordRequest, db: Session = Depends(get_db)):
    """Reset password with verification code"""
    identifier = req.identifier.strip()
    if "@" not in identifier:
        identifier = normalize_phone_number(identifier)
    else:
        identifier = identifier.lower()
    user = db.query(User).filter(
        ((User.email == identifier) | (User.phone == identifier)),
        User.deleted_at.is_(None)
    ).first()
    
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
        
    if not user.reset_code or user.reset_code != req.code:
        raise HTTPException(status_code=400, detail="Invalid reset code")
        
    user.password_hash = hash_password(req.new_password)
    user.reset_code = None
    db.commit()
    
    return {"status": "success", "message": "Password successfully reset."}

@app.post("/api/auth/verify-password")
async def verify_user_password(
    req: VerifyPasswordRequest,
    current_user: User = Depends(get_current_user)
):
    """Verify password for high-privilege/disarm operations"""
    if not verify_password(req.password, current_user.password_hash):
        raise HTTPException(status_code=401, detail="Incorrect password")
    return {"status": "success", "message": "Password verified"}

# ==========================================
# API ENDPOINTS: PROFILE
# ==========================================

@app.post("/api/register")
async def api_register_profile(
    req: ProfileUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Update user profile"""
    user = current_user
    
    if req.emergency_phone:
        if not is_valid_phone(req.emergency_phone):
            raise HTTPException(status_code=400, detail="Invalid emergency phone format")
        user.emergency_phone = normalize_phone_number(req.emergency_phone)
    else:
        user.emergency_phone = ""
        
    user.name = req.name
    user.blood_group = req.blood_group
    user.address = req.address
    user.avatar = req.avatar
    user.updated_at = datetime.utcnow()
    
    db.commit()
    db.refresh(user)
    
    return {
        "status": "success",
        "user": UserResponse.from_orm(user)
    }

# ==========================================
# API ENDPOINTS: TELEMETRY & LOCATION
# ==========================================

@app.post("/api/sync")
async def api_sync(
    sync: TelemetrySync,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Real-time location synchronization with safety score calculation"""
    user = current_user
    timestamp = datetime.utcnow()
    
    # Check for Last Breath Protocol (battery critical)
    last_breath = False
    if getattr(user, "settings_last_breath", True) and sync.battery < 5.0 and user.current_battery >= 5.0:
        last_breath = True
        sos_event = SOSEvent(
            user_id=user.id,
            event_type="LAST_BREATH_PROTOCOL_BROADCAST",
            latitude=sync.lat,
            longitude=sync.lng,
            battery=sync.battery
        )
        db.add(sos_event)
        user.sos_active = True
    
    # Check for SafeZone breach
    violation_logged = False
    if user.safe_zones:
        for safe_zone in user.safe_zones:
            if safe_zone.is_active and safe_zone.latitude != 0.0:
                distance = calculate_haversine_distance(
                    safe_zone.latitude, safe_zone.longitude,
                    sync.lat, sync.lng
                )
                
                if distance > safe_zone.radius:
                    # Check frequency (limit to once every 2 minutes)
                    last_violation = db.query(SafeZoneViolation).filter(
                        SafeZoneViolation.user_id == user.id
                    ).order_by(desc(SafeZoneViolation.created_at)).first()
                    
                    if not last_violation or (timestamp - last_violation.created_at).total_seconds() > 120:
                        violation_logged = True
                        
                        # Log violation
                        violation = SafeZoneViolation(
                            user_id=user.id,
                            latitude=sync.lat,
                            longitude=sync.lng,
                            distance=distance
                        )
                        db.add(violation)
                        
                        # Add to SOS history
                        sos_event = SOSEvent(
                            user_id=user.id,
                            event_type="SAFEZONE_VIOLATION_AUTO_ALERT",
                            latitude=sync.lat,
                            longitude=sync.lng,
                            distance=distance
                        )
                        db.add(sos_event)
    
    # Calculate safety score
    all_hazards = db.query(HazardReportModel).all()
    safety_score = calculate_safety_score(user, all_hazards)
    
    # Update user location
    user.current_lat = sync.lat
    user.current_lng = sync.lng
    user.current_speed = sync.speed
    user.current_battery = sync.battery
    user.current_signal = sync.signal
    user.last_location_update = timestamp
    
    # Store in location history
    location_entry = LocationHistory(
        user_id=user.id,
        latitude=sync.lat,
        longitude=sync.lng,
        speed=sync.speed,
        battery=sync.battery,
        signal_strength=sync.signal,
        safety_score=safety_score
    )
    db.add(location_entry)
    
    db.commit()
    
    return {
        "status": "success",
        "last_breath_triggered": last_breath,
        "violation_detected": violation_logged,
        "sos_active": user.sos_active,
        "safety_score": safety_score
    }

@app.get("/api/location-history")
async def get_location_history(
    current_user: User = Depends(get_current_user),
    limit: int = 50,
    db: Session = Depends(get_db)
):
    """Retrieve user's location history"""
    user = current_user
    
    history = db.query(LocationHistory).filter(
        LocationHistory.user_id == user.id
    ).order_by(desc(LocationHistory.created_at)).limit(limit).all()
    
    return {
        "status": "success",
        "location_history": [
            {
                "lat": h.latitude,
                "lng": h.longitude,
                "speed": h.speed,
                "battery": h.battery,
                "signal": h.signal_strength,
                "safety_score": h.safety_score,
                "timestamp": h.created_at.isoformat()
            }
            for h in history
        ]
    }

# ==========================================
# API ENDPOINTS: EMERGENCY CONTACTS
# ==========================================

@app.post("/api/contacts")
async def api_contacts(
    action: str,
    req: ContactsRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Manage emergency contacts"""
    user = current_user
    
    if action == "add_direct":
        if not req.phone or not is_valid_phone(req.phone):
            raise HTTPException(status_code=400, detail="Invalid phone number format")
        clean_phone = normalize_phone_number(req.phone)
        contact = EmergencyContact(
            user_id=user.id,
            name=req.name,
            phone=clean_phone,
            relation_type=req.relationship or "Family"
        )
        db.add(contact)
        db.commit()
        db.refresh(user)
        
        return {
            "status": "success",
            "contacts": [
                {
                    "name": c.name,
                    "phone": c.phone,
                    "relationship": c.relation_type
                }
                for c in user.emergency_contacts
            ]
        }

    
    elif action == "remove_direct":
        contact_idx = req.index
        if contact_idx is not None and 0 <= contact_idx < len(user.emergency_contacts):
            db.delete(user.emergency_contacts[contact_idx])
            db.commit()
            db.refresh(user)
        
        return {
            "status": "success",
            "contacts": [
                {
                    "name": c.name,
                    "phone": c.phone,
                    "relationship": c.relation_type
                }
                for c in user.emergency_contacts
            ]
        }
    
    raise HTTPException(status_code=400, detail="Invalid action")

@app.get("/api/fetch-contacts")
async def fetch_contacts(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Fetch all data needed by the frontend dashboard"""
    user = current_user
    
    # Get location history (last 50)
    history = db.query(LocationHistory).filter(
        LocationHistory.user_id == user.id
    ).order_by(desc(LocationHistory.created_at)).limit(50).all()
    
    # Get safe zone
    safe_zone_data = {"active": False, "lat": 0.0, "lng": 0.0, "radius": 500.0}
    if user.safe_zones:
        zone = user.safe_zones[0]
        safe_zone_data = {
            "active": zone.is_active,
            "lat": zone.latitude,
            "lng": zone.longitude,
            "radius": zone.radius
        }
    
    # Get safe zone violations
    violations = db.query(SafeZoneViolation).filter(
        SafeZoneViolation.user_id == user.id
    ).order_by(desc(SafeZoneViolation.created_at)).limit(50).all()
    
    # Get emergency circle members (accepted connections)
    circle_connections = db.query(EmergencyCircle).filter(
        EmergencyCircle.user_id == user.id,
        EmergencyCircle.status == "accepted"
    ).all()
    
    circle_data = []
    for conn in circle_connections:
        if conn.connected_user_id:
            peer = db.query(User).filter(User.id == conn.connected_user_id).first()
            if peer:
                circle_data.append({
                    "name": peer.name,
                    "phone": peer.phone,
                    "blood_group": peer.blood_group,
                    "sos_active": peer.sos_active,
                    "location": {
                        "lat": peer.current_lat,
                        "lng": peer.current_lng,
                        "battery": peer.current_battery,
                        "speed": peer.current_speed
                    }
                })
    
    # Pending invites received (where connected_user = me)
    invites_received = db.query(EmergencyCircle).filter(
        EmergencyCircle.connected_user_id == user.id,
        EmergencyCircle.status == "pending"
    ).all()
    
    invites_sent = db.query(EmergencyCircle).filter(
        EmergencyCircle.user_id == user.id,
        EmergencyCircle.status == "pending"
    ).all()
    
    return {
        "emergency_contacts": [
            {
                "name": c.name,
                "phone": c.phone,
                "relationship": c.relation_type
            }
            for c in user.emergency_contacts
        ],
        "circle": circle_data,
        "invites_sent": [
            db.query(User).filter(User.id == inv.connected_user_id).first().phone
            for inv in invites_sent
            if inv.connected_user_id and db.query(User).filter(User.id == inv.connected_user_id).first()
        ],
        "invites_received": [
            db.query(User).filter(User.id == inv.user_id).first().phone
            for inv in invites_received
            if db.query(User).filter(User.id == inv.user_id).first()
        ],
        "location_history": [
            {
                "lat": h.latitude,
                "lng": h.longitude,
                "speed": h.speed,
                "battery": h.battery,
                "signal": h.signal_strength,
                "safety_score": h.safety_score,
                "timestamp": h.created_at.isoformat()
            }
            for h in history
        ],
        "safe_zone": safe_zone_data,
        "safe_zone_violations": [
            {
                "lat": v.latitude,
                "lng": v.longitude,
                "distance": v.distance,
                "timestamp": v.created_at.isoformat()
            }
            for v in violations
        ]
    }

# ==========================================
# API ENDPOINTS: SAFE ZONE
# ==========================================

@app.post("/api/safezone")
async def api_safezone(
    req: SafeZoneConfig,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Configure SafeZone geofence"""
    user = current_user
    
    # Clear existing zones and create new one
    for zone in user.safe_zones:
        db.delete(zone)
    
    if req.active:
        new_zone = SafeZone(
            user_id=user.id,
            latitude=req.lat,
            longitude=req.lng,
            radius=req.radius,
            is_active=req.active
        )
        db.add(new_zone)
    
    db.commit()
    
    return {
        "status": "success",
        "safe_zone": {
            "lat": req.lat,
            "lng": req.lng,
            "radius": req.radius,
            "active": req.active
        }
    }

@app.get("/api/safezone-violations")
async def get_safezone_violations(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Get SafeZone violation history"""
    user = current_user
    
    violations = db.query(SafeZoneViolation).filter(
        SafeZoneViolation.user_id == user.id
    ).order_by(desc(SafeZoneViolation.created_at)).all()
    
    return {
        "status": "success",
        "violations": [
            {
                "lat": v.latitude,
                "lng": v.longitude,
                "distance": v.distance,
                "timestamp": v.created_at.isoformat()
            }
            for v in violations
        ]
    }

# ==========================================
# API ENDPOINTS: HAZARD REPORTING
# ==========================================

@app.post("/api/hazard")
async def api_post_hazard(
    req: HazardReportSchema,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Report hazard/threat to community map"""
    user = current_user
    
    hazard = HazardReportModel(
        user_id=user.id,
        latitude=req.lat,
        longitude=req.lng,
        hazard_type=req.type
    )
    db.add(hazard)
    db.commit()
    
    return {
        "status": "success",
        "hazard_ping": {
            "id": hazard.id,
            "lat": hazard.latitude,
            "lng": hazard.longitude,
            "type": hazard.hazard_type,
            "timestamp": hazard.created_at.isoformat()
        }
    }

@app.get("/api/hazard")
async def api_get_hazards(db: Session = Depends(get_db)):
    """Get all community hazard reports (public endpoint)"""
    hazards = db.query(HazardReportModel).order_by(desc(HazardReportModel.created_at)).limit(100).all()
    
    return {
        "status": "success",
        "hazard_pings": [
            {
                "id": h.id,
                "lat": h.latitude,
                "lng": h.longitude,
                "type": h.hazard_type,
                "reporter": "Anonymous",
                "timestamp": h.created_at.isoformat()
            }
            for h in hazards
        ]
    }

# ==========================================
# API ENDPOINTS: SETTINGS
# ==========================================

@app.post("/api/settings")
async def api_settings(
    settings: Dict[str, Any],
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Update user settings"""
    user = current_user
    
    if "shake_to_alert" in settings:
        user.settings_shake_to_alert = settings["shake_to_alert"]
    if "hardware_trigger" in settings:
        user.settings_hardware_trigger = settings["hardware_trigger"]
    if "last_breath" in settings:
        user.settings_last_breath = settings["last_breath"]
    if "amoled_mode" in settings:
        user.settings_amoled_mode = settings["amoled_mode"]
    if "accent_color" in settings:
        user.settings_accent_color = settings["accent_color"]
    
    user.updated_at = datetime.utcnow()
    db.commit()
    
    return {
        "status": "success",
        "settings": {
            "shake_to_alert": user.settings_shake_to_alert,
            "hardware_trigger": user.settings_hardware_trigger,
            "last_breath": getattr(user, "settings_last_breath", True),
            "amoled_mode": user.settings_amoled_mode,
            "accent_color": user.settings_accent_color
        }
    }

# ==========================================
# API ENDPOINTS: SOS & PANIC FUNCTIONS
# ==========================================

@app.post("/api/sos/trigger")
async def trigger_sos(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Manually trigger SOS alert - notifies all emergency contacts"""
    user = current_user
    
    user.sos_active = True
    
    # Send FCM push notification to user's own device
    if user.fcm_token:
        send_fcm_notification(
            token=user.fcm_token,
            title="🚨 SOS ACTIVATED",
            body=f"Your SafeWatch SOS is broadcasting. Location: {user.current_lat:.5f}, {user.current_lng:.5f}",
            data={"type": "sos_triggered", "lat": str(user.current_lat), "lng": str(user.current_lng)}
        )
    
    sos_event = SOSEvent(
        user_id=user.id,
        event_type="USER_TRIGGERED_SOS",
        latitude=user.current_lat,
        longitude=user.current_lng
    )
    db.add(sos_event)
    db.commit()
    
    # Build the SOS message
    maps_url = f"https://maps.google.com/?q={user.current_lat},{user.current_lng}"
    sos_message = (
        f"URGENT SOS from {user.name}! "
        f"They need immediate help. "
        f"Last known location: {maps_url} "
        f"Blood Group: {user.blood_group}"
    )
      # Notify all emergency contacts via SMS (if Twilio is configured)
    sms_results = []
    twilio_sid = os.getenv("TWILIO_ACCOUNT_SID")
    twilio_token = os.getenv("TWILIO_AUTH_TOKEN")
    twilio_from = os.getenv("TWILIO_FROM_NUMBER")
    
    if twilio_sid and twilio_token and twilio_from:
        try:
            from twilio.rest import Client as TwilioClient
            twilio = TwilioClient(twilio_sid, twilio_token)
            
            for contact in user.emergency_contacts:
                if contact.phone:
                    clean_phone = normalize_phone_number(contact.phone)
                    clean_twilio_from = normalize_phone_number(twilio_from)
                    whatsapp_sent = False
                    # Attempt to send WhatsApp first
                    try:
                        twilio.messages.create(
                            body=sos_message,
                            from_=f"whatsapp:{clean_twilio_from}",
                            to=f"whatsapp:{clean_phone}"
                        )
                        whatsapp_sent = True
                    except Exception as wa_err:
                        print(f"[WhatsApp Alert Failed for {clean_phone}]: {wa_err}")
                        
                    # Always send SMS (either both, or just SMS if WhatsApp failed)
                    try:
                        twilio.messages.create(
                            body=sos_message,
                            from_=clean_twilio_from,
                            to=clean_phone
                        )
                        status_str = "sent (both SMS and WhatsApp)" if whatsapp_sent else "sent (SMS only)"
                        sms_results.append({"contact": contact.name, "status": status_str})
                        
                        db_log = SMSLog(
                            user_id=user.id,
                            recipient_name=contact.name,
                            recipient_phone=clean_phone,
                            message_body=sos_message,
                            status="sent",
                            error_message=None
                        )
                        db.add(db_log)
                    except Exception as sms_err:
                        err_str = str(sms_err)
                        sms_results.append({"contact": contact.name, "status": f"failed: {err_str}"})
                        db_log = SMSLog(
                            user_id=user.id,
                            recipient_name=contact.name,
                            recipient_phone=clean_phone,
                            message_body=sos_message,
                            status="failed",
                            error_message=err_str
                        )
                        db.add(db_log)
            db.commit()
        except Exception as outer_err:
            err_msg = f"Twilio client init error: {str(outer_err)}"
            sms_results.append({"status": err_msg})
            for contact in user.emergency_contacts:
                if contact.phone:
                    db_log = SMSLog(
                        user_id=user.id,
                        recipient_name=contact.name,
                        recipient_phone=normalize_phone_number(contact.phone),
                        message_body=sos_message,
                        status="failed",
                        error_message=err_msg
                    )
                    db.add(db_log)
            db.commit()
    else:
        err_msg = "SMS not configured - add TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM_NUMBER to .env"
        sms_results.append({"status": err_msg})
        for contact in user.emergency_contacts:
            if contact.phone:
                db_log = SMSLog(
                    user_id=user.id,
                    recipient_name=contact.name,
                    recipient_phone=normalize_phone_number(contact.phone),
                    message_body=sos_message,
                    status="failed",
                    error_message=err_msg
                )
                db.add(db_log)
        db.commit()
    
    # Email notification via SMTP or Resend (disabled as SOS alerts should not go to the registered email address)
    email_result = "disabled"
    
    # Also notify circle members via FCM if they have tokens
    fcm_results = []
    if FIREBASE_AVAILABLE and firebase_admin._apps:
        circle_connections = db.query(EmergencyCircle).filter(
            EmergencyCircle.user_id == user.id,
            EmergencyCircle.status == "accepted"
        ).all()
        for conn in circle_connections:
            if conn.connected_user_id:
                peer = db.query(User).filter(User.id == conn.connected_user_id).first()
                if peer and peer.fcm_token:
                    success = send_fcm_notification(
                        token=peer.fcm_token,
                        title=f"🚨 SOS ALERT: {user.name}",
                        body=f"{user.name} needs help! Tap to see location.",
                        data={"type": "peer_sos", "lat": str(user.current_lat), "lng": str(user.current_lng), "name": user.name}
                    )
                    fcm_results.append({"peer": peer.name, "status": "sent" if success else "failed"})

    return {
        "status": "success",
        "sos_active": True,
        "message": "SOS triggered",
        "sms_notifications": sms_results,
        "email_notification": email_result,
        "fcm_notifications": fcm_results
    }

@app.post("/api/fcm-token")
async def register_fcm_token(
    payload: Dict[str, Any],
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Register or update the user's FCM device token for push notifications"""
    token = payload.get("token")
    if not token:
        raise HTTPException(status_code=400, detail="FCM token is required")
    
    current_user.fcm_token = token
    db.commit()
    
    return {"status": "success", "message": "FCM token registered"}

@app.post("/api/fcm-test-push")
async def test_fcm_push(
    current_user: User = Depends(get_current_user)
):
    """Send a test FCM push notification to the current user's registered token"""
    if not current_user.fcm_token:
        raise HTTPException(status_code=400, detail="No registered FCM token found for current user")
        
    if not FIREBASE_AVAILABLE or not firebase_admin._apps:
        raise HTTPException(status_code=500, detail="Firebase Admin SDK is not initialized on the backend")
        
    success = send_fcm_notification(
        token=current_user.fcm_token,
        title="🔔 SafeWatch Test Push",
        body="Congratulations! Your device is successfully registered for push notifications.",
        data={"type": "test", "timestamp": datetime.utcnow().isoformat()}
    )
    
    if success:
        return {"status": "success", "message": "Test push notification sent successfully"}
    else:
        return {"status": "error", "message": "Failed to send test push notification. Check backend logs."}

@app.post("/api/cancel-sos")
async def cancel_sos(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Cancel active SOS alert"""
    user = current_user
    user.sos_active = False
    db.commit()
    
    return {
        "status": "success",
        "sos_active": False,
        "message": "SOS alert cancelled"
    }

@app.post("/api/wipe")
async def api_panic_wipe(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Panic wipe - clear all tracking data"""
    user = current_user
    
    # Delete sensitive data
    db.query(LocationHistory).filter(LocationHistory.user_id == user.id).delete()
    db.query(SafeZoneViolation).filter(SafeZoneViolation.user_id == user.id).delete()
    db.query(SOSEvent).filter(SOSEvent.user_id == user.id).delete()
    
    # Reset user state
    user.sos_active = False
    user.current_lat = 0.0
    user.current_lng = 0.0
    
    db.commit()
    
    return {
        "status": "success",
        "message": "PANIC PURGE COMPLETED. Local telemetry registries zeroed out safely."
    }

@app.get("/api/sos-history")
async def get_sos_history(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Get SOS event history"""
    user = current_user
    
    events = db.query(SOSEvent).filter(
        SOSEvent.user_id == user.id
    ).order_by(desc(SOSEvent.created_at)).limit(100).all()
    
    return {
        "status": "success",
        "sos_history": [
            {
                "type": e.event_type,
                "lat": e.latitude,
                "lng": e.longitude,
                "battery": e.battery,
                "timestamp": e.created_at.isoformat()
            }
            for e in events
        ]
    }

@app.get("/api/sms-logs")
async def get_sms_logs(current_user: User = Depends(get_current_user), db: Session = Depends(get_db)):
    """Get SMS logs history"""
    user = current_user
    
    logs = db.query(SMSLog).filter(
        SMSLog.user_id == user.id
    ).order_by(desc(SMSLog.created_at)).limit(100).all()
    
    return {
        "status": "success",
        "sms_logs": [
            {
                "id": log.id,
                "recipient_name": log.recipient_name,
                "recipient_phone": log.recipient_phone,
                "message_body": log.message_body,
                "status": log.status,
                "error_message": log.error_message,
                "timestamp": log.created_at.isoformat()
            }
            for log in logs
        ]
    }

# ==========================================
# API ENDPOINTS: EMERGENCY CIRCLE INVITES
# ==========================================

@app.post("/api/invite")
async def send_invite(
    req: InviteRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Send emergency circle invitation by phone number"""
    user = current_user
    
    if not is_valid_phone(req.phone):
        raise HTTPException(status_code=400, detail="Invalid phone number format")
        
    clean_phone = normalize_phone_number(req.phone)
    
    # Find target user by phone
    target = db.query(User).filter(User.phone == clean_phone).first()
    if not target:
        raise HTTPException(status_code=404, detail="User not found with that phone number")
    
    if target.id == user.id:
        raise HTTPException(status_code=400, detail="Cannot invite yourself")
    
    # Check if already invited or connected
    existing = db.query(EmergencyCircle).filter(
        EmergencyCircle.user_id == user.id,
        EmergencyCircle.connected_user_id == target.id
    ).first()
    
    if existing:
        raise HTTPException(status_code=400, detail="Invitation already sent or already connected")
    
    invite = EmergencyCircle(
        user_id=user.id,
        connected_user_id=target.id,
        status="pending"
    )
    db.add(invite)
    db.commit()
    
    return {"status": "success", "message": f"Invitation sent to {clean_phone}"}

@app.post("/api/respond-invite")
async def respond_invite(
    req: RespondInviteRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """Accept or decline an invitation by sender's phone"""
    user = current_user
    
    if not is_valid_phone(req.sender_phone):
        raise HTTPException(status_code=400, detail="Invalid sender phone number format")
        
    clean_sender_phone = normalize_phone_number(req.sender_phone)
    
    sender = db.query(User).filter(User.phone == clean_sender_phone).first()
    if not sender:
        raise HTTPException(status_code=404, detail="Sender not found")
    
    invite = db.query(EmergencyCircle).filter(
        EmergencyCircle.user_id == sender.id,
        EmergencyCircle.connected_user_id == user.id,
        EmergencyCircle.status == "pending"
    ).first()
    
    if not invite:
        raise HTTPException(status_code=404, detail="Invitation not found")
    
    if req.accept:
        invite.status = "accepted"
        # Create reverse connection too
        reverse = db.query(EmergencyCircle).filter(
            EmergencyCircle.user_id == user.id,
            EmergencyCircle.connected_user_id == sender.id
        ).first()
        if not reverse:
            reverse = EmergencyCircle(
                user_id=user.id,
                connected_user_id=sender.id,
                status="accepted"
            )
            db.add(reverse)
        else:
            reverse.status = "accepted"
    else:
        invite.status = "rejected"
    
    db.commit()
    
    return {"status": "success", "message": "accepted" if req.accept else "rejected"}

if __name__ == "__main__":
    import uvicorn
    init_db()
    uvicorn.run(app, host="0.0.0.0", port=8000)
