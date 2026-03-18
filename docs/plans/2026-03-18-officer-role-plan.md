# Officer Role Feature - Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a full officer workflow to CivicIssue — officers receive assignments, post progress updates, submit completion proof; admin reviews and approves or requests rework; citizens see full transparency.

**Architecture:** Extend existing backend (FastAPI + SQLAlchemy) with new `complaint_updates` table, add `REWORK` status to complaint ENUM, create new API endpoints for officer actions and admin review. Extend Android app with officer portal screens using same role-based routing pattern as citizen/admin.

**Tech Stack:** FastAPI, SQLAlchemy, Alembic (backend); Kotlin, Jetpack Compose, Retrofit (frontend); MySQL/MariaDB (database)

---

## Task 1: Database Migration — Add REWORK status and complaint_updates table

**Files:**
- Create: `backend/alembic/versions/b2c3d4e5f6g7_add_officer_workflow.py`
- Modify: `backend/models/complaint.py` (line 23-26, status ENUM)

**Step 1: Create the Alembic migration file**

Create `backend/alembic/versions/b2c3d4e5f6g7_add_officer_workflow.py`:

```python
"""Add officer workflow - REWORK status and complaint_updates table"""

from alembic import op
import sqlalchemy as sa

revision = "b2c3d4e5f6g7"
down_revision = "a1b2c3d4e5f6"
branch_labels = None
depends_on = None


def upgrade():
    # 1. Alter complaint status ENUM to add REWORK
    # MySQL requires recreating the ENUM column
    op.alter_column(
        "complaints",
        "status",
        existing_type=sa.Enum("UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED"),
        type_=sa.Enum("UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED", "REWORK"),
        existing_nullable=True,
    )

    # 2. Create complaint_updates table
    op.create_table(
        "complaint_updates",
        sa.Column("id", sa.String(36), primary_key=True),
        sa.Column("complaint_id", sa.String(36), sa.ForeignKey("complaints.id"), nullable=False),
        sa.Column("officer_id", sa.String(36), sa.ForeignKey("users.id"), nullable=False),
        sa.Column("message", sa.Text, nullable=False),
        sa.Column("image_url", sa.String(500), nullable=True),
        sa.Column("created_at", sa.TIMESTAMP, server_default=sa.func.now()),
    )


def downgrade():
    op.drop_table("complaint_updates")
    op.alter_column(
        "complaints",
        "status",
        existing_type=sa.Enum("UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED", "REWORK"),
        type_=sa.Enum("UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED"),
        existing_nullable=True,
    )
```

**Step 2: Update the Complaint model to include REWORK in ENUM**

Modify `backend/models/complaint.py` lines 23-26. Change:
```python
    status = Column(
        Enum("UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED"),
        default="UNASSIGNED",
    )
```
To:
```python
    status = Column(
        Enum("UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED", "REWORK"),
        default="UNASSIGNED",
    )
```

**Step 3: Create the ComplaintUpdate model**

Create `backend/models/complaint_update.py`:

```python
import uuid
from sqlalchemy import Column, String, Text, TIMESTAMP, ForeignKey
from sqlalchemy.sql import func
from database import Base


class ComplaintUpdate(Base):
    __tablename__ = "complaint_updates"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    complaint_id = Column(String(36), ForeignKey("complaints.id"), nullable=False)
    officer_id = Column(String(36), ForeignKey("users.id"), nullable=False)
    message = Column(Text, nullable=False)
    image_url = Column(String(500), nullable=True)
    created_at = Column(TIMESTAMP, server_default=func.now())
```

**Step 4: Run the migration**

```bash
cd backend && alembic upgrade head
```

If Alembic ENUM alter fails on MySQL, run raw SQL instead:
```sql
ALTER TABLE complaints MODIFY COLUMN status ENUM('UNASSIGNED','ASSIGNED','IN_PROGRESS','COMPLETED','RESOLVED','REWORK') DEFAULT 'UNASSIGNED';

CREATE TABLE complaint_updates (
    id CHAR(36) PRIMARY KEY,
    complaint_id CHAR(36) NOT NULL,
    officer_id CHAR(36) NOT NULL,
    message TEXT NOT NULL,
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (complaint_id) REFERENCES complaints(id),
    FOREIGN KEY (officer_id) REFERENCES users(id)
);
```

**Step 5: Verify**

Check the database has the new table and updated ENUM:
```bash
cd backend && python -c "from models.complaint_update import ComplaintUpdate; print('Model OK')"
```

---

## Task 2: Backend Schemas — New request/response models

**Files:**
- Modify: `backend/schemas/complaint.py` (add new schemas)
- Modify: `backend/schemas/admin.py` (add officer stats schemas)

**Step 1: Add new schemas to `backend/schemas/complaint.py`**

Add after the existing `MapDataResponse` class (after line 102):

```python
class ComplaintUpdateCreate(BaseModel):
    message: str
    image_url: Optional[str] = None


class ComplaintUpdateResponse(BaseModel):
    id: str
    complaint_id: str
    officer_id: str
    officer_name: Optional[str] = None
    message: str
    image_url: Optional[str] = None
    created_at: Optional[datetime] = None


class CompleteComplaintRequest(BaseModel):
    notes: str
    resolution_image: Optional[str] = None


class ReworkRequest(BaseModel):
    reason: str
```

**Step 2: Update `ComplaintResponse` to include updates list**

Modify `backend/schemas/complaint.py` class `ComplaintResponse` (line 21-47). Add after `updated_at` field (line 47):

```python
    updates: List[Any] = []
```

**Step 3: Add REWORK to status label in `statusLabel` and `ComplaintStatus` on frontend**

This will be done in Task 8 (frontend models).

**Step 4: Add officer stats schema to `backend/schemas/admin.py`**

Add after `DashboardStats` class (after line 83):

```python
class OfficerStats(BaseModel):
    officer_id: str
    user_id: str
    full_name: str
    email: str
    department: Optional[str] = None
    designation: Optional[str] = None
    workload_count: int
    is_available: bool
    total_assigned: int = 0
    total_completed: int = 0
    total_rework: int = 0
    avg_resolution_hours: Optional[float] = None


class OfficerUpdate(BaseModel):
    department: Optional[str] = None
    designation: Optional[str] = None
    is_available: Optional[bool] = None
```

**Step 5: Add REWORK count to DashboardStats**

Modify `backend/schemas/admin.py` `DashboardStats` class (line 71-83). Add after `completed` field (line 79):

```python
    rework: int = 0
```

---

## Task 3: Backend Service — Officer workflow functions

**Files:**
- Modify: `backend/services/complaint_service.py` (add new functions, modify existing)

**Step 1: Add import for ComplaintUpdate model**

Add to imports at top of `backend/services/complaint_service.py` (after line 8):

