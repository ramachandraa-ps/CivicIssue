# Officer Role Feature Design

## Date: 2026-03-18

## Summary

Add a full officer role to CivicIssue that creates a proper chain of responsibility: Admin assigns complaints to officers, officers work through them with progress updates, submit completion proof, and admin reviews/approves or requests rework.

## Design Decisions

1. **Progress updates**: Officers post custom text + optional photo updates (no fixed workflow stages)
2. **Rework status**: New `REWORK` status when admin declines officer's completion
3. **Role-based routing**: Officer portal lives in the same app, routed by `role=officer`
4. **Admin officer panel**: Full management dashboard with workload, performance stats, department filter
5. **Update visibility**: All progress updates visible to citizen, admin, and officer (full transparency)
6. **Officer scope**: Officers see only their assigned complaints (focused, no browsing unassigned)
7. **Officer dashboard**: Stats + task list + notification bell

## Status Flow

```
Citizen posts → UNASSIGNED
Admin assigns officer → ASSIGNED (notify citizen + officer)
Officer starts work → IN_PROGRESS (notify citizen)
Officer posts updates → IN_PROGRESS (visible to all)
Officer uploads proof → COMPLETED (notify admin for review)
Admin approves → RESOLVED (notify citizen + officer)
Admin declines → REWORK (notify citizen + officer with reason)
Officer resumes → IN_PROGRESS (notify citizen)
Officer re-submits → COMPLETED (back to admin review)
... cycle until approved
```

## New Data Model

### New table: `complaint_updates`

| Column | Type | Notes |
|--------|------|-------|
| id | CHAR(36) UUID | PK |
| complaint_id | CHAR(36) | FK→complaints.id |
| officer_id | CHAR(36) | FK→users.id |
| message | TEXT | Progress text |
| image_url | VARCHAR(500) | Optional photo |
| created_at | TIMESTAMP | Auto |

### Modified: `complaints.status` ENUM

Add `REWORK` to existing: `UNASSIGNED, ASSIGNED, IN_PROGRESS, COMPLETED, RESOLVED, REWORK`

## Officer Dashboard (Mobile)

- **Top stats**: Total assigned, active, completed, rework count
- **Task list**: Assigned complaints with status badges, sorted by priority
- **Notification bell**: Unread count badge (new assignments, rework requests)
- **Tap complaint**: View details, post updates, submit completion

## Officer Screens

1. **OfficerDashboardScreen** - Stats + task list + notifications
2. **OfficerComplaintDetailScreen** - Full complaint view + progress timeline + action buttons
3. **OfficerPostUpdateScreen** - Text + optional photo update form
4. **OfficerCompleteScreen** - Completion notes + proof image upload
5. **OfficerNotificationScreen** - Notification list
6. **OfficerProfileScreen** - Profile view/edit

## Admin Enhancements

1. **Officer Management Screen** - List all officers with workload, performance stats, department filter
2. **Officer Detail Screen** - Individual officer: stats (completed, rework, avg time), assigned complaints
3. **Officer Creation Screen** - Create officer account with department, designation
4. **Complaint Review Screen** - When status=COMPLETED, admin sees proof + progress timeline, approve/decline buttons
5. **Updated Dashboard** - Add officer-related stats (total officers, avg workload, rework rate)

## API Endpoints (New/Modified)

### New Endpoints

- `POST /api/complaints/{id}/updates` - Officer posts progress update
- `GET /api/complaints/{id}/updates` - Get all progress updates for complaint
- `PUT /api/complaints/{id}/complete` - Officer marks complaint as completed with proof
- `PUT /api/complaints/{id}/approve` - Admin approves completed complaint → RESOLVED
- `PUT /api/complaints/{id}/rework` - Admin declines → REWORK with reason
- `GET /api/admin/officers/{id}/stats` - Officer performance stats
- `GET /api/admin/officers/{id}/complaints` - Officer's complaint history
- `PUT /api/admin/officers/{id}` - Update officer details
- `PUT /api/admin/officers/{id}/availability` - Toggle availability

### Modified Endpoints

- `GET /api/complaints` - Add officer role filtering (show only assigned)
- `PUT /api/complaints/{id}/status` - Support REWORK status transitions
- `GET /api/complaints/{id}` - Include progress updates in response

## Notifications

| Event | Recipients | Type | Priority |
|-------|-----------|------|----------|
| Officer assigned | Citizen + Officer | STATUS_UPDATE + ASSIGNMENT | MEDIUM + HIGH |
| Progress update posted | Citizen | STATUS_UPDATE | LOW |
| Officer marks complete | Admin | STATUS_UPDATE | HIGH |
| Admin approves (RESOLVED) | Citizen + Officer | RESOLUTION | HIGH |
| Admin requests rework | Officer + Citizen | STATUS_UPDATE | HIGH |

## Citizen-Side Changes

- **ComplaintDetailScreen**: Show progress updates timeline (officer updates visible)
- **Notifications**: New notification types for progress updates and rework status
- **Status display**: Handle REWORK status display
