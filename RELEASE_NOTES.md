# Keycloak Login reCAPTCHA - Release Notes

## Version 0.2.0 - Keycloak 26.2.5 Compatibility Update

### Overview
This release updates the Keycloak Login reCAPTCHA extension to be compatible with Keycloak 26.2.5. The extension adds Google reCAPTCHA support to the Keycloak login form, enhancing security against automated login attempts.

### Major Changes
1. **Jakarta EE API Migration**: Updated from Java EE to Jakarta EE APIs to match Keycloak 26.2.5 requirements
2. **Error Handling Improvements**: Enhanced error handling in RecaptchaUsernamePasswordForm.java to properly display validation errors
3. **Content Security Policy Updates**: Updated CSP settings to allow loading resources from both google.com and recaptcha.net
4. **Login Template Enhancement**: Updated login.ftl to properly integrate with Keycloak 26.2.5 theme structure
5. **Documentation Updates**: Comprehensive updates to README.md with installation instructions and troubleshooting guidance

### Technical Details

#### API Compatibility Updates
- Updated import statements to use Jakarta EE packages (jakarta.ws.rs.*)
- Updated authentication flow handling to match Keycloak 26.2.5 API

#### Error Handling Improvements
- Fixed error handling in action() method to properly display reCAPTCHA validation errors
- Added proper challenge response when reCAPTCHA validation fails
- Ensured reCAPTCHA script and attributes are re-added when validation fails

#### Security Enhancements
- Updated Content Security Policy to include necessary domains:
  - Added frame-src directives for google.com and recaptcha.net
  - Added script-src directives for google.com, recaptcha.net, and gstatic.com
- Updated X-Frame-Options to allow frames from both google.com and recaptcha.net

#### UI Improvements
- Updated login.ftl template to use proper Keycloak 26.2.5 theme classes
- Ensured reCAPTCHA widget is properly positioned before the form buttons

### Installation and Upgrade
1. Build the project with `mvn clean install`
2. Deploy the resulting JAR file to Keycloak's providers directory
3. Copy the updated login.ftl to Keycloak's themes directory
4. Update your realm's Content Security Policy settings as described in the README

### Known Limitations
- The extension requires manual configuration of the reCAPTCHA site key and secret in the Keycloak admin console
- The login.ftl template must be manually copied to the Keycloak themes directory

### Future Improvements
- Add automated tests for reCAPTCHA validation
- Improve error message customization
- Add support for reCAPTCHA v3