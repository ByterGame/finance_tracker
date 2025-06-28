from sqlalchemy import Column, String, Float, Integer, DateTime, ForeignKey
from sqlalchemy.ext.declarative import declarative_base

Base = declarative_base()

class User(Base):
    __tablename__ = "users"
    email = Column(String, primary_key=True, unique=True)


class Operation(Base):
    __tablename__ = "operations"

    id = Column(Integer, primary_key=True, index=True)
    userEmail = Column(String, ForeignKey("users.email"))
    type = Column(String)
    amount = Column(Float)
    accountName = Column(String)
    currency = Column(String)
    category = Column(String)
    date = Column(DateTime)
    note = Column(String, nullable=True)

class Card(Base):
    __tablename__ = "cards"

    id = Column(Integer, primary_key=True, index=True)
    userEmail = Column(String, ForeignKey("users.email"))
    name = Column(String)
    balance = Column(Float)
    currency = Column(String)
    
class Category(Base):
    __tablename__ = "categories"

    id = Column(Integer, primary_key=True, index=True)
    userEmail = Column(String, ForeignKey("users.email"))
    name = Column(String)
    color = Column(Integer)
