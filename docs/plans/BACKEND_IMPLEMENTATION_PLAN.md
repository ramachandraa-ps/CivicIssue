# CivicIssue Backend — Complete Implementation Plan

## Context

- **Frontend**: Android (Jetpack Compose + Kotlin), 47 screens, 3 Retrofit endpoints, all mock data
- **Backend target**: FastAPI + MariaDB 10.4.32 (XAMPP) + Gemini 2.5 Flash
- **Python**: 3.12.10, venv
- **Scope**: Full implementation — all 13 tables, 35+ endpoints, AI features, frontend wiring

---

## Phase 1: Project Foundation

### Task 1.1 — Delete old backend, create new project structure

**Files to delete:**
- `backend/` (entire existing directory — SQLite-based, plain-text passwords, unusable)

**Files to create:**
```
backend/
├── main.py
├── config.py
├── database.py
├── requirements.txt
├── .env.example
├── .env
├── alembic.ini
├── alembic/
│   └── env.py
├── models/
│   └── __init__.py
├── schemas/
│   └── __init__.py
├── routers/
│   └── __init__.py
├── services/
│   └── __init__.py
├── middleware/
│   └── auth.py
├── utils/
│   ├── security.py
│   └── helpers.py
└── uploads/
    └── .gitkeep
```

**`requirements.txt`:**
```
fastapi
uvicorn[standard]
sqlalchemy
alembic
pymysql
cryptography
python-jose[cryptography]
passlib[bcrypt]
python-multipart
pydantic[email]
pydantic-settings
google-generativeai
httpx
python-dotenv
Pillow
pytest
httpx
```

**`config.py`:**
```python
from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    DATABASE_URL: str = "mysql+pymysql://root:@localhost:3306/civicissue"
    JWT_SECRET_KEY: str = "change-me-in-production"
    JWT_ALGORITHM: str = "HS256"
    JWT_EXPIRY_MINUTES: int = 1440
    GEMINI_API_KEY: str = ""
    SMTP_HOST: str = "smtp.gmail.com"
    SMTP_PORT: int = 587
    SMTP_USER: str = ""
    SMTP_PASSWORD: str = ""
    UPLOAD_DIR: str = "./uploads"
    MAX_FILE_SIZE_MB: int = 10
    NOMINATIM_USER_AGENT: str = "civicissue-app"

    class Config:
        env_file = ".env"

settings = Settings()
```

**`database.py`:**
```python
from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, declarative_base
from config import settings

engine = create_engine(settings.DATABASE_URL, pool_pre_ping=True)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)
Base = declarative_base()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
```

**`main.py`:**
```python
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from database import engine, Base
from config import settings
import os

app = FastAPI(title="CivicIssue API", version="1.0.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

os.makedirs(settings.UPLOAD_DIR, exist_ok=True)
app.mount("/uploads", StaticFiles(directory=settings.UPLOAD_DIR), name="uploads")

# Router includes added in later phases

@app.get("/health")
def health_check():
    return {"status": "ok"}
```

**`.env.example`:**
```
DATABASE_URL=mysql+pymysql://root:@localhost:3306/civicissue
JWT_SECRET_KEY=your-random-secret-key-here
JWT_ALGORITHM=HS256
JWT_EXPIRY_MINUTES=1440
GEMINI_API_KEY=your-gemini-api-key
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASSWORD=your-gmail-app-password
UPLOAD_DIR=./uploads
MAX_FILE_SIZE_MB=10
NOMINATIM_USER_AGENT=civicissue-app
```

### Task 1.2 — Create MySQL database

```sql
-- Run via XAMPP MySQL:
CREATE DATABASE IF NOT EXISTS civicissue CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### Task 1.3 — Setup venv and install dependencies

```bash
cd backend
python -m venv venv
source venv/Scripts/activate  # Windows Git Bash
pip install -r requirements.txt
```

### Task 1.4 — Initialize Alembic

```bash
alembic init alembic
```

Then edit `alembic/env.py` to import `Base` and all models, and `alembic.ini` to use `DATABASE_URL` from config.

**Verification:** `uvicorn main:app --reload` → GET `/health` returns `{"status": "ok"}`

---

## Phase 2: SQLAlchemy Models + Initial Migration

### Task 2.1 — Create all 13 models

Each model file goes in `backend/models/`. All use `CHAR(36)` UUIDs generated via `uuid.uuid4()`.

**`models/user.py`:**
```python
from sqlalchemy import Column, String, Boolean, Enum, TIMESTAMP
from sqlalchemy.sql import func
from database import Base
import uuid

class User(Base):
    __tablename__ = "users"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    full_name = Column(String(255), nullable=False)
    email = Column(String(255), unique=True, nullable=False)
    phone_number = Column(String(20), nullable=True)
    country_code = Column(String(5), nullable=True)
    password_hash = Column(String(255), nullable=False)
    role = Column(Enum("citizen", "admin", "officer"), nullable=False)
    avatar_url = Column(String(500), nullable=True)
    is_verified = Column(Boolean, default=False)
    created_at = Column(TIMESTAMP, server_default=func.now())
    updated_at = Column(TIMESTAMP, server_default=func.now(), onupdate=func.now())
```

**`models/complaint.py`:**
```python
from sqlalchemy import Column, String, Text, Double, Float, Enum, TIMESTAMP, JSON
from sqlalchemy.sql import func
from database import Base
import uuid

