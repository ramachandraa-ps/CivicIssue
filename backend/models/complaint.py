import uuid
from sqlalchemy import Column, String, Text, Double, Float, Enum, TIMESTAMP, JSON
from sqlalchemy.sql import func
from database import Base


class Complaint(Base):
    __tablename__ = "complaints"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    complaint_number = Column(String(20), unique=True)
    citizen_id = Column(String(36), nullable=False)
    title = Column(String(255), nullable=False)
    description = Column(Text, nullable=True)
    category = Column(String(100), nullable=True)
    ai_detected_category = Column(String(100), nullable=True)
    ai_text_category = Column(String(100), nullable=True)
    location_text = Column(String(500), nullable=True)
    latitude = Column(Double, nullable=False)
    longitude = Column(Double, nullable=False)
    priority = Column(Enum("LOW", "MEDIUM", "HIGH"), default="MEDIUM")
    severity_level = Column(Enum("LOW", "MEDIUM", "HIGH", "CRITICAL"), default="MEDIUM")
    status = Column(
        Enum("UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED", "REWORK"),
        default="UNASSIGNED",
    )
    assigned_officer_id = Column(String(36), nullable=True)
    group_id = Column(String(36), nullable=True)
    ai_confidence = Column(Float, nullable=True)
    ai_keywords = Column(JSON, nullable=True)
    resolution_notes = Column(Text, nullable=True)
    resolution_image = Column(String(500), nullable=True)
    resolved_at = Column(TIMESTAMP, nullable=True)
    created_at = Column(TIMESTAMP, server_default=func.now())
    updated_at = Column(TIMESTAMP, server_default=func.now(), onupdate=func.now())
