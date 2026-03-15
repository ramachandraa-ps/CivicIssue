import os
import uuid

from fastapi import APIRouter, Depends, HTTPException, UploadFile, File
from middleware.auth import get_current_user
from models.user import User
from services.gemini_service import analyze_image
from config import settings

router = APIRouter()

ALLOWED_TYPES = {"image/jpeg", "image/png", "image/jpg"}
MAX_FILE_SIZE_BYTES = settings.MAX_FILE_SIZE_MB * 1024 * 1024


def _validate_image(file: UploadFile) -> None:
    """Raise HTTPException if the uploaded file is not an allowed image type."""
    if file.content_type not in ALLOWED_TYPES:
        raise HTTPException(
            status_code=400,
            detail=f"Invalid file type '{file.content_type}'. Allowed types: JPEG, PNG.",
        )


def _save_image(content: bytes, original_filename: str | None) -> tuple[str, str]:
    """Save image bytes to disk with a UUID filename. Returns (filename, filepath)."""
    ext = os.path.splitext(original_filename or "image.jpg")[1] or ".jpg"
    filename = f"{uuid.uuid4().hex}{ext}"
    filepath = os.path.join(settings.UPLOAD_DIR, filename)

    os.makedirs(settings.UPLOAD_DIR, exist_ok=True)

    with open(filepath, "wb") as f:
        f.write(content)

    return filename, filepath


@router.post("/upload")
async def upload_image(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
):
    """Upload an image file. Returns the public URL for the stored image."""
    _validate_image(file)

    content = await file.read()
    if len(content) > MAX_FILE_SIZE_BYTES:
        raise HTTPException(
            status_code=400,
            detail=f"File size exceeds the {settings.MAX_FILE_SIZE_MB}MB limit.",
        )

    filename, _ = _save_image(content, file.filename)

    return {"image_url": f"/uploads/{filename}"}


@router.post("/analyze")
async def analyze_uploaded_image(
    file: UploadFile = File(...),
    current_user: User = Depends(get_current_user),
):
    """Upload an image, save it, and return Gemini AI analysis of the civic issue."""
    _validate_image(file)

    content = await file.read()
    if len(content) > MAX_FILE_SIZE_BYTES:
        raise HTTPException(
            status_code=400,
            detail=f"File size exceeds the {settings.MAX_FILE_SIZE_MB}MB limit.",
        )

    filename, filepath = _save_image(content, file.filename)

    analysis = await analyze_image(filepath)

    return {
        "image_url": f"/uploads/{filename}",
        "detected_category": analysis.get("category", "other"),
        "severity_level": analysis.get("severity", "MEDIUM"),
        "confidence_score": analysis.get("confidence", 0.5),
        "tags": analysis.get("tags", []),
        "description_suggestion": analysis.get("description", ""),
    }
