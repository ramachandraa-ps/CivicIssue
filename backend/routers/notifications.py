from typing import List
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from database import get_db
from middleware.auth import get_current_user
from models.user import User
from schemas.notification import NotificationResponse, UnreadCountResponse
from services.notification_service import (
    get_user_notifications,
    get_unread_count,
    mark_as_read,
    mark_all_as_read,
)

router = APIRouter()


@router.get("/", response_model=List[NotificationResponse])
def list_notifications(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Get the current user's notifications, most recent first."""
    notifications = get_user_notifications(db, current_user.id)
    return notifications


@router.get("/unread-count", response_model=UnreadCountResponse)
def unread_count(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Return the number of unread notifications for the current user."""
    count = get_unread_count(db, current_user.id)
    return {"count": count}


@router.put("/read-all")
def read_all_notifications(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Mark all notifications as read for the current user."""
    updated = mark_all_as_read(db, current_user.id)
    return {"message": "All notifications marked as read", "updated": updated}


@router.put("/{notification_id}/read", response_model=NotificationResponse)
def read_notification(
    notification_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Mark a single notification as read."""
    notification = mark_as_read(db, notification_id, current_user.id)
    if not notification:
        raise HTTPException(status_code=404, detail="Notification not found")
    return notification
