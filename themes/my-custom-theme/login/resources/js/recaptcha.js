var recaptchaValid = false;

function onRecaptchaSuccess(response) {
    recaptchaValid = true;
    document.getElementById('g-recaptcha-response-input').value = response;
    document.getElementById('recaptcha-error').style.display = 'none';
}

function onRecaptchaExpired() {
    recaptchaValid = false;
    document.getElementById('g-recaptcha-response-input').value = '';
}

function validateRecaptcha() {
    if (!recaptchaValid) {
        document.getElementById('recaptcha-error').style.display = 'block';
        return false;
    }
    return true;
}

// Alternative: Use reCAPTCHA v3 (invisible)
function initRecaptchaV3() {
    grecaptcha.ready(function() {
        grecaptcha.execute('YOUR_SITE_KEY', {action: 'login'}).then(function(token) {
            document.getElementById('g-recaptcha-response-input').value = token;
        });
    });
}