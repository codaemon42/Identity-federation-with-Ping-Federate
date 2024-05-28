function setCookie(name, value, days) {
    var expires = "";
    if (days) {
        var date = new Date();
        date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));
        expires = "; expires=" + date.toUTCString();
    }
    document.cookie = name + "=" + (value || "") + expires + "; path=/;SameSite=None;Secure";
}

function split(string, size) {
    var re = new RegExp('.{1,' + size + '}', 'g');
    return string.match(re);
}

function setDeviceProfileCookies(encodedDeviceProfile, cookieNamePrefix) {
    // Our cookies should be 4096 bytes or less
    // So break the encoded device profile into 3896 byte chunks (leaving 200 bytes for the rest of the cookie)
    var chunkLength = 3896;
    var chunks = split(encodedDeviceProfile, chunkLength);
    chunks.map(function (chunk, index) {
        setCookie(cookieNamePrefix + index, chunk, 1);
    });
}

function onCompletion(components) {
    var deviceProfile = {components: components};
    var base64EncodedDeviceProfile = encodeDeviceProfile(deviceProfile);
    var cookieNamePrefix = "pingone.risk.device.profile";
    setDeviceProfileCookies(base64EncodedDeviceProfile, cookieNamePrefix)
}

profileDevice(onCompletion);
