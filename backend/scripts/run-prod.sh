#!/bin/bash
echo "🚀 Starting BookStack in PRODUCTION mode..."
echo "✅ This will PRESERVE existing data"

# Load development environment (skip comments and empty lines)
export $(grep -v '^#' ./env/.env.dev | grep -v '^$' | xargs)

# Run the application
./mvnw spring-boot:run
# ./gradlew bootRun