```python
from models.complaint_update import ComplaintUpdate
```

**Step 2: Add `post_update` function**

Add after `resolve_complaint` function (after line 432):

```python
def post_complaint_update(
    db: Session,
    complaint_id: str,
    officer_id: str,
    message: str,
    image_url: str = None,
) -> dict:
    """Officer posts a progress update on a complaint."""
    complaint = db.query(Complaint).filter(Complaint.id == complaint_id).first()
    if not complaint:
        return None

    # Verify this officer is assigned to this complaint
    if complaint.assigned_officer_id != officer_id:
        return "NOT_ASSIGNED"

    update = ComplaintUpdate(
        id=generate_uuid(),
        complaint_id=complaint_id,
        officer_id=officer_id,
        message=message,
        image_url=image_url,
    )
    db.add(update)

    # Notify citizen about the progress update
    notif = Notification(
        id=generate_uuid(),
        recipient_id=complaint.citizen_id,
        complaint_id=complaint_id,
        title="Progress update on your complaint",
        message=f"Officer update on '{complaint.title}': {message[:100]}",
        type="STATUS_UPDATE",
        priority="LOW",
        image_url=image_url,
    )
    db.add(notif)

    db.commit()
    db.refresh(update)

    officer_user = db.query(User).filter(User.id == officer_id).first()
    return {
        "id": update.id,
        "complaint_id": update.complaint_id,
        "officer_id": update.officer_id,
        "officer_name": officer_user.full_name if officer_user else None,
        "message": update.message,
        "image_url": update.image_url,
        "created_at": update.created_at,
    }
```

**Step 3: Add `get_complaint_updates` function**

Add after `post_complaint_update`:

```python
def get_complaint_updates(db: Session, complaint_id: str) -> list:
    """Return all progress updates for a complaint, ordered by creation time."""
    updates = (
        db.query(ComplaintUpdate)
        .filter(ComplaintUpdate.complaint_id == complaint_id)
        .order_by(ComplaintUpdate.created_at.asc())
        .all()
    )

    result = []
    for u in updates:
        officer_user = db.query(User).filter(User.id == u.officer_id).first()
        result.append({
            "id": u.id,
            "complaint_id": u.complaint_id,
            "officer_id": u.officer_id,
            "officer_name": officer_user.full_name if officer_user else None,
            "message": u.message,
            "image_url": u.image_url,
            "created_at": u.created_at,
        })
    return result
```

**Step 4: Add `complete_complaint` function (officer marks done)**

Add after `get_complaint_updates`:

```python
def complete_complaint(
    db: Session,
    complaint_id: str,
    officer_id: str,
    notes: str,
    resolution_image: str = None,
) -> Complaint:
    """Officer marks a complaint as COMPLETED with proof. Awaits admin review."""
    complaint = db.query(Complaint).filter(Complaint.id == complaint_id).first()
    if not complaint:
        return None

    if complaint.assigned_officer_id != officer_id:
        return "NOT_ASSIGNED"

    if complaint.status not in ("ASSIGNED", "IN_PROGRESS", "REWORK"):
        return "INVALID_STATUS"

    old_status = complaint.status
    complaint.status = "COMPLETED"
    complaint.resolution_notes = notes
    complaint.resolution_image = resolution_image

    # Log status change
    history = ComplaintStatusHistory(
        id=generate_uuid(),
        complaint_id=complaint_id,
        old_status=old_status,
        new_status="COMPLETED",
        changed_by=officer_id,
        notes=notes,
    )
    db.add(history)

    # Notify all admins for review
    admins = db.query(User).filter(User.role == "admin").all()
    for admin in admins:
        notif = Notification(
            id=generate_uuid(),
            recipient_id=admin.id,
            complaint_id=complaint_id,
            title="Complaint ready for review",
            message=f"Officer has marked complaint '{complaint.title}' as completed. Please review.",
            type="STATUS_UPDATE",
            priority="HIGH",
            image_url=resolution_image,
        )
        db.add(notif)

    # Notify citizen
    citizen_notif = Notification(
        id=generate_uuid(),
        recipient_id=complaint.citizen_id,
        complaint_id=complaint_id,
        title="Work completed on your complaint",
        message=f"The officer has completed work on '{complaint.title}'. Awaiting admin review.",
        type="STATUS_UPDATE",
        priority="MEDIUM",
        image_url=resolution_image,
    )
    db.add(citizen_notif)

    db.commit()
    db.refresh(complaint)
    return complaint
```

**Step 5: Add `approve_complaint` function (admin approves)**

Add after `complete_complaint`:

```python
def approve_complaint(
    db: Session,
    complaint_id: str,
    admin_id: str,
) -> Complaint:
    """Admin approves a completed complaint, marking it RESOLVED."""
    complaint = db.query(Complaint).filter(Complaint.id == complaint_id).first()
    if not complaint:
        return None

    if complaint.status != "COMPLETED":
        return "INVALID_STATUS"

    old_status = complaint.status
    complaint.status = "RESOLVED"
    complaint.resolved_at = datetime.utcnow()

    # Decrement officer workload
    if complaint.assigned_officer_id:
        officer = (
            db.query(Officer)
            .filter(Officer.user_id == complaint.assigned_officer_id)
            .first()
        )
        if officer and officer.workload_count and officer.workload_count > 0:
            officer.workload_count -= 1

    # Log status change
    history = ComplaintStatusHistory(
        id=generate_uuid(),
        complaint_id=complaint_id,
        old_status=old_status,
        new_status="RESOLVED",
        changed_by=admin_id,
        notes="Approved by admin",
    )
    db.add(history)

    # Notify citizen
    citizen_notif = Notification(
        id=generate_uuid(),
        recipient_id=complaint.citizen_id,
        complaint_id=complaint_id,
        title="Your complaint has been resolved",
        message=f"Your complaint '{complaint.title}' has been approved and resolved.",
        type="RESOLUTION",
        priority="HIGH",
        image_url=complaint.resolution_image,
    )
    db.add(citizen_notif)

    # Notify officer
    if complaint.assigned_officer_id:
        officer_notif = Notification(
            id=generate_uuid(),
            recipient_id=complaint.assigned_officer_id,
            complaint_id=complaint_id,
            title="Your work has been approved",
            message=f"Admin has approved your work on '{complaint.title}'.",
            type="RESOLUTION",
            priority="MEDIUM",
        )
        db.add(officer_notif)

    db.commit()
    db.refresh(complaint)
    return complaint
```

**Step 6: Add `rework_complaint` function (admin declines)**

Add after `approve_complaint`:

