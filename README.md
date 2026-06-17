# SAFEWATCH - Professional Personal Safety Monitoring System

A comprehensive full-stack application for women's safety with real-time location tracking, emergency alerts, geofencing, and community threat reporting.

## 📁 Project Structure

```
safewatch/
├── backend/                    # FastAPI backend with PostgreSQL
│   ├── main.py                # Legacy JSON API (deprecated)
│   ├── main_new.py            # Current FastAPI production server
│   ├── database.py            # PostgreSQL & SQLAlchemy setup
│   ├── models.py              # Database ORM models (10+ tables)
│   ├── requirements.txt        # Python dependencies
│   ├── .env                   # Environment configuration
│   ├── SETUP_GUIDE.md         # Detailed setup instructions
│   └── README.md              # Backend documentation
│
├── frontend/                   # Single-page application
│   ├── static/
│   │   ├── index.html         # SPA container with all screens
│   │   ├── app.js             # Application logic & API integration
│   │   └── style.css          # Professional dark theme + glassmorphism
│   └── README.md              # Frontend documentation
│
└── README.md                   # This file
```

## 🎯 Quick Navigation

- **Backend Setup:** See [backend/README.md](backend/README.md)
- **Frontend Guide:** See [frontend/README.md](frontend/README.md)
- **Complete Setup:** See [backend/SETUP_GUIDE.md](backend/SETUP_GUIDE.md)

## ⚡ Quick Start

### 1. Backend Setup (5 minutes)

```bash
cd backend
pip install -r requirements.txt
cp .env.example .env
# Edit .env with your PostgreSQL credentials
uvicorn main_new.py:app --reload
```

### 2. Frontend Setup (2 minutes)

Backend automatically serves frontend at `http://localhost:8000`

Or manually serve:
```bash
cd frontend/static
python -m http.server 8000
```

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                        FRONTEND (SPA)                       │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ HTML5 + CSS3 Glassmorphism + Vanilla JavaScript      │  │
│  │ Dark theme, Neon accents, Responsive design         │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓ REST API                         │
├─────────────────────────────────────────────────────────────┤
│                    BACKEND (FastAPI)                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ JWT Authentication | REST Endpoints | Pydantic      │  │
│  │ Data Validation    | CORS Middleware               │  │
│  └──────────────────────────────────────────────────────┘  │
│                          ↓ SQL                              │
├─────────────────────────────────────────────────────────────┤
│                   DATABASE (PostgreSQL)                     │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ SQLAlchemy ORM | 10+ Tables | Relationships         │  │
│  │ Soft Delete    | Indexes    | Cascades             │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 🔑 Key Features

### Core Safety Features
- ✅ Real-time GPS location tracking
- ✅ Safe zone geofencing with violation alerts
- ✅ SOS emergency alert system with automatic triggers
- ✅ Emergency contact circle (family/friends)
- ✅ Community hazard/threat reporting map
- ✅ Battery critical alert (Last Breath Protocol)

### Evidence & Documentation
- ✅ Stealth evidence vault for audio/video metadata
- ✅ Timestamped incident history
- ✅ Location history tracking
- ✅ SOS event logging

### Security & Privacy
- ✅ JWT authentication with 24-hour tokens
- ✅ Bcrypt password hashing
- ✅ Panic wipe (nuclear data deletion)
- ✅ SQLAlchemy soft deletes

### User Experience
- ✅ Professional dark dashboard theme
- ✅ Glassmorphic UI with neon accents
- ✅ Real-time telemetry visualization
- ✅ Responsive design (desktop/tablet/mobile)
- ✅ Accessibility features

## 🔐 Authentication

The app uses JWT (JSON Web Tokens) for secure authentication:

1. User signs up/logs in
2. Backend generates JWT token (24-hour expiration)
3. Token stored in browser localStorage
4. All API requests include: `Authorization: Bearer <token>`
5. Backend validates token before processing requests

## 📊 API Overview

### Main Endpoints

| Method | Endpoint | Purpose |
|--------|----------|---------|
| POST | `/api/auth` | Login/Signup with JWT |
| POST | `/api/sync` | Real-time location + telemetry |
| POST | `/api/safezone` | Create/update geofence |
| POST | `/api/contacts` | Manage emergency contacts |
| POST | `/api/hazard` | Report community threat |
| GET | `/api/hazard` | Fetch hazard map data |
| POST | `/api/vault` | Store evidence metadata |
| POST | `/api/sos` | Trigger SOS alert |
| POST | `/api/settings` | Update preferences |
| POST | `/api/wipe` | Panic data deletion |

Full endpoint documentation in [backend/README.md](backend/README.md)

## 📦 Tech Stack

### Backend
- **Framework:** FastAPI 0.104.1
- **Server:** Uvicorn
- **Database:** PostgreSQL + SQLAlchemy 2.0
- **Authentication:** JWT (PyJWT)
- **Password Hashing:** bcrypt
- **Validation:** Pydantic

### Frontend
- **HTML5:** Semantic markup
- **CSS3:** Custom properties, animations, responsive
- **JavaScript:** Vanilla (no framework)
- **Mapping:** Leaflet.js

### External (Not yet integrated)
- **SMS:** Twilio
- **Email:** SendGrid
- **Push Notifications:** Firebase Cloud Messaging
- **File Storage:** AWS S3
- **Maps:** Google Maps / Mapbox

## 🚀 Running the Application

### Start Backend
```bash
cd backend
python -m pip install -r requirements.txt
uvicorn main_new.py:app --reload --host 0.0.0.0 --port 8000
```

### Access Frontend
```
http://localhost:8000
```

## 🧪 Testing

Test credentials for development:
- **Email:** test@example.com
- **Password:** password123

Or sign up with any email/password combination.

## 📝 Environment Configuration

Backend requires `.env` file:

```env
# Database
DATABASE_URL=postgresql://user:password@localhost/safewatch

# JWT
SECRET_KEY=your-secret-key-here
TOKEN_EXPIRE_MINUTES=1440

# API Configuration
ALLOWED_ORIGINS=["http://localhost:8000", "http://localhost:3000"]

# External APIs (optional)
TWILIO_ACCOUNT_SID=
SENDGRID_API_KEY=
AWS_ACCESS_KEY_ID=
```

See [backend/.env.example](backend/.env.example) for template.

## 🔄 Migration from Legacy System

This project includes both:
- **main.py** - Legacy JSON-based API (deprecated)
- **main_new.py** - Production FastAPI + PostgreSQL (current)

Migration benefits:
- ✅ Real database (PostgreSQL) instead of JSON files
- ✅ Scalable architecture
- ✅ Enterprise security (JWT + bcrypt)
- ✅ Better performance with connection pooling
- ✅ ORM for data consistency

## 📖 Documentation

- [Backend Setup Guide](backend/SETUP_GUIDE.md) - Complete backend configuration
- [Backend README](backend/README.md) - Backend architecture
- [Frontend README](frontend/README.md) - Frontend guide

## 🤝 Contributing

1. Backend changes: Update models in `models.py`, then `database.py`
2. API changes: Modify endpoints in `main_new.py`
3. Frontend changes: Update `app.js` and `style.css`
4. Always test locally before committing

## 📞 Support

For issues or questions:
1. Check [backend/SETUP_GUIDE.md](backend/SETUP_GUIDE.md) troubleshooting section
2. Review error messages in backend logs
3. Verify `.env` configuration
4. Check PostgreSQL connection

## 📄 License

Proprietary - SAFEWATCH Professional Safety System

---

**Last Updated:** 2026-06-16  
**Version:** 2.0 (FastAPI + PostgreSQL)  
**Status:** Production Ready
