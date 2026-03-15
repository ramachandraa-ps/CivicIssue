import math
from collections import Counter
from datetime import datetime, timedelta
from sqlalchemy.orm import Session
from models.complaint import Complaint
from models.issue_group import IssueGroup
from utils.helpers import generate_uuid

EARTH_RADIUS_KM = 6371
MAX_DISTANCE_KM = 2


def haversine(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    """Returns distance in km between two lat/lng points."""
    dlat = math.radians(lat2 - lat1)
    dlon = math.radians(lon2 - lon1)
    a = (
        math.sin(dlat / 2) ** 2
        + math.cos(math.radians(lat1))
        * math.cos(math.radians(lat2))
        * math.sin(dlon / 2) ** 2
    )
    return 2 * EARTH_RADIUS_KM * math.asin(math.sqrt(a))


def _most_common_severity(complaints: list[Complaint]) -> str:
    """Return the most common severity_level among a list of complaints."""
    severity_counts = Counter(c.severity_level for c in complaints if c.severity_level)
    if not severity_counts:
        return "MEDIUM"
    return severity_counts.most_common(1)[0][0]


def _compute_center(complaints: list[Complaint]) -> tuple[float, float]:
    """Compute average center latitude and longitude for a list of complaints."""
    avg_lat = sum(c.latitude for c in complaints) / len(complaints)
    avg_lng = sum(c.longitude for c in complaints) / len(complaints)
    return avg_lat, avg_lng


def check_and_group(db: Session, complaint: Complaint) -> None:
    """Check if the given complaint can be grouped with nearby complaints.

    This function should be called after complaint creation.  It does NOT
    commit the transaction -- the caller is responsible for committing.

    Algorithm:
    1. Find candidate complaints with the same category, not completed/resolved,
       created in the last 30 days, and with a different ID.
    2. Filter candidates to those within 2 km using haversine distance.
    3. If no nearby complaints exist, return (the complaint stays ungrouped).
    4. If any nearby complaint already belongs to a group, add the new complaint
       to that group.  Otherwise, create a new group for all of them.
    5. Recalculate group center and average severity.
    """
    thirty_days_ago = datetime.utcnow() - timedelta(days=30)

    # Step 1: Find candidate complaints -- same category, active, recent
    candidates = (
        db.query(Complaint)
        .filter(
            Complaint.category == complaint.category,
            Complaint.id != complaint.id,
            Complaint.status.notin_(["COMPLETED", "RESOLVED"]),
            Complaint.created_at >= thirty_days_ago,
        )
        .all()
    )

    # Step 2: Filter to those within MAX_DISTANCE_KM
    nearby: list[Complaint] = []
    for candidate in candidates:
        dist = haversine(
            complaint.latitude,
            complaint.longitude,
            candidate.latitude,
            candidate.longitude,
        )
        if dist <= MAX_DISTANCE_KM:
            nearby.append(candidate)

    # Step 3: No nearby complaints -- nothing to group
    if not nearby:
        return

    # Step 4: Check if any nearby complaint already has a group
    existing_group_id = None
    for c in nearby:
        if c.group_id:
            existing_group_id = c.group_id
            break

    if existing_group_id:
        # Add the new complaint to the existing group
        group = db.query(IssueGroup).filter(IssueGroup.id == existing_group_id).first()
        if group is None:
            return

        complaint.group_id = existing_group_id

        # Gather all complaints now in the group (including the new one)
        all_grouped = (
            db.query(Complaint)
            .filter(Complaint.group_id == existing_group_id)
            .all()
        )
        # The new complaint might not be flushed yet with group_id, so ensure
        # it is in the list
        grouped_ids = {c.id for c in all_grouped}
        if complaint.id not in grouped_ids:
            all_grouped.append(complaint)

        # Recalculate center
        center_lat, center_lng = _compute_center(all_grouped)
        group.center_lat = center_lat
        group.center_lng = center_lng
        group.complaint_count = len(all_grouped)
        group.avg_severity = _most_common_severity(all_grouped)
    else:
        # Create a new group for the new complaint + all nearby
        all_to_group = nearby + [complaint]

        center_lat, center_lng = _compute_center(all_to_group)

        group = IssueGroup(
            id=generate_uuid(),
            category=complaint.category,
            center_lat=center_lat,
            center_lng=center_lng,
            radius_meters=2000,
            complaint_count=len(all_to_group),
            avg_severity=_most_common_severity(all_to_group),
            status="ACTIVE",
        )
        db.add(group)

        # Assign group_id to all complaints
        for c in all_to_group:
            c.group_id = group.id

    # Step 6: Flush (don't commit -- caller will commit)
    db.flush()