```python
def rework_complaint(
    db: Session,
    complaint_id: str,
    admin_id: str,
    reason: str,
) -> Complaint:
    """Admin requests rework on a completed complaint."""
    complaint = db.query(Complaint).filter(Complaint.id == complaint_id).first()
    if not complaint:
        return None

    if complaint.status != "COMPLETED":
        return "INVALID_STATUS"

    old_status = complaint.status
    complaint.status = "REWORK"
    complaint.resolution_notes = None
    complaint.resolution_image = None

    # Log status change
    history = ComplaintStatusHistory(
        id=generate_uuid(),
        complaint_id=complaint_id,
        old_status=old_status,
        new_status="REWORK",
        changed_by=admin_id,
        notes=f"Rework requested: {reason}",
    )
    db.add(history)

    # Notify officer
    if complaint.assigned_officer_id:
        officer_notif = Notification(
            id=generate_uuid(),
            recipient_id=complaint.assigned_officer_id,
            complaint_id=complaint_id,
            title="Rework requested on your task",
            message=f"Admin has requested rework on '{complaint.title}': {reason}",
            type="STATUS_UPDATE",
            priority="HIGH",
        )
        db.add(officer_notif)

    # Notify citizen
    citizen_notif = Notification(
        id=generate_uuid(),
        recipient_id=complaint.citizen_id,
        complaint_id=complaint_id,
        title="Complaint requires additional work",
        message=f"Additional work has been requested on your complaint '{complaint.title}'.",
        type="STATUS_UPDATE",
        priority="MEDIUM",
    )
    db.add(citizen_notif)

    db.commit()
    db.refresh(complaint)
    return complaint
```

**Step 7: Modify `_complaint_to_response` to include updates**

In `backend/services/complaint_service.py`, modify `_complaint_to_response` function (lines 37-87). Add after `image_urls` (line 58), before the return dict:

```python
    # Progress updates
    updates = (
        db.query(ComplaintUpdate)
        .filter(ComplaintUpdate.complaint_id == complaint.id)
        .order_by(ComplaintUpdate.created_at.asc())
        .all()
    )
    update_list = []
    for u in updates:
        officer_user = db.query(User).filter(User.id == u.officer_id).first()
        update_list.append({
            "id": u.id,
            "complaint_id": u.complaint_id,
            "officer_id": u.officer_id,
            "officer_name": officer_user.full_name if officer_user else None,
            "message": u.message,
            "image_url": u.image_url,
            "created_at": u.created_at,
        })
```

And add `"updates": update_list,` to the return dict (after `"images": image_urls,` on line 84).

**Step 8: Modify `get_complaints` officer filter**

In `backend/services/complaint_service.py`, modify the officer filter (lines 195-202). Change from showing unassigned to only showing assigned:

```python
    elif user.role == "officer":
        # Officers see only complaints assigned to them
        query = query.filter(Complaint.assigned_officer_id == user.id)
```

**Step 9: Update `get_stats` to include REWORK count**

In `backend/services/complaint_service.py`, add after `completed = _count_status("COMPLETED")` (line 533):

```python
    rework = _count_status("REWORK")
```

And add `"rework": rework,` to the return dict (after `"completed": completed,`).

---

## Task 4: Backend Router — New API endpoints

**Files:**
- Modify: `backend/routers/complaints.py` (add officer endpoints, admin review endpoints)
- Modify: `backend/routers/admin.py` (add officer management endpoints)

**Step 1: Update imports in `backend/routers/complaints.py`**

Add to the imports from `schemas.complaint` (lines 6-16):

```python
from schemas.complaint import (
    ComplaintCreate,
    ComplaintResponse,
    StatusUpdateRequest,
    AssignOfficerRequest,
    ResolveRequest,
    PaginatedResponse,
    ComplaintStats,
    StatusHistoryResponse,
    MapDataResponse,
    ComplaintUpdateCreate,
    ComplaintUpdateResponse,
    CompleteComplaintRequest,
    ReworkRequest,
)
```

Add to the imports from `services.complaint_service` (lines 17-28):

```python
from services.complaint_service import (
    create_complaint,
    get_complaints,
    get_complaint_by_id,
    update_status,
    assign_officer,
    resolve_complaint,
    get_status_history,
    get_similar_complaints,
    get_map_data,
    get_stats,
    post_complaint_update,
    get_complaint_updates,
    complete_complaint,
    approve_complaint,
    rework_complaint,
)
```

Add `require_officer` to middleware import (line 4):

```python
from middleware.auth import get_current_user, require_admin, require_officer
```

**Step 2: Add officer progress update endpoints**

Add after the `similar_complaints` endpoint (after line 277):

