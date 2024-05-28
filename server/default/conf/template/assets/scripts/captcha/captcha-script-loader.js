if (CaptchaUtils.isCaptchaEnabled(document)) {
  // get site key
  const captchaScriptName = CaptchaUtils.getHtmlTemplateVariable(document, 'captchaScriptName');
  if (captchaScriptName === null) {
    throw new Error('CAPTCHA cannot be initialized. Script unknown.');
  }

  if (captchaScriptName) {
    // load CAPTCHA provider script
    // - create the script
    const scriptAttributes = new Map();
    scriptAttributes.set('src', 'assets/scripts/captcha/' + captchaScriptName);
    scriptAttributes.set('defer', null);
    const captchaLibScript = CaptchaUtils.createElement(document, 'script', scriptAttributes);
    // - add it to the document head
    document.head.appendChild(captchaLibScript);
  }
}