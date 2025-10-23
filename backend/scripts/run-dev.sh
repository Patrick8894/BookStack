#!/bin/bash
echo "🚀 Starting BookStack in DEVELOPMENT mode..."
echo "⚠️  This will DELETE ALL existing data!"

# Load development environment (skip comments and empty lines)
export $(grep -v '^#' ./env/.env.dev | grep -v '^$' | xargs)

# Run the application
./mvnw spring-boot:run
# ./gradlew bootRun