```python
# ---------------------------------------------------------------------------
# 11. POST /{id}/updates -- Officer posts a progress update
# ---------------------------------------------------------------------------
@router.post("/{complaint_id}/updates", response_model=ComplaintUpdateResponse)
async def create_complaint_update(
    complaint_id: str,
    image: UploadFile = File(default=None),
    message: str = Form(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_officer),
):
    """Officer posts a progress update with optional image."""
    image_url = None
    if image:
        content = await image.read()
        max_bytes = settings.MAX_FILE_SIZE_MB * 1024 * 1024
        if len(content) > max_bytes:
            raise HTTPException(status_code=400, detail=f"File exceeds {settings.MAX_FILE_SIZE_MB}MB limit")
        ext = os.path.splitext(image.filename or "file.jpg")[1] or ".jpg"
        filename = f"{uuid.uuid4()}{ext}"
        upload_dir = settings.UPLOAD_DIR
        os.makedirs(upload_dir, exist_ok=True)
        filepath = os.path.join(upload_dir, filename)
        with open(filepath, "wb") as f:
            f.write(content)
        image_url = f"/uploads/{filename}"

    result = post_complaint_update(
        db=db,
        complaint_id=complaint_id,
        officer_id=current_user.id,
        message=message,
        image_url=image_url,
    )
    if result is None:
        raise HTTPException(status_code=404, detail="Complaint not found")
    if result == "NOT_ASSIGNED":
        raise HTTPException(status_code=403, detail="You are not assigned to this complaint")
    return result


# ---------------------------------------------------------------------------
# 12. GET /{id}/updates -- Get all progress updates for a complaint
# ---------------------------------------------------------------------------
@router.get("/{complaint_id}/updates", response_model=List[ComplaintUpdateResponse])
def list_complaint_updates(
    complaint_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get all progress updates for a complaint. Visible to all roles."""
    complaint = get_complaint_by_id(db, complaint_id)
    if not complaint:
        raise HTTPException(status_code=404, detail="Complaint not found")
    return get_complaint_updates(db, complaint_id)


# ---------------------------------------------------------------------------
# 13. PUT /{id}/complete -- Officer marks complaint as completed
# ---------------------------------------------------------------------------
@router.put("/{complaint_id}/complete", response_model=ComplaintResponse)
async def officer_complete_complaint(
    complaint_id: str,
    image: UploadFile = File(default=None),
    notes: str = Form(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_officer),
):
    """Officer marks complaint as completed with notes and optional proof image."""
    resolution_image = None
    if image:
        content = await image.read()
        max_bytes = settings.MAX_FILE_SIZE_MB * 1024 * 1024
        if len(content) > max_bytes:
            raise HTTPException(status_code=400, detail=f"File exceeds {settings.MAX_FILE_SIZE_MB}MB limit")
        ext = os.path.splitext(image.filename or "file.jpg")[1] or ".jpg"
        filename = f"{uuid.uuid4()}{ext}"
        upload_dir = settings.UPLOAD_DIR
        os.makedirs(upload_dir, exist_ok=True)
        filepath = os.path.join(upload_dir, filename)
        with open(filepath, "wb") as f:
            f.write(content)
        resolution_image = f"/uploads/{filename}"

    result = complete_complaint(
        db=db,
        complaint_id=complaint_id,
        officer_id=current_user.id,
        notes=notes,
        resolution_image=resolution_image,
    )
    if result is None:
        raise HTTPException(status_code=404, detail="Complaint not found")
    if result == "NOT_ASSIGNED":
        raise HTTPException(status_code=403, detail="You are not assigned to this complaint")
    if result == "INVALID_STATUS":
        raise HTTPException(status_code=400, detail="Complaint cannot be completed from its current status")
    return get_complaint_by_id(db, result.id)


# ---------------------------------------------------------------------------
# 14. PUT /{id}/approve -- Admin approves completed complaint
# ---------------------------------------------------------------------------
@router.put("/{complaint_id}/approve", response_model=ComplaintResponse)
def admin_approve_complaint(
    complaint_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    """Admin approves a completed complaint, marking it RESOLVED."""
    result = approve_complaint(db=db, complaint_id=complaint_id, admin_id=current_user.id)
    if result is None:
        raise HTTPException(status_code=404, detail="Complaint not found")
    if result == "INVALID_STATUS":
        raise HTTPException(status_code=400, detail="Only COMPLETED complaints can be approved")
    return get_complaint_by_id(db, result.id)


# ---------------------------------------------------------------------------
# 15. PUT /{id}/rework -- Admin requests rework
# ---------------------------------------------------------------------------
@router.put("/{complaint_id}/rework", response_model=ComplaintResponse)
def admin_rework_complaint(
    complaint_id: str,
    body: ReworkRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    """Admin requests rework on a completed complaint."""
    result = rework_complaint(
        db=db,
        complaint_id=complaint_id,
        admin_id=current_user.id,
        reason=body.reason,
    )
    if result is None:
        raise HTTPException(status_code=404, detail="Complaint not found")
    if result == "INVALID_STATUS":
        raise HTTPException(status_code=400, detail="Only COMPLETED complaints can be sent for rework")
    return get_complaint_by_id(db, result.id)
```

**Step 3: Update valid statuses in `change_status` endpoint**

Modify `backend/routers/complaints.py` line 173. Change:
```python
    valid_statuses = {"UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED"}
```
To:
```python
    valid_statuses = {"UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED", "REWORK"}
```

**Step 4: Allow officers to update status to IN_PROGRESS**

Modify `backend/routers/complaints.py` the `change_status` endpoint (line 165-190). Change `require_admin` to `require_officer` and add validation:

```python
@router.put("/{complaint_id}/status", response_model=ComplaintResponse)
def change_status(
    complaint_id: str,
    body: StatusUpdateRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_officer),
):
    """Change the status of a complaint. Admin or assigned officer."""
    valid_statuses = {"UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED", "REWORK"}
    if body.status not in valid_statuses:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid status. Must be one of: {', '.join(sorted(valid_statuses))}",
        )

    # Officers can only change status of their assigned complaints
    if current_user.role == "officer":
        from services.complaint_service import get_complaint_by_id as _get
        complaint_data = _get(db, complaint_id)
        if not complaint_data or complaint_data.get("assigned_officer_id") != current_user.id:
            raise HTTPException(status_code=403, detail="You are not assigned to this complaint")
        if body.status not in ("IN_PROGRESS",):
            raise HTTPException(status_code=403, detail="Officers can only set status to IN_PROGRESS")

    complaint = update_status(
        db=db,
        complaint_id=complaint_id,
        new_status=body.status,
        changed_by_id=current_user.id,
        notes=body.notes,
    )
    if not complaint:
        raise HTTPException(status_code=404, detail="Complaint not found")

    return get_complaint_by_id(db, complaint.id)
```

**Step 5: Add officer management endpoints to `backend/routers/admin.py`**

Add imports at top of `backend/routers/admin.py` (after line 22):

```python
from schemas.admin import (
    CategoryCreate,
    CategoryResponse,
    DepartmentCreate,
    DepartmentResponse,
    OfficerCreate,
    OfficerResponse,
    SystemLogResponse,
    DashboardStats,
    OfficerStats,
    OfficerUpdate,
)
```

Add after the `create_officer` endpoint (after line 383):

