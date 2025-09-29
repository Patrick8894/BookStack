#!/bin/bash
echo "🚀 Starting BookStack in DEVELOPMENT mode..."
echo "⚠️  This will DELETE ALL existing data!"

# Load development environment
export $(cat ./env/.env.dev | xargs)

# Run the application
./mvnw spring-boot:run