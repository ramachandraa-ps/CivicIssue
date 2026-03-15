from fastapi import APIRouter, Depends, Query
from middleware.auth import get_current_user
from models.user import User
from services.geocoding_service import reverse_geocode

router = APIRouter()


@router.get("/reverse")
async def reverse_geocode_endpoint(
    lat: float = Query(..., description="Latitude"),
    lng: float = Query(..., description="Longitude"),
    current_user: User = Depends(get_current_user),
):
    """Reverse geocode coordinates to a human-readable address."""
    result = await reverse_geocode(lat, lng)
    return result