```python
# ---------------------------------------------------------------------------
# Update officer details
# ---------------------------------------------------------------------------
@router.put("/officers/{officer_id}", response_model=OfficerResponse)
def update_officer(
    officer_id: str,
    payload: OfficerUpdate,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """Update an officer's department, designation, or availability."""
    officer = db.query(Officer).filter(Officer.id == officer_id).first()
    if not officer:
        raise HTTPException(status_code=404, detail="Officer not found")

    user = db.query(User).filter(User.id == officer.user_id).first()

    if payload.department is not None:
        officer.department = payload.department
    if payload.designation is not None:
        officer.designation = payload.designation
    if payload.is_available is not None:
        officer.is_available = payload.is_available

    db.commit()
    db.refresh(officer)

    create_system_log(
        db,
        action="UPDATE_OFFICER",
        entity_type="officer",
        entity_id=officer.id,
        performed_by=admin.id,
        details={"full_name": user.full_name if user else "Unknown"},
    )

    return OfficerResponse(
        id=officer.id,
        user_id=officer.user_id,
        full_name=user.full_name if user else "",
        email=user.email if user else "",
        department=officer.department,
        designation=officer.designation,
        workload_count=officer.workload_count,
        is_available=officer.is_available,
    )


# ---------------------------------------------------------------------------
# Toggle officer availability
# ---------------------------------------------------------------------------
@router.put("/officers/{officer_id}/availability", response_model=OfficerResponse)
def toggle_officer_availability(
    officer_id: str,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """Toggle an officer's availability status."""
    officer = db.query(Officer).filter(Officer.id == officer_id).first()
    if not officer:
        raise HTTPException(status_code=404, detail="Officer not found")

    user = db.query(User).filter(User.id == officer.user_id).first()
    officer.is_available = not officer.is_available
    db.commit()
    db.refresh(officer)

    return OfficerResponse(
        id=officer.id,
        user_id=officer.user_id,
        full_name=user.full_name if user else "",
        email=user.email if user else "",
        department=officer.department,
        designation=officer.designation,
        workload_count=officer.workload_count,
        is_available=officer.is_available,
    )


# ---------------------------------------------------------------------------
# Officer performance stats
# ---------------------------------------------------------------------------
@router.get("/officers/{officer_id}/stats", response_model=OfficerStats)
def get_officer_stats(
    officer_id: str,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """Get detailed performance stats for a specific officer."""
    officer = db.query(Officer).filter(Officer.id == officer_id).first()
    if not officer:
        raise HTTPException(status_code=404, detail="Officer not found")

    user = db.query(User).filter(User.id == officer.user_id).first()

    # Count complaints by status for this officer
    total_assigned = (
        db.query(func.count(Complaint.id))
        .filter(Complaint.assigned_officer_id == officer.user_id)
        .scalar() or 0
    )
    total_completed = (
        db.query(func.count(Complaint.id))
        .filter(
            Complaint.assigned_officer_id == officer.user_id,
            Complaint.status == "RESOLVED",
        )
        .scalar() or 0
    )

    # Count rework instances from status history
    from models.complaint_status_history import ComplaintStatusHistory
    total_rework = (
        db.query(func.count(ComplaintStatusHistory.id))
        .join(Complaint, ComplaintStatusHistory.complaint_id == Complaint.id)
        .filter(
            Complaint.assigned_officer_id == officer.user_id,
            ComplaintStatusHistory.new_status == "REWORK",
        )
        .scalar() or 0
    )

    # Average resolution time (hours) for resolved complaints
    from sqlalchemy import extract
    resolved_complaints = (
        db.query(Complaint)
        .filter(
            Complaint.assigned_officer_id == officer.user_id,
            Complaint.status == "RESOLVED",
            Complaint.resolved_at.isnot(None),
        )
        .all()
    )
    avg_hours = None
    if resolved_complaints:
        total_hours = sum(
            (c.resolved_at - c.created_at).total_seconds() / 3600
            for c in resolved_complaints
            if c.resolved_at and c.created_at
        )
        avg_hours = round(total_hours / len(resolved_complaints), 1)

    return OfficerStats(
        officer_id=officer.id,
        user_id=officer.user_id,
        full_name=user.full_name if user else "",
        email=user.email if user else "",
        department=officer.department,
        designation=officer.designation,
        workload_count=officer.workload_count,
        is_available=officer.is_available,
        total_assigned=total_assigned,
        total_completed=total_completed,
        total_rework=total_rework,
        avg_resolution_hours=avg_hours,
    )


# ---------------------------------------------------------------------------
# Officer's complaints list
# ---------------------------------------------------------------------------
@router.get("/officers/{officer_id}/complaints")
def get_officer_complaints(
    officer_id: str,
    status: str = None,
    page: int = 1,
    limit: int = 20,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """Get all complaints assigned to a specific officer."""
    officer = db.query(Officer).filter(Officer.id == officer_id).first()
    if not officer:
        raise HTTPException(status_code=404, detail="Officer not found")

    query = db.query(Complaint).filter(Complaint.assigned_officer_id == officer.user_id)
    if status:
        query = query.filter(Complaint.status == status)

    total = query.count()
    query = query.order_by(Complaint.created_at.desc())
    offset = (page - 1) * limit
    complaints = query.offset(offset).limit(limit).all()
    pages = math.ceil(total / limit) if limit else 1

    from services.complaint_service import _complaint_to_response
    items = [_complaint_to_response(db, c) for c in complaints]

    return {"items": items, "total": total, "page": page, "limit": limit, "pages": pages}
```

**Step 6: Update dashboard stats to include REWORK count**

Modify `backend/routers/admin.py` `get_dashboard_stats` function. Add after `completed` count (after line 478):

```python
    rework = (
        db.query(func.count(Complaint.id))
        .filter(Complaint.status == "REWORK")
        .scalar() or 0
    )
```

And add `rework=rework,` to the `DashboardStats` return (after `completed=completed,`).

---

## Task 5: Backend Verification — Test all new endpoints

**Step 1: Start the backend server**

```bash
cd backend && python main.py
```

**Step 2: Test the new endpoints with curl/httpie**

Test officer progress update:
```bash
curl -X POST http://localhost:8000/api/complaints/{id}/updates \
  -H "Authorization: Bearer {officer_token}" \
  -F "message=Site visited, assessing damage"
```

Test officer complete:
```bash
curl -X PUT http://localhost:8000/api/complaints/{id}/complete \
  -H "Authorization: Bearer {officer_token}" \
  -F "notes=Work completed, pothole filled"
```

Test admin approve:
```bash
curl -X PUT http://localhost:8000/api/complaints/{id}/approve \
  -H "Authorization: Bearer {admin_token}"
```

Test admin rework:
```bash
curl -X PUT http://localhost:8000/api/complaints/{id}/rework \
  -H "Authorization: Bearer {admin_token}" \
  -H "Content-Type: application/json" \
  -d '{"reason": "Work incomplete, needs more filling"}'
```

---

## Task 6: Frontend Models — Update Kotlin data classes

**Files:**
- Modify: `app/src/main/java/com/simats/civicissue/ComplaintModels.kt`

**Step 1: Add REWORK to status helpers**

Modify `ComplaintModels.kt` `statusLabel` extension (lines 43-50). Add before `else`:
```kotlin
    "REWORK" -> "Rework Required"
```

Modify `ComplaintStatus` enum (lines 66-72). Add:
```kotlin
    REWORK("Rework Required")
```

**Step 2: Add `updates` field to `Complaint` data class**

Modify `ComplaintModels.kt` `Complaint` data class (lines 7-33). Add after `updatedAt` (line 32):
```kotlin
    val updates: List<ComplaintUpdate> = emptyList()
```

**Step 3: Add new data classes**

Add after `IssueGroupItem` (after line 244):

