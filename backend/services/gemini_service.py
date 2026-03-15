import json
import logging
import google.generativeai as genai
from config import settings

logger = logging.getLogger(__name__)

genai.configure(api_key=settings.GEMINI_API_KEY)
model = genai.GenerativeModel("gemini-2.5-flash")

IMAGE_ANALYSIS_PROMPT = (
    "You are a civic issue analysis AI. Analyze this image of a civic/infrastructure issue.\n"
    "Return ONLY valid JSON (no markdown, no code blocks) with these fields:\n"
    '- "category": one of ["pothole", "garbage", "street_light", "water_leakage", '
    '"drainage", "road_damage", "broken_infrastructure", "other"]\n'
    '- "severity": one of ["LOW", "MEDIUM", "HIGH", "CRITICAL"]\n'
    '- "confidence": float between 0.0 and 1.0\n'
    '- "tags": list of 3-5 descriptive keyword strings\n'
    '- "description": one-line summary of what you see'
)

TEXT_ANALYSIS_PROMPT_TEMPLATE = (
    "You are a civic issue text analyzer. Analyze this citizen's issue description "
    "and extract structured information.\n"
    'Description: "{description}"\n'
    "Return ONLY valid JSON (no markdown, no code blocks) with these fields:\n"
    '- "category": one of ["pothole", "garbage", "street_light", "water_leakage", '
    '"drainage", "road_damage", "broken_infrastructure", "other"]\n'
    '- "keywords": list of relevant keywords extracted from the text\n'
    '- "suggested_priority": one of ["LOW", "MEDIUM", "HIGH"]\n'
    '- "urgency_indicator": brief reason for the suggested priority level'
)

CHATBOT_SYSTEM_PROMPT_TEMPLATE = (
    "You are CivicBot, an AI assistant for the CivicIssue app — a civic issue "
    "reporting platform.\n"
    "User role: {user_role}\n"
    "For citizens: Help with reporting issues, explain the process, answer FAQs "
    "about complaint status.\n"
    "For admins: Help with prioritization, summarize complaint trends, provide "
    "management recommendations.\n"
    "Keep responses concise and helpful. {context}"
)

DEFAULT_IMAGE_ANALYSIS = {
    "category": "other",
    "severity": "MEDIUM",
    "confidence": 0.5,
    "tags": [],
    "description": "Unable to analyze image",
}

DEFAULT_TEXT_ANALYSIS = {
    "category": "other",
    "keywords": [],
    "suggested_priority": "MEDIUM",
    "urgency_indicator": "Could not determine urgency from text",
}


def _parse_json_response(text: str) -> dict:
    """Parse JSON from Gemini response, stripping markdown code fences if present."""
    cleaned = text.strip()
    if cleaned.startswith("```"):
        # Remove opening fence (with optional language tag) and closing fence
        lines = cleaned.split("\n")
        # Drop first line (```json or ```) and last line (```)
        lines = lines[1:]
        if lines and lines[-1].strip() == "```":
            lines = lines[:-1]
        cleaned = "\n".join(lines).strip()
    return json.loads(cleaned)


async def analyze_image(image_path: str) -> dict:
    """Upload an image to Gemini and return structured civic issue analysis."""
    try:
        uploaded_file = genai.upload_file(image_path)
        response = model.generate_content([IMAGE_ANALYSIS_PROMPT, uploaded_file])
        result = _parse_json_response(response.text)
        return result
    except json.JSONDecodeError as e:
        logger.error("Failed to parse Gemini image analysis JSON: %s", e)
        return dict(DEFAULT_IMAGE_ANALYSIS)
    except Exception as e:
        logger.error("Gemini image analysis failed: %s", e)
        return dict(DEFAULT_IMAGE_ANALYSIS)


async def analyze_text(description: str) -> dict:
    """Analyze a citizen's textual issue description and return structured data."""
    try:
        prompt = TEXT_ANALYSIS_PROMPT_TEMPLATE.format(description=description)
        response = model.generate_content(prompt)
        result = _parse_json_response(response.text)
        return result
    except json.JSONDecodeError as e:
        logger.error("Failed to parse Gemini text analysis JSON: %s", e)
        return dict(DEFAULT_TEXT_ANALYSIS)
    except Exception as e:
        logger.error("Gemini text analysis failed: %s", e)
        return dict(DEFAULT_TEXT_ANALYSIS)


async def chat(
    message: str, history: list, user_role: str, context: str = ""
) -> str:
    """Conduct a chatbot conversation with Gemini, incorporating history and role."""
    try:
        system_prompt = CHATBOT_SYSTEM_PROMPT_TEMPLATE.format(
            user_role=user_role, context=context
        )

        # Build the conversation as a single prompt with history
        conversation_parts = [system_prompt, ""]

        for entry in history:
            role_label = "User" if entry.get("is_user") else "Assistant"
            conversation_parts.append(f"{role_label}: {entry.get('text', '')}")

        conversation_parts.append(f"User: {message}")
        conversation_parts.append("Assistant:")

        full_prompt = "\n".join(conversation_parts)
        response = model.generate_content(full_prompt)
        return response.text
    except Exception as e:
        logger.error("Gemini chat failed: %s", e)
        return "I'm sorry, I'm having trouble processing your request right now. Please try again later."
