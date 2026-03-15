from pydantic import BaseModel, EmailStr
from typing import Optional, List
from datetime import datetime


class CategoryCreate(BaseModel):
    name: str
    description: Optional[str] = None
    icon: Optional[str] = None


class CategoryResponse(BaseModel):
    id: str
    name: str
    description: Optional[str] = None
    icon: Optional[str] = None
    is_active: bool
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class DepartmentCreate(BaseModel):
    name: str
    description: Optional[str] = None


class DepartmentResponse(BaseModel):
    id: str
    name: str
    description: Optional[str] = None
    is_active: bool
    created_at: Optional[datetime] = None

    class Config:
        from_attributes = True


class OfficerCreate(BaseModel):
    full_name: str
    email: EmailStr
    password: str
    phone_number: Optional[str] = None
    department: Optional[str] = None
    designation: Optional[str] = None


class OfficerResponse(BaseModel):
    id: str
    user_id: str
    full_name: str
    email: str
    department: Optional[str] = None
    designation: Optional[str] = None
    workload_count: int
    is_available: bool


class SystemLogResponse(BaseModel):
    id: str
    action: str
    entity_type: Optional[str] = None
    entity_id: Optional[str] = None
    performed_by: str
    performed_by_name: Optional[str] = None
    details: Optional[dict] = None
    created_at: Optional[datetime] = None


class DashboardStats(BaseModel):
    total_complaints: int
    total_citizens: int
    total_officers: int
    unassigned: int
    assigned: int
    in_progress: int
    resolved: int
    completed: int
    by_category: dict
    by_severity: dict
    recent_7_days: int
    resolution_rate: float
