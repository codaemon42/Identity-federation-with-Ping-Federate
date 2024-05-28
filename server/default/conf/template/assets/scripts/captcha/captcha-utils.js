const CaptchaUtils = Object.create({
  name: "Captcha Utils",

  createElement: function (document, type, attributes) {
    const input = document.createElement(type)
    attributes.forEach(function (value, key) {
      val = value || "";
      input.setAttribute(key, val);
    });
    return input;
  },

  createInputElement: function (document, attributes) {
    return createElement(document, "input", attributes)
  },

  isCaptchaEnabled: function (document) {
    const captchaEnabled = document.getElementById('captchaEnabled');
    if (captchaEnabled === null) {
      return false;
    }

    const captchaEnabledValue = JSON.parse(captchaEnabled.textContent);
    // value is a boolean
    if (typeof captchaEnabledValue === 'boolean') {
      return captchaEnabledValue;
    }

    return false;
  },

  getHtmlTemplateVariable: function (document, variableName) {
    const variable = document.getElementById(variableName);
    if (variable === null) {
      return null;
    }
    return JSON.parse(variable.textContent);
  }
});

const CaptchaManager = Object.create({
  name: "Captcha Manager",
  executeFunction: undefined,
  cleanupFunction: undefined,

  registerExecute: function (f) {
    this.executeFunction = f;
  },

  execute: function (main) {
    if (typeof this.executeFunction === 'function') {
      this.executeFunction();
    } else {
      main();
    }
  },

  registerCleanup: function (f) {
    this.cleanupFunction = f;
  },

  cleanup: function () {
    if (typeof this.cleanupFunction === 'function') {
      this.cleanupFunction();
    }
  }
});
