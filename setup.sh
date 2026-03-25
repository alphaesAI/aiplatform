#!/bin/bash

# PHIA Health Pipeline - Complete Setup Script
# This script does everything in one go

echo "🚀 PHIA Health Pipeline - Complete Setup"
echo "========================================"
echo ""

# Check if running with sudo
if [ "$EUID" -ne 0 ]; then 
    echo "❌ This script needs sudo access"
    echo "Please run: sudo ./setup.sh"
    exit 1
fi

cd /home/mi/Desktop/AI_projects/PHIA_app

# Step 1: Clean up old containers
echo "🧹 Step 1: Cleaning up old containers..."
docker-compose down -v 2>/dev/null || true

# Step 2: Start services
echo ""
echo "📦 Step 2: Starting Docker services..."
docker-compose up -d

# Step 3: Wait for PostgreSQL
echo ""
echo "⏳ Step 3: Waiting for PostgreSQL to be ready (30 seconds)..."
sleep 30

# Step 4: Initialize schema
echo ""
echo "🗄️  Step 4: Initializing database schema..."
docker exec -i health-pipeline-postgres psql -U health_user -d health_pipeline << 'EOF'
CREATE TABLE IF NOT EXISTS health_data_queue (
    id SERIAL PRIMARY KEY,
    queue_id UUID DEFAULT gen_random_uuid(),
    pseudo_id VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    duration_minutes FLOAT,
    activity_name VARCHAR(100),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    avg_hr_bpm INTEGER,
    max_hr_bpm INTEGER,
    elevation_gain_m FLOAT,
    distance_meters FLOAT,
    calories_kcal FLOAT,
    steps INTEGER,
    active_zone_minutes INTEGER,
    speed_mps FLOAT,
    status VARCHAR(20) DEFAULT 'pending',
    retry_count INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_status ON health_data_queue(status);
CREATE INDEX IF NOT EXISTS idx_pseudo_id ON health_data_queue(pseudo_id);
CREATE INDEX IF NOT EXISTS idx_date ON health_data_queue(date);
CREATE INDEX IF NOT EXISTS idx_created_at ON health_data_queue(created_at);

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS \$\$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
\$\$ language 'plpgsql';

DROP TRIGGER IF EXISTS update_health_data_queue_updated_at ON health_data_queue;
CREATE TRIGGER update_health_data_queue_updated_at 
    BEFORE UPDATE ON health_data_queue 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();
EOF

# Step 5: Verify services
echo ""
echo "✅ Step 5: Verifying services..."
echo ""

# Check PostgreSQL
if docker exec health-pipeline-postgres pg_isready -U health_user -d health_pipeline > /dev/null 2>&1; then
    echo "✅ PostgreSQL is running"
else
    echo "❌ PostgreSQL is not running"
fi

# Check API
if curl -s http://localhost:8000/health | grep -q "healthy"; then
    echo "✅ API server is running"
else
    echo "❌ API server is not running"
fi

# Check Elasticsearch
if curl -s http://localhost:9200/_cluster/health | grep -q "cluster_name"; then
    echo "✅ Elasticsearch is running"
else
    echo "❌ Elasticsearch is not running"
fi

# Step 6: Test with sample data
echo ""
echo "📤 Step 6: Testing with sample data..."
RESPONSE=$(curl -s -X POST http://localhost:8000/api/health/queue \
  -H "Content-Type: application/json" \
  -d '{
    "pseudo_id": "test_user_123",
    "date": "2026-03-21",
    "duration_minutes": 45.5,
    "activity_name": "Running",
    "steps": 10000,
    "calories_kcal": 450.0
  }')

if echo "$RESPONSE" | grep -q "queue_id"; then
    echo "✅ Test data queued successfully"
    QUEUE_ID=$(echo "$RESPONSE" | grep -o '"queue_id":"[^"]*"' | cut -d'"' -f4)
    echo "   Queue ID: $QUEUE_ID"
else
    echo "❌ Failed to queue test data"
fi

# Step 7: Show database contents
echo ""
echo "📊 Step 7: Database contents:"
docker exec health-pipeline-postgres psql -U health_user -d health_pipeline -c "SELECT queue_id, pseudo_id, activity_name, steps, status FROM health_data_queue ORDER BY created_at DESC LIMIT 3;"

# Summary
echo ""
echo "========================================"
echo "✅ SETUP COMPLETE!"
echo "========================================"
echo ""
echo "Services running:"
docker-compose ps
echo ""
echo "Next steps:"
echo "1. Build Android app: cd /home/mi/Desktop/AI_projects/PHIA_app && ./gradlew assembleDebug"
echo "2. Install on device: ./gradlew installDebug"
echo "3. Open app and sync health data"
echo ""
echo "Useful commands:"
echo "  - View logs: sudo docker-compose logs -f api"
echo "  - Check queue: sudo docker exec health-pipeline-postgres psql -U health_user -d health_pipeline -c 'SELECT * FROM health_data_queue;'"
echo "  - Stop services: sudo docker-compose down"
echo ""
