import requests
import json
from datetime import datetime

# VITALIS Deep-Dive Test Payload (37 Metrics)
test_payload = {
    "pseudo_id": "VITAL_USER_98765",
    "pseudo_id2": "VITAL_USER_98765_SEC",
    "date": "2026-04-28",
    "datetime": datetime.now().isoformat(),
    
    # Activity
    "activity_name": "Evening Trail Run",
    "duration_minutes": 45.5,
    "start_time": "2026-04-28T18:00:00",
    "end_time": "2026-04-28T18:45:30",
    "avg_hr_bpm": 152,
    "max_hr_bpm": 178,
    "elevation_gain_m": 120.5,
    "distance_meters": 6500.0,
    "calories_kcal": 550.0,
    "steps": 8432,
    "speed_mps": 2.38,
    
    # Active Zones
    "active_zone_minutes": 45,
    "fatburn_active_zone_minutes": 22,
    "cardio_active_zone_minutes": 18,
    "peak_active_zone_minutes": 5,
    
    # Biometrics
    "age": 32,
    "gender": "Non-Binary",
    "weight_kg": 74.0,
    "height_cm": 178.0,
    
    # Vitals
    "resting_heart_rate": 62,
    "heart_rate_variability": 55.0,
    "stress_management_score": 78,
    
    # Sleep
    "sleep_minutes": 435,
    "rem_sleep_minutes": 90,
    "deep_sleep_minutes": 105,
    "awake_minutes": 15,
    "light_sleep_minutes": 225,
    "bed_time": "2026-04-27T22:30:00",
    "wake_up_time": "2026-04-28T06:45:00",
    
    # Sleep Percentages
    "deep_sleep_percent": 24.1,
    "rem_sleep_percent": 20.7,
    "awake_percent": 3.4,
    "light_sleep_percent": 51.7
}

def run_test():
    url = "http://localhost:8000/api/health/queue"
    print(f"🚀 Sending 37-metric payload to {url}...")
    
    try:
        response = requests.post(
            url, 
            json=test_payload,
            headers={"Content-Type": "application/json"}
        )
        
        print(f"Status Code: {response.status_code}")
        if response.status_code == 200:
            print("✅ Success! Payload accepted.")
            print(json.dumps(response.json(), indent=2))
        else:
            print(f"❌ Failed! Error: {response.text}")
            
    except Exception as e:
        print(f"❌ Connection Error: {e}")

if __name__ == "__main__":
    run_test()