class Complaint(Base):
    __tablename__ = "complaints"
    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    complaint_number = Column(String(20), unique=True)
    citizen_id = Column(String(36), nullable=False)  # FK → users.id
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=True)
    category = Column(String(100))
    ai_detected_category = Column(String(100))
    ai_text_category = Column(String(100))
    location_text = Column(String(500))
    latitude = Column(Double, nullable=False)
    longitude = Column(Double, nullable=False)
    priority = Column(Enum("LOW", "MEDIUM", "HIGH"), default="MEDIUM")
    severity_level = Column(Enum("LOW", "MEDIUM", "HIGH", "CRITICAL"), default="MEDIUM")
    status = Column(Enum("UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED"), default="UNASSIGNED")
    assigned_officer_id = Column(String(36), nullable=True)
    group_id = Column(String(36), nullable=True)
    ai_confidence = Column(Float, nullable=True)
    ai_keywords = Column(JSON, nullable=True)
    resolution_notes = Column(Text, nullable=True)
    resolved_at = Column(TIMESTAMP, nullable=True)
    created_at = Column(TIMESTAMP, server_default=func.now())
    updated_at = Column(TIMESTAMP, server_default=func.now(), onupdate=func.now())
```

**Remaining models** (same pattern, one file each):
- `models/complaint_image.py` — ComplaintImage (id, complaint_id, image_url, ai_analysis JSON, uploaded_at)
- `models/issue_group.py` — IssueGroup (id, category, center_lat, center_lng, radius_meters, complaint_count, avg_severity, status, timestamps)
- `models/notification.py` — Notification (id, recipient_id, complaint_id, title, message, type enum, priority enum, is_read, created_at)
- `models/complaint_status_history.py` — ComplaintStatusHistory (id, complaint_id, old_status, new_status, changed_by, notes, created_at)
- `models/officer.py` — Officer (id, user_id, department, designation, workload_count, is_available)
- `models/category.py` — Category (id, name, description, icon, is_active, created_at)
- `models/department.py` — Department (id, name, description, is_active, created_at)
- `models/system_log.py` — SystemLog (id, action, entity_type, entity_id, performed_by, details JSON, created_at)
- `models/otp.py` — OTPVerification (id, user_id, otp_code, purpose enum, is_used, expires_at, created_at)
- `models/chatbot.py` — ChatbotSession + ChatbotMessage (two classes, one file)

**`models/__init__.py`:**
```python
from models.user import User
from models.complaint import Complaint
from models.complaint_image import ComplaintImage
from models.issue_group import IssueGroup
from models.notification import Notification
from models.complaint_status_history import ComplaintStatusHistory
from models.officer import Officer
from models.category import Category
from models.department import Department
from models.system_log import SystemLog
from models.otp import OTPVerification
from models.chatbot import ChatbotSession, ChatbotMessage
```

### Task 2.2 — Generate and run Alembic migration

```bash
alembic revision --autogenerate -m "initial schema"
alembic upgrade head
```

**Verification:** Connect to MySQL and run `SHOW TABLES;` — should list all 13 tables.

---

## Phase 3: Authentication System

### Task 3.1 — Security utilities

**`utils/security.py`:**
```python
from passlib.context import CryptContext
from jose import jwt, JWTError
from datetime import datetime, timedelta
from config import settings

pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

def hash_password(password: str) -> str:
    return pwd_context.hash(password)

def verify_password(plain: str, hashed: str) -> bool:
    return pwd_context.verify(plain, hashed)

def create_access_token(data: dict) -> str:
    to_encode = data.copy()
    expire = datetime.utcnow() + timedelta(minutes=settings.JWT_EXPIRY_MINUTES)
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)

def decode_access_token(token: str) -> dict:
    return jwt.decode(token, settings.JWT_SECRET_KEY, algorithms=[settings.JWT_ALGORITHM])
```

**`utils/helpers.py`:**
```python
import uuid
import random

def generate_uuid() -> str:
    return str(uuid.uuid4())

def generate_complaint_number() -> str:
    return f"#CE-{random.randint(1000, 9999)}"
```

### Task 3.2 — Auth middleware (JWT dependency)

**`middleware/auth.py`:**
```python
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from sqlalchemy.orm import Session
from database import get_db
from utils.security import decode_access_token
from models.user import User

security = HTTPBearer()

def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security),
    db: Session = Depends(get_db)
) -> User:
    token = credentials.credentials
    try:
        payload = decode_access_token(token)
        user_id = payload.get("sub")
        if not user_id:
            raise HTTPException(status_code=401, detail="Invalid token")
    except Exception:
        raise HTTPException(status_code=401, detail="Invalid or expired token")
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=401, detail="User not found")
    return user

def require_admin(current_user: User = Depends(get_current_user)) -> User:
    if current_user.role != "admin":
        raise HTTPException(status_code=403, detail="Admin access required")
    return current_user

def require_officer(current_user: User = Depends(get_current_user)) -> User:
    if current_user.role not in ("admin", "officer"):
        raise HTTPException(status_code=403, detail="Officer access required")
    return current_user
```

### Task 3.3 — Auth schemas

**`schemas/auth.py`:**
```python
from pydantic import BaseModel, EmailStr
from typing import Optional

class SignupRequest(BaseModel):
    full_name: str
    email: EmailStr
    phone_number: Optional[str] = None
    country_code: Optional[str] = None
    password: str
    role: str = "citizen"

class LoginRequest(BaseModel):
    email: EmailStr
    password: str

class AuthResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: dict

class VerifyEmailRequest(BaseModel):
    email: EmailStr
    otp_code: str

class ForgotPasswordRequest(BaseModel):
    email: EmailStr

class ResetPasswordRequest(BaseModel):
    email: EmailStr
    otp_code: str
    new_password: str

class ChangePasswordRequest(BaseModel):
    current_password: str
    new_password: str
```

### Task 3.4 — Email service

**`services/email_service.py`:**
```python
import smtplib
from email.mime.text import MIMEText
from config import settings

