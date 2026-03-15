from pydantic import BaseModel
from typing import Optional, List, Any
from datetime import datetime


class ComplaintCreate(BaseModel):
    title: str
    description: Optional[str] = None
    category: Optional[str] = None
    ai_detected_category: Optional[str] = None
    ai_text_category: Optional[str] = None
    location_text: Optional[str] = None
    latitude: float
    longitude: float
    priority: Optional[str] = "MEDIUM"
    severity_level: Optional[str] = "MEDIUM"
    ai_confidence: Optional[float] = None
    ai_keywords: Optional[list] = None


class ComplaintResponse(BaseModel):
    id: str
    complaint_number: str
    citizen_id: str
    citizen_name: str
    title: str
    description: Optional[str] = None
    category: Optional[str] = None
    ai_detected_category: Optional[str] = None
    ai_text_category: Optional[str] = None
    location_text: Optional[str] = None
    latitude: float
    longitude: float
    priority: str
    severity_level: str
    status: str
    assigned_officer_id: Optional[str] = None
    assigned_officer_name: Optional[str] = None
    group_id: Optional[str] = None
    ai_confidence: Optional[float] = None
    ai_keywords: Optional[list] = None
    resolution_notes: Optional[str] = None
    resolved_at: Optional[datetime] = None
    images: List[str] = []
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


class StatusUpdateRequest(BaseModel):
    status: str
    notes: Optional[str] = None


class AssignOfficerRequest(BaseModel):
    officer_id: str


class ResolveRequest(BaseModel):
    resolution_notes: str


class PaginatedResponse(BaseModel):
    items: List[Any]
    total: int
    page: int
    limit: int
    pages: int


class ComplaintStats(BaseModel):
    total: int
    unassigned: int
    assigned: int
    in_progress: int
    resolved: int
    completed: int
    by_category: dict
    by_severity: dict
    recent_7_days: int


class StatusHistoryResponse(BaseModel):
    id: str
    complaint_id: str
    old_status: Optional[str] = None
    new_status: str
    changed_by: str
    changed_by_name: Optional[str] = None
    notes: Optional[str] = None
    created_at: Optional[datetime] = None


class MapDataResponse(BaseModel):
    id: str
    latitude: float
    longitude: float
    category: Optional[str] = None
    severity_level: str
    status: str
    title: str
