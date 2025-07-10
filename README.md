# keycloak-login-recaptcha

By default Keycloak only supports reCAPTCHA for Registration, not login. This module adds reCAPTCHA support to the login form for enhanced security. This version is compatible with Keycloak 26.2.5.

# How to use
* First run `mvn clean install`. It will produce `target/recaptcha-login.jar`.
* Then start Keycloak with `docker-compose up`
    * The `target/recaptcha-login.jar` is mounted to `/opt/keycloak/providers/recaptcha-login.jar`, where it will
      be detected by Keycloak automatically as a provider.
    * `login.ftl` is the login template inside the `base` theme for Keycloak in `/opt/keycloak/themes/base/login/login.ftl`.
    We override it to add reCAPTCHA related elements directly. The modified `login.ftl` file in the project
      is mounted into the container.
    * We inject realm settings into Keycloak's container using `playground-realm.json`.
      One of the most important realm settings is `X-Frame-Options` which is set to `ALLOW-FROM https://www.google.com https://www.recaptcha.net` in 
      the file. The other important setting is `Content-Security-Policy` which is set to `frame-src 'self' https://www.google.com https://www.recaptcha.net; frame-ancestors 'self'; object-src 'none'; script-src 'self' https://www.google.com https://www.recaptcha.net https://www.gstatic.com;`.
* Go to `localhost:8080` and login with `admin` username and `admin` password.
* Make sure You're in `Playground` realm. Go to `Authentication` section in left side menu. Go to `Flows` tab.
* Pick `Browser With Recaptcha` flow in the drop down
* Under `Recaptcha Username Password Form` row click `Actions` and then `Config`
* Put the `sitekey` and `secret` obtained from `https://www.google.com/recaptcha/admin` into the config
* Go back to `Authentication` side menu, go to `Bindings` tab and change `browser flow` to `browser with recaptcha`
* Go to `Users` side menu, click `Add user`. create a user and make it enabled, and set it's credential properly.
* Go back to `Users`, click `View all users` and on that specifiec user you have created in prev step, click `impersonate`.
* Then `Sign Out` and then `Sign In`. If you have done everything precisely, then you'll be able to see the recaptcha form.


# Explanation

Inside the `login.ftl`, we have added the reCAPTCHA widget before the form buttons section:

```ftl
<#if recaptchaRequired??>
    <div class="${properties.kcFormGroupClass!}">
        <div class="${properties.kcInputWrapperClass!}">
            <div class="g-recaptcha" data-size="compact" data-sitekey="${recaptchaSiteKey}"></div>
        </div>
    </div>
</#if>
```

In the `RecaptchaUsernamePasswordForm` class, we set `recaptchaRequired` to `true` and provide the `recaptchaSiteKey` value. The reCAPTCHA JavaScript is added dynamically by the authenticator. This approach ensures that the reCAPTCHA widget is only displayed when using the reCAPTCHA-enabled authentication flow.

# Troubleshooting

## Common Issues

1. **reCAPTCHA not appearing on the login form**
   - Verify that you've selected "Browser With Recaptcha" as the browser flow in Authentication → Bindings
   - Check that you've configured the reCAPTCHA site key and secret correctly
   - Ensure the Content Security Policy allows loading resources from google.com and recaptcha.net

2. **Content Security Policy errors**
   - If you see CSP errors in the browser console, verify that your realm's Content Security Policy includes:
     ```
     frame-src 'self' https://www.google.com https://www.recaptcha.net;
     script-src 'self' https://www.google.com https://www.recaptcha.net https://www.gstatic.com;
     ```

3. **Authentication errors after reCAPTCHA validation**
   - Check the Keycloak server logs for any exceptions
   - Verify that your reCAPTCHA site key and secret are valid and for the correct domain

4. **Compatibility with Keycloak 26.2.5**
   - This version uses Jakarta EE APIs instead of the older Java EE APIs
   - The error handling has been updated to work with the current Keycloak API
