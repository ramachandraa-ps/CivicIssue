import uuid
from sqlalchemy import Column, String, Text, Boolean, TIMESTAMP
from sqlalchemy.sql import func
from database import Base


class ChatbotSession(Base):
    __tablename__ = "chatbot_sessions"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id = Column(String(36), nullable=False)
    created_at = Column(TIMESTAMP, server_default=func.now())


class ChatbotMessage(Base):
    __tablename__ = "chatbot_messages"

    id = Column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    session_id = Column(String(36), nullable=False)
    text = Column(Text, nullable=False)
    is_user = Column(Boolean, nullable=False)
    created_at = Column(TIMESTAMP, server_default=func.now())
