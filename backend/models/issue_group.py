import uuid
from sqlalchemy import Column, String, Double, Float, Integer, Enum, TIMESTAMP
from sqlalchemy.sql import func
from database import Base


class IssueGroup(Base):
    __tablename__ = "issue_groups"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    category = Column(String(100), nullable=False)
    center_lat = Column(Double, nullable=False)
    center_lng = Column(Double, nullable=False)
    radius_meters = Column(Float, default=2000)
    complaint_count = Column(Integer, default=1)
    avg_severity = Column(String(20), nullable=True)
    status = Column(Enum("ACTIVE", "RESOLVED"), default="ACTIVE")
    created_at = Column(TIMESTAMP, server_default=func.now())
    updated_at = Column(TIMESTAMP, server_default=func.now(), onupdate=func.now())