def send_otp_email(to_email: str, otp_code: str, purpose: str):
    subject = "CivicIssue - Email Verification" if purpose == "EMAIL_VERIFY" else "CivicIssue - Password Reset"
    body = f"Your OTP code is: {otp_code}\n\nThis code expires in 10 minutes."
    msg = MIMEText(body)
    msg["Subject"] = subject
    msg["From"] = settings.SMTP_USER
    msg["To"] = to_email
    with smtplib.SMTP(settings.SMTP_HOST, settings.SMTP_PORT) as server:
        server.starttls()
        server.login(settings.SMTP_USER, settings.SMTP_PASSWORD)
        server.sendmail(settings.SMTP_USER, to_email, msg.as_string())
```

### Task 3.5 — Auth service

**`services/auth_service.py`:**
Handles signup (create user + send OTP), login (verify password + return JWT), verify-email, forgot-password, verify-otp, reset-password, change-password. Each function takes `db: Session` and the relevant schema, queries the User model, and uses `utils/security.py` for hashing/tokens.

### Task 3.6 — Auth router

**`routers/auth.py`:**
7 endpoints matching PRD section 5.1:
- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/verify-email`
- `POST /api/auth/forgot-password`
- `POST /api/auth/verify-otp`
- `POST /api/auth/reset-password`
- `PUT /api/auth/change-password` (requires auth)

### Task 3.7 — Register auth router in main.py

```python
from routers import auth
app.include_router(auth.router, prefix="/api/auth", tags=["Authentication"])
```

**Verification:**
1. `POST /api/auth/signup` with `{"full_name":"Test","email":"test@test.com","password":"test123","role":"citizen"}` → returns 201 with token
2. `POST /api/auth/login` with same credentials → returns token
3. `GET /health` with `Authorization: Bearer <token>` → works

---

## Phase 4: User Profile

### Task 4.1 — User schemas

**`schemas/user.py`:**
```python
from pydantic import BaseModel
from typing import Optional

class UserProfile(BaseModel):
    id: str
    full_name: str
    email: str
    phone_number: Optional[str]
    country_code: Optional[str]
    role: str
    avatar_url: Optional[str]
    is_verified: bool

class UpdateProfileRequest(BaseModel):
    full_name: Optional[str] = None
    phone_number: Optional[str] = None
    country_code: Optional[str] = None
```

### Task 4.2 — Users router

**`routers/users.py`:**
- `GET /api/users/me` — returns current user profile
- `PUT /api/users/me` — update name, phone, country code
- `PUT /api/users/me/avatar` — upload avatar image (multipart), save to uploads/, update avatar_url

**Verification:** Login → GET /api/users/me → returns user data

---

## Phase 5: Image Upload & Static Serving

### Task 5.1 — Images router

**`routers/images.py`:**
- `POST /api/images/upload` — accepts multipart file, validates MIME (JPEG/PNG), max 10MB, saves to `uploads/` with UUID filename, returns `{"image_url": "/uploads/<uuid>.jpg"}`
- `POST /api/images/analyze` — upload image → save → call Gemini → return analysis result

Image validation logic:
```python
import os, uuid
from PIL import Image
from fastapi import UploadFile, HTTPException
from config import settings

ALLOWED_TYPES = {"image/jpeg", "image/png"}

async def save_upload(file: UploadFile) -> str:
    if file.content_type not in ALLOWED_TYPES:
        raise HTTPException(400, "Only JPEG/PNG allowed")
    contents = await file.read()
    if len(contents) > settings.MAX_FILE_SIZE_MB * 1024 * 1024:
        raise HTTPException(400, "File too large")
    ext = "jpg" if "jpeg" in file.content_type else "png"
    filename = f"{uuid.uuid4()}.{ext}"
    filepath = os.path.join(settings.UPLOAD_DIR, filename)
    with open(filepath, "wb") as f:
        f.write(contents)
    return f"/uploads/{filename}"
```

**Verification:** Upload a JPEG → get back URL → access `http://localhost:8000/uploads/<file>` in browser

---

## Phase 6: Gemini AI Service

### Task 6.1 — Gemini service (ALL AI in one file)

**`services/gemini_service.py`:**

Three functions, all calling `google.generativeai`:

```python
import google.generativeai as genai
import json
from config import settings

genai.configure(api_key=settings.GEMINI_API_KEY)
model = genai.GenerativeModel("gemini-2.5-flash")

async def analyze_image(image_path: str) -> dict:
    """Send image to Gemini with classification prompt. Returns dict with
    category, severity, confidence, tags, description."""
    image = genai.upload_file(image_path)
    prompt = """You are a civic issue analysis AI. Analyze this image of a civic/infrastructure issue.
    Return ONLY valid JSON with these fields:
    - "category": one of ["pothole", "garbage", "street_light", "water_leakage", "drainage", "road_damage", "broken_infrastructure", "other"]
    - "severity": one of ["LOW", "MEDIUM", "HIGH", "CRITICAL"]
    - "confidence": float between 0.0 and 1.0
    - "tags": list of 3-5 descriptive keyword strings
    - "description": one-line summary of what you see"""
    response = model.generate_content([prompt, image])
    return json.loads(response.text.strip().strip("```json").strip("```"))

async def analyze_text(description: str) -> dict:
    """Send text to Gemini for category/keyword extraction."""
    prompt = f"""You are a civic issue text analyzer. Analyze this citizen's issue description.
    Description: "{description}"
    Return ONLY valid JSON with these fields:
    - "category": one of ["pothole", "garbage", "street_light", "water_leakage", "drainage", "road_damage", "broken_infrastructure", "other"]
    - "keywords": list of relevant keywords
    - "suggested_priority": one of ["LOW", "MEDIUM", "HIGH"]
    - "urgency_indicator": brief reason for the suggested priority level"""
    response = model.generate_content(prompt)
    return json.loads(response.text.strip().strip("```json").strip("```"))

async def chat(message: str, history: list, user_role: str, context: str = "") -> str:
    """Chatbot with system prompt + conversation history."""
    system = f"""You are CivicBot, an AI assistant for the CivicIssue app.
    User role: {user_role}
    For citizens: Help with reporting issues, explain the process, answer FAQs.
    For admins: Help with prioritization, summarize trends, provide recommendations.
    Keep responses concise and helpful. {context}"""
    chat_session = model.start_chat(history=[
        {"role": "user" if msg["is_user"] else "model", "parts": [msg["text"]]}
        for msg in history
    ])
    response = chat_session.send_message(f"{system}\n\nUser: {message}")
    return response.text
```

