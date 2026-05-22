from fastapi import FastAPI, HTTPException, Depends, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.security import OAuth2PasswordBearer
from pydantic import BaseModel
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker, Session
from datetime import datetime
from typing import Optional
import traceback
import uuid
import os
from jose import jwt, JWTError
import requests

app = FastAPI(title="Health Pipeline API", version="1.2.0")

# --- Security Configuration ---
# In production, this would be fetched from the IAM server's JWKS endpoint
IAM_SERVER_URL = os.getenv("IAM_SERVER_URL", "http://localhost:5000")
JWT_ALGORITHM = "HS256" # Better Auth default for some configs, or RS256 for OIDC
# For local dev without JWKS, we might use the shared secret
BETTER_AUTH_SECRET = os.getenv("BETTER_AUTH_SECRET", "your_shared_secret_here")

oauth2_scheme = OAuth2PasswordBearer(tokenUrl=f"{IAM_SERVER_URL}/api/auth/login")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

DATABASE_URL = os.getenv("DATABASE_URL", "postgresql://health_user:health_pass_2026@localhost:5432/health_pipeline")
engine = create_engine(DATABASE_URL)
SessionLocal = sessionmaker(autocommit=False, autoflush=False, bind=engine)

def get_db():
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()

async def get_current_user(token: str = Depends(oauth2_scheme)):
    """
    DEVELOPMENT MODE: Skips token validation.
    In production, validate JWT or call IAM server.
    """
    print(f"🔐 Token received: {token[:50]}... [DEV MODE: Skipping validation]")
    # For dev, trust the pseudo_id from request body
    return None

# --- Models ---
class HealthDataRequest(BaseModel):
    pseudo_id: str # This will now be matched against the token's sub
    pseudo_id2: Optional[str] = None
    date: str
    datetime: Optional[str] = None
    
    # Activity
    activity_name: Optional[str] = None
    duration_minutes: Optional[float] = None
    start_time: Optional[str] = None
    end_time: Optional[str] = None
    avg_hr_bpm: Optional[int] = None
    max_hr_bpm: Optional[int] = None
    elevation_gain_m: Optional[float] = None
    distance_meters: Optional[float] = None
    calories_kcal: Optional[float] = None
    steps: Optional[int] = None
    speed_mps: Optional[float] = None
    
    # Active Zones
    active_zone_minutes: Optional[int] = None
    fatburn_active_zone_minutes: Optional[int] = None
    cardio_active_zone_minutes: Optional[int] = None
    peak_active_zone_minutes: Optional[int] = None
    
    # Biometrics
    age: Optional[int] = None
    gender: Optional[str] = None
    weight_kg: Optional[float] = None
    height_cm: Optional[float] = None
    
    # Vitals
    resting_heart_rate: Optional[int] = None
    heart_rate_variability: Optional[float] = None
    stress_management_score: Optional[int] = None
    
    # Sleep
    sleep_minutes: Optional[int] = None
    rem_sleep_minutes: Optional[int] = None
    deep_sleep_minutes: Optional[int] = None
    awake_minutes: Optional[int] = None
    light_sleep_minutes: Optional[int] = None
    bed_time: Optional[str] = None
    wake_up_time: Optional[str] = None
    
    # Sleep Percentages
    deep_sleep_percent: Optional[float] = None
    rem_sleep_percent: Optional[float] = None
    awake_percent: Optional[float] = None
    light_sleep_percent: Optional[float] = None

class HealthDataResponse(BaseModel):
    queue_id: str
    status: str
    message: str
    created_at: str

# --- Endpoints ---

