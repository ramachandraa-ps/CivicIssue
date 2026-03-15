import uuid
from sqlalchemy import Column, String, Text, Boolean, Enum, TIMESTAMP
from sqlalchemy.sql import func
from database import Base


class Notification(Base):
    __tablename__ = "notifications"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    recipient_id = Column(String(36), nullable=False)
    complaint_id = Column(String(36), nullable=True)
    title = Column(String(255), nullable=False)
    message = Column(Text, nullable=False)
    type = Column(
        Enum("STATUS_UPDATE", "NEW_ISSUE", "ASSIGNMENT", "RESOLUTION", "SYSTEM"),
        nullable=False,
    )
    priority = Column(Enum("LOW", "MEDIUM", "HIGH"), default="MEDIUM")
    is_read = Column(Boolean, default=False)
    created_at = Column(TIMESTAMP, server_default=func.now())
