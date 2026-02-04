<#import "../layout.ftl" as layout>
<@layout.myLayout "App information">
<div class="dashboard-page">
    <header class="dashboard-page-header">
        <div>
            <a href="/dashboard" class="dashboard-page-back"><span class="icon is-small"><i class="fas fa-arrow-left"></i></span> Applications</a>
            <h1 class="dashboard-page-title">App information for '${app.name}'</h1>
            <p class="dashboard-page-subtitle">Use these values to integrate Karakal into your application.</p>
        </div>
        <a class="button is-dark" href="/dashboard/app/${app.appId}">
            <span class="icon is-small"><i class="fas fa-edit"></i></span>
            <span>Edit application</span>
        </a>
    </header>

    <section class="dashboard-section">
        <header class="dashboard-section-header">
            <h2 class="dashboard-section-title">Integration</h2>
        </header>
        <div class="dashboard-section-content">
            <div class="dashboard-info-list">
                <div class="dashboard-info-item">
                    <label class="label">App id</label>
                    <p class="control has-icons-left">
                        <input class="input" type="text" disabled value="${app.id}">
                        <span class="icon is-left"><i class="fas fa-fingerprint"></i></span>
                    </p>
                    <p class="help">The unique identifier of your application.</p>
                </div>
                <div class="dashboard-info-item">
                    <label class="label">Cookie name</label>
                    <p class="control has-icons-left">
                        <input class="input" type="text" disabled value="__Host-karakal-auth">
                        <span class="icon is-left"><i class="fas fa-cookie"></i></span>
                    </p>
                    <p class="help">The cookie name used for the auth session.</p>
                </div>
                <div class="dashboard-info-item">
                    <label class="label">URL</label>
                    <p class="control has-icons-left">
                        <input class="input" type="text" disabled value="${app.url}">
                        <span class="icon is-left"><i class="fas fa-globe"></i></span>
                    </p>
                    <p class="help">The URL your app is accessible at. Used for Cookies, JWT and CORS verifications.</p>
                </div>
                <div class="dashboard-info-item">
                    <label class="label">JWKS URL</label>
                    <p class="control has-icons-left">
                        <input class="input" type="text" disabled value="${url}/api/v1/app/${app.appId}/jwks.json">
                        <span class="icon is-left"><i class="fas fa-key"></i></span>
                    </p>
                    <p class="help">The URL where you can fetch the public key to validate the JWT from Karakal.</p>
                </div>
                <div class="dashboard-info-item">
                    <label class="label">Auth Div-Element</label>
                    <p class="control has-icons-left">
                        <input class="input" type="text" disabled value="&lt;div id=&quot;karakal-auth&quot;&gt;&lt;/div&gt;">
                        <span class="icon is-left"><i class="fas fa-code"></i></span>
                    </p>
                    <p class="help">The required Div-Element to put in the body of your application login page.</p>
                </div>
                <div class="dashboard-info-item">
                    <label class="label">Script URL</label>
                    <p class="control has-icons-left">
                        <input class="input" type="text" disabled value="${url}/api/v1/assets/${app.appId}/karakal.min.js">
                        <span class="icon is-left"><i class="fas fa-file-code"></i></span>
                    </p>
                    <p class="help">The required JavaScript for WebAuthn to be included in your application login page.</p>
                </div>
            </div>
        </div>
    </section>
</div>
</@layout.myLayout>
