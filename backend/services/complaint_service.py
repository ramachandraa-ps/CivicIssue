from sqlalchemy.orm import Session
from sqlalchemy import func, or_
from models.complaint import Complaint
from models.complaint_image import ComplaintImage
from models.complaint_status_history import ComplaintStatusHistory
from models.user import User
from models.notification import Notification
from models.officer import Officer
from models.complaint_update import ComplaintUpdate
from utils.helpers import generate_uuid, generate_complaint_number
from datetime import datetime, timedelta
import math
import json as json_lib


# ---------------------------------------------------------------------------
# Haversine helper
# ---------------------------------------------------------------------------

def haversine(lat1, lon1, lat2, lon2):
    """Return the great-circle distance in kilometres between two points."""
    R = 6371  # Earth radius in km
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = (
        math.sin(dlat / 2) ** 2
        + math.cos(math.radians(lat1))
        * math.cos(math.radians(lat2))
        * math.sin(dlon / 2) ** 2
    )
    return 2 * R * math.asin(math.sqrt(a))


# ---------------------------------------------------------------------------
# Internal helper: convert a Complaint ORM object to a response dict
# ---------------------------------------------------------------------------

def _complaint_to_response(db: Session, complaint: Complaint) -> dict:
    """Build a ComplaintResponse-compatible dict from an ORM Complaint."""
    # Citizen name
    citizen = db.query(User).filter(User.id == complaint.citizen_id).first()
    citizen_name = citizen.full_name if citizen else "Unknown"

    # Officer name (if assigned)
    assigned_officer_name = None
    if complaint.assigned_officer_id:
        officer_user = (
            db.query(User).filter(User.id == complaint.assigned_officer_id).first()
        )
        if officer_user:
            assigned_officer_name = officer_user.full_name

    # Image URLs
    images = (
        db.query(ComplaintImage.image_url)
        .filter(ComplaintImage.complaint_id == complaint.id)
        .all()
    )
    image_urls = [img[0] for img in images]

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

    return {
        "id": complaint.id,
        "complaint_number": complaint.complaint_number,
        "citizen_id": complaint.citizen_id,
        "citizen_name": citizen_name,
        "title": complaint.title,
        "description": complaint.description,
        "category": complaint.category,
        "ai_detected_category": complaint.ai_detected_category,
        "ai_text_category": complaint.ai_text_category,
        "location_text": complaint.location_text,
        "latitude": complaint.latitude,
        "longitude": complaint.longitude,
        "priority": complaint.priority,
        "severity_level": complaint.severity_level,
        "status": complaint.status,
        "assigned_officer_id": complaint.assigned_officer_id,
        "assigned_officer_name": assigned_officer_name,
        "group_id": complaint.group_id,
        "ai_confidence": complaint.ai_confidence,
        "ai_keywords": complaint.ai_keywords,
        "resolution_notes": complaint.resolution_notes,
        "resolution_image": complaint.resolution_image,
        "resolved_at": complaint.resolved_at,
        "images": image_urls,
        "updates": update_list,
        "created_at": complaint.created_at,
        "updated_at": complaint.updated_at,
    }


# ---------------------------------------------------------------------------
# Create complaint
# ---------------------------------------------------------------------------

def create_complaint(
    db: Session,
    citizen_id: str,
    data,
    image_urls: list = None,
) -> Complaint:
    """Create a new complaint, associated images, initial status log, and admin notifications."""
    if image_urls is None:
        image_urls = []

    complaint_id = generate_uuid()
    complaint_number = generate_complaint_number()

    complaint = Complaint(
        id=complaint_id,
        complaint_number=complaint_number,
        citizen_id=citizen_id,
        title=data.title,
        description=data.description,
        category=data.category,
        ai_detected_category=data.ai_detected_category,
        ai_text_category=data.ai_text_category,
        location_text=data.location_text,
        latitude=data.latitude,
        longitude=data.longitude,
        priority=data.priority or "MEDIUM",
        severity_level=data.severity_level or "MEDIUM",
        status="UNASSIGNED",
        ai_confidence=data.ai_confidence,
        ai_keywords=data.ai_keywords,
    )
    db.add(complaint)

    # Create image records
    for url in image_urls:
        img = ComplaintImage(
            id=generate_uuid(),
            complaint_id=complaint_id,
            image_url=url,
        )
        db.add(img)

    # Log initial status
    history = ComplaintStatusHistory(
        id=generate_uuid(),
        complaint_id=complaint_id,
        old_status=None,
        new_status="UNASSIGNED",
        changed_by=citizen_id,
        notes="Complaint created",
    )
    db.add(history)

    # Notify all admins
    admins = db.query(User).filter(User.role == "admin").all()
    category_label = data.category or "General"
    for admin in admins:
        notif = Notification(
            id=generate_uuid(),
            recipient_id=admin.id,
            complaint_id=complaint_id,
            title=f"New {category_label} issue reported",
            message=f"A new complaint '{data.title}' has been filed and needs attention.",
            type="NEW_ISSUE",
            priority="MEDIUM",
        )
        db.add(notif)

    # Attempt grouping (non-critical -- silently skip if service not ready)
    try:
        from services.grouping_service import check_and_group
        check_and_group(db, complaint)
    except Exception:
        pass

    db.commit()
    db.refresh(complaint)
    return complaint