**Verification:** Test `analyze_text("There's a huge pothole on MG Road")` → returns JSON with category "pothole"

---

## Phase 7: Complaint CRUD

### Task 7.1 — Complaint schemas

**`schemas/complaint.py`:**
```python
from pydantic import BaseModel
from typing import Optional, List
from datetime import datetime

class ComplaintCreate(BaseModel):
    title: str
    description: Optional[str] = None
    category: Optional[str] = None
    ai_detected_category: Optional[str] = None
    ai_text_category: Optional[str] = None
    location_text: Optional[str] = None
    latitude: float
    longitude: float
    priority: Optional[str] = "MEDIUM"
    severity_level: Optional[str] = "MEDIUM"
    ai_confidence: Optional[float] = None
    ai_keywords: Optional[list] = None

class ComplaintResponse(BaseModel):
    id: str
    complaint_number: str
    citizen_id: str
    citizen_name: str  # joined from users table
    title: str
    description: Optional[str]
    category: Optional[str]
    location_text: Optional[str]
    latitude: float
    longitude: float
    priority: str
    severity_level: str
    status: str
    assigned_officer_id: Optional[str]
    assigned_officer_name: Optional[str]  # joined from users table
    group_id: Optional[str]
    ai_confidence: Optional[float]
    ai_keywords: Optional[list]
    resolution_notes: Optional[str]
    images: List[str]  # list of image URLs
    created_at: datetime
    updated_at: datetime

class StatusUpdateRequest(BaseModel):
    status: str
    notes: Optional[str] = None

class AssignOfficerRequest(BaseModel):
    officer_id: str

class ResolveRequest(BaseModel):
    resolution_notes: str

class PaginatedResponse(BaseModel):
    items: list
    total: int
    page: int
    limit: int
    pages: int

class ComplaintStats(BaseModel):
    total: int
    unassigned: int
    assigned: int
    in_progress: int
    resolved: int
    by_category: dict
    by_severity: dict
    recent_7_days: int
```

### Task 7.2 — Complaint service

**`services/complaint_service.py`:**
- `create_complaint(db, citizen_id, data, image_urls)` — generates complaint number, saves record + images, triggers grouping check, creates admin notification
- `get_complaints(db, user, filters, page, limit)` — citizen sees own, admin sees all, supports filtering by status/category/priority/severity/search, pagination
- `get_complaint_by_id(db, complaint_id)` — returns full complaint with images and user info
- `update_status(db, complaint_id, new_status, changed_by, notes)` — updates status, logs to history table, creates notification
- `assign_officer(db, complaint_id, officer_id, assigned_by)` — sets assigned_officer_id, updates officer workload, creates notifications for both citizen and officer
- `resolve_complaint(db, complaint_id, notes, resolved_by)` — sets status=RESOLVED, resolution_notes, resolved_at, notifies citizen
- `get_status_history(db, complaint_id)` — returns timeline
- `get_similar_complaints(db, complaint_id)` — same category within 2km using Haversine
- `get_map_data(db)` — returns all complaints as `{id, lat, lng, category, severity, status}`
- `get_stats(db)` — aggregated dashboard stats

### Task 7.3 — Complaints router

**`routers/complaints.py`:**
10 endpoints matching PRD section 5.2. The `POST /` endpoint accepts multipart form (images + JSON body as a form field).

### Task 7.4 — Register in main.py

**Verification:** Create complaint via POST with images → GET returns it with complaint_number and images

---

## Phase 8: Complaint Status & History

Already handled within Task 7.2 (`update_status` logs to `complaint_status_history` table). The `GET /{id}/history` endpoint is in Task 7.3.

**Verification:** Update status → GET history → see timeline with old_status, new_status, changed_by, notes, timestamp

---

## Phase 9: Nearby Issue Grouping

### Task 9.1 — Grouping service

**`services/grouping_service.py`:**

```python
import math
from sqlalchemy.orm import Session
from models.complaint import Complaint
from models.issue_group import IssueGroup
from datetime import datetime, timedelta

EARTH_RADIUS_KM = 6371
MAX_DISTANCE_KM = 2

def haversine(lat1, lon1, lat2, lon2) -> float:
    """Returns distance in km between two lat/lng points."""
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = math.sin(dlat/2)**2 + math.cos(math.radians(lat1)) * math.cos(math.radians(lat2)) * math.sin(dlon/2)**2
    return 2 * EARTH_RADIUS_KM * math.asin(math.sqrt(a))

def check_and_group(db: Session, complaint: Complaint):
    """Called after complaint creation. Finds nearby same-category complaints
    within 2km and 30 days, adds to existing group or creates new one."""
    cutoff = datetime.utcnow() - timedelta(days=30)
    candidates = db.query(Complaint).filter(
        Complaint.category == complaint.category,
        Complaint.status.notin_(["COMPLETED", "RESOLVED"]),
        Complaint.created_at >= cutoff,
        Complaint.id != complaint.id
    ).all()

    nearby = [c for c in candidates if haversine(
        complaint.latitude, complaint.longitude, c.latitude, c.longitude
    ) <= MAX_DISTANCE_KM]

    if not nearby:
        return  # stays ungrouped

    # Check if any nearby complaint already has a group
    existing_group_id = None
    for c in nearby:
        if c.group_id:
            existing_group_id = c.group_id
            break

    if existing_group_id:
        group = db.query(IssueGroup).get(existing_group_id)
        complaint.group_id = existing_group_id
        group.complaint_count += 1
        # Recalculate center
        all_in_group = db.query(Complaint).filter(Complaint.group_id == existing_group_id).all()
        group.center_lat = sum(c.latitude for c in all_in_group) / len(all_in_group)
        group.center_lng = sum(c.longitude for c in all_in_group) / len(all_in_group)
    else:
        # Create new group
        group = IssueGroup(
            category=complaint.category,
            center_lat=sum(c.latitude for c in [complaint] + nearby) / (len(nearby) + 1),
            center_lng=sum(c.longitude for c in [complaint] + nearby) / (len(nearby) + 1),
            complaint_count=len(nearby) + 1,
        )
        db.add(group)
        db.flush()
        complaint.group_id = group.id
        for c in nearby:
            c.group_id = group.id

    db.commit()
```

