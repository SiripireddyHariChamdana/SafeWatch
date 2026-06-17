# SafeWatch Backend Setup Guide

## Overview

Your SafeWatch backend has been upgraded from JSON file storage to a **PostgreSQL database** with **JWT authentication**. Here's what changed and what you need to do.

---

## ✅ What's Done (No External APIs Needed)

### 1. **Authentication** ✓
- [x] User signup/login with **JWT tokens**
- [x] Password hashing with **bcrypt** (more secure than SHA256)
- [x] Session management via tokens
- [x] Token expiration (24 hours by default)

### 2. **Database Models** ✓
- [x] User accounts with full profile data
- [x] Location history (unlimited, indexed for performance)
- [x] Safe Zone management & violation tracking
- [x] Emergency contacts CRUD
- [x] Hazard reporting (community threat map)
- [x] SOS event logging
- [x] Evidence vault metadata
- [x] Emergency circle (trusted connections)

### 3. **API Endpoints** ✓
- [x] `/api/auth` - Login/Signup with JWT
- [x] `/api/register` - Profile updates
- [x] `/api/sync` - Location tracking with safety scoring
- [x] `/api/safezone` - Geofence configuration
- [x] `/api/hazard` - Report & retrieve hazards
- [x] `/api/contacts` - Emergency contact management
- [x] `/api/vault` - Evidence metadata storage
- [x] `/api/settings` - User preferences
- [x] `/api/sos/*` - SOS triggering & history
- [x] `/api/wipe` - Panic data deletion
- [x] `/api/health` - System health check

### 4. **Security Features** ✓
- [x] Password hashing with bcrypt
- [x] JWT token-based authentication
- [x] CORS configuration
- [x] Soft delete capability (users)
- [x] Database connection pooling

---

## ⚠️ What Needs External APIs (Not Implemented Yet)

| Feature | Service | Why | Cost |
|---------|---------|-----|------|
| **SMS Alerts** | Twilio | Send SMS to emergency contacts | $0.0075/SMS |
| **Email Notifications** | SendGrid | Send email alerts | Free tier available |
| **Push Notifications** | Firebase Cloud Messaging (FCM) | Mobile push alerts | Free tier available |
| **Evidence Storage** | AWS S3 or similar | Store audio/video files | $0.023/GB/month |

---

## 🚀 Setup Instructions

### Step 1: Install PostgreSQL

**Windows:**
```powershell
# Option A: Download from https://www.postgresql.org/download/windows/
# Option B: Using Chocolatey
choco install postgresql
```

**macOS:**
```bash
brew install postgresql@15
brew services start postgresql@15
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

**Verify installation:**
```powershell
psql --version
```

### Step 2: Create Database

```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create database
CREATE DATABASE safewatch;

-- Create user (optional, better for production)
CREATE USER safewatch_user WITH PASSWORD 'your_secure_password';
ALTER ROLE safewatch_user SET client_encoding TO 'utf8';
ALTER ROLE safewatch_user SET default_transaction_isolation TO 'read committed';
ALTER ROLE safewatch_user SET default_transaction_deferrable TO on;
ALTER ROLE safewatch_user SET default_transaction_read_committed TO on;
GRANT ALL PRIVILEGES ON DATABASE safewatch TO safewatch_user;
```

### Step 3: Update Environment Variables

Edit `backend/.env` file in the backend directory:
```bash
DATABASE_URL=postgresql://postgres:password@localhost:5432/safewatch
SECRET_KEY=your-random-secret-key-here
```

**Generate a secure secret key:**
```powershell
python -c "import secrets; print(secrets.token_urlsafe(32))"
```

### Step 4: Install Python Dependencies

From the **project root** directory:

```powershell
# Create virtual environment
python -m venv venv

# Activate virtual environment
# Windows:
.\venv\Scripts\Activate.ps1
# macOS/Linux:
source venv/bin/activate

# Install packages
pip install -r requirements.txt
```

### Step 5: Initialize Database Tables

From the **project root** directory:

```powershell
# Run the launcher (automatically initializes database)
python run.py
```

The database tables will be created automatically on startup. You should see:
```
[*] Initializing SafeWatch database...
[*] Starting SafeWatch server at http://localhost:8000
```

### Step 6: Verify Setup

Open your browser and visit:
- **Frontend:** http://localhost:8000
- **API Docs:** http://localhost:8000/docs
- **Health Check:** http://localhost:8000/api/health

---

## 📊 Database Schema Overview

```
Users (accounts)
├── Location History (50,000+ records possible)
├── Safe Zones (per user)
├── Safe Zone Violations
├── Emergency Contacts
├── Emergency Circle (trusted connections)
├── Hazard Reports (public community map)
├── SOS Events
└── Vault Items (metadata only)
```

---

## 🔄 Project Structure

```
safewatch/
├── run.py                    # 🚀 Main launcher - use this!
├── requirements.txt          # Python dependencies
├── backend/
│   ├── main.py              # FastAPI application (PRODUCTION)
│   ├── database.py          # PostgreSQL connection
│   ├── models.py            # SQLAlchemy ORM models
│   ├── .env                 # Environment variables (create from .env.example)
│   ├── .env.example         # Environment template
│   └── SETUP_GUIDE.md       # This file
├── frontend/
│   └── static/
│       ├── index.html       # HTML interface
│       ├── app.js           # JavaScript logic
│       └── style.css        # Styling
└── static/                  # (Legacy - can be removed)
```

---

## 📝 API Changes from Old Backend

### Old Request (JSON file):
```python
# No token needed, everything local
POST /api/auth
{ "action": "login", "email": "user@example.com", "password": "..." }
```

### New Request (Database + JWT):
```python
# Response includes token
POST /api/auth
{ "action": "login", "email": "user@example.com", "password": "..." }

