from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class NotificationResponse(BaseModel):
    id: str
    recipient_id: str
    complaint_id: Optional[str] = None
    title: str
    message: str
    type: str
    priority: str
    is_read: bool
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class UnreadCountResponse(BaseModel):
    count: int
