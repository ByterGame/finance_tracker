import random
from datetime import datetime
import logging

from fastapi import FastAPI, HTTPException, Depends, Request, Query
from fastapi.responses import JSONResponse
from sqlalchemy.orm import Session
from pydantic import EmailStr

from . import models
from .database import SessionLocal, engine
from . import schemas
from .services import email


models.Base.metadata.create_all(bind=engine)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI()

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


email_codes = {}

@app.post("/send_code")
def send_code(request: schemas.EmailRequest):
    code = str(random.randint(100000, 999999))
    email_codes[request.email] = code

    try:
        email.send_email(request.email, code)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to send email: {e}")

    return {"message": "Code sent"}


@app.post("/verify_code")
def verify_code(request: schemas.CodeVerificationRequest):
    if email_codes.get(request.email) == request.code:
        return {"message": "Verified"}
    else:
        raise HTTPException(status_code=400, detail="Invalid code")   
    

@app.post("/register_user")
def register_user(request: schemas.EmailRequest, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.email == request.email).first()
    if user:
        return JSONResponse(
            status_code=409,
            content={ "message": "Пользователь уже зарегистрирован" }
        )
    
    new_user = models.User(email=request.email)
    db.add(new_user)
    db.commit()
    return { "message": "Пользователь успешно зарегистрирован" }


@app.post("/save_operation")
def save_operation(request: schemas.OperationRequest, db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.email == request.userEmail).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    card = db.query(models.Card).filter(
        models.Card.userEmail == request.userEmail,
        models.Card.name == request.accountName
    ).first()

    if not card:
        raise HTTPException(status_code=404, detail="Card not found")

    if request.type == "Income":
        card.balance += request.amount
    elif request.type == "Expense":
        card.balance -= request.amount
    else:
        raise HTTPException(status_code=400, detail="Invalid operation type")
    
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


@app.post("/save_card")
def save_card(request: schemas.CardRequest, db: Session = Depends(get_db)):
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


@app.post("/save_category")
def save_category(request: schemas.CategoryRequest,  db: Session = Depends(get_db)):
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


@app.post("/delete_user_data")
def delete_user_data(request: schemas.EmailRequest, db: Session = Depends(get_db)):
    print(f"Received delete request for {request.email}")
    user = db.query(models.User).filter(models.User.email == request.email).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    db.query(models.Operation).filter(models.Operation.userEmail == request.email).delete()
    db.query(models.Card).filter(models.Card.userEmail == request.email).delete()
    db.query(models.Category).filter(models.Category.userEmail == request.email).delete()

    db.commit()

    return {"message": "User data deleted successfully"}


@app.get("/get_user_data", response_model=schemas.UserDataResponse)
def get_user_data(email: EmailStr = Query(...), db: Session = Depends(get_db)):
    user = db.query(models.User).filter(models.User.email == email).first()
    if not user:
        raise HTTPException(status_code=404, detail="User not found")

    cards = db.query(models.Card).filter(models.Card.userEmail == email).all()
    categories = db.query(models.Category).filter(models.Category.userEmail == email).all()
    operations = db.query(models.Operation).filter(models.Operation.userEmail == email).all()
    data = {
        "cards": [
            {"name": c.name, "balance": c.balance, "currency": c.currency} for c in cards
        ],
        "categories": [
            {"name": cat.name, "color": cat.color} for cat in categories
        ],
        "operations": [
            {
                "type": op.type,
                "amount": op.amount,
                "accountName": op.accountName,
                "currency": op.currency,
                "category": op.category,
                "date": int(op.date.timestamp() * 1000),
                "note": op.note
            } for op in operations
        ]
    }
    return data


@app.middleware("http")
async def log_requests(request: Request, call_next):
    logger.info(f"Incoming request: {request.method} {request.url}")
    response = await call_next(request)
    return response
    