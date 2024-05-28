function clearMessage(element, timeout) {
    setTimeout(function () {
        element.style.display = 'none'
    }, timeout);
}

function showMessage(element, timeout) {
    setTimeout(function () {
        element.style.display = 'block'
    }, timeout);
}

const messages = document.querySelectorAll('.page-messages .message');
for (i = 0; i < messages.length; i++) {
    showMessage(messages[i], 250 * i);
}

// hide messages after 5 seconds.
setInterval(function(){
    const messages = document.querySelectorAll('.page-messages .message');
    for (var i = 0; i < messages.length; i++) {
        clearMessage(messages[i], 750 * i);
    }
}, 5000);

const closebuttons = document.querySelectorAll('.page-messages .message > a.close');
// attach listener to remove message when close button is clicked.
for (i = 0; i < closebuttons.length; i++) {
    var element = closebuttons[i];
    element.addEventListener('click', function (event) {
        var messagediv = event.target.parentNode;
        messagediv.parentNode.removeChild(messagediv);
    })
}

function showHideRequirements() {
    var tips = document.getElementById('password-requirements');
    if (tips.className.search(/\bopen\b/) === -1) {
        tips.className += ' open';
    }
    else {
        tips.className = tips.className.replace(/\bopen\b/,'');
    }
}

function isCheckBoxFieldMissing(field) {
    var checkboxes = document.getElementsByName(field.name);

    var foundChecked = false;
    for (var i = 0; i < checkboxes.length; i++) {
        if(checkboxes[i].checked) {
            foundChecked = true;
        }
    }

    if (!foundChecked) {
        // Use the name if no id is provided (for check box groups)
        var errorField = document.getElementById("required-" + checkboxes[0].name);
        errorField.classList.add("show");
    }

    return !foundChecked;
}

function hideCheckboxRequired(field){
    var id = field.name;
    var requiredDisplay  = document.getElementById("required-" + id);
    requiredDisplay.classList.remove("show");
}

function isSelectFieldMissing(field) {
    if (field.tagName === 'SELECT' && field.selectedIndex === 0 && field.options[0].disabled) {
        showErrorDisplay(field);
        return true;
    }
    return false;
}

function isStandardFieldMissing(field){
    if (!field.value) {
        showErrorDisplay(field);
        return true;
    }
    return false;
}

function showErrorDisplay(field) {
    var container = document.getElementById("container-" + field.id);
    container.classList.add("error");
    container.classList.add("form-error");
    container.classList.add("required");
}

function showRequiredDisplay(field) {
    var container = document.getElementById("container-" + field.id);
    container.classList.add("required");
}

function removeRequiredDisplay(field) {
    var container = document.getElementById("container-" + field.id);
    container.classList.remove("error");
    container.classList.remove("form-error");
    container.classList.remove("required");
}

function toggleRequiredDisplay(field) {
    if (!field.value || field.value === "")
        showRequiredDisplay(field);
    else
        removeRequiredDisplay(field);
}