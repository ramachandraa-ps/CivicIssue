from pydantic import BaseModel, EmailStr
from typing import Optional


class SignupRequest(BaseModel):
    full_name: str
    email: EmailStr
    phone_number: Optional[str] = None
    country_code: Optional[str] = None
    password: str
    role: str = "citizen"


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class AuthResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: dict


class VerifyEmailRequest(BaseModel):
    email: EmailStr
    otp_code: str


class ForgotPasswordRequest(BaseModel):
    email: EmailStr


class VerifyOtpRequest(BaseModel):
    email: EmailStr
    otp_code: str


class ResetPasswordRequest(BaseModel):
    email: EmailStr
    otp_code: str
    new_password: str


class ChangePasswordRequest(BaseModel):
    current_password: str
    new_password: str
