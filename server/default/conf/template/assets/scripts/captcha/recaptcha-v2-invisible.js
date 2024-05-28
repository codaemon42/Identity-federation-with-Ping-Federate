// check CAPTCHA attributes
const captchaAttributes = CaptchaUtils.getHtmlTemplateVariable(document, 'captchaAttributes');
if (captchaAttributes === null || captchaAttributes['siteKey'] === null) {
  throw new Error('CAPTCHA cannot be initialized. CAPTCHA "site key" value is missing.');
}
// load captcha library
// - create the script
const scriptAttributes = new Map();
scriptAttributes.set('src', 'https://www.google.com/recaptcha/api.js');
scriptAttributes.set('async', null);
scriptAttributes.set('defer', null);
const captchaScriptNonce = CaptchaUtils.getHtmlTemplateVariable(document, 'captchaCSPNonce');
if (captchaScriptNonce != null) {
    scriptAttributes.set('nonce', captchaScriptNonce);
}
const captchaLibScript = CaptchaUtils.createElement(document, 'script', scriptAttributes);
// - add it to the document head
document.head.appendChild(captchaLibScript);

// inject div into the form
// - create the div
const divAttributes = new Map();
divAttributes.set('id', 'recaptcha');
divAttributes.set('class', 'g-recaptcha recaptcha');
divAttributes.set('data-badge', 'bottomright');
divAttributes.set('data-sitekey', captchaAttributes['siteKey']);
divAttributes.set('data-callback', 'submitForm');
divAttributes.set('data-expired-callback', 'resetFormSubmission');
divAttributes.set('data-error-callback', 'resetFormSubmission');
divAttributes.set('data-size', 'invisible');
const captchaDiv = CaptchaUtils.createElement(document, 'div', divAttributes);
// - add it to the form
document.forms[0].appendChild(captchaDiv);

// CAPTCHA processing methods.
CaptchaManager.registerExecute(function () {
  grecaptcha.execute();
});
CaptchaManager.registerCleanup(function () {
  grecaptcha.reset();
});

// callback function and observer to allow for form resubmission
function resetFormSubmission() {
  formSubmitted = false;
}

const backgroundObserver = new MutationObserver(() => {
  const puzzleIframe = document.querySelector('iframe[src^=\'https://www.google.com/recaptcha/api2/bframe\']');
  if (puzzleIframe) {
    backgroundObserver.disconnect();
    puzzleIframe.parentNode?.parentNode?.firstChild?.addEventListener('click', resetFormSubmission);
  }
});
backgroundObserver.observe(document.body, { childList: true, subtree: true });