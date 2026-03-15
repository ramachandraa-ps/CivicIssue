from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from database import get_db
from middleware.auth import require_admin
from models.user import User
from models.issue_group import IssueGroup
from models.complaint import Complaint

router = APIRouter()


@router.get("/")
def list_active_groups(
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """List all active issue groups. Admin only."""
    groups = (
        db.query(IssueGroup)
        .filter(IssueGroup.status == "ACTIVE")
        .order_by(IssueGroup.created_at.desc())
        .all()
    )

    return [
        {
            "id": g.id,
            "category": g.category,
            "center_lat": g.center_lat,
            "center_lng": g.center_lng,
            "radius_meters": g.radius_meters,
            "complaint_count": g.complaint_count,
            "avg_severity": g.avg_severity,
            "status": g.status,
            "created_at": g.created_at,
        }
        for g in groups
    ]


@router.get("/{group_id}")
def get_group_detail(
    group_id: str,
    db: Session = Depends(get_db),
    _admin: User = Depends(require_admin),
):
    """Get group detail with all member complaints. Admin only."""
    group = db.query(IssueGroup).filter(IssueGroup.id == group_id).first()
    if not group:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Issue group not found",
        )

    # Fetch complaints in this group, joined with User to get citizen_name
    complaints_with_users = (
        db.query(Complaint, User.full_name)
        .outerjoin(User, Complaint.citizen_id == User.id)
        .filter(Complaint.group_id == group_id)
        .order_by(Complaint.created_at.desc())
        .all()
    )

    complaint_list = [
        {
            "id": c.id,
            "complaint_number": c.complaint_number,
            "title": c.title,
            "category": c.category,
            "status": c.status,
            "severity_level": c.severity_level,
            "latitude": c.latitude,
            "longitude": c.longitude,
            "citizen_name": citizen_name,
        }
        for c, citizen_name in complaints_with_users
    ]

    return {
        "id": group.id,
        "category": group.category,
        "center_lat": group.center_lat,
        "center_lng": group.center_lng,
        "radius_meters": group.radius_meters,
        "complaint_count": group.complaint_count,
        "avg_severity": group.avg_severity,
        "status": group.status,
        "created_at": group.created_at,
        "updated_at": group.updated_at,
        "complaints": complaint_list,
    }
