import uuid
import random


def generate_uuid() -> str:
    """Generate a random UUID4 string."""
    return str(uuid.uuid4())


def generate_complaint_number() -> str:
    """Generate a complaint number in the format #CE-XXXX (random 4 digits)."""
    digits = random.randint(1000, 9999)
    return f"#CE-{digits}"
