from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session
from database import get_db
from schemas.auth import (
    SignupRequest,
    LoginRequest,
    AuthResponse,
    VerifyEmailRequest,
    ForgotPasswordRequest,
    VerifyOtpRequest,
    ResetPasswordRequest,
    ChangePasswordRequest,
)
from services.auth_service import (
    signup,
    login,
    verify_email,
    forgot_password,
    verify_otp,
    reset_password,
    change_password,
    resend_otp,
)
from middleware.auth import get_current_user
from models.user import User

router = APIRouter()


@router.post("/signup", response_model=AuthResponse, status_code=status.HTTP_201_CREATED)
def signup_route(data: SignupRequest, db: Session = Depends(get_db)):
    """Register a new user account."""
    try:
        result = signup(db, data)
        return result
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.post("/login", response_model=AuthResponse)
def login_route(data: LoginRequest, db: Session = Depends(get_db)):
    """Authenticate and receive an access token."""
    try:
        result = login(db, data)
        return result
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail=str(e))


@router.post("/verify-email")
def verify_email_route(data: VerifyEmailRequest, db: Session = Depends(get_db)):
    """Verify a user's email address using an OTP code."""
    try:
        result = verify_email(db, data)
        return result
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.post("/resend-otp")
def resend_otp_route(data: ForgotPasswordRequest, db: Session = Depends(get_db)):
    """Resend email verification OTP."""
    try:
        result = resend_otp(db, data.email)
        return result
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.post("/forgot-password")
def forgot_password_route(data: ForgotPasswordRequest, db: Session = Depends(get_db)):
    """Request a password reset OTP via email."""
    try:
        result = forgot_password(db, data)
        return result
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail=str(e))


@router.post("/verify-otp")
def verify_otp_route(data: VerifyOtpRequest, db: Session = Depends(get_db)):
    """Verify an OTP code for password reset."""
    try:
        result = verify_otp(db, data)
        return result
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.post("/reset-password")
def reset_password_route(data: ResetPasswordRequest, db: Session = Depends(get_db)):
    """Reset password after OTP verification."""
    try:
        # First verify the OTP
        verify_otp(db, VerifyOtpRequest(email=data.email, otp_code=data.otp_code))
        # Then reset the password
        result = reset_password(db, data.email, data.new_password)
        return result
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))


@router.put("/change-password")
def change_password_route(
    data: ChangePasswordRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """Change password for the currently authenticated user."""
    try:
        result = change_password(
            db, current_user.id, data.current_password, data.new_password
        )
        return result
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))
