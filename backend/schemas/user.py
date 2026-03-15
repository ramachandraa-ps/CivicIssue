from pydantic import BaseModel
from typing import Optional
from datetime import datetime


class UserProfile(BaseModel):
    id: str
    full_name: str
    email: str
    phone_number: Optional[str] = None
    country_code: Optional[str] = None
    role: str
    avatar_url: Optional[str] = None
    is_verified: bool
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class UpdateProfileRequest(BaseModel):
    full_name: Optional[str] = None
    phone_number: Optional[str] = None
    country_code: Optional[str] = None
