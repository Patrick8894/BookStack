#!/bin/bash
echo "🧪 Starting BookStack in TEST mode..."

# Load development environment (skip comments and empty lines)
export $(grep -v '^#' ./env/.env.dev | grep -v '^$' | xargs)

# Run tests
./mvnw test