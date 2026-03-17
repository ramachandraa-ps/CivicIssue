import smtplib
import logging
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from config import settings

logger = logging.getLogger(__name__)


def send_otp_email(to_email: str, otp_code: str, purpose: str) -> None:
    """Send an OTP email for email verification or password reset.

    Args:
        to_email: Recipient email address.
        otp_code: The 6-digit OTP code.
        purpose: Either "EMAIL_VERIFY" or "PASSWORD_RESET".
    """
    if purpose == "EMAIL_VERIFY":
        subject = "CivicIssue - Verify Your Email"
        body_heading = "Email Verification"
        body_message = "Please use the following OTP to verify your email address:"
    else:
        subject = "CivicIssue - Password Reset"
        body_heading = "Password Reset"
        body_message = "Please use the following OTP to reset your password:"

    html_body = f"""
    <html>
    <body style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto;">
        <div style="background-color: #f8f9fa; padding: 20px; border-radius: 8px;">
            <h2 style="color: #333;">{body_heading}</h2>
            <p style="color: #555;">{body_message}</p>
            <div style="background-color: #fff; padding: 15px; border-radius: 4px;
                        text-align: center; margin: 20px 0;">
                <span style="font-size: 32px; font-weight: bold; letter-spacing: 8px;
                             color: #2563eb;">{otp_code}</span>
            </div>
            <p style="color: #888; font-size: 13px;">
                This code expires in 10 minutes. If you did not request this, please
                ignore this email.
            </p>
            <hr style="border: none; border-top: 1px solid #eee; margin: 20px 0;">
            <p style="color: #aaa; font-size: 12px;">CivicIssue Team</p>
        </div>
    </body>
    </html>
    """

    msg = MIMEMultipart("alternative")
    msg["Subject"] = subject
    msg["From"] = settings.SMTP_USER
    msg["To"] = to_email
    msg.attach(MIMEText(html_body, "html"))

    try:
        with smtplib.SMTP(settings.SMTP_HOST, settings.SMTP_PORT, timeout=30) as server:
            server.starttls()
            server.login(settings.SMTP_USER, settings.SMTP_PASSWORD)
            server.sendmail(settings.SMTP_USER, to_email, msg.as_string())
        logger.info("OTP email sent to %s for purpose %s", to_email, purpose)
    except Exception as e:
        logger.error("Failed to send OTP email to %s: %s", to_email, str(e))
