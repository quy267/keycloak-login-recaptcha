package org.keycloak.marjaa.providers.login.recaptcha.authenticator;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.utils.FormMessage;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.messages.Messages;
import org.keycloak.services.validation.Validation;
import org.keycloak.util.JsonSerialization;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

import java.io.InputStream;
import java.util.*;

public class RecaptchaUsernamePasswordForm extends UsernamePasswordForm implements Authenticator {
    public static final String G_RECAPTCHA_RESPONSE = "g-recaptcha-response";
    public static final String SITE_KEY = "site.key";
    public static final String SITE_SECRET = "secret";
    public static final String USE_RECAPTCHA_NET = "useRecaptchaNet";
    private static final Logger logger = Logger.getLogger(RecaptchaUsernamePasswordForm.class);

    private String siteKey;

    @Override
    protected Response createLoginForm(LoginFormsProvider form) {
        form.setAttribute("recaptchaRequired", true);
        form.setAttribute("recaptchaSiteKey", siteKey);
        return super.createLoginForm(form);
    }

    @Override
    public void authenticate(AuthenticationFlowContext context) {
        context.getEvent().detail(Details.AUTH_METHOD, "auth_method");
        if (logger.isInfoEnabled()) {
            logger.info(
                    "validateRecaptcha(AuthenticationFlowContext, boolean, String, String) - Before the validation");
        }

        AuthenticatorConfigModel captchaConfig = context.getAuthenticatorConfig();
        LoginFormsProvider form = context.form();
        // Get locale safely, handling null user
        String userLanguageTag = context.getSession().getContext().resolveLocale(null).toLanguageTag();

        if (captchaConfig == null || captchaConfig.getConfig() == null
                || captchaConfig.getConfig().get(SITE_KEY) == null
                || captchaConfig.getConfig().get(SITE_SECRET) == null) {
            form.addError(new FormMessage(null, Messages.RECAPTCHA_NOT_CONFIGURED));
            // Create and force a challenge response to avoid flow status issues
            Response challengeResponse = createLoginForm(form);
            context.forceChallenge(challengeResponse);
            return;
        }
        siteKey = captchaConfig.getConfig().get(SITE_KEY);
        form.setAttribute("recaptchaRequired", true);
        form.setAttribute("recaptchaSiteKey", siteKey);
        form.addScript("https://www." + getRecaptchaDomain(captchaConfig) + "/recaptcha/api.js?hl=" + userLanguageTag);

        super.authenticate(context);
    }

    @Override
    public void action(AuthenticationFlowContext context) {
        if (logger.isDebugEnabled()) {
            logger.debug("action(AuthenticationFlowContext) - start");
        }
        MultivaluedMap<String, String> formData = context.getHttpRequest().getDecodedFormParameters();
        List<FormMessage> errors = new ArrayList<>();
        boolean success = false;
        context.getEvent().detail(Details.AUTH_METHOD, "auth_method");

        String captcha = formData.getFirst(G_RECAPTCHA_RESPONSE);
        if (Validation.isBlank(captcha)) {
            // If captcha is blank, set success to false and log the issue
            logger.warn("reCAPTCHA response is blank or missing");
            success = false;
        } else {
            AuthenticatorConfigModel captchaConfig = context.getAuthenticatorConfig();
            if (captchaConfig == null || captchaConfig.getConfig() == null || captchaConfig.getConfig().get(SITE_SECRET) == null) {
                // If captcha config is missing, log the issue and set success to false
                logger.error("reCAPTCHA configuration is missing or incomplete");
                success = false;
            } else {
                String secret = captchaConfig.getConfig().get(SITE_SECRET);
                success = validateRecaptcha(context, success, captcha, secret);
            }
        }
        if (success) {
            super.action(context);
        } else {
            // Add error message for failed reCAPTCHA validation
            LoginFormsProvider form = context.form();
            form.addError(new FormMessage(null, Messages.RECAPTCHA_FAILED));

            // Re-add the reCAPTCHA script and attributes
            AuthenticatorConfigModel captchaConfig = context.getAuthenticatorConfig();
            // Get locale safely, handling null user
            String userLanguageTag = context.getSession().getContext().resolveLocale(null).toLanguageTag();

            // Check if captchaConfig is valid before using it
            if (captchaConfig != null && captchaConfig.getConfig() != null && captchaConfig.getConfig().get(SITE_KEY) != null) {
                String siteKey = captchaConfig.getConfig().get(SITE_KEY);
                form.setAttribute("recaptchaRequired", true);
                form.setAttribute("recaptchaSiteKey", siteKey);
                form.addScript("https://www." + getRecaptchaDomain(captchaConfig) + "/recaptcha/api.js?hl=" + userLanguageTag);
            } else {
                // Log the issue if captchaConfig is missing or incomplete
                logger.error("reCAPTCHA configuration is missing or incomplete in action() method");
                // Still set recaptchaRequired to true to ensure the form is displayed correctly
                form.setAttribute("recaptchaRequired", true);
                // Use a default site key (this won't work for validation but will prevent errors in the UI)
                form.setAttribute("recaptchaSiteKey", "missing-site-key");
            }

            // Clear the reCAPTCHA response from form data
            formData.remove(G_RECAPTCHA_RESPONSE);

            // Set error in the context and return the login form
            Response challengeResponse = createLoginForm(form);
            // Use form challenge instead of failure challenge to avoid error.ftl template
            context.forceChallenge(challengeResponse);
            return;
        }

        if (logger.isDebugEnabled()) {
            logger.debug("action(AuthenticationFlowContext) - end");
        }
    }