### Task 9.2 — Groups router

**`routers/groups.py`:**
- `GET /api/groups/` — list active groups with complaint counts (admin only)
- `GET /api/groups/{id}` — group detail with all member complaints (admin only)

**Verification:** Create two complaints with same category and nearby coordinates → both get grouped

---

## Phase 10: Notification System

### Task 10.1 — Notification service

**`services/notification_service.py`:**
```python
def create_notification(db, recipient_id, complaint_id, title, message, type, priority):
    """Creates a notification record."""

def notify_admins_new_complaint(db, complaint):
    """Creates notification for ALL admin users when new complaint arrives."""

def notify_status_change(db, complaint, old_status, new_status):
    """Notifies the reporting citizen of status changes."""

def notify_officer_assigned(db, complaint, officer_user_id):
    """Notifies the assigned officer."""
```

### Task 10.2 — Notification schemas + router

**`schemas/notification.py`:**
```python
class NotificationResponse(BaseModel):
    id: str
    complaint_id: Optional[str]
    title: str
    message: str
    type: str
    priority: str
    is_read: bool
    created_at: datetime
```

**`routers/notifications.py`:**
- `GET /api/notifications/` — user's notifications
- `GET /api/notifications/unread-count` — `{"count": N}`
- `PUT /api/notifications/{id}/read` — mark one as read
- `PUT /api/notifications/read-all` — mark all as read

**Verification:** Create complaint → check admin notifications → mark as read → unread count decreases

---

## Phase 11: Officer Management & Assignment

### Task 11.1 — Officer endpoints (in admin router)

**`routers/admin.py`:**
- `GET /api/admin/officers` — list officers with user info and workload
- `POST /api/admin/officers` — create new officer (creates user with role "officer" + officer record)

Assignment is handled in Phase 7 (`PUT /api/complaints/{id}/assign`).

**Verification:** Create officer → assign to complaint → officer workload_count increments → both citizen and officer get notifications

---

## Phase 12: Admin Endpoints

### Task 12.1 — Admin schemas

**`schemas/admin.py`:**
```python
class CategoryCreate(BaseModel):
    name: str
    description: Optional[str] = None
    icon: Optional[str] = None

class DepartmentCreate(BaseModel):
    name: str
    description: Optional[str] = None

class OfficerCreate(BaseModel):
    full_name: str
    email: str
    password: str
    phone_number: Optional[str] = None
    department: Optional[str] = None
    designation: Optional[str] = None

class DashboardStats(BaseModel):
    total_complaints: int
    total_citizens: int
    total_officers: int
    unassigned: int
    assigned: int
    in_progress: int
    resolved: int
    by_category: dict
    by_severity: dict
    recent_7_days: int
    resolution_rate: float
```

### Task 12.2 — Admin router

**`routers/admin.py`:**
All admin-only endpoints:
- Categories CRUD: GET, POST, PUT, DELETE `/api/admin/categories`
- Departments CRUD: GET, POST, PUT, DELETE `/api/admin/departments`
- Officers: GET, POST `/api/admin/officers`
- `GET /api/admin/system-logs` — paginated audit logs
- `GET /api/admin/dashboard-stats` — full dashboard statistics

**Verification:** Create category → list categories → update → soft delete (is_active=false)

---

## Phase 13: Geocoding

### Task 13.1 — Geocoding service + router

**`services/geocoding_service.py`:**
```python
import httpx
from config import settings

async def reverse_geocode(lat: float, lng: float) -> dict:
    async with httpx.AsyncClient() as client:
        response = await client.get(
            "https://nominatim.openstreetmap.org/reverse",
            params={"lat": lat, "lon": lng, "format": "json"},
            headers={"User-Agent": settings.NOMINATIM_USER_AGENT}
        )
        data = response.json()
        return {
            "display_name": data.get("display_name", ""),
            "address": data.get("address", {})
        }
```

**`routers/geo.py`:**
- `GET /api/geo/reverse?lat=X&lng=Y` — returns address from Nominatim

**Verification:** Call with known coordinates → get address string

---

## Phase 14: Chatbot Endpoints

### Task 14.1 — AI router (chatbot + text analysis)

**`routers/ai.py`:**
- `POST /api/ai/analyze-text` — calls `gemini_service.analyze_text()`
- `POST /api/ai/chatbot` — creates/continues session, calls `gemini_service.chat()`, saves messages
- `GET /api/ai/chatbot/history` — returns user's chat sessions and messages

**Verification:** Send chatbot message → get AI response → check history persists

---

## Phase 15: Map Data & Similar Issues

Already handled in Phase 7:
- `GET /api/complaints/map-data` — all complaints as markers
- `GET /api/complaints/{id}/similar` — nearby same-category complaints

**Verification:** Create 3 complaints in same area → call similar → returns the other 2

---

## Phase 16: Seed Data

### Task 16.1 — Seed script in main.py startup event

