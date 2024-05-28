function profileDevice(callback) {
    // Initialize the SDK
    onPingOneSignalsReady(function () {
        _pingOneSignals.init().then(function () {
            console.log("PingOne Signals initialized successfully");
        }).catch(function (e) {
            console.error("SDK Init failed", e);
        });

        // get device profiling data immediately
        getDeviceProfileData(callback)
    });
}

function onPingOneSignalsReady(callback) {
    if (window['_pingOneSignalsReady']) {
        callback();
    } else {
        document.addEventListener('PingOneSignalsReadyEvent', callback);
    }
}

/**
 * Calls the getData method and with the output, calls the callback function.
 *
 * @param callback
 */
function getDeviceProfileData(callback) {
    _pingOneSignals.getData()
        .then(result => callback(result))
        .catch(error => console.error('getData Error!', error));
}

function setCookie(name, value, days) {
    var expires = "";
    if (days) {
        var date = new Date();
        date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
        expires = "; expires=" + date.toUTCString();
    }

    // you can explicitly set a domain by adding the following to the end of the cookie string: "; domain=example.com"
    document.cookie = name + "=" + (value || "") + expires + "; path=/;SameSite=None;Secure";
}

profileDevice(function () {
    // set cookie and the local storage to indicate that device profiling has been completed
    var cookieName = "pingone.protect.device.profile";
    setCookie(cookieName, "signals", 1);
    sessionStorage.setItem( cookieName, "signals");
});
