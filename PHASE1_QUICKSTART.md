# PHASE 1: INTEGRATION - QUICK START GUIDE

## 🎯 What We Built

✅ PostgreSQL database with health_data_queue table  
✅ FastAPI server (bridge between Android and database)  
✅ Android data mapper (Health Connect → Queue schema)  
✅ Updated CloudSyncService (uses new API)  
✅ Docker compose with all services  
✅ Integration test script  

---

## 🚀 HOW TO RUN

### Step 1: Start All Services

```bash
cd /home/mi/Desktop/AI_projects/PHIA_app

# Start Docker services
docker-compose up -d

# Wait for services to start (30 seconds)
sleep 30

# Check status
docker-compose ps
```

**Expected output:**
```
NAME                          STATUS
health-pipeline-api           Up
health-pipeline-postgres      Up (healthy)
health-pipeline-elasticsearch Up (healthy)
health-pipeline-kibana        Up
health-pipeline-redis         Up
```

### Step 2: Verify Services

```bash
# Check PostgreSQL
docker exec health-pipeline-postgres pg_isready -U health_user -d health_pipeline

# Check API
curl http://localhost:8000/health

# Check Elasticsearch
curl http://localhost:9200/_cluster/health
```

### Step 3: Run Integration Test

```bash
./test_integration.sh
```

This will:
- ✅ Check all services are running
- ✅ Send test health data to API
- ✅ Verify data in PostgreSQL
- ✅ Check queue status

### Step 4: Build & Run Android App

```bash
cd /home/mi/Desktop/AI_projects/PHIA_app

# Build APK
./gradlew assembleDebug

# Install on device/emulator
./gradlew installDebug
```

**In the app:**
1. Grant Health Connect permissions
2. Tap "Sync Data" button
3. Check logs for success message

### Step 5: Verify Data Flow

```bash
# Check data in PostgreSQL
docker exec -it health-pipeline-postgres psql -U health_user -d health_pipeline

# In psql:
SELECT * FROM health_data_queue ORDER BY created_at DESC LIMIT 5;

# Exit psql
\q
```

### Step 6: Run Pipeline (Airflow)

```bash
cd /home/mi/Desktop/AI_projects/aiplatform

# The pipeline DAG will run hourly automatically
# Or trigger manually if you have Airflow running
```

### Step 7: Check Elasticsearch

```bash
# Wait for pipeline to process (up to 1 hour or manual trigger)

# Check if data is indexed
curl http://localhost:9200/health_queue_sessions/_search?pretty

# Or use Kibana
# Open: http://localhost:5601
```

---

## 📊 ARCHITECTURE FLOW

```
┌─────────────────┐
│  Android App    │
│  (Health Data)  │
└────────┬────────┘
         │ POST /api/health/queue
         ▼
┌─────────────────┐
│  FastAPI Server │ ✅ NEW!
│  localhost:8000 │
└────────┬────────┘
         │ INSERT
         ▼
┌─────────────────┐
│   PostgreSQL    │ ✅ NEW!
│  health_data_   │
│     queue       │
└────────┬────────┘
         │ Airflow DAG
         ▼
┌─────────────────┐
│  ETL Pipeline   │ ✅ Existing
│  (aiplatform)   │
└────────┬────────┘
         │ Bulk Index
         ▼
┌─────────────────┐
│ Elasticsearch   │ ✅ Existing
│  localhost:9200 │
└─────────────────┘
```

---

## 🔧 TROUBLESHOOTING

### Problem: API server not starting

```bash
# Check logs
docker-compose logs api

# Rebuild
docker-compose build api
docker-compose up -d api
```

### Problem: PostgreSQL connection failed

```bash
# Check PostgreSQL logs
docker-compose logs postgres

# Restart PostgreSQL
docker-compose restart postgres

# Verify schema
docker exec -it health-pipeline-postgres psql -U health_user -d health_pipeline -c "\dt"
```

### Problem: Android app can't connect

**For Emulator:**
- Use `http://10.0.2.2:8000` in ApiClient.kt

**For Physical Device:**
- Find your computer's IP: `ip addr show` or `ifconfig`
- Use `http://YOUR_IP:8000` in ApiClient.kt
- Make sure device and computer are on same network

### Problem: No data in queue

```bash
# Check API logs
docker-compose logs api

# Test API manually
curl -X POST http://localhost:8000/api/health/queue \
  -H "Content-Type: application/json" \
  -d '{"pseudo_id":"test","date":"2026-03-21","steps":1000}'

# Check database
docker exec -it health-pipeline-postgres psql -U health_user -d health_pipeline
SELECT * FROM health_data_queue;
```

---

## 📝 FILES CREATED

### Backend (API Server)
- `/home/mi/Desktop/AI_projects/PHIA_app/api_server/main.py` - FastAPI server
- `/home/mi/Desktop/AI_projects/PHIA_app/api_server/requirements.txt` - Python dependencies
- `/home/mi/Desktop/AI_projects/PHIA_app/api_server/Dockerfile` - Docker image
- `/home/mi/Desktop/AI_projects/PHIA_app/schema.sql` - Database schema
- `/home/mi/Desktop/AI_projects/PHIA_app/docker-compose.yml` - Updated with PostgreSQL & API

### Android App
- `app/src/main/java/com/healthpipeline/data/models/QueueModels.kt` - Queue data models
- `app/src/main/java/com/healthpipeline/data/mappers/HealthDataMapper.kt` - Data mapper
- `app/src/main/java/com/healthpipeline/data/ApiService.kt` - Updated with queue endpoints
- `app/src/main/java/com/healthpipeline/CloudSyncService.kt` - Updated sync logic

### Testing
- `/home/mi/Desktop/AI_projects/PHIA_app/test_integration.sh` - Integration test script

---

## ✅ SUCCESS CRITERIA

- [ ] All Docker services running
- [ ] API health check returns "healthy"
- [ ] PostgreSQL accepts connections
- [ ] Test data inserted via API
- [ ] Data visible in PostgreSQL queue table
- [ ] Android app syncs successfully
- [ ] Pipeline processes queue data
- [ ] Data appears in Elasticsearch

---

## 🎯 NEXT STEPS (Phase 2)

Once Phase 1 is working:

1. **Refactor MainActivity.kt** (68KB → multiple files)
2. **Implement MVVM architecture**
3. **Create ViewModels**
4. **Create Repository layer**
5. **Separate concerns**

See `/home/mi/3_IMPLEMENTATION_PHASES.md` for details.

---

## 📞 NEED HELP?

Check the logs:
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f api
docker-compose logs -f postgres

# Android app logs
adb logcat | grep "CloudSyncService"
```

---

**Ready to test! Run `./test_integration.sh` to verify everything works.**
