const api = document.currentScript.getAttribute('data-api');
const applicationId = document.currentScript.getAttribute('data-application-id');
const container = document.getElementById("karakal-auth");

if (!api || api.trim() === "") {
    console.log("data-api is missing or empty");
    if (container) {
        container.innerHTML = error();
    }
}

if (!applicationId || applicationId.trim() === "") {
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
<div class="card success-card">
    <div class="header">
        <div class="logo">✓</div>
        <h1>Erfolg!</h1>
        <p class="lead">Deine Registrierung wurde erfolgreich abgeschlossen.</p>
    </div>
    <div class="button-container">
        <button class="btn" id="to-login">Jetzt anmelden</button>
    </div>
</div>
`;
}

function error() {
    return `
<div class="card error-card">
    <div class="header">
        <div class="logo">✕</div>
        <h1>Fehler!</h1>
        <p class="lead">Ooops.... da ist etwas schief gelaufen. Bitte versuche es erneut.</p>
    </div>
    <div class="button-container">
        <button class="btn" id="to-register">Erneut versuchen</button>
    </div>
</div>
`;
}

if (api && api.trim() !== "" &&
    applicationId && applicationId.trim() !== "" &&
    container) {
    console.log("Karakal-Auth-Init complete");

    function base64urlToBuffer(base64url) {
        if (typeof base64url !== "string") {
            if (base64url instanceof ArrayBuffer) return base64url;
            if (ArrayBuffer.isView(base64url)) return base64url.buffer;
            throw new TypeError("Challenge/User-ID ist kein String und kein ArrayBuffer");
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
        const res = await fetch(api + '/api/v1/register-init', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username, applicationId})
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

        const res2 = await fetch(api + '/api/v1/register-complete', {
            method: 'POST',
            headers: {'Content-Type': 'application/json', 'x-username': username, 'x-application-id': applicationId},
            body: JSON.stringify(serializedCredential)
        });

        if (res.status === 200) {
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
        const res = await fetch(api + '/api/v1/login-init', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({username, applicationId})
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
        const resp = await fetch(api + '/api/v1/login-complete', {
            method: 'POST',
            body: JSON.stringify(assertion),
            headers: {'Content-Type': 'application/json', 'x-username': username, 'x-application-id': applicationId},
        });

        if (resp.status === 200) {
            window.location.replace(resp.headers.get("x-login-redirect"));
        } else {
            container.innerHTML = error();
        }
    }

    function getLoginFormHTML() {
        return `
  <main class="card" role="main">
<div class="header">
<h1>Anmeldung</h1>
<p class="lead">Bitte gib deine E-Mail-Adresse ein um dich anzumdeldem.</p>
</div>


<div class="input-group">
<label for="username">E-Mail-Adresse</label>
<div class="input">
<input id="username" name="username" type="text" placeholder="Deine E-Mail-Adresse" required autocomplete="username" />
</div>
</div>


<div class="button-container">
<button class="btn" id="login-init">Anmelden</button>
</div>


<div class="footer">
Noch keinen Account? <a href="#" id="register-form">Jetzt registrieren</a>
</div>
</main>`;
    }

    function getRegisterFormHTML() {
        return `
  <main class="card" role="main">
<div class="header">
<h1>Registrierung</h1>
<p class="lead">Bitte gib eine E-Mail-Adresse ein um dich zu registrieren.</p>
</div>


<div class="input-group">
<label for="username">E-Mail-Adresse</label>
<div class="input">
<input id="username" name="username" type="text" placeholder="Deine E-Mail-Adresse" required autocomplete="username" />
</div>
</div>


<div class="button-container">
<button class="btn" id="register-init">Registrieren</button>
</div>


<div class="footer">
Bereis einen Account? <a href="#" id="login-form">Jetzt anmelden</a>
</div>
</main>`;
    }

    function showForm(formType) {
        if (container) {
            if (formType === 'login') {
                container.innerHTML = getLoginFormHTML();
                const registerForm = document.getElementById("register-form");
                registerForm.addEventListener('click', function (e) {
                    e.preventDefault();
                    showForm('register');
                });

                const login = document.getElementById("login-init");
                login.addEventListener('click', function (e) {
                    e.preventDefault();
                    const username = document.getElementById("username").value;
                    loginInit(username);
                })
            } else {
                container.innerHTML = getRegisterFormHTML();
                const loginForm = document.getElementById("login-form");
                loginForm.addEventListener('click', function (e) {
                    e.preventDefault();
                    showForm('login');
                });

                const register = document.getElementById("register-init");
                register.addEventListener('click', function (e) {
                    e.preventDefault();
                    const username = document.getElementById("username").value;
                    registerInit(username);
                })
            }
        }
    }

    showForm('login');
}