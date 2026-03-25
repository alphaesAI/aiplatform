from fastapi import FastAPI, HTTPException, Depends
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from sqlalchemy import create_engine, text
from sqlalchemy.orm import sessionmaker, Session
from datetime import datetime
from typing import Optional
import uuid
import os

app = FastAPI(title="Health Pipeline API", version="1.0.0")

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

class HealthDataRequest(BaseModel):
    pseudo_id: str
    date: str
    duration_minutes: Optional[float] = None
    activity_name: Optional[str] = None
    start_time: Optional[str] = None
    end_time: Optional[str] = None
    avg_hr_bpm: Optional[int] = None
    max_hr_bpm: Optional[int] = None
    elevation_gain_m: Optional[float] = None
    distance_meters: Optional[float] = None
    calories_kcal: Optional[float] = None
    steps: Optional[int] = None
    active_zone_minutes: Optional[int] = None
    speed_mps: Optional[float] = None
    resting_hr_mins: Optional[int] = None
    fat_burn_mins: Optional[int] = None
    cardio_mins: Optional[int] = None
    peak_mins: Optional[int] = None
    sleep_hours: Optional[float] = None
    sleep_quality_score: Optional[int] = None
    sleep_efficiency: Optional[int] = None

class HealthDataResponse(BaseModel):
    queue_id: str
    status: str
    message: str
    created_at: str

@app.post("/api/health/queue", response_model=HealthDataResponse)
async def queue_health_data(data: HealthDataRequest, db: Session = Depends(get_db)):
    try:
        queue_id = str(uuid.uuid4())
        
        query = text("""
            INSERT INTO health_data_queue (
                queue_id, pseudo_id, date, duration_minutes, activity_name,
                start_time, end_time, avg_hr_bpm, max_hr_bpm, elevation_gain_m,
                distance_meters, calories_kcal, steps, active_zone_minutes,
                speed_mps, status,
                -- ADD THESE NEW COLUMNS --
                resting_hr_mins, fat_burn_mins, cardio_mins, peak_mins,
                sleep_hours, sleep_quality_score, sleep_efficiency
            ) VALUES (
                :queue_id, :pseudo_id, :date, :duration_minutes, :activity_name,
                :start_time, :end_time, :avg_hr_bpm, :max_hr_bpm, :elevation_gain_m,
                :distance_meters, :calories_kcal, :steps, :active_zone_minutes,
                :speed_mps, 'pending',
                -- ADD THESE NEW PLACEHOLDERS --
                :resting_hr_mins, :fat_burn_mins, :cardio_mins, :peak_mins,
                :sleep_hours, :sleep_quality_score, :sleep_efficiency
            )
        """)

        
        db.execute(query, {
            "queue_id": queue_id,
            "pseudo_id": data.pseudo_id,
            "date": data.date,
            "duration_minutes": data.duration_minutes,
            "activity_name": data.activity_name,
            "start_time": data.start_time,
            "end_time": data.end_time,
            "avg_hr_bpm": data.avg_hr_bpm,
            "max_hr_bpm": data.max_hr_bpm,
            "elevation_gain_m": data.elevation_gain_m,
            "distance_meters": data.distance_meters,
            "calories_kcal": data.calories_kcal,
            "steps": data.steps,
            "active_zone_minutes": data.active_zone_minutes,
            "speed_mps": data.speed_mps,
            # --- ADD THESE NEW MAPPINGS ---
            "resting_hr_mins": data.resting_hr_mins,
            "fat_burn_mins": data.fat_burn_mins,
            "cardio_mins": data.cardio_mins,
            "peak_mins": data.peak_mins,
            "sleep_hours": data.sleep_hours,
            "sleep_quality_score": data.sleep_quality_score,
            "sleep_efficiency": data.sleep_efficiency
        })
        db.commit()
        
        return HealthDataResponse(
            queue_id=queue_id,
            status="queued",
            message="Health data queued successfully",
            created_at=datetime.now().isoformat()
        )
        
    except Exception as e:
        db.rollback()
        raise HTTPException(status_code=500, detail=str(e))

@app.get("/api/health/status/{queue_id}")
async def get_queue_status(queue_id: str, db: Session = Depends(get_db)):
    query = text("""
        SELECT queue_id, status, created_at, updated_at, error_message
        FROM health_data_queue
        WHERE queue_id = :queue_id
    """)
    
    result = db.execute(query, {"queue_id": queue_id}).fetchone()
    
    if not result:
        raise HTTPException(status_code=404, detail="Queue ID not found")
    
    return {
        "queue_id": str(result[0]),
        "status": result[1],
        "created_at": result[2].isoformat(),
        "updated_at": result[3].isoformat(),
        "error_message": result[4]
    }

@app.get("/health")
async def health_check():
    return {"status": "healthy", "service": "health-pipeline-api"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
