from sqlalchemy.orm import Session
from models.notification import Notification
from models.user import User
from utils.helpers import generate_uuid


def create_notification(
    db: Session,
    recipient_id: str,
    title: str,
    message: str,
    type: str,
    priority: str = "MEDIUM",
    complaint_id: str = None,
) -> Notification:
    """Create a new notification record. Caller manages the transaction (no commit)."""
    notification = Notification(
        id=generate_uuid(),
        recipient_id=recipient_id,
        complaint_id=complaint_id,
        title=title,
        message=message,
        type=type,
        priority=priority,
    )
    db.add(notification)
    db.flush()
    return notification


def notify_admins_new_complaint(db: Session, complaint) -> None:
    """Notify all admin users about a newly created complaint."""
    admins = db.query(User).filter(User.role == "admin").all()
    location = complaint.location_text or "unknown location"
    for admin in admins:
        create_notification(
            db=db,
            recipient_id=admin.id,
            title=f"New {complaint.category} issue reported",
            message=f"A new complaint '{complaint.title}' has been reported at {location}",
            type="NEW_ISSUE",
            priority=complaint.priority or "MEDIUM",
            complaint_id=complaint.id,
        )


def notify_status_change(
    db: Session, complaint, old_status: str, new_status: str
) -> None:
    """Notify the citizen who filed the complaint about a status change."""
    if new_status == "ASSIGNED":
        title = f"Your issue {complaint.complaint_number} has been assigned"
        notification_type = "STATUS_UPDATE"
        priority = "MEDIUM"
    elif new_status == "IN_PROGRESS":
        title = f"Work has started on your issue {complaint.complaint_number}"
        notification_type = "STATUS_UPDATE"
        priority = "MEDIUM"
    elif new_status in ("COMPLETED", "RESOLVED"):
        title = f"Your issue {complaint.complaint_number} has been resolved"
        notification_type = "RESOLUTION"
        priority = "HIGH"
    else:
        title = f"Status update on your issue {complaint.complaint_number}"
        notification_type = "STATUS_UPDATE"
        priority = "MEDIUM"

    message = f"Status changed from {old_status} to {new_status}"

    create_notification(
        db=db,
        recipient_id=complaint.citizen_id,
        title=title,
        message=message,
        type=notification_type,
        priority=priority,
        complaint_id=complaint.id,
    )


def notify_officer_assigned(
    db: Session, complaint, officer_user_id: str
) -> None:
    """Notify an officer that they have been assigned to a complaint."""
    location = complaint.location_text or "unknown location"
    create_notification(
        db=db,
        recipient_id=officer_user_id,
        title=f"You have been assigned to issue {complaint.complaint_number}",
        message=f"Complaint: {complaint.title} at {location}",
        type="ASSIGNMENT",
        priority="HIGH",
        complaint_id=complaint.id,
    )


def get_user_notifications(
    db: Session, user_id: str, limit: int = 50
) -> list[Notification]:
    """Return the most recent notifications for a user."""
    return (
        db.query(Notification)
        .filter(Notification.recipient_id == user_id)
        .order_by(Notification.created_at.desc())
        .limit(limit)
        .all()
    )


def get_unread_count(db: Session, user_id: str) -> int:
    """Return the count of unread notifications for a user."""
    return (
        db.query(Notification)
        .filter(
            Notification.recipient_id == user_id,
            Notification.is_read == False,
        )
        .count()
    )


def mark_as_read(db: Session, notification_id: str, user_id: str) -> Notification:
    """Mark a single notification as read. Verifies ownership."""
    notification = (
        db.query(Notification)
        .filter(Notification.id == notification_id)
        .first()
    )
    if not notification:
        return None
    if notification.recipient_id != user_id:
        return None
    notification.is_read = True
    db.commit()
    db.refresh(notification)
    return notification


def mark_all_as_read(db: Session, user_id: str) -> int:
    """Mark all unread notifications as read for a user. Returns updated count."""
    updated = (
        db.query(Notification)
        .filter(
            Notification.recipient_id == user_id,
            Notification.is_read == False,
        )
        .update({"is_read": True})
    )
    db.commit()
    return updated
