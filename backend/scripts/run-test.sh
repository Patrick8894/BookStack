#!/bin/bash
echo "🧪 Starting BookStack in TEST mode..."

# Load test environment
export $(cat ./env/.env.test | xargs)

# Run tests
./mvnw test