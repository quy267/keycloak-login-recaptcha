#!/bin/bash

echo "🚀 Setting up Keycloak with custom theme and reCAPTCHA..."

# Check if Docker is running
if ! docker info > /dev/null 2>&1; then
    echo "❌ Docker is not running. Please start Docker first."
    exit 1
fi

# Create necessary directories
echo "📁 Creating directory structure..."
mkdir -p themes/my-custom-theme/{login/{resources/{css,js},messages},META-INF}

# Copy theme files (assuming they exist in a source directory)
echo "🎨 Setting up custom theme..."
# Note: You should have already created the theme files from the previous answer

# Start services
echo "🐳 Starting Docker containers..."
docker-compose down -v  # Clean any existing containers
docker-compose up -d

# Wait for Keycloak to be ready
echo "⏳ Waiting for Keycloak to start (this may take a minute)..."
until curl -f -s http://localhost:8080/health/ready > /dev/null; do
    sleep 5
    echo -n "."
done
echo ""

echo "✅ Keycloak is ready!"
echo ""
echo "📋 Access Information:"
echo "   Keycloak Admin Console: http://localhost:8080/admin"
echo "   Username: admin"
echo "   Password: admin"
echo ""
echo "   Test Realm Login: http://localhost:8080/realms/test-realm/account"
echo "   Test User: testuser"
echo "   Password: test123"
echo ""
echo "   MailHog Web UI: http://localhost:8025"
echo ""
echo "🔑 reCAPTCHA Test Keys are configured (works on localhost)"