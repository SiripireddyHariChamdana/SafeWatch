#!/usr/bin/env python3
"""
SafeWatch - Root launcher
Run this from the project root: python run.py
"""
import sys
import os

# Add backend directory to Python path so all imports resolve correctly
_BACKEND_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "backend")
sys.path.insert(0, _BACKEND_DIR)

if __name__ == "__main__":
    import uvicorn
    from database import init_db
    from main import app

    print("[*] Initializing SafeWatch database...")
    init_db()
    print("[*] Starting SafeWatch server at http://localhost:8000")
    print("[*] Frontend available at: http://localhost:8000/static/index.html")
    print("[*] API docs available at: http://localhost:8000/docs")

    # Disable reload in CI environments (GitHub Actions sets CI=true)
    is_ci = os.environ.get("CI", "").lower() in ("true", "1", "yes")

    uvicorn.run(
        "main:app",
        host="0.0.0.0",
        port=8000,
        reload=not is_ci,
        reload_dirs=[_BACKEND_DIR] if not is_ci else None
    )