Response:
{
    "status": "success",
    "access_token": "eyJhbGc...",
    "token_type": "bearer",
    "user": { ... }
}

# Use token in Authorization header for all requests
GET /api/sync
Header: Authorization: Bearer eyJhbGc...
```

---

## 🔐 Security Best Practices

1. **Change SECRET_KEY in production:**
   ```powershell
   python -c "import secrets; print(secrets.token_urlsafe(32))"
   ```

2. **Use strong database password:**
   ```sql
   ALTER USER postgres WITH PASSWORD 'StrongPassword123!';
   ```

3. **Set up HTTPS** when deploying to production

4. **Limit CORS origins:**
   ```
   ALLOWED_ORIGINS=https://yourdomain.com,https://app.yourdomain.com
   ```

---

## 📋 What You Need to Add (External APIs)

### 1. SMS Alerts (Twilio)

Install:
```powershell
pip install twilio
```

Update `backend/.env`:
```
TWILIO_ACCOUNT_SID=your_sid
TWILIO_AUTH_TOKEN=your_token
TWILIO_PHONE_NUMBER=+1234567890
```

Add to `backend/main.py`:
```python
from twilio.rest import Client

def send_sms_alert(phone: str, message: str):
    client = Client(os.getenv("TWILIO_ACCOUNT_SID"), os.getenv("TWILIO_AUTH_TOKEN"))
    client.messages.create(
        body=message,
        from_=os.getenv("TWILIO_PHONE_NUMBER"),
        to=phone
    )
```

### 2. Email Alerts (SendGrid)

Install:
```powershell
pip install sendgrid
```

Update `backend/.env`:
```
SENDGRID_API_KEY=your_api_key
```

### 3. Push Notifications (Firebase)

Install:
```powershell
pip install firebase-admin
```

### 4. Evidence File Storage (AWS S3)

Install:
```powershell
pip install boto3
```

---

## 🧪 Testing the Backend

```powershell
# Health check
curl http://localhost:8000/api/health

# Signup
curl -X POST http://localhost:8000/api/auth \
  -H "Content-Type: application/json" \
  -d '{"action":"signup","email":"test@example.com","password":"test123","name":"Test User"}'

# Login
curl -X POST http://localhost:8000/api/auth \
  -H "Content-Type: application/json" \
  -d '{"action":"login","email":"test@example.com","password":"test123"}'

# Get hazards (no token needed)
curl http://localhost:8000/api/hazard
```

---

## 📞 Support Files

- `run.py` - Main launcher script (entry point)
- `requirements.txt` - Python dependencies
- `backend/.env` - Environment variables (create this)
- `backend/.env.example` - Template for environment variables
- `backend/database.py` - Database connection setup
- `backend/models.py` - SQLAlchemy ORM models
- `backend/main.py` - FastAPI backend application

---

## ✅ Checklist Before Production

- [ ] PostgreSQL installed and running
- [ ] Database created (`safewatch`)
- [ ] Environment variables configured (`backend/.env` with SECRET_KEY, DATABASE_URL)
- [ ] Virtual environment created and activated
- [ ] All dependencies installed (`pip install -r requirements.txt`)
- [ ] Database tables initialized (run `python run.py` once)
- [ ] Frontend tested and working
- [ ] JWT token validation working
- [ ] CORS origins configured properly
- [ ] Token expiration set appropriately
- [ ] SSL/HTTPS configured
- [ ] Database backups scheduled
- [ ] Error logging set up
- [ ] Rate limiting implemented
- [ ] External APIs added (Twilio, SendGrid, etc.) as needed

---

## 🚨 Troubleshooting

**Error: "could not connect to server"**
- Check PostgreSQL is running: `pg_isready`
- Verify DATABASE_URL in `backend/.env`
- Verify PostgreSQL credentials

**Error: "relation does not exist" or "no such table"**
- Database tables not created yet
- Run: `python run.py` once to initialize all tables
- Check that `backend/database.py` and `backend/models.py` are present

**Error: "ModuleNotFoundError: No module named 'main'"**
- Make sure you're running from project root with `python run.py`
- Verify `backend/main.py` exists
- Check that `sys.path` includes the backend directory

**Error: "Invalid token"**
- Token might be expired (24 hour default)
- Regenerate by logging in again
- Clear localStorage and sign up fresh

**Error: "CORS error"**
- Add your frontend URL to ALLOWED_ORIGINS in `backend/.env`
- For local testing, use: `ALLOWED_ORIGINS=http://localhost:8000`

**Error: "psql: command not found"**
- PostgreSQL not installed correctly
- Reinstall from official PostgreSQL website
- Make sure bin directory is in system PATH

**Error: "python: can't find '__main__' module"**
- Run from project root: `python run.py` (not from backend directory)
- Don't try to run `python backend/main.py` directly

---

## 🎯 Quick Start Commands

```bash
# One-time setup (first time only)
python -m venv venv
.\venv\Scripts\Activate.ps1  # or: source venv/bin/activate
pip install -r requirements.txt

# Start the server (every time you want to run it)
python run.py

# Then open in browser:
http://localhost:8000
```

---

Let me know if you need help adding the external APIs (SMS, email, push notifications)!
