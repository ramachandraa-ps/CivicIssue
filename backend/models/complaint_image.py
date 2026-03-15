import uuid
from sqlalchemy import Column, String, JSON, TIMESTAMP
from sqlalchemy.sql import func
from database import Base


class ComplaintImage(Base):
    __tablename__ = "complaint_images"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    complaint_id = Column(String(36), nullable=False)
    image_url = Column(String(500), nullable=False)
    ai_analysis = Column(JSON, nullable=True)
    uploaded_at = Column(TIMESTAMP, server_default=func.now())
