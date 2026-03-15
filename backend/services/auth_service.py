import random
from datetime import datetime, timedelta
from sqlalchemy.orm import Session
from models.user import User
from models.otp import OTPVerification
from utils.security import hash_password, verify_password, create_access_token
from utils.helpers import generate_uuid
from services.email_service import send_otp_email
from schemas.auth import (
    SignupRequest,
    LoginRequest,
    VerifyEmailRequest,
    ForgotPasswordRequest,
    VerifyOtpRequest,
)


def _user_dict(user: User) -> dict:
    """Build a serializable dict from a User model instance."""
    return {
        "id": user.id,
        "full_name": user.full_name,
        "email": user.email,
        "role": user.role,
        "is_verified": user.is_verified,
        "avatar_url": user.avatar_url,
        "phone_number": user.phone_number,
        "country_code": user.country_code,
    }


def _generate_otp() -> str:
    """Generate a random 6-digit OTP code."""
    return str(random.randint(100000, 999999))


def signup(db: Session, data: SignupRequest) -> dict:
    """Register a new user, send verification OTP, and return auth response."""
    existing = db.query(User).filter(User.email == data.email).first()
    if existing:
        raise ValueError("An account with this email already exists")

    if data.role not in ("citizen", "admin", "officer"):
        raise ValueError("Invalid role. Must be citizen, admin, or officer")

    user = User(
        id=generate_uuid(),
        full_name=data.full_name,
        email=data.email,
        phone_number=data.phone_number,
        country_code=data.country_code,
        password_hash=hash_password(data.password),
        role=data.role,
        is_verified=False,
    )
    db.add(user)
    db.flush()

    otp_code = _generate_otp()
    otp_record = OTPVerification(
        id=generate_uuid(),
        user_id=user.id,
        otp_code=otp_code,
        purpose="EMAIL_VERIFY",
        is_used=False,
        expires_at=datetime.now() + timedelta(minutes=10),
    )
    db.add(otp_record)
    db.commit()
    db.refresh(user)

    send_otp_email(user.email, otp_code, "EMAIL_VERIFY")

    token = create_access_token({"sub": user.id})
    return {
        "access_token": token,
        "token_type": "bearer",
        "user": _user_dict(user),
    }


def login(db: Session, data: LoginRequest) -> dict:
    """Authenticate a user by email and password, return auth response."""
    user = db.query(User).filter(User.email == data.email).first()
    if not user:
        raise ValueError("Invalid email or password")

    if not verify_password(data.password, user.password_hash):
        raise ValueError("Invalid email or password")

    token = create_access_token({"sub": user.id})
    return {
        "access_token": token,
        "token_type": "bearer",
        "user": _user_dict(user),
    }


def verify_email(db: Session, data: VerifyEmailRequest) -> dict:
    """Verify a user's email using the OTP code."""
    user = db.query(User).filter(User.email == data.email).first()
    if not user:
        raise ValueError("User not found")

    otp_record = (
        db.query(OTPVerification)
        .filter(
            OTPVerification.user_id == user.id,
            OTPVerification.purpose == "EMAIL_VERIFY",
            OTPVerification.is_used == False,
        )
        .order_by(OTPVerification.created_at.desc())
        .first()
    )

    if not otp_record:
        raise ValueError("No valid OTP found. Please request a new one")

    if otp_record.expires_at < datetime.now():
        raise ValueError("OTP has expired. Please request a new one")

    if otp_record.otp_code != data.otp_code:
        raise ValueError("Invalid OTP code")

    otp_record.is_used = True
    user.is_verified = True
    db.commit()
    db.refresh(user)

    return {"message": "Email verified successfully", "user": _user_dict(user)}


def resend_otp(db: Session, email: str) -> dict:
    """Resend email verification OTP. Invalidates previous OTPs."""
    user = db.query(User).filter(User.email == email).first()
    if not user:
        raise ValueError("User not found")

    # Mark old unused verification OTPs as used
    old_otps = (
        db.query(OTPVerification)
        .filter(
            OTPVerification.user_id == user.id,
            OTPVerification.purpose == "EMAIL_VERIFY",
            OTPVerification.is_used == False,
        )
        .all()
    )
    for otp in old_otps:
        otp.is_used = True

    # Generate and send new OTP
    otp_code = _generate_otp()
    otp_record = OTPVerification(
        id=generate_uuid(),
        user_id=user.id,
        otp_code=otp_code,
        purpose="EMAIL_VERIFY",
        is_used=False,
        expires_at=datetime.now() + timedelta(minutes=10),
    )
    db.add(otp_record)
    db.commit()

    send_otp_email(user.email, otp_code, "EMAIL_VERIFY")

    return {"message": "OTP resent successfully"}


def forgot_password(db: Session, data: ForgotPasswordRequest) -> dict:
    """Generate a password-reset OTP and send it to the user's email."""
    user = db.query(User).filter(User.email == data.email).first()
    if not user:
        raise ValueError("No account found with this email")

    otp_code = _generate_otp()
    otp_record = OTPVerification(
        id=generate_uuid(),
        user_id=user.id,
        otp_code=otp_code,
        purpose="PASSWORD_RESET",
        is_used=False,
        expires_at=datetime.now() + timedelta(minutes=10),
    )
    db.add(otp_record)
    db.commit()

    send_otp_email(user.email, otp_code, "PASSWORD_RESET")

    return {"message": "Password reset OTP sent to your email"}


def verify_otp(db: Session, data: VerifyOtpRequest) -> dict:
    """Validate an OTP for password reset without consuming it yet."""
    user = db.query(User).filter(User.email == data.email).first()
    if not user:
        raise ValueError("User not found")

    otp_record = (
        db.query(OTPVerification)
        .filter(
            OTPVerification.user_id == user.id,
            OTPVerification.purpose == "PASSWORD_RESET",
            OTPVerification.is_used == False,
        )
        .order_by(OTPVerification.created_at.desc())
        .first()
    )

    if not otp_record:
        raise ValueError("No valid OTP found. Please request a new one")

    if otp_record.expires_at < datetime.now():
        raise ValueError("OTP has expired. Please request a new one")

    if otp_record.otp_code != data.otp_code:
        raise ValueError("Invalid OTP code")

    return {"message": "OTP verified successfully"}


def reset_password(db: Session, email: str, new_password: str) -> dict:
    """Reset a user's password after OTP verification."""
    user = db.query(User).filter(User.email == email).first()
    if not user:
        raise ValueError("User not found")

    # Mark any unused password-reset OTPs as used
    unused_otps = (
        db.query(OTPVerification)
        .filter(
            OTPVerification.user_id == user.id,
            OTPVerification.purpose == "PASSWORD_RESET",
            OTPVerification.is_used == False,
        )
        .all()
    )
    for otp in unused_otps:
        otp.is_used = True

    user.password_hash = hash_password(new_password)
    db.commit()
    db.refresh(user)

    return {"message": "Password reset successfully"}


def change_password(
    db: Session, user_id: str, current_password: str, new_password: str
) -> dict:
    """Change password for an authenticated user after verifying the current one."""
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise ValueError("User not found")

    if not verify_password(current_password, user.password_hash):
        raise ValueError("Current password is incorrect")

    user.password_hash = hash_password(new_password)
    db.commit()
    db.refresh(user)

    return {"message": "Password changed successfully"}