```python
@app.on_event("startup")
async def seed_data():
    db = SessionLocal()
    try:
        # Create default categories if none exist
        if db.query(Category).count() == 0:
            for name in ["Pothole", "Garbage/Waste", "Street Light", "Water Leakage",
                         "Drainage", "Road Damage", "Broken Infrastructure", "Other"]:
                db.add(Category(name=name))

        # Create default departments
        if db.query(Department).count() == 0:
            for name in ["Roads & Infrastructure", "Sanitation", "Electricity",
                         "Water Supply", "General Maintenance"]:
                db.add(Department(name=name))

        # Create default admin
        if not db.query(User).filter(User.email == "admin@civicissue.com").first():
            db.add(User(
                full_name="Admin",
                email="admin@civicissue.com",
                password_hash=hash_password("admin123"),
                role="admin",
                is_verified=True
            ))

        db.commit()
    finally:
        db.close()
```

**Verification:** Start server → check categories table has 8 rows, departments has 5, admin user exists

---

## Phase 17: Frontend Wiring (Android/Kotlin)

This is the largest phase. Every screen currently using mock data must be connected to the real backend.

### Task 17.1 — New/updated data models

**Modify `app/src/main/java/com/example/civicissue/data/model/ComplaintModels.kt`:**

Replace existing data classes with backend-compatible versions:

```kotlin
// Current (to delete):
data class Complaint(val id: String, val citizenName: String, ...)

// New (matches backend response):
data class Complaint(
    val id: String,
    @SerializedName("complaint_number") val complaintNumber: String,
    @SerializedName("citizen_id") val citizenId: String,
    @SerializedName("citizen_name") val citizenName: String,
    val title: String,
    val description: String?,
    val category: String?,
    @SerializedName("location_text") val locationText: String?,
    val latitude: Double,
    val longitude: Double,
    val priority: String,
    @SerializedName("severity_level") val severityLevel: String,
    val status: String,
    @SerializedName("assigned_officer_id") val assignedOfficerId: String?,
    @SerializedName("assigned_officer_name") val assignedOfficerName: String?,
    @SerializedName("group_id") val groupId: String?,
    @SerializedName("ai_confidence") val aiConfidence: Float?,
    @SerializedName("ai_keywords") val aiKeywords: List<String>?,
    @SerializedName("resolution_notes") val resolutionNotes: String?,
    val images: List<String>,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("updated_at") val updatedAt: String
)
```

**Create `app/src/main/java/com/example/civicissue/data/model/AuthModels.kt`:**
```kotlin
data class SignupRequest(val full_name: String, val email: String, val password: String, val phone_number: String?, val country_code: String?, val role: String = "citizen")
data class LoginRequest(val email: String, val password: String)
data class AuthResponse(val access_token: String, val token_type: String, val user: UserProfile)
data class UserProfile(val id: String, val full_name: String, val email: String, val phone_number: String?, val country_code: String?, val role: String, val avatar_url: String?, val is_verified: Boolean)
```

**Create `app/src/main/java/com/example/civicissue/data/model/NotificationModels.kt`:**
```kotlin
data class NotificationItem(val id: String, val complaint_id: String?, val title: String, val message: String, val type: String, val priority: String, val is_read: Boolean, val created_at: String)
data class UnreadCountResponse(val count: Int)
```

**Create `app/src/main/java/com/example/civicissue/data/model/AIModels.kt`:**
```kotlin
data class ImageAnalysisResult(val image_url: String, val detected_category: String, val severity_level: String, val confidence_score: Float, val tags: List<String>, val description_suggestion: String)
data class TextAnalysisResult(val detected_category: String, val keywords: List<String>, val suggested_priority: String, val urgency_indicator: String)
data class ChatRequest(val message: String, val session_id: String?)
data class ChatResponse(val response: String, val session_id: String)
```

**Create `app/src/main/java/com/example/civicissue/data/model/AdminModels.kt`:**
```kotlin
data class CategoryItem(val id: String, val name: String, val description: String?, val icon: String?, val is_active: Boolean)
data class DepartmentItem(val id: String, val name: String, val description: String?, val is_active: Boolean)
data class OfficerDetail(val id: String, val user_id: String, val full_name: String, val email: String, val department: String?, val designation: String?, val workload_count: Int, val is_available: Boolean)
data class DashboardStats(val total_complaints: Int, val total_citizens: Int, val total_officers: Int, val unassigned: Int, val assigned: Int, val in_progress: Int, val resolved: Int, val by_category: Map<String, Int>, val by_severity: Map<String, Int>, val recent_7_days: Int, val resolution_rate: Float)
```

### Task 17.2 — TokenManager

**Create `app/src/main/java/com/example/civicissue/data/TokenManager.kt`:**
```kotlin
object TokenManager {
    private var token: String? = null
    private var currentUser: UserProfile? = null

    fun saveToken(t: String) { token = t }
    fun getToken(): String? = token
    fun saveUser(u: UserProfile) { currentUser = u }
    fun getUser(): UserProfile? = currentUser
    fun clear() { token = null; currentUser = null }
    fun isLoggedIn(): Boolean = token != null
}
```

### Task 17.3 — Update RetrofitClient with JWT interceptor

**Modify `app/src/main/java/com/example/civicissue/network/RetrofitClient.kt`:**
```kotlin
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:8000/"

    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val requestBuilder = chain.request().newBuilder()
            TokenManager.getToken()?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
            chain.proceed(requestBuilder.build())
        }
        .build()

    val instance: CivicApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CivicApiService::class.java)
    }
}
```

### Task 17.4 — Expand CivicApiService to 35+ endpoints

**Modify `app/src/main/java/com/example/civicissue/network/CivicApiService.kt`:**

Replace current 3 endpoints with full API:

