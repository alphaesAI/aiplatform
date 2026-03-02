# HealthPipeline 🏥

A comprehensive health data management system that enables seamless collection, processing, and synchronization of health metrics from Android devices to a cloud-based backend.

## 🏗️ Project Architecture

HealthPipeline consists of two main components:

1. **Backend API Framework** - Python-based REST API for data processing and storage
2. **Android Application** - Native Android app for health data collection and sync

## 📁 Project Structure

```
Pipeline/
├── api-framework/          # Backend API (Python)
│   ├── routes/
│   │   └── queue_api.py    # Queue endpoints for health data
│   ├── services/
│   │   └── postgres_client.py  # PostgreSQL database operations
│   ├── main.py             # FastAPI application entry point
│   ├── requirements.txt    # Python dependencies
│   └── .env.dev           # Development environment variables
├── android-app/            # Android Application (Kotlin)
│   └── app/src/main/java/com/healthpipeline/
│       ├── data/
│       │   ├── ApiService.kt          # API communication layer
│       │   └── HealthDataModels.kt    # Data models
│       ├── CloudSyncService.kt        # Background sync service
│       ├── MainActivity.kt           # Main application activity
│       └── ui/                        # UI components and screens
├── docker-compose.yml      # Container orchestration
├── build_and_test.sh      # Build and test automation
└── README.md              # This file
```

## 🚀 Features

### Backend API
- **Queue Management**: Efficient health data queuing system
- **PostgreSQL Integration**: Robust data storage with PostgreSQL
- **RESTful API**: Clean and well-documented endpoints
- **Environment Configuration**: Separate development and production configs

### Android Application
- **Health Data Collection**: Integration with Android Health Connect
- **Background Sync**: Automatic data synchronization with cloud
- **Queue-based Processing**: Efficient data upload with queue management
- **Modern UI**: Material Design 3 with Jetpack Compose

## 📝 Recent Changes

### Backend API Modifications
- ✅ **Created**: `routes/queue_api.py` - New queue endpoint implementation
- ✅ **Modified**: `services/postgres_client.py` - Added queue insertion functionality
- ✅ **Updated**: `main.py` - Integrated queue router into main application
- ✅ **Simplified**: `requirements.txt` - Streamlined dependencies
- ✅ **Configured**: `.env.dev` - Added PostgreSQL credentials

**Refactoring Cleanup**:
- ❌ Removed: `routes/health_api.py`, `routes/dev_health_api.py`
- ❌ Removed: `services/elasticsearch_client.py`
- ❌ Removed: `services/transformer.py`, `services/transformer_simple.py`
- ❌ Removed: `config/config.py`

### Android App Modifications
- ✅ **Enhanced**: `data/ApiService.kt` - Added queue models and endpoint integration
- ✅ **Extended**: `CloudSyncService.kt` - Added `queueSessionMetrics()` function
- ✅ **Updated**: `MainActivity.kt` - Integrated queue functionality
- ✅ **Configured**: `local.properties` - Set Android SDK path

## 🛠️ Technology Stack

### Backend
- **Framework**: FastAPI (Python)
- **Database**: PostgreSQL
- **ORM**: SQLAlchemy
- **Containerization**: Docker
- **Environment Management**: Python-dotenv

### Android
- **Language**: Kotlin
- **UI Framework**: Jetpack Compose
- **Architecture**: MVVM
- **Health Integration**: Android Health Connect API
- **Networking**: Retrofit/OkHttp
- **Background Processing**: WorkManager

## 📋 Prerequisites

### Backend Development
- Python 3.8+
- PostgreSQL 12+
- Docker & Docker Compose
- Git

### Android Development
- Android Studio Arctic Fox or later
- Android SDK (API 24+)
- Kotlin 1.5+
- Physical Android device or emulator with Health Connect

## 🚀 Getting Started

### Backend Setup

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Saythu000/HealthPipeline.git
   cd HealthPipeline
   ```

2. **Set up environment**:
   ```bash
   cd api-framework
   cp .env.template .env.dev
   # Edit .env.dev with your PostgreSQL credentials
   ```

3. **Install dependencies**:
   ```bash
   pip install -r requirements.txt
   ```

4. **Run with Docker** (recommended):
   ```bash
   docker-compose up -d
   ```

5. **Or run locally**:
   ```bash
   python main.py
   ```

### Android Setup

1. **Open in Android Studio**:
   ```bash
   # Open the android-app directory in Android Studio
   ```

2. **Configure local.properties**:
   ```properties
   # Set your Android SDK path
   sdk.dir=/path/to/your/android/sdk
   ```

3. **Build and run**:
   ```bash
   ./gradlew assembleDebug
   ./gradlew installDebug
   ```

## 📊 API Endpoints

### Queue Management
- `POST /api/queue/health-data` - Queue health metrics for processing
- `GET /api/queue/status` - Check queue processing status
- `GET /api/queue/metrics` - Get queue performance metrics

## 🔧 Configuration

### Environment Variables
Create a `.env.dev` file in the `api-framework` directory:

```env
# PostgreSQL Configuration
DB_HOST=localhost
DB_PORT=5432
DB_NAME=healthpipeline
DB_USER=your_username
DB_PASSWORD=your_password

# API Configuration
API_HOST=0.0.0.0
API_PORT=8000
DEBUG=true
```

## 🧪 Testing

### Backend Tests
```bash
cd api-framework
python -m pytest tests/
```

### Android Tests
```bash
cd android-app
./gradlew test
```

## 📈 Monitoring & Logging

- **Backend**: Structured logging with configurable levels
- **Android**: Comprehensive logging for sync operations and health data collection
- **Database**: Query performance monitoring
- **API**: Request/response logging and metrics

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🆘 Support

For support and questions:
- Create an issue in the GitHub repository
- Check the documentation in the `docs/` directory
- Review the API documentation at `http://localhost:8000/docs` (when running)

## 🔮 Roadmap

- [ ] Enhanced analytics dashboard
- [ ] Real-time health data streaming
- [ ] Machine learning integration for health insights
- [ ] Multi-platform support (iOS, Web)
- [ ] Advanced security features
- [ ] Integration with wearable devices

---

**HealthPipeline** - Empowering better health through seamless data integration 🏥💙
