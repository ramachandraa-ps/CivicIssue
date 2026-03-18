from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form
from sqlalchemy.orm import Session
from database import get_db
from middleware.auth import get_current_user, require_admin, require_officer
from models.user import User
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
from config import settings
from typing import List, Optional
import os
import uuid
import json

router = APIRouter()


# ---------------------------------------------------------------------------
# 1. POST / -- Create a new complaint (multipart: images + JSON data field)
# ---------------------------------------------------------------------------
@router.post("/", response_model=ComplaintResponse)
async def create_new_complaint(
    images: List[UploadFile] = File(default=[]),
    data: str = Form(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Create a complaint. The *data* form field should contain a JSON string
    matching ComplaintCreate.  Optionally attach image files via *images*."""
    try:
        data_dict = json.loads(data)
        complaint_data = ComplaintCreate(**data_dict)
    except (json.JSONDecodeError, Exception) as e:
        raise HTTPException(status_code=400, detail=f"Invalid complaint data: {str(e)}")

    # Save uploaded images to disk
    image_urls: list[str] = []
    upload_dir = settings.UPLOAD_DIR
    os.makedirs(upload_dir, exist_ok=True)

    for image in images:
        # Validate file size (read content once)
        content = await image.read()
        max_bytes = settings.MAX_FILE_SIZE_MB * 1024 * 1024
        if len(content) > max_bytes:
            raise HTTPException(
                status_code=400,
                detail=f"File {image.filename} exceeds the {settings.MAX_FILE_SIZE_MB}MB limit",
            )

        ext = os.path.splitext(image.filename or "file.jpg")[1] or ".jpg"
        filename = f"{uuid.uuid4()}{ext}"
        filepath = os.path.join(upload_dir, filename)

        with open(filepath, "wb") as f:
            f.write(content)

        image_urls.append(f"/uploads/{filename}")

    complaint = create_complaint(
        db=db,
        citizen_id=current_user.id,
        data=complaint_data,
        image_urls=image_urls,
    )

    # Return the full response
    result = get_complaint_by_id(db, complaint.id)
    return result


# ---------------------------------------------------------------------------
# 2. GET / -- List complaints with filtering, search, and pagination
# ---------------------------------------------------------------------------
@router.get("/", response_model=PaginatedResponse)
def list_complaints(
    status: Optional[str] = None,
    category: Optional[str] = None,
    priority: Optional[str] = None,
    severity: Optional[str] = None,
    search: Optional[str] = None,
    page: int = 1,
    limit: int = 20,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Return a paginated list of complaints.  Citizens see only their own;
    admins and officers see all (officers may be filtered to assigned)."""
    return get_complaints(
        db=db,
        user=current_user,
        status=status,
        category=category,
        priority=priority,
        severity=severity,
        search=search,
        page=page,
        limit=limit,
    )


# ---------------------------------------------------------------------------
# 3. GET /stats -- Admin dashboard statistics (BEFORE /{id})
# ---------------------------------------------------------------------------
@router.get("/stats", response_model=ComplaintStats)
def complaint_stats(
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    """Return aggregate complaint statistics.  Admin only."""
    return get_stats(db)


# ---------------------------------------------------------------------------
# 4. GET /map-data -- All complaints as lightweight map markers (BEFORE /{id})
# ---------------------------------------------------------------------------
@router.get("/map-data", response_model=List[MapDataResponse])
def map_data(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Return every complaint as a map-marker payload."""
    return get_map_data(db)


# ---------------------------------------------------------------------------
# 5. GET /{id} -- Single complaint detail
# ---------------------------------------------------------------------------
@router.get("/{complaint_id}", response_model=ComplaintResponse)
def get_complaint(
    complaint_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Fetch a single complaint by ID."""
    result = get_complaint_by_id(db, complaint_id)
    if not result:
        raise HTTPException(status_code=404, detail="Complaint not found")
    return result


# ---------------------------------------------------------------------------
# 6. PUT /{id}/status -- Update complaint status (admin only)
# ---------------------------------------------------------------------------
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

    # Officers can only change status of their assigned complaints to IN_PROGRESS
    if current_user.role == "officer":
        complaint_data = get_complaint_by_id(db, complaint_id)
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


# ---------------------------------------------------------------------------
# 7. PUT /{id}/assign -- Assign an officer (admin only)
# ---------------------------------------------------------------------------
@router.put("/{complaint_id}/assign", response_model=ComplaintResponse)
def assign_officer_to_complaint(
    complaint_id: str,
    body: AssignOfficerRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    """Assign an officer to a complaint.  Admin only."""
    complaint = assign_officer(
        db=db,
        complaint_id=complaint_id,
        officer_user_id=body.officer_id,
        assigned_by_id=current_user.id,
    )
    if not complaint:
        raise HTTPException(
            status_code=404,
            detail="Complaint or officer not found",
        )

    return get_complaint_by_id(db, complaint.id)


# ---------------------------------------------------------------------------
# 8. PUT /{id}/resolve -- Resolve a complaint (admin only)
# ---------------------------------------------------------------------------
@router.put("/{complaint_id}/resolve", response_model=ComplaintResponse)
def resolve(
    complaint_id: str,
    body: ResolveRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    """Mark a complaint as resolved with notes.  Admin only."""
    complaint = resolve_complaint(
        db=db,
        complaint_id=complaint_id,
        notes=body.resolution_notes,
        resolved_by_id=current_user.id,
        resolution_image=body.resolution_image,
    )
    if not complaint:
        raise HTTPException(status_code=404, detail="Complaint not found")

    return get_complaint_by_id(db, complaint.id)


# ---------------------------------------------------------------------------
# 9. GET /{id}/history -- Status change timeline
# ---------------------------------------------------------------------------
@router.get("/{complaint_id}/history", response_model=List[StatusHistoryResponse])
def status_history(
    complaint_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Return the full status-change history for a complaint."""
    # Verify complaint exists
    complaint = get_complaint_by_id(db, complaint_id)
    if not complaint:
        raise HTTPException(status_code=404, detail="Complaint not found")

    return get_status_history(db, complaint_id)


# ---------------------------------------------------------------------------
# 10. GET /{id}/similar -- Nearby similar complaints
# ---------------------------------------------------------------------------
@router.get("/{complaint_id}/similar", response_model=List[ComplaintResponse])
def similar_complaints(
    complaint_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Find complaints with the same category within a 2 km radius."""
    # Verify complaint exists
    complaint = get_complaint_by_id(db, complaint_id)
    if not complaint:
        raise HTTPException(status_code=404, detail="Complaint not found")

    return get_similar_complaints(db, complaint_id)


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