```kotlin
interface CivicApiService {
    // Auth
    @POST("api/auth/signup") suspend fun signup(@Body body: SignupRequest): AuthResponse
    @POST("api/auth/login") suspend fun login(@Body body: LoginRequest): AuthResponse
    @POST("api/auth/verify-email") suspend fun verifyEmail(@Body body: VerifyEmailRequest): Map<String, String>
    @POST("api/auth/forgot-password") suspend fun forgotPassword(@Body body: ForgotPasswordRequest): Map<String, String>
    @POST("api/auth/verify-otp") suspend fun verifyOtp(@Body body: VerifyOtpRequest): Map<String, String>
    @POST("api/auth/reset-password") suspend fun resetPassword(@Body body: ResetPasswordRequest): Map<String, String>
    @PUT("api/auth/change-password") suspend fun changePassword(@Body body: ChangePasswordRequest): Map<String, String>

    // Complaints
    @GET("api/complaints") suspend fun getComplaints(@QueryMap filters: Map<String, String>): PaginatedResponse<Complaint>
    @Multipart
    @POST("api/complaints") suspend fun createComplaint(@Part images: List<MultipartBody.Part>, @Part("data") body: RequestBody): Complaint
    @GET("api/complaints/{id}") suspend fun getComplaint(@Path("id") id: String): Complaint
    @PUT("api/complaints/{id}/status") suspend fun updateComplaintStatus(@Path("id") id: String, @Body body: StatusUpdateRequest): Complaint
    @PUT("api/complaints/{id}/assign") suspend fun assignOfficer(@Path("id") id: String, @Body body: AssignOfficerRequest): Complaint
    @PUT("api/complaints/{id}/resolve") suspend fun resolveComplaint(@Path("id") id: String, @Body body: ResolveRequest): Complaint
    @GET("api/complaints/{id}/history") suspend fun getComplaintHistory(@Path("id") id: String): List<StatusHistoryItem>
    @GET("api/complaints/{id}/similar") suspend fun getSimilarComplaints(@Path("id") id: String): List<Complaint>
    @GET("api/complaints/map-data") suspend fun getMapData(): List<MapMarker>
    @GET("api/complaints/stats") suspend fun getComplaintStats(): ComplaintStats

    // Images & AI
    @Multipart
    @POST("api/images/upload") suspend fun uploadImage(@Part image: MultipartBody.Part): Map<String, String>
    @Multipart
    @POST("api/images/analyze") suspend fun analyzeImage(@Part image: MultipartBody.Part): ImageAnalysisResult
    @POST("api/ai/analyze-text") suspend fun analyzeText(@Body body: TextAnalysisRequest): TextAnalysisResult
    @POST("api/ai/chatbot") suspend fun chat(@Body body: ChatRequest): ChatResponse
    @GET("api/ai/chatbot/history") suspend fun getChatHistory(): List<ChatMessage>

    // Notifications
    @GET("api/notifications") suspend fun getNotifications(): List<NotificationItem>
    @GET("api/notifications/unread-count") suspend fun getUnreadCount(): UnreadCountResponse
    @PUT("api/notifications/{id}/read") suspend fun markNotificationRead(@Path("id") id: String): Map<String, String>
    @PUT("api/notifications/read-all") suspend fun markAllNotificationsRead(): Map<String, String>

    // Users
    @GET("api/users/me") suspend fun getProfile(): UserProfile
    @PUT("api/users/me") suspend fun updateProfile(@Body body: UpdateProfileRequest): UserProfile
    @Multipart
    @PUT("api/users/me/avatar") suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): UserProfile

    // Admin
    @GET("api/admin/categories") suspend fun getCategories(): List<CategoryItem>
    @POST("api/admin/categories") suspend fun createCategory(@Body body: CategoryCreate): CategoryItem
    @PUT("api/admin/categories/{id}") suspend fun updateCategory(@Path("id") id: String, @Body body: CategoryCreate): CategoryItem
    @DELETE("api/admin/categories/{id}") suspend fun deleteCategory(@Path("id") id: String): Map<String, String>
    @GET("api/admin/departments") suspend fun getDepartments(): List<DepartmentItem>
    @POST("api/admin/departments") suspend fun createDepartment(@Body body: DepartmentCreate): DepartmentItem
    @GET("api/admin/officers") suspend fun getOfficers(): List<OfficerDetail>
    @POST("api/admin/officers") suspend fun createOfficer(@Body body: OfficerCreateRequest): OfficerDetail
    @GET("api/admin/system-logs") suspend fun getSystemLogs(@QueryMap params: Map<String, String>): PaginatedResponse<SystemLogItem>
    @GET("api/admin/dashboard-stats") suspend fun getDashboardStats(): DashboardStats

    // Groups
    @GET("api/groups") suspend fun getIssueGroups(): List<IssueGroupItem>
    @GET("api/groups/{id}") suspend fun getIssueGroup(@Path("id") id: String): IssueGroupDetail

    // Geocoding
    @GET("api/geo/reverse") suspend fun reverseGeocode(@Query("lat") lat: Double, @Query("lng") lng: Double): GeoResponse
}
```

### Task 17.5 — Wire auth screens

**Screens to modify:**

| Screen File | Current Behavior | New Behavior |
|---|---|---|
| `ui/auth/LoginScreen.kt` | Navigates to home with manual role selection | Calls `api.login()`, saves token via TokenManager, navigates based on `user.role` |
| `ui/auth/SignUpScreen.kt` | Direct navigation, no validation | Calls `api.signup()`, navigates to OTP verification |
| `ui/auth/OTPVerificationScreen.kt` | Hardcoded success | Calls `api.verifyEmail()` |
| `ui/auth/ForgotPasswordScreen.kt` | No backend | Calls `api.forgotPassword()` |
| `ui/auth/SetNewPasswordScreen.kt` | No backend | Calls `api.resetPassword()` |
| `ui/auth/ResetPasswordScreen.kt` | No backend | Calls `api.verifyOtp()` then `api.resetPassword()` |

### Task 17.6 — Wire citizen screens

