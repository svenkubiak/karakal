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
<div class="card success-card">
    <div class="header">
        <div class="logo">✓</div>
        <h1>Success!</h1>
        <p class="lead">Your registration was completed successfully.</p>
    </div>
    <div class="button-container">
        <button class="btn" id="to-login">Sign in now</button>
    </div>
</div>
`;
}

function error() {
    return `
<div class="card error-card">
    <div class="header">
        <div class="logo">✕</div>
        <h1>Error!</h1>
        <p class="lead">Oops... something went wrong. Please try again.</p>
    </div>
    <div class="button-container">
        <button class="btn" id="to-register">Try again</button>
    </div>
</div>
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
            window.location.replace(resp.headers.get("karakal-login-redirect"));
        } else {
            container.innerHTML = error();
        }
    }

    function getLoginFormHTML() {
        return `
<main class="card" role="main">
    <div class="header">
        <h1>Sign In</h1>
        <p class="lead">Please enter your email address to sign in.</p>
    </div>
    <div class="input-group">
        <label for="username">Email Address</label>
        <div class="input">
            <input id="username" name="username" type="text" placeholder="Your email address" required autocomplete="username" />
        </div>
 <div class="field has-addons" id="different-account">
  <p class="control is-expanded has-text-centered">
    <span class="small-text has-text-centered"><a href="#" id="use-different-account">Sign in with another account</a></span>
  </p>
</div>
    </div>
    <div class="button-container">
        <button class="btn" id="login-init">Sign In</button>
    </div>
    <#if registration>
        <div class="footer">
            Don’t have an account? <a href="#" id="register-form">Register now</a>
        </div>
    </#if>
</main>
`;
    }

    function getRegisterFormHTML() {
        return `
<main class="card" role="main">
    <div class="header">
        <h1>Registration</h1>
        <p class="lead">Please enter an email address to register.</p>
    </div>
    <div class="input-group">
        <label for="username">Email Address</label>
        <div class="input">
            <input id="username" name="username" type="text" placeholder="Your email address" required autocomplete="username" />
        </div>
    </div>
    <div class="button-container">
        <button class="btn" id="register-init">Register</button>
    </div>
    <div class="footer">
        Already have an account? <a href="#" id="login-form">Sign in now</a>
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