# ---------------------------------------------------------------------------
# List / search complaints
# ---------------------------------------------------------------------------

def get_complaints(
    db: Session,
    user,
    status: str = None,
    category: str = None,
    priority: str = None,
    severity: str = None,
    search: str = None,
    page: int = 1,
    limit: int = 20,
) -> dict:
    """Return a paginated, filtered list of complaints appropriate for the user's role."""
    query = db.query(Complaint)

    # Role-based filtering
    if user.role == "citizen":
        query = query.filter(Complaint.citizen_id == user.id)
    elif user.role == "officer":
        # Officers see only complaints assigned to them
        query = query.filter(Complaint.assigned_officer_id == user.id)
    # admin sees everything

    # Optional filters
    if status:
        query = query.filter(Complaint.status == status)
    if category:
        query = query.filter(Complaint.category == category)
    if priority:
        query = query.filter(Complaint.priority == priority)
    if severity:
        query = query.filter(Complaint.severity_level == severity)
    if search:
        search_term = f"%{search}%"
        query = query.filter(
            or_(
                Complaint.title.ilike(search_term),
                Complaint.description.ilike(search_term),
            )
        )

    # Total before pagination
    total = query.count()

    # Order by most recent first
    query = query.order_by(Complaint.created_at.desc())

    # Pagination
    offset = (page - 1) * limit
    complaints = query.offset(offset).limit(limit).all()

    pages = math.ceil(total / limit) if limit else 1

    items = [_complaint_to_response(db, c) for c in complaints]

    return {
        "items": items,
        "total": total,
        "page": page,
        "limit": limit,
        "pages": pages,
    }


# ---------------------------------------------------------------------------
# Single complaint detail
# ---------------------------------------------------------------------------

def get_complaint_by_id(db: Session, complaint_id: str) -> dict:
    """Return a single complaint as a response dict, or None if not found."""
    complaint = db.query(Complaint).filter(Complaint.id == complaint_id).first()
    if not complaint:
        return None
    return _complaint_to_response(db, complaint)


# ---------------------------------------------------------------------------
# Status update
# ---------------------------------------------------------------------------

def update_status(
    db: Session,
    complaint_id: str,
    new_status: str,
    changed_by_id: str,
    notes: str = None,
) -> Complaint:
    """Change the status of a complaint and log the transition."""
    complaint = db.query(Complaint).filter(Complaint.id == complaint_id).first()
    if not complaint:
        return None

    old_status = complaint.status
    complaint.status = new_status

    if new_status == "RESOLVED":
        complaint.resolved_at = datetime.utcnow()

    # History record
    history = ComplaintStatusHistory(
        id=generate_uuid(),
        complaint_id=complaint_id,
        old_status=old_status,
        new_status=new_status,
        changed_by=changed_by_id,
        notes=notes,
    )
    db.add(history)

    # Notify the citizen who filed the complaint
    notif = Notification(
        id=generate_uuid(),
        recipient_id=complaint.citizen_id,
        complaint_id=complaint_id,
        title=f"Complaint status updated to {new_status}",
        message=f"Your complaint '{complaint.title}' status changed from {old_status} to {new_status}.",
        type="STATUS_UPDATE",
        priority="MEDIUM",
    )
    db.add(notif)

    db.commit()
    db.refresh(complaint)
    return complaint


# ---------------------------------------------------------------------------
# Assign officer
# ---------------------------------------------------------------------------

