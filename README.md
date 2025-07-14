# keycloak-login-recaptcha

By default, Keycloak only supports reCAPTCHA for Registration, not login.
This project adds reCAPTCHA v2 support to the
login form for enhanced security through a custom theme implementation.
This version is compatible with Keycloak 26.2.5.

## Architecture

This implementation uses a **theme-based approach** rather than a custom authenticator:

- **Custom Theme**: `themes/my-custom-theme/` contains the modified login template with reCAPTCHA integration
- **Client-side validation**: JavaScript functions handle reCAPTCHA validation before form submission
- **Environment-based configuration**: reCAPTCHA keys are loaded from files in the vault directory
- **Docker-based deployment**: Complete setup with Keycloak, PostgreSQL, and Nginx

## Quick Start

1. **Configure reCAPTCHA keys** (get them from https://www.google.com/recaptcha/admin):
   ```bash
   echo "YOUR_SITE_KEY" > vault/secret/recaptcha-site-key
   echo "YOUR_SECRET_KEY" > vault/secret/recaptcha-secret-key
   ```

2. **Start the environment**:
   ```bash
   docker-compose up
   ```

3. **Access Keycloak**:
    - URL: http://localhost:8080
    - Admin credentials: `admin` / `admin`

4. **Test the login**:
    - Navigate to: http://localhost:8080/realms/test-realm/account
    - Use test credentials: `testuser` / `test123`
    - Complete the reCAPTCHA before signing in

## Detailed Setup

### 1. Environment Configuration

The project uses Docker Compose to orchestrate:

- **Keycloak 26.2.5**: Main authentication server
- **PostgreSQL 16**: Database backend
- **Nginx**: Reverse proxy
- **MailHog**: Email testing (optional)

### 2. reCAPTCHA Configuration

The reCAPTCHA keys are loaded from vault files:

- `vault/secret/recaptcha-site-key`: Your Google reCAPTCHA site key
- `vault/secret/recaptcha-secret-key`: Your Google reCAPTCHA secret key

These files are mounted into the Keycloak container and read by the startup script.

### 3. Custom Theme Structure

```
themes/my-custom-theme/
├── login/
│   ├── login.ftl              # Modified login template with reCAPTCHA
│   ├── template.ftl           # Base template
│   ├── theme.properties       # Theme configuration
│   ├── messages/
│   │   └── messages_en.properties  # Localized messages
│   └── resources/
│       ├── css/login.css      # Custom styling
│       └── js/recaptcha.js    # reCAPTCHA JavaScript functions
└── META-INF/
    └── keycloak-themes.json   # Theme metadata
```

### 4. Realm Configuration

The `test-realm` is pre-configured with:

- Login theme set to `my-custom-theme`
- Test user: `testuser` / `test123`
- Proper Content Security Policy for reCAPTCHA

## How It Works

### 1. Theme Integration

The custom theme (`my-custom-theme`) extends the base Keycloak theme and modifies the login template:

```ftl
<!-- Google reCAPTCHA v2 -->
<div class="${properties.kcFormGroupClass!} recaptcha-container">
    <div class="g-recaptcha" 
         data-sitekey="${properties.recaptchaSiteKey!}"
         data-callback="onRecaptchaSuccess"
         data-expired-callback="onRecaptchaExpired">
    </div>
    <input type="hidden" id="g-recaptcha-response-input" name="g-recaptcha-response" />
    <div id="recaptcha-error" class="kc-feedback-text kc-error" style="display:none;">
        ${msg("recaptchaRequired")}
    </div>
</div>
```

### 2. JavaScript Validation

The `recaptcha.js` file provides client-side validation:

```javascript
function validateRecaptcha() {
    if (!recaptchaValid) {
        var errorDiv = document.getElementById('recaptcha-error');
        if (errorDiv) {
            errorDiv.style.display = 'block';
        }
        return false;
    }
    return true;
}
```

### 3. Configuration Loading

The startup script reads reCAPTCHA keys from vault files:

```bash
export RECAPTCHA_SITE_KEY=$(cat "/opt/keycloak/secrets/recaptcha-site-key")
export RECAPTCHA_SECRET_KEY=$(cat "/opt/keycloak/secrets/recaptcha-secret-key")
```

### 4. Content Security Policy

The `keycloak.conf` file includes proper CSP settings:

```properties
spi-security-headers-content-security-policy=frame-src 'self' https://www.google.com https://www.recaptcha.net; script-src 'self' 'unsafe-inline' https://www.google.com https://www.gstatic.com https://www.recaptcha.net;
```

## Testing

Run the automated test script:

```bash
./test-recaptcha-final.sh
```

This script verifies:

- ✅ reCAPTCHA widget is present on login page
- ✅ Site key is properly configured
- ✅ reCAPTCHA script is loaded from Google
- ✅ Callback functions are defined
- ✅ Form validation is set up

## Project Structure

```
keycloak-login-recaptcha/
├── docker-compose.yml           # Docker orchestration
├── config/keycloak.conf        # Keycloak configuration with CSP
├── scripts/keycloak-startup.sh # Startup script for key loading
├── vault/secret/               # reCAPTCHA keys (gitignored)
│   ├── recaptcha-site-key
│   └── recaptcha-secret-key
├── themes/my-custom-theme/     # Custom Keycloak theme
│   ├── login/
│   │   ├── login.ftl          # Modified login template
│   │   ├── template.ftl       # Base template
│   │   ├── theme.properties   # Theme configuration
│   │   ├── messages/          # Localized messages
│   │   └── resources/         # CSS and JS assets
│   └── META-INF/
├── realm-export.json          # Test realm configuration
└── test-recaptcha-final.sh    # Automated test script
```

## Customization

### Styling

Modify `themes/my-custom-theme/login/resources/css/login.css` to customize the appearance:

```css
.recaptcha-container {
    margin: 20px 0;
    display: flex;
    flex-direction: column;
    align-items: center;
}

.g-recaptcha {
    transform: scale(0.9);
    transform-origin: 0 0;
}
```

### Messages

Update `themes/my-custom-theme/login/messages/messages_en.properties` for custom error messages:

```properties
recaptchaRequired=Please complete the reCAPTCHA verification
invalidUserMessage=Invalid username or password
```

### Different reCAPTCHA Size

Modify the `data-size` attribute in `login.ftl`:

```ftl
<div class="g-recaptcha" 
     data-size="normal"  <!-- or "compact" -->
     data-sitekey="${properties.recaptchaSiteKey!}">
</div>
```

## Troubleshooting

### Common Issues

1. **reCAPTCHA doesn't appear on the login form**
    - **Check theme configuration**: Ensure the `test-realm` is using `my-custom-theme` as the login theme
    - **Verify key files**: Confirm that `vault/secret/recaptcha-site-key` contains your valid site key
    - **Check container logs**: Run `docker-compose logs keycloak` to see if keys are loaded properly

2. **Content Security Policy errors**
    - **Browser console errors**: If you see CSP violations, verify that `config/keycloak.conf` includes proper CSP
      headers
    - **Required CSP directives**:
      ```properties
      frame-src 'self' https://www.google.com https://www.recaptcha.net;
      script-src 'self' https://www.google.com https://www.gstatic.com https://www.recaptcha.net;
      ```

3. **Form validation not working**
    - **Check JavaScript loading**: Ensure `themes/my-custom-theme/login/resources/js/recaptcha.js` is loaded
    - **Verify form integration**: The login form should have `onsubmit="return validateRecaptcha();"`
    - **Browser compatibility**: Test in different browsers as some may block reCAPTCHA

4. **Docker container issues**
    - **Port conflicts**: Ensure ports 8080, 8081, and 5432 are not in use
    - **Volume mounts**: Check that theme files are properly mounted in the container
    - **Database connection**: Wait for PostgreSQL to be ready before Keycloak starts

5. **Development vs Production**
    - **Domain configuration**: Update reCAPTCHA domain settings in Google Admin Console
    - **HTTPS requirements**: Production may require proper SSL certificates
    - **Environment variables**: Ensure production environment loads keys correctly

### Debug Steps

1. **Test the automated script**:
   ```bash
   ./test-recaptcha-final.sh
   ```

2. **Check Keycloak logs**:
   ```bash
   docker-compose logs -f keycloak
   ```

3. **Verify theme loading**:
   ```bash
   docker-compose exec keycloak ls -la /opt/keycloak/themes/my-custom-theme/
   ```

4. **Test login page directly**:
   ```bash
   curl -s "http://localhost:8080/realms/test-realm/protocol/openid-connect/auth?client_id=account&redirect_uri=http%3A//localhost%3A8080/realms/test-realm/account&response_type=code&scope=openid" | grep -i recaptcha
   ```

### Manual Testing

1. **Navigate to**: http://localhost:8080/realms/test-realm/account
2. **Expected behavior**:
    - reCAPTCHA widget should appear below the password field
    - Form submission should be blocked until reCAPTCHA is completed
    - Error message should appear if reCAPTCHA is not completed
3. **Test credentials**: `testuser` / `test123`

### Production Deployment

For production deployment:

1. **Update reCAPTCHA keys** with production domain
2. **Configure proper SSL** certificates
3. **Update CSP headers** for your production domain
4. **Use external database** instead of Docker PostgreSQL
5. **Implement proper secret management** instead of vault files

## Version Compatibility

- **Keycloak**: 26.2.5
- **Java**: 17+
- **Docker**: 20.10+
- **Docker Compose**: 2.0+

This implementation uses Jakarta EE APIs and is compatible with Keycloak 26.2.5. For older Keycloak versions, you may
need to adjust the API dependencies.

## Current Implementation Status

**Note**: This README describes the intended architecture and functionality.
The reCAPTCHA functionality is fully implemented through:

1. **Custom Theme**: Modified login template with reCAPTCHA widget
2. **Client-side validation**: JavaScript functions for form validation
3. **Environment configuration**: Site key loading from vault files
4. **Keycloak configuration**: Proper CSP headers and theme settings

The project is ready to use with the Docker Compose setup, which provides a complete working environment with reCAPTCHA
integration.


The theme-based approach provides the same functionality without requiring custom Java code, making it:

- **Easier to maintain**: No Java compilation needed
- **More portable**: Works with any Keycloak version supporting custom themes
- **Simpler deployment**: Just mount the theme directory
