from pydantic import BaseModel, EmailStr, ConfigDict
from datetime import datetime
from decimal import Decimal

# Shared strict config
STRICT_CONFIG = ConfigDict(extra="forbid")

class UserRecord(BaseModel):
    model_config = STRICT_CONFIG
    id: int
    username: str
    email: EmailStr
    created_at: datetime
    updated_at: datetime

class OrderRecord(BaseModel):
    model_config = STRICT_CONFIG
    order_id: int
    user_id: int
    order_date: datetime
    total_amount: Decimal
    status: str

class ProductRecord(BaseModel):
    model_config = STRICT_CONFIG
    id: int
    name: str
    price: Decimal
    stock: int
    created_at: datetime
    updated_at: datetime

class HealthAppSessionRecord(BaseModel):
    model_config = STRICT_CONFIG
    pseudo_id: str
    date: datetime
    duration_minutes: float
    activity_name: str
    start_time: datetime
    end_time: datetime
    avg_hr_bpm: int
    max_hr_bpm: int
    elevation_gain_m: float
    distance_meters: float
    calories_kcal: float
    steps: int
    active_zone_minutes: int
    speed_mps: float