    private String getRecaptchaDomain(AuthenticatorConfigModel config) {
        // If config is null, use google.com as the default domain
        if (config == null) {
            logger.debug("AuthenticatorConfigModel is null, using google.com as the default domain");
            return "google.com";
        }

        // If config.getConfig() is null, use google.com as the default domain
        if (config.getConfig() == null) {
            logger.debug("AuthenticatorConfigModel.getConfig() is null, using google.com as the default domain");
            return "google.com";
        }

        // If USE_RECAPTCHA_NET is not set or is not "true", use google.com as the default domain
        String useRecaptchaStr = config.getConfig().get(USE_RECAPTCHA_NET);
        boolean useRecaptcha = "true".equalsIgnoreCase(useRecaptchaStr);

        if (useRecaptcha) {
            return "recaptcha.net";
        }

        return "google.com";
    }

    protected boolean validateRecaptcha(AuthenticationFlowContext context, boolean success, String captcha, String secret) {
        HttpClient httpClient = context.getSession().getProvider(HttpClientProvider.class).getHttpClient();
        // Get the authenticator config safely
        AuthenticatorConfigModel authConfig = context.getAuthenticatorConfig();
        HttpPost post = new HttpPost("https://www." + getRecaptchaDomain(authConfig) + "/recaptcha/api/siteverify");
        List<NameValuePair> formparams = new LinkedList<>();
        formparams.add(new BasicNameValuePair("secret", secret));
        formparams.add(new BasicNameValuePair("response", captcha));

        // Get remote address safely, handling null connection
        String remoteAddr = "unknown";
        if (context.getConnection() != null) {
            remoteAddr = context.getConnection().getRemoteAddr();
        } else {
            logger.warn("Connection is null, using 'unknown' as remote address");
        }
        formparams.add(new BasicNameValuePair("remoteip", remoteAddr));
        try {
            UrlEncodedFormEntity form = new UrlEncodedFormEntity(formparams, "UTF-8");
            post.setEntity(form);
            HttpResponse response = httpClient.execute(post);

            // Check if response entity is null
            if (response.getEntity() == null) {
                logger.error("Response entity is null from reCAPTCHA verification");
                return false;
            }

            InputStream content = response.getEntity().getContent();
            try {
                // Check if content is empty
                if (content.available() == 0) {
                    logger.error("Empty response content from reCAPTCHA verification");
                    return false;
                }

                try {
                    Map json = JsonSerialization.readValue(content, Map.class);

                    // Check if json is null or empty
                    if (json == null || json.isEmpty()) {
                        logger.error("Empty or null JSON response from reCAPTCHA verification");
                        return false;
                    }

                    Object val = json.get("success");
                    success = Boolean.TRUE.equals(val);

                    // Log the response for debugging
                    if (logger.isDebugEnabled()) {
                        logger.debug("reCAPTCHA verification response: " + json);
                    }
                } catch (Exception e) {
                    logger.error("Failed to parse JSON response from reCAPTCHA verification", e);
                    return false;
                }
            } finally {
                content.close();
            }
        } catch (Exception e) {
            // Log the exception with more details for better debugging
            logger.error("reCAPTCHA validation failed", e);
            ServicesLogger.LOGGER.recaptchaFailed(e);
        }
        return success;
    }

}
