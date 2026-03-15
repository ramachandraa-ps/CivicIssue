import os
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from config import settings
from database import SessionLocal
from models.user import User
from models.category import Category
from models.department import Department
from utils.security import hash_password
from utils.helpers import generate_uuid

# Import all routers
from routers.auth import router as auth_router
from routers.users import router as users_router
from routers.images import router as images_router
from routers.complaints import router as complaints_router
from routers.groups import router as groups_router
from routers.notifications import router as notifications_router
from routers.admin import router as admin_router
from routers.geo import router as geo_router
from routers.ai import router as ai_router


def seed_data():
    """Seed default categories, departments, and admin user on first run."""
    db = SessionLocal()
    try:
        # Default categories
        if db.query(Category).count() == 0:
            for name in [
                "Pothole",
                "Garbage/Waste",
                "Street Light",
                "Water Leakage",
                "Drainage",
                "Road Damage",
                "Broken Infrastructure",
                "Other",
            ]:
                db.add(Category(id=generate_uuid(), name=name))
            db.commit()

        # Default departments
        if db.query(Department).count() == 0:
            for name in [
                "Roads & Infrastructure",
                "Sanitation",
                "Electricity",
                "Water Supply",
                "General Maintenance",
            ]:
                db.add(Department(id=generate_uuid(), name=name))
            db.commit()

        # Default admin user
        if not db.query(User).filter(User.email == "admin@civicissue.com").first():
            db.add(
                User(
                    id=generate_uuid(),
                    full_name="Admin",
                    email="admin@civicissue.com",
                    password_hash=hash_password("admin123"),
                    role="admin",
                    is_verified=True,
                )
            )
            db.commit()
    finally:
        db.close()


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Startup: seed data
    seed_data()
    yield
    # Shutdown: nothing needed


app = FastAPI(title="CivicIssue API", version="1.0.0", lifespan=lifespan)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

os.makedirs(settings.UPLOAD_DIR, exist_ok=True)
app.mount("/uploads", StaticFiles(directory=settings.UPLOAD_DIR), name="uploads")

# Register all routers
app.include_router(auth_router, prefix="/api/auth", tags=["Authentication"])
app.include_router(users_router, prefix="/api/users", tags=["Users"])
app.include_router(images_router, prefix="/api/images", tags=["Images"])
app.include_router(complaints_router, prefix="/api/complaints", tags=["Complaints"])
app.include_router(groups_router, prefix="/api/groups", tags=["Groups"])
app.include_router(notifications_router, prefix="/api/notifications", tags=["Notifications"])
app.include_router(admin_router, prefix="/api/admin", tags=["Admin"])
app.include_router(geo_router, prefix="/api/geo", tags=["Geocoding"])
app.include_router(ai_router, prefix="/api/ai", tags=["AI"])


@app.get("/health")
def health_check():
    return {"status": "ok"}
