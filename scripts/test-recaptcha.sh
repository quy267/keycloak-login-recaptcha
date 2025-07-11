#!/bin/bash

echo "🧪 Testing reCAPTCHA integration..."

# Test if login page loads with reCAPTCHA
response=$(curl -s http://localhost:8080/realms/test-realm/protocol/openid-connect/auth?client_id=test-app&redirect_uri=http://localhost:3000&response_type=code&scope=openid)

if echo "$response" | grep -q "g-recaptcha"; then
    echo "✅ reCAPTCHA found on login page"
else
    echo "❌ reCAPTCHA not found on login page"
fi

# Open browser to test
echo "🌐 Opening browser to test login page..."
if command -v xdg-open > /dev/null; then
    xdg-open "http://localhost:8080/realms/test-realm/account"
elif command -v open > /dev/null; then
    open "http://localhost:8080/realms/test-realm/account"
else
    echo "Please open: http://localhost:8080/realms/test-realm/account"
fi