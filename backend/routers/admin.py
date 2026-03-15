from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from sqlalchemy.orm import Session
from sqlalchemy import func
from database import get_db
from middleware.auth import require_admin
from models.user import User
from models.category import Category
from models.department import Department
from models.officer import Officer
from models.system_log import SystemLog
from models.complaint import Complaint
from schemas.admin import (
    CategoryCreate,
    CategoryResponse,
    DepartmentCreate,
    DepartmentResponse,
    OfficerCreate,
    OfficerResponse,
    SystemLogResponse,
    DashboardStats,
)
from utils.security import hash_password
from utils.helpers import generate_uuid
from datetime import datetime, timedelta
from typing import List
import math

router = APIRouter()


# ---------------------------------------------------------------------------
# Paginated response schema (defined locally to avoid circular imports)
# ---------------------------------------------------------------------------
class PaginatedResponse(BaseModel):
    items: list
    total: int
    page: int
    limit: int
    pages: int


# ---------------------------------------------------------------------------
# Helper: create a system log entry
# ---------------------------------------------------------------------------
def create_system_log(
    db: Session,
    action: str,
    entity_type: str,
    entity_id: str,
    performed_by: str,
    details: dict = None,
) -> SystemLog:
    log = SystemLog(
        id=generate_uuid(),
        action=action,
        entity_type=entity_type,
        entity_id=entity_id,
        performed_by=performed_by,
        details=details,
    )
    db.add(log)
    db.commit()
    db.refresh(log)
    return log


# ===================================================================
# CATEGORIES
# ===================================================================

