// check CAPTCHA attributes
const captchaAttributes = CaptchaUtils.getHtmlTemplateVariable(document, 'captchaAttributes');
if (captchaAttributes === null || captchaAttributes['siteKey'] === null) {
    throw new Error('CAPTCHA cannot be initialized. Missing CAPTCHA attributes.');
}

// load captcha library
// - create the script
const scriptAttributes = new Map();
scriptAttributes.set('src', 'https://www.google.com/recaptcha/api.js?render=' + captchaAttributes['siteKey']);
scriptAttributes.set('async', null);
scriptAttributes.set('defer', null);
const captchaScriptNonce = CaptchaUtils.getHtmlTemplateVariable(document, 'captchaCSPNonce');
if (captchaScriptNonce != null) {
    scriptAttributes.set('nonce', captchaScriptNonce);
}
const captchaLibScript = CaptchaUtils.createElement(document, 'script', scriptAttributes);
// - add it to the document head
document.head.appendChild(captchaLibScript);

// captcha processing
CaptchaManager.registerExecute(function () {
    grecaptcha.ready(function() {
        grecaptcha.execute(captchaAttributes['siteKey'], {action: captchaAttributes['action']}).then(function(token) {
            let input = document.createElement("input");
            input.setAttribute("type", "hidden");
            input.setAttribute("name", "g-recaptcha-response");
            input.setAttribute("value", token);
            document.getElementsByTagName("form")[0].appendChild(input)
            submitForm();
        })
    });
});
