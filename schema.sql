-- Health Pipeline Database Schema
-- Optimized for VITALIS deep-dive metrics

CREATE DATABASE health_pipeline;

\c health_pipeline

CREATE TABLE health_data_queue (
    id SERIAL PRIMARY KEY,
    queue_id UUID DEFAULT gen_random_uuid(),
    status VARCHAR(20) DEFAULT 'pending',
    retry_count INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    -- 🆔 IDENTIFIERS
    pseudo_id VARCHAR(255) NOT NULL,
    pseudo_id2 VARCHAR(255),
    date DATE NOT NULL,
    datetime TIMESTAMP,

    -- 🏃 ACTIVITY & SESSION DETAILS
    activity_name VARCHAR(100),
    duration_minutes FLOAT,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    avg_hr_bpm INTEGER,
    max_hr_bpm INTEGER,
    elevation_gain_m FLOAT,
    distance_meters FLOAT,
    calories_kcal FLOAT,
    steps INTEGER,
    speed_mps FLOAT,

    -- 🔥 ACTIVE ZONES
    active_zone_minutes INTEGER,
    fatburn_active_zone_minutes INTEGER,
    cardio_active_zone_minutes INTEGER,
    peak_active_zone_minutes INTEGER,

    -- 👤 BIOMETRICS
    age INTEGER,
    gender VARCHAR(20),
    weight_kg FLOAT,
    height_cm FLOAT,

    -- 💓 VITALS
    resting_heart_rate INTEGER,
    heart_rate_variability FLOAT,
    stress_management_score INTEGER,

    -- 😴 SLEEP ANALYSIS
    sleep_minutes INTEGER,
    rem_sleep_minutes INTEGER,
    deep_sleep_minutes INTEGER,
    awake_minutes INTEGER,
    light_sleep_minutes INTEGER,
    bed_time TIMESTAMP,
    wake_up_time TIMESTAMP,

    -- 📊 SLEEP PERCENTAGES
    deep_sleep_percent FLOAT,
    rem_sleep_percent FLOAT,
    awake_percent FLOAT,
    light_sleep_percent FLOAT
);

-- Indexes for performance
CREATE INDEX idx_status ON health_data_queue(status);
CREATE INDEX idx_pseudo_id ON health_data_queue(pseudo_id);
CREATE INDEX idx_date ON health_data_queue(date);

-- Function to auto-update updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_health_data_queue_updated_at 
    BEFORE UPDATE ON health_data_queue 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();