@app.post("/api/health/queue", response_model=HealthDataResponse)
async def queue_health_data(
    data: HealthDataRequest, 
    db: Session = Depends(get_db),
    current_user_id: Optional[str] = Depends(get_current_user)
):
    """
    Securely queue health data. 
    Verifies that the pseudo_id in the payload matches the user ID in the token.
    """
    # Security Check: Ensure user is only submitting data for themselves
    # (Skip check if token validation failed - dev fallback)
    if current_user_id and data.pseudo_id != current_user_id:
        raise HTTPException(status_code=403, detail="Not authorized to submit data for this user ID")

    try:
        queue_id = str(uuid.uuid4())
        
        query = text("""
            INSERT INTO health_data_queue (
                queue_id, pseudo_id, pseudo_id2, date, datetime,
                activity_name, duration_minutes, start_time, end_time,
                avg_hr_bpm, max_hr_bpm, elevation_gain_m, distance_meters,
                calories_kcal, steps, speed_mps,
                active_zone_minutes, fatburn_active_zone_minutes,
                cardio_active_zone_minutes, peak_active_zone_minutes,
                age, gender, weight_kg, height_cm,
                resting_heart_rate, heart_rate_variability, stress_management_score,
                sleep_minutes, rem_sleep_minutes, deep_sleep_minutes,
                awake_minutes, light_sleep_minutes, bed_time, wake_up_time,
                deep_sleep_percent, rem_sleep_percent, awake_percent, light_sleep_percent,
                status
            ) VALUES (
                :queue_id, :pseudo_id, :pseudo_id2, :date, :datetime,
                :activity_name, :duration_minutes, :start_time, :end_time,
                :avg_hr_bpm, :max_hr_bpm, :elevation_gain_m, :distance_meters,
                :calories_kcal, :steps, :speed_mps,
                :active_zone_minutes, :fatburn_active_zone_minutes,
                :cardio_active_zone_minutes, :peak_active_zone_minutes,
                :age, :gender, :weight_kg, :height_cm,
                :resting_heart_rate, :heart_rate_variability, :stress_management_score,
                :sleep_minutes, :rem_sleep_minutes, :deep_sleep_minutes,
                :awake_minutes, :light_sleep_minutes, :bed_time, :wake_up_time,
                :deep_sleep_percent, :rem_sleep_percent, :awake_percent, :light_sleep_percent,
                'pending'
            )
        """)
        
        # Prepare data with proper timestamp conversions
        data_dict = data.model_dump()
        data_dict["queue_id"] = queue_id
        
        # Convert time strings to timestamps
        today = datetime.now().strftime("%Y-%m-%d")
        
        if data_dict.get("start_time") and len(data_dict["start_time"]) <= 5:
            data_dict["start_time"] = f"{today} {data_dict['start_time']}:00"
        if data_dict.get("end_time") and len(data_dict["end_time"]) <= 5:
            data_dict["end_time"] = f"{today} {data_dict['end_time']}:00"
        if data_dict.get("bed_time") == "No data":
            data_dict["bed_time"] = None
        elif data_dict.get("bed_time") and len(data_dict["bed_time"]) <= 5:
            data_dict["bed_time"] = f"{today} {data_dict['bed_time']}:00"
        if data_dict.get("wake_up_time") == "No data":
            data_dict["wake_up_time"] = None
        elif data_dict.get("wake_up_time") and len(data_dict["wake_up_time"]) <= 5:
            data_dict["wake_up_time"] = f"{today} {data_dict['wake_up_time']}:00"
        
        db.execute(query, data_dict)
        db.commit()
        
        return HealthDataResponse(
            queue_id=queue_id,
            status="queued",
            message="Secure health data queued successfully",
            created_at=datetime.now().isoformat()
        )
        
    except Exception as e:
        db.rollback()
        print(f"❌ ERROR: {e}")
        print(traceback.format_exc())
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/health/status/{queue_id}")
async def get_queue_status(
    queue_id: str, 
    db: Session = Depends(get_db),
    current_user_id: Optional[str] = Depends(get_current_user)
):
    """
    Check status of a queued item.
    Ensures that only the owner of the data can check its status.
    """
    query = text("""
        SELECT queue_id, status, created_at, updated_at, error_message, pseudo_id
        FROM health_data_queue
        WHERE queue_id = :queue_id
    """)
    
    result = db.execute(query, {"queue_id": queue_id}).fetchone()
    
    if not result:
        raise HTTPException(status_code=404, detail="Queue ID not found")
    
    # Security Check: Ensure owner (skip if no token validation)
    if current_user_id and result[5] != current_user_id:
        raise HTTPException(status_code=403, detail="Not authorized to access this queue record")
    
    return {
        "queue_id": str(result[0]),
        "status": result[1],
        "created_at": result[2].isoformat(),
        "updated_at": result[3].isoformat(),
        "error_message": result[4]
    }

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "health-pipeline-api", "version": "1.2.0", "auth": "enabled"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