@router.get("/categories", response_model=List[CategoryResponse])
def list_categories(
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """List all categories (including inactive ones) for admin management."""
    categories = db.query(Category).order_by(Category.created_at.desc()).all()
    return categories


@router.post("/categories", response_model=CategoryResponse, status_code=201)
def create_category(
    payload: CategoryCreate,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """Create a new category."""
    existing = db.query(Category).filter(Category.name == payload.name).first()
    if existing:
        raise HTTPException(status_code=400, detail="Category with this name already exists")

    category = Category(
        id=generate_uuid(),
        name=payload.name,
        description=payload.description,
        icon=payload.icon,
        is_active=True,
    )
    db.add(category)
    db.commit()
    db.refresh(category)

    create_system_log(
        db,
        action="CREATE_CATEGORY",
        entity_type="category",
        entity_id=category.id,
        performed_by=admin.id,
        details={"name": category.name},
    )

    return category


@router.put("/categories/{category_id}", response_model=CategoryResponse)
def update_category(
    category_id: str,
    payload: CategoryCreate,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """Update an existing category's name, description, and/or icon."""
    category = db.query(Category).filter(Category.id == category_id).first()
    if not category:
        raise HTTPException(status_code=404, detail="Category not found")

    # Check for name uniqueness if name is changing
    if payload.name != category.name:
        duplicate = db.query(Category).filter(Category.name == payload.name).first()
        if duplicate:
            raise HTTPException(status_code=400, detail="Category with this name already exists")

    category.name = payload.name
    if payload.description is not None:
        category.description = payload.description
    if payload.icon is not None:
        category.icon = payload.icon

    db.commit()
    db.refresh(category)

    create_system_log(
        db,
        action="UPDATE_CATEGORY",
        entity_type="category",
        entity_id=category.id,
        performed_by=admin.id,
        details={"name": category.name},
    )

    return category


@router.delete("/categories/{category_id}")
def delete_category(
    category_id: str,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """Soft-delete a category by setting is_active to False."""
    category = db.query(Category).filter(Category.id == category_id).first()
    if not category:
        raise HTTPException(status_code=404, detail="Category not found")

    category.is_active = False
    db.commit()

    create_system_log(
        db,
        action="DELETE_CATEGORY",
        entity_type="category",
        entity_id=category.id,
        performed_by=admin.id,
        details={"name": category.name},
    )

    return {"message": "Category deactivated successfully"}


# ===================================================================
# DEPARTMENTS
# ===================================================================

@router.get("/departments", response_model=List[DepartmentResponse])
def list_departments(
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """List all departments (including inactive ones) for admin management."""
    departments = db.query(Department).order_by(Department.created_at.desc()).all()
    return departments


@router.post("/departments", response_model=DepartmentResponse, status_code=201)
def create_department(
    payload: DepartmentCreate,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """Create a new department."""
    existing = db.query(Department).filter(Department.name == payload.name).first()
    if existing:
        raise HTTPException(status_code=400, detail="Department with this name already exists")

    department = Department(
        id=generate_uuid(),
        name=payload.name,
        description=payload.description,
        is_active=True,
    )
    db.add(department)
    db.commit()
    db.refresh(department)

    create_system_log(
        db,
        action="CREATE_DEPARTMENT",
        entity_type="department",
        entity_id=department.id,
        performed_by=admin.id,
        details={"name": department.name},
    )

    return department


@router.put("/departments/{department_id}", response_model=DepartmentResponse)
def update_department(
    department_id: str,
    payload: DepartmentCreate,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """Update an existing department's name and/or description."""
    department = db.query(Department).filter(Department.id == department_id).first()
    if not department:
        raise HTTPException(status_code=404, detail="Department not found")

    # Check for name uniqueness if name is changing
    if payload.name != department.name:
        duplicate = db.query(Department).filter(Department.name == payload.name).first()
        if duplicate:
            raise HTTPException(status_code=400, detail="Department with this name already exists")

    department.name = payload.name
    if payload.description is not None:
        department.description = payload.description

    db.commit()
    db.refresh(department)

    create_system_log(
        db,
        action="UPDATE_DEPARTMENT",
        entity_type="department",
        entity_id=department.id,
        performed_by=admin.id,
        details={"name": department.name},
    )

    return department


@router.delete("/departments/{department_id}")
def delete_department(
    department_id: str,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """Soft-delete a department by setting is_active to False."""
    department = db.query(Department).filter(Department.id == department_id).first()
    if not department:
        raise HTTPException(status_code=404, detail="Department not found")

    department.is_active = False
    db.commit()

    create_system_log(
        db,
        action="DELETE_DEPARTMENT",
        entity_type="department",
        entity_id=department.id,
        performed_by=admin.id,
        details={"name": department.name},
    )

    return {"message": "Department deactivated successfully"}


# ===================================================================
# OFFICERS
# ===================================================================

@router.get("/officers", response_model=List[OfficerResponse])
def list_officers(
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """List all officers with their user profile information."""
    results = (
        db.query(Officer, User)
        .join(User, Officer.user_id == User.id)
        .all()
    )

    officers = []
    for officer, user in results:
        officers.append(
            OfficerResponse(
                id=officer.id,
                user_id=officer.user_id,
                full_name=user.full_name,
                email=user.email,
                department=officer.department,
                designation=officer.designation,
                workload_count=officer.workload_count,
                is_available=officer.is_available,
            )
        )
    return officers


@router.post("/officers", response_model=OfficerResponse, status_code=201)
def create_officer(
    payload: OfficerCreate,
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """Create a new officer: creates a User with role='officer' and a linked Officer record."""
    # Check if email is already taken
    existing_user = db.query(User).filter(User.email == payload.email).first()
    if existing_user:
        raise HTTPException(status_code=400, detail="A user with this email already exists")

    # Create the User record
    user_id = generate_uuid()
    new_user = User(
        id=user_id,
        full_name=payload.full_name,
        email=payload.email,
        phone_number=payload.phone_number,
        password_hash=hash_password(payload.password),
        role="officer",
        is_verified=True,
    )
    db.add(new_user)
    db.flush()  # Flush to get user id before creating officer

    # Create the Officer record
    officer_id = generate_uuid()
    new_officer = Officer(
        id=officer_id,
        user_id=user_id,
        department=payload.department,
        designation=payload.designation,
        workload_count=0,
        is_available=True,
    )
    db.add(new_officer)
    db.commit()
    db.refresh(new_user)
    db.refresh(new_officer)

    create_system_log(
        db,
        action="CREATE_OFFICER",
        entity_type="officer",
        entity_id=officer_id,
        performed_by=admin.id,
        details={"full_name": payload.full_name, "email": payload.email},
    )

    return OfficerResponse(
        id=new_officer.id,
        user_id=new_officer.user_id,
        full_name=new_user.full_name,
        email=new_user.email,
        department=new_officer.department,
        designation=new_officer.designation,
        workload_count=new_officer.workload_count,
        is_available=new_officer.is_available,
    )


# ===================================================================
# SYSTEM LOGS
# ===================================================================

@router.get("/system-logs")
def list_system_logs(
    page: int = Query(1, ge=1),
    limit: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """Paginated system log listing with performer name resolution."""
    total = db.query(func.count(SystemLog.id)).scalar()
    total_pages = math.ceil(total / limit) if total > 0 else 1

    offset = (page - 1) * limit

    rows = (
        db.query(SystemLog, User.full_name)
        .outerjoin(User, SystemLog.performed_by == User.id)
        .order_by(SystemLog.created_at.desc())
        .offset(offset)
        .limit(limit)
        .all()
    )

    items = []
    for log, performed_by_name in rows:
        items.append(
            SystemLogResponse(
                id=log.id,
                action=log.action,
                entity_type=log.entity_type,
                entity_id=log.entity_id,
                performed_by=log.performed_by,
                performed_by_name=performed_by_name,
                details=log.details,
                created_at=log.created_at,
            ).model_dump()
        )

    return {
        "items": items,
        "total": total,
        "page": page,
        "limit": limit,
        "pages": total_pages,
    }


# ===================================================================
# DASHBOARD STATS
# ===================================================================

@router.get("/dashboard-stats", response_model=DashboardStats)
def get_dashboard_stats(
    db: Session = Depends(get_db),
    admin: User = Depends(require_admin),
):
    """Return aggregated dashboard statistics for the admin panel."""
    # Total counts
    total_complaints = db.query(func.count(Complaint.id)).scalar() or 0
    total_citizens = (
        db.query(func.count(User.id)).filter(User.role == "citizen").scalar() or 0
    )
    total_officers = db.query(func.count(Officer.id)).scalar() or 0

    # Status counts
    unassigned = (
        db.query(func.count(Complaint.id))
        .filter(Complaint.status == "UNASSIGNED")
        .scalar() or 0
    )
    assigned = (
        db.query(func.count(Complaint.id))
        .filter(Complaint.status == "ASSIGNED")
        .scalar() or 0
    )
    in_progress = (
        db.query(func.count(Complaint.id))
        .filter(Complaint.status == "IN_PROGRESS")
        .scalar() or 0
    )
    resolved = (
        db.query(func.count(Complaint.id))
        .filter(Complaint.status == "RESOLVED")
        .scalar() or 0
    )
    completed = (
        db.query(func.count(Complaint.id))
        .filter(Complaint.status == "COMPLETED")
        .scalar() or 0
    )

    # By category
    category_rows = (
        db.query(Complaint.category, func.count(Complaint.id))
        .group_by(Complaint.category)
        .all()
    )
    by_category = {row[0] or "Uncategorized": row[1] for row in category_rows}

    # By severity
    severity_rows = (
        db.query(Complaint.severity_level, func.count(Complaint.id))
        .group_by(Complaint.severity_level)
        .all()
    )
    by_severity = {row[0] or "UNKNOWN": row[1] for row in severity_rows}

    # Recent 7 days
    seven_days_ago = datetime.utcnow() - timedelta(days=7)
    recent_7_days = (
        db.query(func.count(Complaint.id))
        .filter(Complaint.created_at >= seven_days_ago)
        .scalar() or 0
    )

    # Resolution rate
    resolution_rate = (resolved / total_complaints) if total_complaints > 0 else 0.0

    return DashboardStats(
        total_complaints=total_complaints,
        total_citizens=total_citizens,
        total_officers=total_officers,
        unassigned=unassigned,
        assigned=assigned,
        in_progress=in_progress,
        resolved=resolved,
        completed=completed,
        by_category=by_category,
        by_severity=by_severity,
        recent_7_days=recent_7_days,
        resolution_rate=round(resolution_rate, 4),
    )