```kotlin
// ===== Complaint Updates (Officer Progress) =====
data class ComplaintUpdate(
    val id: String = "",
    @SerializedName("complaint_id") val complaintId: String = "",
    @SerializedName("officer_id") val officerId: String = "",
    @SerializedName("officer_name") val officerName: String? = null,
    val message: String = "",
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("created_at") val createdAt: String? = null
)

// Request models for officer actions
data class CompleteComplaintRequest(val notes: String, val resolution_image: String? = null)
data class ReworkRequest(val reason: String)

// Officer stats
data class OfficerStats(
    @SerializedName("officer_id") val officerId: String = "",
    @SerializedName("user_id") val userId: String = "",
    @SerializedName("full_name") val fullName: String = "",
    val email: String = "",
    val department: String? = null,
    val designation: String? = null,
    @SerializedName("workload_count") val workloadCount: Int = 0,
    @SerializedName("is_available") val isAvailable: Boolean = true,
    @SerializedName("total_assigned") val totalAssigned: Int = 0,
    @SerializedName("total_completed") val totalCompleted: Int = 0,
    @SerializedName("total_rework") val totalRework: Int = 0,
    @SerializedName("avg_resolution_hours") val avgResolutionHours: Float? = null
)

data class OfficerUpdate(
    val department: String? = null,
    val designation: String? = null,
    val is_available: Boolean? = null
)
```

**Step 4: Add REWORK to `statusColor` helper**

Add a `statusColor` extension after `priorityColor` (after line 41):

```kotlin
val Complaint.statusColor: Color get() = when (status) {
    "UNASSIGNED" -> Color(0xFF9E9E9E)
    "ASSIGNED" -> Color(0xFF1976D2)
    "IN_PROGRESS" -> Color(0xFFF9A825)
    "COMPLETED" -> Color(0xFF388E3C)
    "RESOLVED" -> Color(0xFF2E7D32)
    "REWORK" -> Color(0xFFD32F2F)
    else -> Color(0xFF9E9E9E)
}
```

**Step 5: Add `rework` field to `DashboardStats`**

Modify `DashboardStats` data class (lines 184-197). Add after `completed`:
```kotlin
    val rework: Int = 0,
```

---

## Task 7: Frontend API Service — Add new Retrofit endpoints

**Files:**
- Modify: `app/src/main/java/com/simats/civicissue/CivicApiService.kt`

**Step 1: Add new endpoints to `CivicApiService.kt`**

Add after the complaints section (after line 29):

```kotlin
    // Officer workflow
    @Multipart
    @POST("api/complaints/{id}/updates") suspend fun postComplaintUpdate(
        @Path("id") id: String,
        @Part image: MultipartBody.Part? = null,
        @Part("message") message: RequestBody
    ): ComplaintUpdate

    @GET("api/complaints/{id}/updates") suspend fun getComplaintUpdates(
        @Path("id") id: String
    ): List<ComplaintUpdate>

    @Multipart
    @PUT("api/complaints/{id}/complete") suspend fun completeComplaint(
        @Path("id") id: String,
        @Part image: MultipartBody.Part? = null,
        @Part("notes") notes: RequestBody
    ): Complaint

    @PUT("api/complaints/{id}/approve") suspend fun approveComplaint(
        @Path("id") id: String
    ): Complaint

    @PUT("api/complaints/{id}/rework") suspend fun reworkComplaint(
        @Path("id") id: String,
        @Body body: ReworkRequest
    ): Complaint
```

Add to admin section (after line 62):

```kotlin
    @PUT("api/admin/officers/{id}") suspend fun updateOfficer(
        @Path("id") id: String,
        @Body body: OfficerUpdate
    ): Officer

    @PUT("api/admin/officers/{id}/availability") suspend fun toggleOfficerAvailability(
        @Path("id") id: String
    ): Officer

    @GET("api/admin/officers/{id}/stats") suspend fun getOfficerStats(
        @Path("id") id: String
    ): OfficerStats

    @GET("api/admin/officers/{id}/complaints") suspend fun getOfficerComplaints(
        @Path("id") id: String,
        @QueryMap params: Map<String, String> = emptyMap()
    ): PaginatedResponse<Complaint>
```

---

## Task 8: Frontend — Officer Dashboard Screen

**Files:**
- Create: `app/src/main/java/com/simats/civicissue/OfficerDashboardScreen.kt`

**Step 1: Create OfficerDashboardScreen**

Create `app/src/main/java/com/simats/civicissue/OfficerDashboardScreen.kt`:

This screen should follow the same pattern as `CitizenDashboardScreen.kt` and `AdminDashboardScreen.kt`. It should include:

- Top app bar with "Officer Dashboard" title and notification bell icon with badge
- Stats cards row: Active Tasks, Completed, Rework Count, Total Assigned
- Task list showing assigned complaints with status badges (ASSIGNED, IN_PROGRESS, REWORK)
- Each task card shows: complaint title, category, priority, status, location
- Tap on task navigates to `officer_complaint_detail/{complaintId}`
- Bottom nav: Home, Profile, Notifications
- Logout in drawer/profile

Key implementation details:
- Fetch complaints via `api.getComplaints()` (backend already filters by officer role)
- Fetch unread notification count via `api.getUnreadCount()`
- Sort by: REWORK first (needs immediate attention), then by priority
- Use same color scheme and design language as existing screens

---

## Task 9: Frontend — Officer Complaint Detail Screen

**Files:**
- Create: `app/src/main/java/com/simats/civicissue/OfficerComplaintDetailScreen.kt`

**Step 1: Create OfficerComplaintDetailScreen**

Create `app/src/main/java/com/simats/civicissue/OfficerComplaintDetailScreen.kt`:

This screen shows full complaint details with officer action buttons. It should include:

- Complaint info section: title, description, category, priority, severity, location, images
- Progress updates timeline: list of all updates with officer name, message, optional image, timestamp
- Action buttons based on current status:
  - **ASSIGNED**: "Start Work" button → changes status to IN_PROGRESS
  - **IN_PROGRESS**: "Post Update" button + "Mark Complete" button
  - **REWORK**: "Resume Work" button → changes to IN_PROGRESS, shows admin's rework reason prominently
- Status history timeline (reuse pattern from `ComplaintDetailScreen.kt`)

Key implementation details:
- Fetch complaint via `api.getComplaint(complaintId)`
- Fetch updates via `api.getComplaintUpdates(complaintId)`
- Fetch history via `api.getComplaintHistory(complaintId)`
- "Start Work" calls `api.updateComplaintStatus(id, StatusUpdateRequest("IN_PROGRESS"))`
- "Post Update" navigates to `officer_post_update/{complaintId}`
- "Mark Complete" navigates to `officer_complete/{complaintId}`

---

## Task 10: Frontend — Officer Post Update Screen

**Files:**
- Create: `app/src/main/java/com/simats/civicissue/OfficerPostUpdateScreen.kt`

**Step 1: Create OfficerPostUpdateScreen**

Create `app/src/main/java/com/simats/civicissue/OfficerPostUpdateScreen.kt`:

Simple form screen for posting progress updates:

- Text field for update message (required, multiline)
- Optional image capture (camera or gallery picker, same pattern as `ReportIssueScreen.kt`)
- "Submit Update" button
- Loading state while submitting
- Success → navigate back to complaint detail

