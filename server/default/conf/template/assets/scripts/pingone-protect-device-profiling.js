function profileDevice(callback, behavioralDataCollection = true) {
    // Initialize the SDK
    onPingOneSignalsReady(function () {
        _pingOneSignals.init({
            behavioralDataCollection: behavioralDataCollection
        }).then(function () {
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