var recaptchaValid = false;

function onRecaptchaSuccess(response) {
    recaptchaValid = true;
    document.getElementById('g-recaptcha-response-input').value = response;
    var errorDiv = document.getElementById('recaptcha-error');
    if (errorDiv) {
        errorDiv.style.display = 'none';
    }
}

function onRecaptchaExpired() {
    recaptchaValid = false;
    document.getElementById('g-recaptcha-response-input').value = '';
}

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