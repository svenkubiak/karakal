<#import "../layout.ftl" as layout>
<@layout.myLayout "Layout">
<div class="container">
    <div class="columns">
        <div class="column is-half">
            <h1 class="title">Add information for '${app.name}'</h1>
        </div>
    </div>
    <div class="columns is-centered is-multiline">
        <div class="column">
            <div class="field has-text-left">
                <label class="label">
                    App id
                </label>
                <p class="control has-icons-left ">
                    <input class="input" type="text" name="name" disabled value="${app.id}">
                    <span class="icon is-left"><i class="fas fa-tag"></i></span>
                </p>
                <p class="help">The unique identifier of your application.</p>
            </div>
            <div class="field has-text-left">
                <label class="label">
                    URL
                </label>
                <p class="control has-icons-left ">
                    <input class="input" type="text" name="name" disabled value="${app.url}">
                    <span class="icon is-left"><i class="fas fa-globe"></i></span>
                </p>
                <p class="help">The URL your app is accessible at. User for Cookies, JWT and CORS verifications.</p>
            </div>
            <div class="field has-text-left">
                <label class="label">
                    JWKS URL
                </label>
                <p class="control has-icons-left ">
                    <input class="input" type="text" name="name" disabled value="${url}/api/v1/app/${app.appId}/jwks.json">
                    <span class="icon is-left"><i class="fas fa-globe"></i></span>
                </p>
                <p class="help">The URL where you can fetch the public key of your application to validate the JWT from Karakal in order to check a valid user session.</p>
            </div>
            <div class="field has-text-left">
                <label class="label">
                    Auth Div-Element
                </label>
                <p class="control has-icons-left ">
                    <input class="input" type="text" name="name" disabled value="&lt;div id=&quot;karakal-auth&quot;&gt;&lt;/div&gt;">
                    <span class="icon is-left"><i class="fas fa-scroll"></i></span>
                </p>
                <p class="help">The required Div-Element to put in the body of your Application login page. This is where the login and registration forms are loaded.</p>
            </div>
            <div class="field has-text-left">
                <label class="label">
                    Script URL
                </label>
                <p class="control has-icons-left ">
                    <input class="input" type="text" name="name" disabled value="${url}/api/v1/assets/${app.appId}/karakal.min.js">
                    <span class="icon is-left"><i class="fas fa-scroll"></i></span>
                </p>
                <p class="help">The required JavaScript containing the complete login for WebAuthn to be but in the Html part of your Application login page.</p>
            </div>
        </div>
    </div>
</div>
</@layout.myLayout>