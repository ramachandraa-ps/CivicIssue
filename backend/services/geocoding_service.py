import httpx
from config import settings

NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse"


async def reverse_geocode(lat: float, lng: float) -> dict:
    """Reverse geocode coordinates to human-readable address via Nominatim."""
    try:
        async with httpx.AsyncClient() as client:
            response = await client.get(
                NOMINATIM_URL,
                params={
                    "lat": lat,
                    "lon": lng,
                    "format": "json",
                    "addressdetails": 1,
                },
                headers={"User-Agent": settings.NOMINATIM_USER_AGENT},
                timeout=10.0,
            )
            response.raise_for_status()
            data = response.json()
            return {
                "display_name": data.get("display_name", ""),
                "address": data.get("address", {}),
                "lat": data.get("lat"),
                "lon": data.get("lon"),
            }
    except Exception as e:
        return {
            "display_name": f"Location ({lat}, {lng})",
            "address": {},
            "error": str(e),
        }
