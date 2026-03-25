# 🔧 QUICK FIX GUIDE - PostgreSQL Issues

## Issues Fixed:
1. ✅ Removed obsolete `version` field from docker-compose.yml
2. ✅ Fixed PostgreSQL volume mount issue
3. ✅ Created separate schema initialization script
4. ✅ Updated test script to require sudo

---

## 🚀 NEW STARTUP PROCEDURE

### Step 1: Start Services
```bash
cd /home/mi/Desktop/AI_projects/PHIA_app
sudo docker-compose up -d
```

### Step 2: Wait for PostgreSQL (30 seconds)
```bash
sleep 30
```

### Step 3: Initialize Schema
```bash
./init_schema.sh
```

### Step 4: Verify Everything Works
```bash
# Check PostgreSQL
sudo docker exec health-pipeline-postgres pg_isready -U health_user -d health_pipeline

# Check table exists
sudo docker exec health-pipeline-postgres psql -U health_user -d health_pipeline -c "\dt"

# Check API
curl http://localhost:8000/health

# Check Elasticsearch
curl http://localhost:9200/_cluster/health
```

### Step 5: Run Integration Test
```bash
sudo ./test_integration.sh
```

---

## 🐛 TROUBLESHOOTING

### If PostgreSQL still fails:

```bash
# 1. Stop everything
sudo docker-compose down -v

# 2. Remove volumes (CAUTION: deletes data)
sudo docker volume rm phia_app_postgres_data

# 3. Start fresh
sudo docker-compose up -d

# 4. Wait 30 seconds
sleep 30

# 5. Initialize schema
./init_schema.sh
```

### Check PostgreSQL logs:
```bash
sudo docker-compose logs postgres
```

### Access PostgreSQL directly:
```bash
sudo docker exec -it health-pipeline-postgres psql -U health_user -d health_pipeline
```

---

## ✅ COMPLETE STARTUP COMMANDS (Copy-Paste)

```bash
# Navigate to project
cd /home/mi/Desktop/AI_projects/PHIA_app

# Clean start (if needed)
sudo docker-compose down -v

# Start services
sudo docker-compose up -d

# Wait for PostgreSQL
echo "Waiting 30 seconds for PostgreSQL..."
sleep 30

# Initialize schema
./init_schema.sh

# Verify
sudo docker-compose ps
curl http://localhost:8000/health
curl http://localhost:9200/_cluster/health

# Test integration
sudo ./test_integration.sh
```

---

## 📝 WHAT CHANGED

### docker-compose.yml
- Removed `version: '3.8'` (obsolete)
- Removed schema.sql mount (caused permission issues)
- Added `PGDATA` environment variable
- Added `restart: unless-stopped`

### New Files
- `init_schema.sh` - Separate schema initialization
- `QUICKFIX_GUIDE.md` - This file

### Updated Files
- `test_integration.sh` - Now requires sudo

---

## 🎯 EXPECTED OUTPUT

After running the commands above, you should see:

```
✅ PostgreSQL is running
✅ API server is running  
✅ Elasticsearch is running
✅ Schema created successfully
✅ Data queued successfully
✅ Data found in queue table
```

---

**Try the complete startup commands above!** 🚀
