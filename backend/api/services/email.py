import os
import smtplib
from email.mime.text import MIMEText
from dotenv import load_dotenv


load_dotenv()


def send_email(to_email: str, code: str):
    smtp_server = "smtp.gmail.com"
    smtp_port = 587
    sender_email = os.getenv("SMTP_EMAIL")
    sender_password = os.getenv("SMTP_PASSWORD")

    msg = MIMEText(f"<b>Your code:</b> {code}", "html")
    msg["Subject"] = "Verification Code"
    msg["From"] = sender_email
    msg["To"] = to_email

    try:
        with smtplib.SMTP(smtp_server, smtp_port) as server:
            server.starttls()
            server.login(sender_email, sender_password)
            server.send_message(msg)
    except smtplib.SMTPException as e:
        print("Ошибка отправки письма:", e)