def assign_officer(
    db: Session,
    complaint_id: str,
    officer_user_id: str,
    assigned_by_id: str,
) -> Complaint:
    """Assign an officer to a complaint, update workload, and notify both parties."""
    complaint = db.query(Complaint).filter(Complaint.id == complaint_id).first()
    if not complaint:
        return None

    officer = db.query(Officer).filter(Officer.user_id == officer_user_id).first()
    if not officer:
        return None

    old_status = complaint.status
    complaint.assigned_officer_id = officer_user_id
    complaint.status = "ASSIGNED"

    # Increment officer workload
    officer.workload_count = (officer.workload_count or 0) + 1

    # Log status change
    history = ComplaintStatusHistory(
        id=generate_uuid(),
        complaint_id=complaint_id,
        old_status=old_status,
        new_status="ASSIGNED",
        changed_by=assigned_by_id,
        notes=f"Assigned to officer {officer_user_id}",
    )
    db.add(history)

    # Notify citizen
    citizen_notif = Notification(
        id=generate_uuid(),
        recipient_id=complaint.citizen_id,
        complaint_id=complaint_id,
        title="Your issue has been assigned",
        message=f"Your complaint '{complaint.title}' has been assigned to an officer.",
        type="ASSIGNMENT",
        priority="MEDIUM",
    )
    db.add(citizen_notif)

    # Notify officer
    officer_notif = Notification(
        id=generate_uuid(),
        recipient_id=officer_user_id,
        complaint_id=complaint_id,
        title="You have been assigned to an issue",
        message=f"You have been assigned to complaint '{complaint.title}'.",
        type="ASSIGNMENT",
        priority="HIGH",
    )
    db.add(officer_notif)

    db.commit()
    db.refresh(complaint)
    return complaint


# ---------------------------------------------------------------------------
# Resolve complaint
# ---------------------------------------------------------------------------

def resolve_complaint(
    db: Session,
    complaint_id: str,
    notes: str,
    resolved_by_id: str,
    resolution_image: str | None = None,
) -> Complaint:
    """Mark a complaint as resolved, adjust officer workload, and notify citizen."""
    complaint = db.query(Complaint).filter(Complaint.id == complaint_id).first()
    if not complaint:
        return None

    old_status = complaint.status
    complaint.status = "RESOLVED"
    complaint.resolution_notes = notes
    complaint.resolution_image = resolution_image
    complaint.resolved_at = datetime.utcnow()

    # Decrement officer workload if an officer was assigned
    if complaint.assigned_officer_id:
        officer = (
            db.query(Officer)
            .filter(Officer.user_id == complaint.assigned_officer_id)
            .first()
        )
        if officer and officer.workload_count and officer.workload_count > 0:
            officer.workload_count -= 1

    # Status history
    history = ComplaintStatusHistory(
        id=generate_uuid(),
        complaint_id=complaint_id,
        old_status=old_status,
        new_status="RESOLVED",
        changed_by=resolved_by_id,
        notes=notes,
    )
    db.add(history)

    # Notify citizen
    notif = Notification(
        id=generate_uuid(),
        recipient_id=complaint.citizen_id,
        complaint_id=complaint_id,
        title="Your complaint has been resolved",
        message=f"Your complaint '{complaint.title}' has been resolved. Notes: {notes}",
        type="RESOLUTION",
        priority="MEDIUM",
        image_url=resolution_image,
    )
    db.add(notif)

    db.commit()
    db.refresh(complaint)
    return complaint


# ---------------------------------------------------------------------------
# Post complaint update
# ---------------------------------------------------------------------------

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

    if complaint.assigned_officer_id != officer_id:
        return "NOT_ASSIGNED"

    # Auto-transition ASSIGNED -> IN_PROGRESS when officer posts first update
    if complaint.status == "ASSIGNED":
        old_status = complaint.status
        complaint.status = "IN_PROGRESS"
        history = ComplaintStatusHistory(
            id=generate_uuid(),
            complaint_id=complaint_id,
            old_status=old_status,
            new_status="IN_PROGRESS",
            changed_by=officer_id,
            notes="Auto-transitioned: officer posted progress update",
        )
        db.add(history)

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


# ---------------------------------------------------------------------------
# Get complaint updates
# ---------------------------------------------------------------------------

def get_complaint_updates(db: Session, complaint_id: str) -> list:
    """Return all progress updates for a complaint."""
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


# ---------------------------------------------------------------------------
# Complete complaint
# ---------------------------------------------------------------------------

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


# ---------------------------------------------------------------------------
# Approve complaint
# ---------------------------------------------------------------------------

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

    if complaint.assigned_officer_id:
        officer = (
            db.query(Officer)
            .filter(Officer.user_id == complaint.assigned_officer_id)
            .first()
        )
        if officer and officer.workload_count and officer.workload_count > 0:
            officer.workload_count -= 1

    history = ComplaintStatusHistory(
        id=generate_uuid(),
        complaint_id=complaint_id,
        old_status=old_status,
        new_status="RESOLVED",
        changed_by=admin_id,
        notes="Approved by admin",
    )
    db.add(history)

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


