const api = "${api}"
const appId = "${appId}";
const container = document.getElementById("karakal-auth");

if (!api || api.trim() === "") {
console.log("data-api is missing or empty");
if (container) {
container.innerHTML = error();
}
}

if (!appId || appId.trim() === "") {
console.log("data-application-id is missing or empty");
if (container) {
container.innerHTML = error();
}
}

if (!container) {
console.log("Container element with id 'karakal-auth' does not exist");
}

function success() {
return `
<main>
    <div class="auth-box">
        <div class="content has-text-centered">
            <div class="icon is-large has-text-success" style="font-size: 3rem;">✓</div>
            <h1 class="title is-4 has-text-success">Success!</h1>
            <p class="subtitle is-6">Your registration was completed successfully.</p>
        </div>
        <div class="mt-4 has-text-centered"">
        <button class="button is-primary is-fullwidth" id="to-login">Sign in now</button>
    </div>
    </div>
</main>
`;
}

function error() {
return `
<main>
    <div class="auth-box">
        <div class="content has-text-centered">
            <div class="icon is-large has-text-danger" style="font-size: 3rem;">✕</div>
            <h1 class="title is-4 has-text-danger">Error!</h1>
            <p class="subtitle is-6">Oops... something went wrong.<br>Please try again.</p>
        </div>
        <div class="mt-4 has-text-centered">
            <button class="button is-danger is-fullwidth" id="to-register">Try again</button>
        </div>
    </div>
</main>
`;
}

