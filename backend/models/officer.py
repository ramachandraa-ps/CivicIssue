import uuid
from sqlalchemy import Column, String, Integer, Boolean
from database import Base


class Officer(Base):
    __tablename__ = "officers"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), unique=True, nullable=False)
    department = Column(String(100), nullable=True)
    designation = Column(String(100), nullable=True)
    workload_count = Column(Integer, default=0)
    is_available = Column(Boolean, default=True)
