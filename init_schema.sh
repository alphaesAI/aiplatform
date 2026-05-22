#!/bin/bash

# Initialize PostgreSQL schema using schema.sql
# Run this AFTER PostgreSQL container is running

echo "🗄️  Initializing PostgreSQL schema from schema.sql..."

# Wait for PostgreSQL to be ready
echo "⏳ Waiting for PostgreSQL to be ready..."
sleep 5

# Create schema by piping schema.sql into the container
# We first drop the table if it exists to ensure a clean start with new columns
sudo docker exec -i health-pipeline-postgres psql -U health_user -d health_pipeline -c "DROP TABLE IF EXISTS health_data_queue;"

sudo docker exec -i health-pipeline-postgres psql -U health_user -d health_pipeline < schema.sql

echo "✅ Schema initialization complete!"
