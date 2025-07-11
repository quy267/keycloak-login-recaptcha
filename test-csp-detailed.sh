#!/bin/bash

echo "🔍 Detailed CSP and reCAPTCHA testing..."

# Get the login page
echo "📄 Fetching login page..."
response=$(curl -s "http://localhost:8080/realms/test-realm/protocol/openid-connect/auth?client_id=account&redirect_uri=http%3A//localhost%3A8080/realms/test-realm/account&response_type=code&scope=openid")

# Check what CSP header is being sent
echo "🔒 Current CSP header:"
curl -I "http://localhost:8080/realms/test-realm/protocol/openid-connect/auth?client_id=account&redirect_uri=http%3A//localhost%3A8080/realms/test-realm/account&response_type=code&scope=openid" 2>/dev/null | grep -i "content-security-policy" || echo "No CSP header found"

echo ""
echo "📋 Checking reCAPTCHA elements in page:"

# Check for reCAPTCHA elements
if echo "$response" | grep -q "g-recaptcha"; then
    echo "✅ reCAPTCHA widget div found"
else
    echo "❌ reCAPTCHA widget div NOT found"
fi

if echo "$response" | grep -q "data-sitekey="; then
    echo "✅ reCAPTCHA site key present"
    echo "   Site key: $(echo "$response" | grep -o 'data-sitekey="[^"]*"' | head -1)"
else
    echo "❌ reCAPTCHA site key missing"
fi

if echo "$response" | grep -q "recaptcha/api.js"; then
    echo "✅ reCAPTCHA API script included"
else
    echo "❌ reCAPTCHA API script NOT included"
fi

echo ""
echo "🧪 Testing actual reCAPTCHA loading with browser automation..."

# Create a simple HTML test file
cat > /tmp/test-recaptcha.html << 'EOF'
<!DOCTYPE html>
<html>
<head>
    <title>reCAPTCHA Test</title>
    <script src="https://www.google.com/recaptcha/api.js" async defer></script>
</head>
<body>
    <div class="g-recaptcha" data-sitekey="6LcN830rAAAAAOsEP0IOfmJkLVU_gFm79jNMRyDm"></div>
    <script>
        window.onload = function() {
            console.log('Page loaded');
            setTimeout(function() {
                var recaptchaFrame = document.querySelector('iframe[src*="recaptcha"]');
                if (recaptchaFrame) {
                    console.log('✅ reCAPTCHA iframe loaded successfully');
                    document.body.innerHTML += '<div style="color: green; font-size: 20px;">✅ reCAPTCHA loaded successfully!</div>';
                } else {
                    console.log('❌ reCAPTCHA iframe failed to load');
                    document.body.innerHTML += '<div style="color: red; font-size: 20px;">❌ reCAPTCHA failed to load - CSP blocking</div>';
                }
            }, 3000);
        };
    </script>
</body>
</html>
EOF

echo "📝 Test file created at /tmp/test-recaptcha.html"
echo "🌐 You can open this file in a browser to test reCAPTCHA loading independently"

echo ""
echo "💡 Recommendation: Since Keycloak's CSP is proving difficult to override,"
echo "   let's check if reCAPTCHA v3 (which doesn't use iframes) would work better."
echo "   Or we need to find the correct Keycloak configuration method."
