from fastapi import FastAPI, HTTPException
from pydantic import BaseModel, EmailStr
import random
import smtplib
from email.mime.text import MIMEText
import os
from dotenv import load_dotenv
from email.mime.text import MIMEText
import smtplib

load_dotenv()

app = FastAPI()

email_codes = {}

class EmailRequest(BaseModel):
    email: EmailStr

class CodeVerificationRequest(BaseModel):
    email: EmailStr
    code: str

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

@app.post("/send_code")
def send_code(request: EmailRequest):
    code = str(random.randint(100000, 999999))
    email_codes[request.email] = code

    try:
        send_email(request.email, code)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to send email: {e}")

    return {"message": "Code sent"}

@app.post("/verify_code")
def verify_code(request: CodeVerificationRequest):
    if email_codes.get(request.email) == request.code:
        return {"message": "Verified"}
    else:
        raise HTTPException(status_code=400, detail="Invalid code")
