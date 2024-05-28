function profileDevice(onCompletion) {
    var options = {
        excludes: {
            // Unreliable on Windows, see https://github.com/Valve/fingerprintjs2/issues/375
            'enumerateDevices': true,
            // devicePixelRatio depends on browser zoom, and it's impossible to detect browser zoom
            'pixelRatio': true,
            // DNT depends on incognito mode for some browsers (Chrome) and it's impossible to detect incognito mode
            'doNotTrack': true,
            // uses js fonts already
            'fontsFlash': true,
            'webdriver': true,
            'canvas': true,
            'webglVendorAndRenderer': isIe(),
            'fonts': true,
            'touchSupport': true,
            'webgl': true,
            'audio': true
        }
    };
    if (window.requestIdleCallback) {
        requestIdleCallback(function () {
            Fingerprint2.get(options, function (components) {
                onCompletion(components);
            })
        })
    } else {
        setTimeout(function () {
            Fingerprint2.get(options, function (components) {
                onCompletion(components);
            })
        }, 500)
    }
}

function encodeDeviceProfile(deviceProfile) {
    return window.btoa(JSON.stringify(deviceProfile));
}

function transformComponentsToDeviceProfile(components) {
    var deviceProfile = {};
    components.map(function (component) {
        var key = component.key;
        var value = component.value;
        if (value === "not available" || value === "excluded") {
            value = null
        } else if (key === "touchSupport") {
            value = value.map(function (v) {
                return v.toString();
            });
        } else if (key === "addBehavior") {
            key = "addBehaviour"
        }
        deviceProfile[key] = value;
    });
    return {deviceProfile: deviceProfile};
}

function isIe() {
    var ua = window.navigator.userAgent;
    var msie = ua.indexOf("MSIE ");
    return msie > 0 || !!navigator.userAgent.match(/Trident.*rv\:11\./);
}