from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from typing import Optional, List
from sqlalchemy.orm import Session
from database import get_db
from middleware.auth import get_current_user
from models.user import User
from models.chatbot import ChatbotSession, ChatbotMessage
from services.gemini_service import analyze_text as gemini_analyze_text, chat as gemini_chat
from utils.helpers import generate_uuid

router = APIRouter()


# ---------------------------------------------------------------------------
# Request / Response models
# ---------------------------------------------------------------------------

class TextAnalysisRequest(BaseModel):
    description: str


class TextAnalysisResponse(BaseModel):
    detected_category: str
    keywords: list
    suggested_priority: str
    urgency_indicator: str


class ChatRequest(BaseModel):
    message: str
    session_id: Optional[str] = None


class ChatResponse(BaseModel):
    response: str
    session_id: str


class ChatMessageResponse(BaseModel):
    id: str
    session_id: str
    text: str
    is_user: bool
    created_at: Optional[str] = None


class ChatSessionResponse(BaseModel):
    id: str
    messages: List[ChatMessageResponse]
    created_at: Optional[str] = None


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------

@router.post("/analyze-text", response_model=TextAnalysisResponse)
async def analyze_text_endpoint(
    data: TextAnalysisRequest,
    current_user: User = Depends(get_current_user),
):
    """Analyze a civic issue description and return structured metadata."""
    result = await gemini_analyze_text(data.description)
    return TextAnalysisResponse(
        detected_category=result.get("category", "other"),
        keywords=result.get("keywords", []),
        suggested_priority=result.get("suggested_priority", "MEDIUM"),
        urgency_indicator=result.get("urgency_indicator", ""),
    )


@router.post("/chatbot", response_model=ChatResponse)
async def chatbot_endpoint(
    data: ChatRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Send a message to the AI chatbot and receive a response."""
    session_id = data.session_id

    # Create a new session if none was provided
    if not session_id:
        session_id = generate_uuid()
        new_session = ChatbotSession(id=session_id, user_id=current_user.id)
        db.add(new_session)
        db.commit()
    else:
        # Verify the session belongs to the current user
        session = (
            db.query(ChatbotSession)
            .filter(
                ChatbotSession.id == session_id,
                ChatbotSession.user_id == current_user.id,
            )
            .first()
        )
        if not session:
            raise HTTPException(status_code=404, detail="Chat session not found")

    # Load chat history for this session
    history_rows = (
        db.query(ChatbotMessage)
        .filter(ChatbotMessage.session_id == session_id)
        .order_by(ChatbotMessage.created_at.asc())
        .all()
    )
    history = [{"text": msg.text, "is_user": msg.is_user} for msg in history_rows]

    # Persist the user's message
    user_msg = ChatbotMessage(
        id=generate_uuid(),
        session_id=session_id,
        text=data.message,
        is_user=True,
    )
    db.add(user_msg)
    db.commit()

    # Get AI response
    ai_response_text = await gemini_chat(data.message, history, current_user.role)

    # Persist the AI response
    ai_msg = ChatbotMessage(
        id=generate_uuid(),
        session_id=session_id,
        text=ai_response_text,
        is_user=False,
    )
    db.add(ai_msg)
    db.commit()

    return ChatResponse(response=ai_response_text, session_id=session_id)


@router.get("/chatbot/history", response_model=List[ChatSessionResponse])
def chatbot_history_endpoint(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Retrieve the current user's recent chat sessions with messages."""
    sessions = (
        db.query(ChatbotSession)
        .filter(ChatbotSession.user_id == current_user.id)
        .order_by(ChatbotSession.created_at.desc())
        .limit(10)
        .all()
    )

    result: List[ChatSessionResponse] = []
    for session in sessions:
        messages = (
            db.query(ChatbotMessage)
            .filter(ChatbotMessage.session_id == session.id)
            .order_by(ChatbotMessage.created_at.asc())
            .all()
        )
        result.append(
            ChatSessionResponse(
                id=session.id,
                created_at=str(session.created_at) if session.created_at else None,
                messages=[
                    ChatMessageResponse(
                        id=msg.id,
                        session_id=msg.session_id,
                        text=msg.text,
                        is_user=msg.is_user,
                        created_at=str(msg.created_at) if msg.created_at else None,
                    )
                    for msg in messages
                ],
            )
        )

    return result
