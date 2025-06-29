from pydantic import BaseModel, EmailStr
from typing import List, Optional


class EmailRequest(BaseModel):
    email: EmailStr

class CodeVerificationRequest(BaseModel):
    email: EmailStr
    code: str

class OperationRequest(BaseModel):
    userEmail: EmailStr
    type: str
    amount: float
    accountName: str
    currency: str
    category: str
    date: int
    note: str | None = None

class CardRequest(BaseModel):
    userEmail: EmailStr
    name: str
    balance: float
    currency: str

class CategoryRequest(BaseModel):
    userEmail: EmailStr
    name: str
    color: int

class OperationOut(BaseModel):
    type: str
    amount: float
    accountName: str
    currency: str
    category: str
    date: int
    note: Optional[str]

class CardOut(BaseModel):
    name: str
    balance: float
    currency: str

class CategoryOut(BaseModel):
    name: str
    color: int

class UserDataResponse(BaseModel):
    cards: List[CardOut]
    categories: List[CategoryOut]
    operations: List[OperationOut]