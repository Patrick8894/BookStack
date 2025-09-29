#!/bin/bash
echo "🚀 Starting BookStack in PRODUCTION mode..."
echo "✅ This will PRESERVE existing data"

# Load production environment
export $(cat ./env/.env.prod | xargs)

# Run the application
./mvnw spring-boot:run