Key implementation details:
- Submit via multipart: `api.postComplaintUpdate(complaintId, imagePart, messagePart)`
- Reuse image capture logic from `ReportIssueScreen.kt`
- Show toast/snackbar on success

---

## Task 11: Frontend — Officer Complete Complaint Screen

**Files:**
- Create: `app/src/main/java/com/simats/civicissue/OfficerCompleteScreen.kt`

**Step 1: Create OfficerCompleteScreen**

Create `app/src/main/java/com/simats/civicissue/OfficerCompleteScreen.kt`:

Form screen for marking complaint as completed:

- Text field for completion notes (required, multiline)
- Image capture for proof photo (camera or gallery, strongly encouraged)
- Preview of captured image
- "Submit for Review" button
- Note text: "Your completion will be reviewed by an admin before being marked as resolved."
- Loading state while submitting
- Success → navigate back to dashboard

Key implementation details:
- Submit via multipart: `api.completeComplaint(complaintId, imagePart, notesPart)`
- Reuse image capture logic from `AdminResolveIssueScreen.kt`

---

## Task 12: Frontend — Officer Notification Screen

**Files:**
- Create: `app/src/main/java/com/simats/civicissue/OfficerNotificationScreen.kt`

**Step 1: Create OfficerNotificationScreen**

Follow exact same pattern as `CitizenNotificationScreen.kt`:

- List of notifications sorted by most recent
- Read/unread visual distinction
- Tap to mark as read
- "Mark All Read" button in top bar
- Notification types: ASSIGNMENT (new task), STATUS_UPDATE (rework request), RESOLUTION (work approved)

---

## Task 13: Frontend — Officer Profile Screen

**Files:**
- Create: `app/src/main/java/com/simats/civicissue/OfficerProfileScreen.kt`

**Step 1: Create OfficerProfileScreen**

Follow same pattern as `CitizenProfileScreen.kt`:

- Display name, email, department, designation
- Avatar with upload capability
- Edit profile button → reuse `EditProfileScreen`
- Change password button → reuse `ChangePasswordScreen`
- Logout button

---

## Task 14: Frontend — Navigation Routes for Officer

**Files:**
- Modify: `app/src/main/java/com/simats/civicissue/MainActivity.kt`

**Step 1: Add officer routing to login success**

Modify `MainActivity.kt` `onLoginSuccess` callback (lines 48-57). Change:

```kotlin
onLoginSuccess = { userRole ->
    if (userRole == "admin") {
        navController.navigate("admin_dashboard") {
            popUpTo(0) { inclusive = true }
        }
    } else {
        navController.navigate("citizen_dashboard") {
            popUpTo(0) { inclusive = true }
        }
    }
}
```

To:

```kotlin
onLoginSuccess = { userRole ->
    when (userRole) {
        "admin" -> navController.navigate("admin_dashboard") {
            popUpTo(0) { inclusive = true }
        }
        "officer" -> navController.navigate("officer_dashboard") {
            popUpTo(0) { inclusive = true }
        }
        else -> navController.navigate("citizen_dashboard") {
            popUpTo(0) { inclusive = true }
        }
    }
}
```

**Step 2: Add officer composable routes**

Add after `admin_resolve_issue` route (after line 270):

```kotlin
// ===== OFFICER ROUTES =====
composable("officer_dashboard") {
    OfficerDashboardScreen(
        onNotificationsClick = { navController.navigate("officer_notifications") },
        onProfileClick = { navController.navigate("officer_profile") },
        onComplaintClick = { complaintId ->
            navController.navigate("officer_complaint_detail/$complaintId")
        },
        onLogoutClick = { navController.navigate("logout") }
    )
}

composable("officer_complaint_detail/{complaintId}") { backStackEntry ->
    val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
    OfficerComplaintDetailScreen(
        complaintId = complaintId,
        onBack = { navController.popBackStack() },
        onPostUpdate = { navController.navigate("officer_post_update/$complaintId") },
        onComplete = { navController.navigate("officer_complete/$complaintId") }
    )
}

composable("officer_post_update/{complaintId}") { backStackEntry ->
    val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
    OfficerPostUpdateScreen(
        complaintId = complaintId,
        onBack = { navController.popBackStack() },
        onSuccess = { navController.popBackStack() }
    )
}

composable("officer_complete/{complaintId}") { backStackEntry ->
    val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
    OfficerCompleteScreen(
        complaintId = complaintId,
        onBack = { navController.popBackStack() },
        onSuccess = {
            navController.navigate("officer_dashboard") {
                popUpTo("officer_dashboard") { inclusive = true }
            }
        }
    )
}

composable("officer_notifications") {
    OfficerNotificationScreen(
        onBack = { navController.popBackStack() }
    )
}

composable("officer_profile") {
    OfficerProfileScreen(
        onBack = { navController.popBackStack() },
        onEditProfile = { navController.navigate("edit_profile") },
        onChangePassword = { navController.navigate("officer_change_password") },
        onLogoutClick = {
            TokenManager.clear()
            navController.navigate("role_selection") {
                popUpTo(0) { inclusive = true }
            }
        }
    )
}

composable("officer_change_password") {
    ChangePasswordScreen(
        onBack = { navController.popBackStack() },
        onUpdatePassword = {
            navController.navigate("password_updated/Officer") {
                popUpTo("officer_profile") { inclusive = true }
            }
        }
    )
}
```

---

## Task 15: Frontend — Admin Review Screen (Approve/Rework)

**Files:**
- Create: `app/src/main/java/com/simats/civicissue/AdminReviewScreen.kt`
- Modify: `app/src/main/java/com/simats/civicissue/MainActivity.kt`

**Step 1: Create AdminReviewScreen**

Create `app/src/main/java/com/simats/civicissue/AdminReviewScreen.kt`:

This screen is shown when admin clicks on a COMPLETED complaint to review it:

- Full complaint details (title, description, category, images)
- Officer's progress updates timeline
- Resolution notes and proof image from officer
- Status history
- Two action buttons:
  - **"Approve"** (green) → calls `api.approveComplaint(complaintId)` → RESOLVED
  - **"Request Rework"** (red) → shows dialog with reason text field → calls `api.reworkComplaint(complaintId, ReworkRequest(reason))`
- On success → navigate back to dashboard

**Step 2: Add navigation route**

Add to `MainActivity.kt` after officer routes:

```kotlin
composable("admin_review/{complaintId}") { backStackEntry ->
    val complaintId = backStackEntry.arguments?.getString("complaintId") ?: ""
    AdminReviewScreen(
        complaintId = complaintId,
        onBack = { navController.popBackStack() },
        onReviewComplete = {
            navController.navigate("admin_dashboard") {
                popUpTo("admin_dashboard") { inclusive = true }
            }
        }
    )
}
```

**Step 3: Update ComplaintDetailScreen to show review button for COMPLETED complaints**

