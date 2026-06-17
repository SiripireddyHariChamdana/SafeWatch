"""
Database Configuration and Connection
PostgreSQL setup with SQLAlchemy ORM
"""
import os
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker, declarative_base
from dotenv import load_dotenv

# Load environment variables from both the root directory and backend/.env
load_dotenv() # Load from current working directory (.env in root)
_ENV_PATH = os.path.join(os.path.dirname(__file__), ".env")
load_dotenv(_ENV_PATH) # Load from backend/.env

# ==========================================
# DATABASE CONFIGURATION
# ==========================================

DATABASE_URL = os.getenv(
    "DATABASE_URL",
    "postgresql://postgres:password@localhost:5432/safewatch"
)

SECRET_KEY = os.getenv(
    "SECRET_KEY",
    "your-secret-key-change-this-in-production"
)

# Initialize engine with connection pooling
try:
    if DATABASE_URL.startswith("postgresql"):
        # Test connection with quick timeout
        temp_engine = create_engine(
            DATABASE_URL,
            connect_args={"connect_timeout": 5}
        )
        try:
            with temp_engine.connect() as conn:
                conn.execute(text("SELECT 1"))
            print("[✓] PostgreSQL connection successful")
        finally:
            temp_engine.dispose()

        # Production-ready engine with connection pooling
        engine = create_engine(
            DATABASE_URL,
            echo=False,
            pool_size=10,
            max_overflow=20,
            pool_pre_ping=True,
            connect_args={"connect_timeout": 10}
        )
    else:
        # Fallback for SQLite or custom URLs
        engine = create_engine(
            DATABASE_URL,
            connect_args={"check_same_thread": False}
        )
        print(f"[*] Using database: {DATABASE_URL}")

except Exception as e:
    print(f"[!] Database connection failed: {e}")
    print("[!] Make sure PostgreSQL is running and DATABASE_URL is correct in backend/.env")
    raise

# Session factory
SessionLocal = sessionmaker(
    autocommit=False,
    autoflush=False,
    bind=engine
)

# Base class for all ORM models
Base = declarative_base()

# ==========================================
# DATABASE INITIALIZATION & DEPENDENCY
# ==========================================

def init_db():
    """
    Create all database tables from models.
    Called automatically on application startup.
    """
    try:
        Base.metadata.create_all(bind=engine)
        print("[✓] Database tables initialized")
    except Exception as e:
        print(f"[!] Failed to initialize database: {e}")
        raise

def get_db():
    """
    Dependency injection for database sessions.
    Use in FastAPI endpoints: db: Session = Depends(get_db)
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