| Screen | Changes |
|---|---|
| `CitizenHomeScreen.kt` | Fetch real complaints from API, real stats |
| `NewComplaintScreen.kt` | Upload images via multipart, call AI analyze, submit real complaint |
| `ComplaintDetailsScreen.kt` | Fetch from `api.getComplaint(id)`, show real images/status |
| `ComplaintTrackingScreen.kt` | Fetch `api.getComplaintHistory(id)` for timeline |
| `MyComplaintsScreen.kt` | Fetch `api.getComplaints(page, filters)` with pagination |
| `CitizenNotificationsScreen.kt` | Fetch `api.getNotifications()`, mark read |
| `MapScreen.kt` | Fetch `api.getMapData()` for markers |
| `NearbyIssuesScreen.kt` | Fetch `api.getSimilarComplaints(id)` |
| `AIDetectionScreen.kt` | Call `api.analyzeImage()` with real camera image |
| `ChatBotScreen.kt` | Call `api.chat()` with session management |
| `CitizenProfileScreen.kt` | Fetch/update via `api.getProfile()` / `api.updateProfile()` |
| `SettingsScreen.kt` | Wire change password to `api.changePassword()` |

### Task 17.7 — Wire admin screens

| Screen | Changes |
|---|---|
| `AdminDashboardScreen.kt` | Fetch `api.getDashboardStats()` for real numbers |
| `AdminComplaintsListScreen.kt` | Fetch complaints with filters, real pagination |
| `AdminComplaintDetailScreen.kt` | Real data, assign officer, update status, resolve |
| `AdminOfficerManagementScreen.kt` | Fetch `api.getOfficers()`, create new officers |
| `AdminAssignOfficerScreen.kt` | Call `api.assignOfficer(complaintId, officerId)` |
| `AdminNotificationsScreen.kt` | Fetch `api.getNotifications()` |
| `AdminReportsScreen.kt` | Fetch `api.getComplaintStats()` |
| `AdminAnalyticsScreen.kt` | Fetch real analytics data |
| `AdminSystemLogsScreen.kt` | Fetch `api.getSystemLogs()` |
| `AdminSettingsScreen.kt` | Wire to real endpoints |
| `AdminCategoryManagementScreen.kt` | CRUD via `api.getCategories()`, `createCategory()`, etc. |

### Task 17.8 — Multipart image upload helper

**Create `app/src/main/java/com/example/civicissue/utils/ImageUtils.kt`:**
```kotlin
fun Bitmap.toMultipartPart(name: String = "image"): MultipartBody.Part {
    val stream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 85, stream)
    val requestBody = stream.toByteArray().toRequestBody("image/jpeg".toMediaType())
    return MultipartBody.Part.createFormData(name, "image.jpg", requestBody)
}

fun Uri.toMultipartPart(context: Context, name: String = "image"): MultipartBody.Part {
    val inputStream = context.contentResolver.openInputStream(this)!!
    val bytes = inputStream.readBytes()
    val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())
    return MultipartBody.Part.createFormData(name, "image.jpg", requestBody)
}
```

---

## Phase 18: Testing

### Task 18.1 — Backend unit tests

**Create `backend/tests/` directory with:**
- `test_auth.py` — test signup, login, token generation, password hashing
- `test_complaints.py` — test CRUD, filtering, pagination
- `test_gemini.py` — test with mocked Gemini responses
- `test_grouping.py` — test Haversine calculation, grouping logic
- `test_notifications.py` — test auto-creation on events

**Test runner:**
```bash
cd backend
pytest tests/ -v
```

### Task 18.2 — Integration verification

Manual checklist:
1. Start backend: `uvicorn main:app --reload`
2. Run Android app on emulator
3. Test signup → login → create complaint with image → see AI analysis → check notification
4. Login as admin → see dashboard stats → assign officer → resolve complaint
5. Verify notification appears for citizen after resolution

---

## Phase 19: Post-Implementation

### Task 19.1 — Gmail App Password Guide

After implementation, provide user with step-by-step guide to:
1. Enable 2-Step Verification on Google Account
2. Generate App Password at myaccount.google.com/apppasswords
3. Add to `.env` as `SMTP_PASSWORD`

### Task 19.2 — Gemini API Key Setup

Guide to get API key from https://aistudio.google.com/apikey and add to `.env`

---

## Execution Order (Dependencies)

```
Phase 1 (Foundation) ──┐
                       ├── Phase 2 (Models + Migration)
                       │         │
                       │         ├── Phase 3 (Auth) ──────┐
                       │         │                        ├── Phase 4 (User Profile)
                       │         │                        ├── Phase 7 (Complaints) ──┐
                       │         │                        │                          ├── Phase 8 (Status History)
                       │         │                        │                          ├── Phase 9 (Grouping)
                       │         │                        │                          ├── Phase 10 (Notifications)
                       │         │                        │                          ├── Phase 11 (Officers)
                       │         │                        │                          └── Phase 15 (Map/Similar)
                       │         │                        ├── Phase 12 (Admin)
                       │         │                        └── Phase 14 (Chatbot)
                       │         └── Phase 16 (Seed Data)
                       ├── Phase 5 (Image Upload)
                       ├── Phase 6 (Gemini Service)
                       └── Phase 13 (Geocoding)

Phase 17 (Frontend Wiring) ── depends on ALL backend phases
Phase 18 (Testing) ── depends on Phase 17
Phase 19 (Post-Implementation) ── final step
```

## Estimated File Count

| Area | New Files | Modified Files |
|---|---|---|
| Backend | ~30 files | 0 (fresh build) |
| Frontend models | 4 new | 1 modified (ComplaintModels.kt) |
| Frontend network | 1 new (TokenManager) | 2 modified (RetrofitClient, CivicApiService) |
| Frontend screens | 0 new | ~30 modified |
| Frontend utils | 1 new (ImageUtils) | 0 |
| Tests | 5 new | 0 |
| **Total** | **~41 new** | **~33 modified** |
