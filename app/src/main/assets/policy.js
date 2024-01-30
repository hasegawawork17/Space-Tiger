var condition = document.getElementById('conditionCheckbox');
var policy = document.getElementById('privacyCheckbox');
var contbtn = document.getElementById('continuebtn');

function checkRadio(){
    contbtn.disabled = !(condition.checked && policy.checked);
}

function checkSubmit() {
    jsBridge.postMessage("UserConsent", "Accepted");
}

function checkDismiss() {
    jsBridge.postMessage("UserConsent","Dismissed");
}
