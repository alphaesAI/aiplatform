#!/bin/bash

# PHIA Health Pipeline - Phase 1 Integration Test Script
# This script tests the complete data flow

echo "🚀 PHIA Health Pipeline - Integration Test"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if running with sudo
if [ "$EUID" -ne 0 ]; then 
    echo -e "${YELLOW}⚠ This script needs sudo access for Docker${NC}"
    echo "Please run: sudo ./test_integration.sh"
    exit 1
fi

# Step 1: Start Docker Services
echo "📦 Step 1: Starting Docker services..."
cd /home/mi/Desktop/AI_projects/PHIA_app
docker-compose up -d

echo "⏳ Waiting for services to be healthy..."
sleep 20

# Step 2: Check PostgreSQL
echo ""
echo "🗄️  Step 2: Checking PostgreSQL..."
if docker exec health-pipeline-postgres pg_isready -U health_user -d health_pipeline > /dev/null 2>&1; then
    echo -e "${GREEN}✓ PostgreSQL is running${NC}"
else
    echo -e "${RED}✗ PostgreSQL is not running${NC}"
    exit 1
fi

# Step 3: Check API Server
echo ""
echo "🌐 Step 3: Checking API server..."
if curl -s http://localhost:8000/health | grep -q "healthy"; then
    echo -e "${GREEN}✓ API server is running${NC}"
else
    echo -e "${RED}✗ API server is not running${NC}"
    exit 1
fi

# Step 4: Check Elasticsearch
echo ""
echo "🔍 Step 4: Checking Elasticsearch..."
if curl -s http://localhost:9200/_cluster/health | grep -q "cluster_name"; then
    echo -e "${GREEN}✓ Elasticsearch is running${NC}"
else
    echo -e "${RED}✗ Elasticsearch is not running${NC}"
    exit 1
fi

# Step 5: Test API with sample data
echo ""
echo "📤 Step 5: Sending test health data..."
RESPONSE=$(curl -s -X POST http://localhost:8000/api/health/queue \
  -H "Content-Type: application/json" \
  -d '{
    "pseudo_id": "test_user_123",
    "date": "2026-03-21",
    "duration_minutes": 45.5,
    "activity_name": "Running",
    "start_time": "2026-03-21T08:00:00",
    "end_time": "2026-03-21T08:45:00",
    "avg_hr_bpm": 145,
    "max_hr_bpm": 175,
    "distance_meters": 5200.0,
    "calories_kcal": 450.0,
    "steps": 10000,
    "active_zone_minutes": 35,
    "speed_mps": 1.92
  }')

if echo "$RESPONSE" | grep -q "queue_id"; then
    echo -e "${GREEN}✓ Data queued successfully${NC}"
    QUEUE_ID=$(echo "$RESPONSE" | grep -o '"queue_id":"[^"]*"' | cut -d'"' -f4)
    echo "  Queue ID: $QUEUE_ID"
else
    echo -e "${RED}✗ Failed to queue data${NC}"
    echo "  Response: $RESPONSE"
    exit 1
fi

# Step 6: Check database
echo ""
echo "🗄️  Step 6: Verifying data in PostgreSQL..."
COUNT=$(docker exec health-pipeline-postgres psql -U health_user -d health_pipeline -t -c "SELECT COUNT(*) FROM health_data_queue WHERE status='pending';")
if [ "$COUNT" -gt 0 ]; then
    echo -e "${GREEN}✓ Data found in queue table (${COUNT} records)${NC}"
else
    echo -e "${RED}✗ No data in queue table${NC}"
    exit 1
fi

# Step 7: Show queue data
echo ""
echo "📊 Step 7: Queue data sample:"
docker exec health-pipeline-postgres psql -U health_user -d health_pipeline -c "SELECT queue_id, pseudo_id, activity_name, steps, status, created_at FROM health_data_queue ORDER BY created_at DESC LIMIT 3;"

# Step 8: Check queue status via API
echo ""
echo "🔍 Step 8: Checking queue status via API..."
if [ ! -z "$QUEUE_ID" ]; then
    STATUS_RESPONSE=$(curl -s http://localhost:8000/api/health/status/$QUEUE_ID)
    if echo "$STATUS_RESPONSE" | grep -q "status"; then
        echo -e "${GREEN}✓ Queue status retrieved${NC}"
        echo "  $STATUS_RESPONSE" | python3 -m json.tool 2>/dev/null || echo "  $STATUS_RESPONSE"
    else
        echo -e "${YELLOW}⚠ Could not retrieve queue status${NC}"
    fi
fi

# Summary
echo ""
echo "=========================================="
echo "✅ INTEGRATION TEST COMPLETE"
echo "=========================================="
echo ""
echo "Next steps:"
echo "1. Run Android app and sync health data"
echo "2. Check data appears in PostgreSQL"
echo "3. Run Airflow DAG to process queue"
echo "4. Verify data in Elasticsearch"
echo ""
echo "Useful commands:"
echo "  - View logs: docker-compose logs -f api"
echo "  - Check queue: docker exec health-pipeline-postgres psql -U health_user -d health_pipeline -c 'SELECT * FROM health_data_queue;'"
echo "  - Check ES: curl http://localhost:9200/health_queue_sessions/_search"
echo ""