if (api && api.trim() !== "" && appId && appId.trim() !== "" && container) {
console.log("Karakal-Auth-Init complete");

function base64urlToBuffer(base64url) {
if (typeof base64url !== "string") {
if (base64url instanceof ArrayBuffer) return base64url;
if (ArrayBuffer.isView(base64url)) return base64url.buffer;
}
let base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');
while (base64.length % 4) base64 += '=';
let str = atob(base64);
let bytes = new Uint8Array(str.length);
for (let i = 0; i < str.length; i++) bytes[i] = str.charCodeAt(i);
return bytes.buffer;
}

function bufferToBase64Url(buffer) {
let bytes = new Uint8Array(buffer);
let str = "";
for (let i = 0; i < bytes.length; ++i) str += String.fromCharCode(bytes[i]);
let base64 = btoa(str);
return base64.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

async function registerInit(username) {
const res = await fetch('${api}/api/v1/register-init', {
method: 'POST',
headers: {'Content-Type': 'application/json'},
body: JSON.stringify({username, appId})
});

let options = await res.json();

if (typeof options.challenge === "object" && options.challenge !== null && 'value' in options.challenge) {
options.challenge = base64urlToBuffer(options.challenge.value);
} else if (typeof options.challenge === "string") {
options.challenge = base64urlToBuffer(options.challenge);
}

if (typeof options.user.id === "string") {
options.user.id = base64urlToBuffer(options.user.id);
}

const credential = await navigator.credentials.create({publicKey: options});

const serializedCredential = {
id: credential.id,
rawId: bufferToBase64Url(credential.rawId),
type: credential.type,
response: {
clientDataJSON: bufferToBase64Url(credential.response.clientDataJSON),
attestationObject: bufferToBase64Url(credential.response.attestationObject)
},
authenticatorAttachment: credential.authenticatorAttachment
};

const res2 = await fetch('${api}/api/v1/register-complete', {
method: 'POST',
headers: {'Content-Type': 'application/json', 'karakal-username': username, 'karakal-app-id': appId},
body: JSON.stringify(serializedCredential)
});

if (res2.status === 200) {
container.innerHTML = success();
const toLogin = document.getElementById("to-login");
toLogin.addEventListener('click', function (e) {
e.preventDefault();
showForm('login');
});
} else {
container.innerHTML = error();
const toRegister = document.getElementById("to-register");
toRegister.addEventListener('click', function (e) {
e.preventDefault();
showForm('register');
});
}

}

async function loginInit(username) {
const res = await fetch('${api}/api/v1/login-init', {
method: 'POST',
headers: {'Content-Type': 'application/json'},
body: JSON.stringify({username, appId})
});
const options = await res.json();

options.allowCredentials.forEach(cred => {
if (typeof cred.id === "string") {
cred.id = base64urlToBuffer(cred.id);
}
});

if (typeof options.challenge === "string") {
options.challenge = base64urlToBuffer(options.challenge);
}

const assertion = await navigator.credentials.get({publicKey: options});
const resp = await fetch('${api}/api/v1/login-complete', {
method: 'POST',
body: JSON.stringify(assertion),
headers: {'Content-Type': 'application/json', 'karakal-username': username, 'karakal-app-id': appId},
});

if (resp.status === 200) {
localStorage.setItem('username', username);
const data = await resp.json();
const name = data.name;
const jwt = data.jwt;
const maxAge = data.maxAge;
const redirect= data.redirect;

document.cookie = encodeURIComponent(name) + "=" + encodeURIComponent(jwt) +
"; max-age=" + maxAge +
"; path=/" +
"; SameSite=Strict" +
"; Secure";

window.location.replace(redirect);
} else {
container.innerHTML = error();
}
}

function getLoginFormHTML() {
return `
<main>
    <div class="auth-box">
        <h1 class="title auth-title">Sign In</h1>
        <p class="auth-subtitle">Please enter your email address to sign in.</p>
        <form>
            <div class="field">
                <label class="label">Email Address</label>
                <div class="control">
                    <input class="input" id="username" name="username" type="email" placeholder="your@email.com" required autocomplete="username">
                </div>
            </div>
            <button class="button is-link" id="login-init">Sign In</button>
        </form>
        <#if registration>
            <div class="alt-link">
                Don’t have an account? <a href="#" id="register-form">Register</a>
            </div>
        </#if>
        <div class="alt-link" id="different-account">
            <a href="#" id="use-different-account">Sign in with another account</a>
        </div>
    </div>
</main>
`;
}

function getRegisterFormHTML() {
return `
<main>
    <div class="auth-box" id="register">
        <h1 class="title auth-title">Register</h1>
        <p class="auth-subtitle">Create an account to get started.</p>
        <div class="field">
            <label class="label">Email Address</label>
            <div class="control">
                <input class="input" id="username" name="username" type="email" placeholder="your@email.com" required autocomplete="username">
            </div>
        </div>
        <button class="button is-link" id="register-init">Register</button>
        </form>
        <div class="alt-link">
            Already have an account? <a href="#" id="login-form">Sign In</a>
        </div>
    </div>
</main>
`;
}

function showForm(formType) {
if (formType === 'login') {
container.innerHTML = getLoginFormHTML();

const usernameInput = document.getElementById("username");
if (usernameInput) {
const username = localStorage.getItem("username");
if (username !== null && username !== undefined && username !== "") {
usernameInput.value = username;
document.getElementById('username').disabled = true;
} else {
const differentAccount = document.getElementById("different-account");
if (differentAccount) {
differentAccount.remove();
}
}
}

const differentAccount = document.getElementById("use-different-account");
if (differentAccount) {
differentAccount.addEventListener("click", (e) => {
e.preventDefault();
document.getElementById('username').disabled = false;
document.getElementById("username").value = "";
localStorage.removeItem("username");
const differentAccount = document.getElementById("different-account");
if (differentAccount) {
differentAccount.remove();
}
})
}

const login = document.getElementById("login-init");
if (login) {
login.addEventListener('click', function (e) {
e.preventDefault();
const username = document.getElementById("username").value;
if (username) {
loginInit(username);
}
});

document.getElementById("username").addEventListener('keydown', function (event) {
if (event.key === 'Enter') {
event.preventDefault();
const username = document.getElementById("username").value;
if (username) {
loginInit(username);
}
}
});
}

const registerForm = document.getElementById("register-form");
if (registerForm) {
registerForm.addEventListener('click', function (e) {
e.preventDefault();
showForm('register');
});
}
} else {
container.innerHTML = getRegisterFormHTML();

const register = document.getElementById("register-init");
if (register) {
register.addEventListener('click', function (e) {
e.preventDefault();
const username = document.getElementById("username").value;
if (username) {
registerInit(username);
}
});

document.getElementById("username").addEventListener('keydown', function (event) {
if (event.key === 'Enter') {
event.preventDefault();
const username = document.getElementById("username").value;
if (username) {
registerInit(username);
}
}
});
}

const loginForm = document.getElementById("login-form");
if (loginForm) {
loginForm.addEventListener('click', function (e) {
e.preventDefault();
showForm('login');
});
}
}
}

showForm('login');
}