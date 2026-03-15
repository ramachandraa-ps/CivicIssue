from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form
from sqlalchemy.orm import Session
from database import get_db
from middleware.auth import get_current_user, require_admin
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
    current_user: User = Depends(require_admin),
):
    """Change the status of a complaint.  Admin only."""
    valid_statuses = {"UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED"}
    if body.status not in valid_statuses:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid status. Must be one of: {', '.join(sorted(valid_statuses))}",
        )

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
