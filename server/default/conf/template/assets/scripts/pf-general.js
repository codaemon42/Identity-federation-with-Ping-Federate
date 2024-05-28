function getScreenWidth() {
    return (window.outerHeight) ? window.outerWidth : document.body.clientWidth;
}

function isMobile() {
    if (/Android|webOS|iPhone|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)) {
        return true;
    } else {
        return getScreenWidth() <= 480;
    }
}

function setMobile(isMobile, isPingOneApp) {
    var bodyTag = document.getElementsByTagName('body')[0];
    var className = ' mobile',
        hasClass = (bodyTag.className.indexOf(className) !== -1);

    if (isMobile && !hasClass) {
        bodyTag.className += className;
    } else if (!isMobile && hasClass) {
        bodyTag.className = bodyTag.className.replace(className, '');
    }
}

function toggleMobile (isPingOneApp, bodyTag) {
    if (/Android|webOS|iPhone|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)) {
        setMobile(true, isPingOneApp);
    } else {
        setMobile((getScreenWidth() <= 480), isPingOneApp);
        window.onresize = function() {
            setMobile((getScreenWidth() <= 480), isPingOneApp);
        }
    }
}

function setFocus(element) {
    var platform = navigator.platform;
    if (platform != null && platform.indexOf("iPhone") == -1) {
        document.getElementById(element).focus();
    }
}

function registerEventHandler(elementId, event, eventHandler) {
    if (document.getElementById(elementId)){
        document.getElementById(elementId).addEventListener(event, eventHandler);
    }
}

function registerEventHandlerForClass(className, event, eventHandler) {
    var elements = document.getElementsByClassName(className);
    for (var i = 0; i < elements.length; i++) {
        elements[i].addEventListener(event, eventHandler);
    }
}

function handleReturnPress(elementId, handler) {
    if (document.getElementById(elementId)){
        document.getElementById(elementId).addEventListener('keypress', function(event){
            var keycode;
            if (window.event)
                keycode = window.event.keyCode;
            else if (e)
                keycode = e.which;
            else
                return true;

            if (keycode === 13) {
                handler(event);
                return false;
            }
            else
                return true;
        });
    }
}

function handleReturnPressForClass(className, handler) {
    var elements = document.getElementsByClassName(className);
    for (var i = 0; i < elements.length; i++) {
        handleReturnPress(elements[i].id, handler);
    }
}

function toggleRequirementsDisplay(showRequirements) {
    var requirements = document.getElementById("req-message");
    var up = document.getElementById("up-arrow");
    var down = document.getElementById("down-arrow");

    if (requirements) {
        if (showRequirements) {
            requirements.style.display = "inline-block";
            down.style.display = "none";
            up.style.display = "inline-block";
        } else {
            requirements.style.display = "none";
            down.style.display = "inline-block";
            up.style.display = "none";
        }
    }
}

function revealPassword(passwordElementId, revealElementId) {
    var inputElement = document.getElementById(passwordElementId);
    var revealElement = document.getElementById(revealElementId);

    if (inputElement.type === 'password') {
        inputElement.setAttribute('type', 'text');
        revealElement.className = 'password-show-button icon-view-hidden';
    }
    else {
        inputElement.setAttribute('type', 'password');
        revealElement.className = 'password-show-button icon-view';
    }
}