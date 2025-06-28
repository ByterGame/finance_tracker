from fastapi import FastAPI, HTTPException, Depends
from pydantic import BaseModel, EmailStr
import random
import smtplib
from email.mime.text import MIMEText
import os
from dotenv import load_dotenv
from email.mime.text import MIMEText
import smtplib
from sqlalchemy.orm import Session
from . import models
from .database import SessionLocal, engine
from datetime import datetime

load_dotenv()

models.Base.metadata.create_all(bind=engine)

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
    

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

class EmailRequest(BaseModel):
    email: EmailStr

@app.post("/register_user")
def register_user(request: EmailRequest, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.email == request.email).first()
    if user:
        return { "message": "Пользователь уже зарегистрирован" }
    
    new_user = models.User(email=request.email)
    db.add(new_user)
    db.commit()
    return { "message": "Пользователь успешно зарегистрирован" }

class OperationRequest(BaseModel):
    userEmail: EmailStr
    type: str
    amount: float
    accountName: str
    currency: str
    category: str
    date: int
    note: str | None = None

@app.post("/save_operation")
def save_operation(request: OperationRequest, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.email == request.userEmail).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    operation = models.Operation(
        userEmail=request.userEmail,
        type=request.type,
        amount=request.amount,
        accountName=request.accountName,
        currency=request.currency,
        category=request.category,
        date=datetime.fromtimestamp(request.date / 1000.0),
        note=request.note
    )
    db.add(operation)
    db.commit()
    return {"message": "Operation saved"}


class CardRequest(BaseModel):
    userEmail: EmailStr
    name: str
    balance: float
    currency: str

@app.post("/save_card")
def save_card(request: CardRequest, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.email == request.userEmail).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    card = models.Card(
        userEmail=request.userEmail,
        name=request.name,
        balance=request.balance,
        currency=request.currency
    )
    db.add(card)
    db.commit()
    return {"message": "Card saved"}

class CategoryRequest(BaseModel):
    userEmail: EmailStr
    name: str
    color: int

@app.post("/save_category")
def save_category(request: CategoryRequest,  db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.email == request.userEmail).first()
    
    if not user:
        raise HTTPException(status_code=404, detail="User not found")
    
    category = models.Category(
        userEmail=request.userEmail,
        name=request.name,
        color=request.color
    )
    db.add(category)
    db.commit()
    return {"message": "Category saved"}
