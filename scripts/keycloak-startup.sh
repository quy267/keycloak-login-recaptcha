#!/bin/bash

# Read the reCAPTCHA site key from the mounted file and export it as an environment variable
SITE_KEY_FILE="/opt/keycloak/secrets/recaptcha-site-key"
SECRET_KEY_FILE="/opt/keycloak/secrets/recaptcha-secret-key"

if [ -f "$SITE_KEY_FILE" ]; then
    export RECAPTCHA_SITE_KEY=$(cat "$SITE_KEY_FILE" | tr -d '\n\r')
    echo "✓ reCAPTCHA site key loaded from file: $SITE_KEY_FILE"
else
    echo "⚠ Warning: reCAPTCHA site key file not found at $SITE_KEY_FILE, using fallback"
fi

if [ -f "$SECRET_KEY_FILE" ]; then
    export RECAPTCHA_SECRET_KEY=$(cat "$SECRET_KEY_FILE" | tr -d '\n\r')
    echo "✓ reCAPTCHA secret key loaded from file: $SECRET_KEY_FILE"
else
    echo "⚠ Warning: reCAPTCHA secret key file not found at $SECRET_KEY_FILE, using fallback"
fi

echo "🚀 Starting Keycloak with file-based reCAPTCHA configuration..."

# Execute the original Keycloak command
exec /opt/keycloak/bin/kc.sh "$@"
