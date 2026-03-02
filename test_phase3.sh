#!/bin/bash

echo "🚀 Health Pipeline - Phase 3 Testing"
echo "===================================="

# Start API server
echo "🔧 Starting API server..."
cd /mnt/d/Pipeline/api-framework
source ../venv/bin/activate

# Kill existing server
pkill -f "uvicorn.*main:app" 2>/dev/null
sleep 2

# Start new server
uvicorn main:app --host 0.0.0.0 --port 8000 --reload &
SERVER_PID=$!
echo "Server PID: $SERVER_PID"

echo "⏳ Waiting for server to start..."
sleep 5

echo ""
echo "🧪 Testing Phase 3 API Endpoints..."

echo "1. Testing health insights..."
curl -s http://localhost:8000/api/health/insights/test_user | jq '.' || echo "❌ Insights test failed"

echo ""
echo "2. Testing data export (JSON)..."
curl -s "http://localhost:8000/api/health/export/test_user?format=json" | jq '.' || echo "❌ JSON export test failed"

echo ""
echo "3. Testing goals creation..."
curl -s -X POST http://localhost:8000/api/health/goals/test_user \
  -H "Content-Type: application/json" \
  -d '{"type": "steps", "target": 10000}' | jq '.' || echo "❌ Goal creation test failed"

echo ""
echo "4. Testing goals retrieval..."
curl -s http://localhost:8000/api/health/goals/test_user | jq '.' || echo "❌ Goals retrieval test failed"

echo ""
echo "5. Testing existing endpoints..."
curl -s http://localhost:8000/health | jq '.' || echo "❌ Health check failed"

echo ""
echo "📱 Building Android App..."
cd ../android-app

# Check if gradlew exists
if [ -f "./gradlew" ]; then
    echo "Building APK..."
    ./gradlew assembleDebug --quiet
    
    if [ $? -eq 0 ]; then
        echo "✅ Android app built successfully!"
        echo "📦 APK: android-app/app/build/outputs/apk/debug/app-debug.apk"
    else
        echo "❌ Android build failed"
    fi
else
    echo "⚠️  Gradle wrapper not found, skipping Android build"
fi

cd ..

echo ""
echo "✅ Phase 3 Testing Complete!"
echo ""
echo "📋 New Features Available:"
echo "  🔍 Search & Filter - Find specific health records"
echo "  🎯 Goals & Targets - Set and track health goals"
echo "  🧠 AI Insights - Smart health recommendations"
echo "  📊 Data Export - Export data in JSON/CSV format"
echo ""
echo "🔗 API Documentation: http://localhost:8000/docs"
echo "🔧 Server PID: $SERVER_PID (kill $SERVER_PID to stop)"
echo ""
echo "📱 Install APK on device and test new features!"
