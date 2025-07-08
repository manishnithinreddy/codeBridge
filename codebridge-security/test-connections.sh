#!/bin/bash

echo "Testing connections to external services..."
echo "=========================================="

# Test Redis connection
echo "Testing Redis connection to 10.212.183.94:6379..."
if command -v redis-cli &> /dev/null; then
    if redis-cli -h 10.212.183.94 -p 6379 ping > /dev/null 2>&1; then
        echo "✅ Redis connection successful"
    else
        echo "❌ Redis connection failed"
    fi
else
    echo "⚠️  redis-cli not found, testing with nc..."
    if nc -zv 10.212.183.94 6379 2>/dev/null; then
        echo "✅ Redis port is accessible"
    else
        echo "❌ Redis port is not accessible"
    fi
fi

echo ""

# Test PostgreSQL connection
echo "Testing PostgreSQL connection to 10.212.183.94:5432..."
if command -v pg_isready &> /dev/null; then
    if pg_isready -h 10.212.183.94 -p 5432 > /dev/null 2>&1; then
        echo "✅ PostgreSQL connection successful"
    else
        echo "❌ PostgreSQL connection failed"
    fi
else
    echo "⚠️  pg_isready not found, testing with nc..."
    if nc -zv 10.212.183.94 5432 2>/dev/null; then
        echo "✅ PostgreSQL port is accessible"
    else
        echo "❌ PostgreSQL port is not accessible"
    fi
fi

echo ""
echo "If connections are successful, you can run the application with:"
echo "java -jar target/codebridge-security-0.1.0-SNAPSHOT.jar --spring.profiles.active=external"
