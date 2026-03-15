from fastapi import APIRouter, Depends, HTTPException, UploadFile, File
from sqlalchemy.orm import Session
from database import get_db
from middleware.auth import get_current_user
from models.user import User
from schemas.user import UserProfile, UpdateProfileRequest
from config import settings
import os
import uuid

router = APIRouter()

ALLOWED_MIME_TYPES = {"image/jpeg", "image/png"}
MAX_FILE_SIZE_BYTES = settings.MAX_FILE_SIZE_MB * 1024 * 1024


@router.get("/me", response_model=UserProfile)
def get_me(current_user: User = Depends(get_current_user)):
    """Return the authenticated user's profile."""
    return current_user


@router.put("/me", response_model=UserProfile)
def update_me(
    payload: UpdateProfileRequest,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Update the authenticated user's name, phone number, or country code."""
    if payload.full_name is not None:
        current_user.full_name = payload.full_name
    if payload.phone_number is not None:
        current_user.phone_number = payload.phone_number
    if payload.country_code is not None:
        current_user.country_code = payload.country_code

    db.commit()
    db.refresh(current_user)
    return current_user


@router.put("/me/avatar", response_model=UserProfile)
async def update_avatar(
    file: UploadFile = File(...),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Upload or replace the authenticated user's avatar image."""
    # Validate MIME type
    if file.content_type not in ALLOWED_MIME_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid file type '{file.content_type}'. Only JPEG and PNG are allowed.",
        )

    # Read file content and validate size
    content = await file.read()
    if len(content) > MAX_FILE_SIZE_BYTES:
        raise HTTPException(
            status_code=400,
            detail=f"File size exceeds the {settings.MAX_FILE_SIZE_MB}MB limit.",
        )

    # Build a unique filename preserving the original extension
    ext = os.path.splitext(file.filename or "avatar.png")[1] or ".png"
    filename = f"{uuid.uuid4().hex}{ext}"
    filepath = os.path.join(settings.UPLOAD_DIR, filename)

    # Ensure upload directory exists
    os.makedirs(settings.UPLOAD_DIR, exist_ok=True)

    # Delete the previous avatar file if one exists
    if current_user.avatar_url:
        old_path = os.path.join(
            settings.UPLOAD_DIR, os.path.basename(current_user.avatar_url)
        )
        if os.path.isfile(old_path):
            os.remove(old_path)

    # Write the new file to disk
    with open(filepath, "wb") as f:
        f.write(content)

    # Persist the new avatar URL
    current_user.avatar_url = f"/uploads/{filename}"
    db.commit()
    db.refresh(current_user)
    return current_user