# ---------------------------------------------------------------------------
# Rework complaint
# ---------------------------------------------------------------------------

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

    history = ComplaintStatusHistory(
        id=generate_uuid(),
        complaint_id=complaint_id,
        old_status=old_status,
        new_status="REWORK",
        changed_by=admin_id,
        notes=f"Rework requested: {reason}",
    )
    db.add(history)

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


# ---------------------------------------------------------------------------
# Status history
# ---------------------------------------------------------------------------

def get_status_history(db: Session, complaint_id: str) -> list:
    """Return the full status-change timeline for a complaint."""
    records = (
        db.query(ComplaintStatusHistory)
        .filter(ComplaintStatusHistory.complaint_id == complaint_id)
        .order_by(ComplaintStatusHistory.created_at.asc())
        .all()
    )

    result = []
    for rec in records:
        changed_by_user = db.query(User).filter(User.id == rec.changed_by).first()
        result.append(
            {
                "id": rec.id,
                "complaint_id": rec.complaint_id,
                "old_status": rec.old_status,
                "new_status": rec.new_status,
                "changed_by": rec.changed_by,
                "changed_by_name": changed_by_user.full_name if changed_by_user else None,
                "notes": rec.notes,
                "created_at": rec.created_at,
            }
        )
    return result


# ---------------------------------------------------------------------------
# Similar complaints (geographic + category)
# ---------------------------------------------------------------------------

def get_similar_complaints(db: Session, complaint_id: str) -> list:
    """Find complaints with the same category within 2 km that are not resolved."""
    complaint = db.query(Complaint).filter(Complaint.id == complaint_id).first()
    if not complaint:
        return []

    # All complaints in the same category that are not resolved (and not the same complaint)
    candidates = (
        db.query(Complaint)
        .filter(
            Complaint.category == complaint.category,
            Complaint.id != complaint.id,
            Complaint.status != "RESOLVED",
        )
        .all()
    )

    similar = []
    for c in candidates:
        dist = haversine(complaint.latitude, complaint.longitude, c.latitude, c.longitude)
        if dist <= 2.0:  # within 2 km
            similar.append(_complaint_to_response(db, c))

    return similar


# ---------------------------------------------------------------------------
# Map data (lightweight projection)
# ---------------------------------------------------------------------------

def get_map_data(db: Session) -> list:
    """Return all complaints as lightweight map-marker dicts."""
    complaints = db.query(Complaint).all()
    return [
        {
            "id": c.id,
            "latitude": c.latitude,
            "longitude": c.longitude,
            "category": c.category,
            "severity_level": c.severity_level,
            "status": c.status,
            "title": c.title,
        }
        for c in complaints
    ]


# ---------------------------------------------------------------------------
# Dashboard stats
# ---------------------------------------------------------------------------

def get_stats(db: Session) -> dict:
    """Return aggregate complaint statistics for the admin dashboard."""
    total = db.query(func.count(Complaint.id)).scalar() or 0

    # Counts by status
    def _count_status(s: str) -> int:
        return db.query(func.count(Complaint.id)).filter(Complaint.status == s).scalar() or 0

    unassigned = _count_status("UNASSIGNED")
    assigned = _count_status("ASSIGNED")
    in_progress = _count_status("IN_PROGRESS")
    resolved = _count_status("RESOLVED")
    completed = _count_status("COMPLETED")
    rework = _count_status("REWORK")

    # By category
    cat_rows = (
        db.query(Complaint.category, func.count(Complaint.id))
        .group_by(Complaint.category)
        .all()
    )
    by_category = {row[0] or "Uncategorized": row[1] for row in cat_rows}

    # By severity
    sev_rows = (
        db.query(Complaint.severity_level, func.count(Complaint.id))
        .group_by(Complaint.severity_level)
        .all()
    )
    by_severity = {row[0] or "Unknown": row[1] for row in sev_rows}

    # Recent 7 days
    seven_days_ago = datetime.utcnow() - timedelta(days=7)
    recent_7_days = (
        db.query(func.count(Complaint.id))
        .filter(Complaint.created_at >= seven_days_ago)
        .scalar()
        or 0
    )

    return {
        "total": total,
        "unassigned": unassigned,
        "assigned": assigned,
        "in_progress": in_progress,
        "resolved": resolved,
        "completed": completed,
        "rework": rework,
        "by_category": by_category,
        "by_severity": by_severity,
        "recent_7_days": recent_7_days,
    }
