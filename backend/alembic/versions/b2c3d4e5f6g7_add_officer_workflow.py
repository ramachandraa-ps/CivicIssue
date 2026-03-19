"""Add officer workflow - REWORK status and complaint_updates table"""

from alembic import op
from sqlalchemy import inspect
import sqlalchemy as sa

revision = "b2c3d4e5f6g7"
down_revision = "a1b2c3d4e5f6"
branch_labels = None
depends_on = None


def upgrade():
    op.alter_column(
        "complaints",
        "status",
        existing_type=sa.Enum("UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED"),
        type_=sa.Enum("UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED", "REWORK"),
        existing_nullable=True,
    )

    bind = op.get_bind()
    insp = inspect(bind)
    if "complaint_updates" not in insp.get_table_names():
        op.create_table(
            "complaint_updates",
            sa.Column("id", sa.String(36), primary_key=True),
            sa.Column("complaint_id", sa.String(36), sa.ForeignKey("complaints.id"), nullable=False),
            sa.Column("officer_id", sa.String(36), sa.ForeignKey("users.id"), nullable=False),
            sa.Column("message", sa.Text, nullable=False),
            sa.Column("image_url", sa.String(500), nullable=True),
            sa.Column("created_at", sa.TIMESTAMP, server_default=sa.func.now()),
        )


def downgrade():
    op.drop_table("complaint_updates")
    op.alter_column(
        "complaints",
        "status",
        existing_type=sa.Enum("UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED", "REWORK"),
        type_=sa.Enum("UNASSIGNED", "ASSIGNED", "IN_PROGRESS", "COMPLETED", "RESOLVED"),
        existing_nullable=True,
    )