In `ComplaintDetailScreen.kt`, when the user is admin and status is "COMPLETED", show a "Review" button that navigates to `admin_review/{complaintId}`.

---

## Task 16: Frontend — Admin Officer Management Screen

**Files:**
- Create: `app/src/main/java/com/simats/civicissue/ManageOfficersScreen.kt`
- Modify: `app/src/main/java/com/simats/civicissue/SettingsScreen.kt` (add navigation)
- Modify: `app/src/main/java/com/simats/civicissue/MainActivity.kt` (add route)

**Step 1: Create ManageOfficersScreen**

Create `app/src/main/java/com/simats/civicissue/ManageOfficersScreen.kt`:

This is the admin's officer management dashboard:

- List of all officers with:
  - Name, department, designation
  - Workload count badge
  - Availability toggle
  - Tap to see officer detail/stats
- Search/filter by department
- FAB (floating action button) to create new officer → navigates to existing officer creation flow
- Each officer card shows: name, dept, workload count, availability status

**Step 2: Create OfficerDetailScreen for admin**

Create `app/src/main/java/com/simats/civicissue/AdminOfficerDetailScreen.kt`:

Shows detailed officer stats:
- Officer info: name, email, department, designation
- Performance stats: total assigned, completed, rework count, avg resolution time
- List of their assigned complaints (paginated)
- Toggle availability button
- Edit department/designation

**Step 3: Add navigation routes**

Add to `MainActivity.kt`:

```kotlin
composable("manage_officers") {
    ManageOfficersScreen(
        onBack = { navController.popBackStack() },
        onOfficerClick = { officerId ->
            navController.navigate("admin_officer_detail/$officerId")
        },
        onCreateOfficer = { navController.navigate("create_officer") }
    )
}

composable("admin_officer_detail/{officerId}") { backStackEntry ->
    val officerId = backStackEntry.arguments?.getString("officerId") ?: ""
    AdminOfficerDetailScreen(
        officerId = officerId,
        onBack = { navController.popBackStack() },
        onComplaintClick = { complaintId ->
            navController.navigate("complaint_detail/$complaintId")
        }
    )
}

composable("create_officer") {
    CreateOfficerScreen(
        onBack = { navController.popBackStack() },
        onSuccess = { navController.popBackStack() }
    )
}
```

**Step 4: Add "Manage Officers" to SettingsScreen**

Modify `SettingsScreen.kt` to add a "Manage Officers" navigation option alongside existing "Manage Categories" and "Manage Departments".

---

## Task 17: Frontend — Update ComplaintDetailScreen for Progress Updates

**Files:**
- Modify: `app/src/main/java/com/simats/civicissue/ComplaintDetailScreen.kt`

**Step 1: Add progress updates timeline**

Modify `ComplaintDetailScreen.kt` to show progress updates from `complaint.updates` list between the complaint details and the status history sections:

- Section header: "Progress Updates" (only shown if updates exist)
- Each update shows: officer name, message, optional image, timestamp
- Timeline connector between updates (vertical line + dots)
- This is visible to citizens, officers, and admins (full transparency)

**Step 2: Handle REWORK status display**

Add REWORK status chip with red color to the status display section.

**Step 3: Add review button for admins**

When `TokenManager.getUserRole() == "admin"` and `complaint.status == "COMPLETED"`, show a prominent "Review Completion" button that navigates to `admin_review/{complaintId}`.

---

## Task 18: Frontend — Create Officer Screen (Admin)

**Files:**
- Create: `app/src/main/java/com/simats/civicissue/CreateOfficerScreen.kt`

**Step 1: Create CreateOfficerScreen**

Form screen for admin to create new officer accounts:

- Full name (required)
- Email (required, validated)
- Password (required, with strength indicator - reuse from SignUpScreen)
- Phone number (optional)
- Department (dropdown from departments list via `api.getDepartments()`)
- Designation (text field)
- "Create Officer" button
- Loading state, success toast, error handling

Key implementation:
- Submit via `api.createOfficer(OfficerCreateRequest(...))`
- On success → pop back to officer management screen

---

## Task 19: Verification — End-to-end testing

**Step 1: Create a test officer account**

Via admin panel or API:
```bash
curl -X POST http://localhost:8000/api/admin/officers \
  -H "Authorization: Bearer {admin_token}" \
  -H "Content-Type: application/json" \
  -d '{"full_name":"Test Officer","email":"officer@test.com","password":"officer123","department":"Roads & Infrastructure","designation":"Field Inspector"}'
```

**Step 2: Test complete flow**

1. Login as citizen → create a complaint
2. Login as admin → assign complaint to officer
3. Verify citizen gets "assigned" notification
4. Verify officer gets "assignment" notification
5. Login as officer → see complaint on dashboard
6. Officer: "Start Work" → status changes to IN_PROGRESS
7. Verify citizen gets "in progress" notification
8. Officer: post progress update with text + image
9. Verify citizen can see the update on complaint detail
10. Officer: "Mark Complete" with notes + proof image
11. Verify admin gets "ready for review" notification
12. Verify citizen gets "work completed" notification
13. Login as admin → review the completed complaint
14. Admin: "Request Rework" with reason
15. Verify officer gets "rework" notification with reason
16. Verify citizen gets "additional work needed" notification
17. Officer: resume work (IN_PROGRESS), post update, mark complete again
18. Admin: "Approve" → complaint becomes RESOLVED
19. Verify citizen gets "resolved" notification
20. Verify officer gets "approved" notification
21. Verify officer workload count decremented
22. Check admin officer management: stats updated correctly

---

## Execution Order Summary

| Task | Description | Dependencies |
|------|-------------|--------------|
| 1 | DB migration + model | None |
| 2 | Backend schemas | None |
| 3 | Backend service functions | Task 1, 2 |
| 4 | Backend router endpoints | Task 3 |
| 5 | Backend verification | Task 4 |
| 6 | Frontend models | None |
| 7 | Frontend API service | Task 6 |
| 8 | Officer dashboard screen | Task 7 |
| 9 | Officer complaint detail screen | Task 7 |
| 10 | Officer post update screen | Task 7 |
| 11 | Officer complete screen | Task 7 |
| 12 | Officer notification screen | Task 7 |
| 13 | Officer profile screen | Task 7 |
| 14 | Navigation routes | Task 8-13 |
| 15 | Admin review screen | Task 7 |
| 16 | Admin officer management | Task 7 |
| 17 | Update complaint detail | Task 7 |
| 18 | Create officer screen | Task 7 |
| 19 | End-to-end verification | All tasks |

**Parallelizable groups:**
- Tasks 1+2+6 (all independent foundation work)
- Tasks 8+9+10+11+12+13+15+16+17+18 (all frontend screens, independent of each other)
