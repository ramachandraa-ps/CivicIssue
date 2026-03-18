import uuid
from sqlalchemy import Column, String, Text, TIMESTAMP, ForeignKey
from sqlalchemy.sql import func
from database import Base


class ComplaintUpdate(Base):
    __tablename__ = "complaint_updates"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    complaint_id = Column(String(36), ForeignKey("complaints.id"), nullable=False)
    officer_id = Column(String(36), ForeignKey("users.id"), nullable=False)
    message = Column(Text, nullable=False)
    image_url = Column(String(500), nullable=True)
    created_at = Column(TIMESTAMP, server_default=func.now())
