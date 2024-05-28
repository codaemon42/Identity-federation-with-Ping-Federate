// check CAPTCHA attributes
const captchaAttributes = CaptchaUtils.getHtmlTemplateVariable(document, 'captchaAttributes');
if (captchaAttributes === null || captchaAttributes['siteKey'] === null) {
    throw new Error('CAPTCHA cannot be initialized. Missing CAPTCHA attributes.');
}

// check if the signal script is already loaded
// if not, load it
const scripts = document.getElementsByTagName('script');
let isSignalScriptLoaded = false;
for (let i = 0; i < scripts.length; i++) {
    const script = scripts[i];
    if (script.src && script.src.includes('signals-sdk')) {
        isSignalScriptLoaded = true;
        break;
    }
}

// load captcha library
// - create the script
if (!isSignalScriptLoaded) {
    const scriptAttributes = new Map();
    scriptAttributes.set('src', 'assets/scripts/signals-sdk-5.2.7.js');
    const captchaScriptNonce = CaptchaUtils.getHtmlTemplateVariable(document, 'captchaCSPNonce');
    if (captchaScriptNonce != null) {
        scriptAttributes.set('nonce', captchaScriptNonce);
    }
    const captchaLibScript = CaptchaUtils.createElement(document, 'script', scriptAttributes);
    // - add it to the document head
    document.head.appendChild(captchaLibScript);
}

const captchaSignalsReadyCallback = () => new Promise(resolve => {
    if (window['_pingOneSignalsReady']) {
        resolve()
    } else {
        document.addEventListener('PingOneSignalsReadyEvent', resolve)
    }
})

captchaSignalsReadyCallback()
    .then(() => window._pingOneSignals.init())
    .catch(e => console.error(e));

// captcha processing
CaptchaManager.registerExecute(function () {
    if (!window._pingOneSignals) {
        console.error("PingOne Signals not initialized");
        submitForm();
        return;
    }

    window._pingOneSignals.getData()
        .then(data => {
            let input = document.createElement("input");
            input.setAttribute("type", "hidden");
            input.setAttribute("name", "signals-response");
            input.setAttribute("value", data);
            document.getElementsByTagName("form")[0].appendChild(input)
            submitForm();
        })
        .catch(e => {
            console.error(e);
            submitForm();
        });
});
