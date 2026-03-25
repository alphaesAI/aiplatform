-- Health Pipeline Database Schema
-- This table stores health data from Android app before processing

CREATE DATABASE health_pipeline;

\c health_pipeline

-- Main queue table
CREATE TABLE health_data_queue (
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

-- Indexes for performance
CREATE INDEX idx_status ON health_data_queue(status);
CREATE INDEX idx_pseudo_id ON health_data_queue(pseudo_id);
CREATE INDEX idx_date ON health_data_queue(date);
CREATE INDEX idx_created_at ON health_data_queue(created_at);

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
