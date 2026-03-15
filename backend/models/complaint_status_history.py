import uuid
from sqlalchemy import Column, String, Text, TIMESTAMP
from sqlalchemy.sql import func
from database import Base


class ComplaintStatusHistory(Base):
    __tablename__ = "complaint_status_history"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    complaint_id = Column(String(36), nullable=False)
    old_status = Column(String(50), nullable=True)
    new_status = Column(String(50), nullable=False)
    changed_by = Column(String(36), nullable=False)
    notes = Column(Text, nullable=True)
    created_at = Column(TIMESTAMP, server_default=func.now())
