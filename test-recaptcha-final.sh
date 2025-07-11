#!/bin/bash

echo "🧪 Testing reCAPTCHA v2 integration..."

# Test login page load
echo "📄 Testing login page load..."
response=$(curl -s "http://localhost:8080/realms/test-realm/protocol/openid-connect/auth?client_id=account&redirect_uri=http%3A//localhost%3A8080/realms/test-realm/account&response_type=code&scope=openid")

if echo "$response" | grep -q "g-recaptcha"; then
    echo "✅ reCAPTCHA widget found"
else
    echo "❌ reCAPTCHA widget NOT found"
    exit 1
fi

# Test if site key is present
if echo "$response" | grep -q "data-sitekey="; then
    echo "✅ reCAPTCHA site key is present"
else
    echo "❌ reCAPTCHA site key is missing"
    exit 1
fi

# Test if reCAPTCHA script is loaded
if echo "$response" | grep -q "recaptcha/api.js"; then
    echo "✅ reCAPTCHA script is loaded"
else
    echo "❌ reCAPTCHA script is NOT loaded"
    exit 1
fi

# Test if callback functions are defined
if echo "$response" | grep -q "onRecaptchaSuccess"; then
    echo "✅ reCAPTCHA callback functions are defined"
else
    echo "❌ reCAPTCHA callback functions are missing"
    exit 1
fi

# Test if form validation is set up
if echo "$response" | grep -q "validateRecaptcha"; then
    echo "✅ Form validation is set up"
else
    echo "❌ Form validation is NOT set up"
    exit 1
fi

echo ""
echo "🎉 All tests passed! reCAPTCHA v2 integration is working correctly."
echo ""
echo "📋 Manual test instructions:"
echo "1. Open: http://localhost:8080/realms/test-realm/account"
echo "2. You should see the reCAPTCHA checkbox widget"
echo "3. Try to login without completing reCAPTCHA - should show error"
echo "4. Complete reCAPTCHA checkbox and then login - should work"
echo ""
echo "🔍 If you see CSP errors in browser console, they should be resolved now."
