import uuid
from sqlalchemy import Column, String, Boolean, Enum, TIMESTAMP
from sqlalchemy.sql import func
from database import Base


class OTPVerification(Base):
    __tablename__ = "otp_verifications"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), nullable=False)
    otp_code = Column(String(6), nullable=False)
    purpose = Column(Enum("EMAIL_VERIFY", "PASSWORD_RESET"), nullable=False)
    is_used = Column(Boolean, default=False)
    expires_at = Column(TIMESTAMP, nullable=False)
    created_at = Column(TIMESTAMP, server_default=func.now())
