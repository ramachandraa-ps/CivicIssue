import uuid
from sqlalchemy import Column, String, JSON, TIMESTAMP
from sqlalchemy.sql import func
from database import Base


class SystemLog(Base):
    __tablename__ = "system_logs"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    action = Column(String(100), nullable=False)
    entity_type = Column(String(50), nullable=True)
    entity_id = Column(String(36), nullable=True)
    performed_by = Column(String(36), nullable=False)
    details = Column(JSON, nullable=True)
    created_at = Column(TIMESTAMP, server_default=func.now())
