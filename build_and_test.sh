#!/bin/bash

echo "🚀 Health Pipeline - Build and Test Script"
echo "=========================================="

# Check if we're in the right directory
if [ ! -d "android-app" ] || [ ! -d "api-framework" ]; then
    echo "❌ Please run this script from the Pipeline root directory"
    exit 1
fi

echo "📱 Building Android App..."
cd android-app
./gradlew assembleDebug

if [ $? -eq 0 ]; then
    echo "✅ Android app built successfully!"
    echo "📦 APK location: android-app/app/build/outputs/apk/debug/app-debug.apk"
else
    echo "❌ Android build failed"
    exit 1
fi

cd ..

echo ""
echo "🔧 Starting API Server..."
cd api-framework
source ../venv/bin/activate

# Kill any existing server
pkill -f "uvicorn.*main:app" 2>/dev/null

# Start server in background
uvicorn main:app --host 0.0.0.0 --port 8000 --reload &
SERVER_PID=$!

echo "⏳ Waiting for server to start..."
sleep 5

# Test API endpoints
echo "🧪 Testing API endpoints..."

echo "Testing health check..."
curl -s http://localhost:8000/health | jq '.' || echo "Health check failed"

echo ""
echo "Testing sync endpoint..."
curl -s http://localhost:8000/api/health/sync | jq '.' || echo "Sync test failed"

echo ""
echo "Testing development users..."
curl -s http://localhost:8000/api/health/users | jq '.' || echo "Users test failed"

echo ""
echo "✅ Build and test completed!"
echo ""
echo "📋 Next Steps:"
echo "1. Install APK on Android device/emulator"
echo "2. Update API_BASE_URL in BuildConfig if needed"
echo "3. Test sync functionality between app and API"
echo ""
echo "🔗 API Server running at: http://localhost:8000"
echo "📚 API Documentation: http://localhost:8000/docs"
echo ""
echo "To stop the server: kill $SERVER_PID"
