<#import "../layout.ftl" as layout>
<#function attrEsc str>
  <#if str??><#return str?replace('&','&amp;')?replace('"','&quot;')?replace('<','&lt;')?replace('>','&gt;')><#else><#return ''></#if>
</#function>
<#assign authDivValue='&lt;div id=&quot;karakal-auth&quot;&gt;&lt;/div&gt;' />
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
                    <div class="dashboard-info-field">
                        <p class="control has-icons-left">
                            <input class="input" type="text" disabled value="${app.id}">
                            <span class="icon is-left"><i class="fas fa-fingerprint"></i></span>
                        </p>
                        <button type="button" class="copy-btn" aria-label="Copy to clipboard" title="Copy" data-copy-value="${attrEsc(app.id!)}">
                            <i class="fas fa-copy"></i>
                        </button>
                    </div>
                    <p class="help">The unique identifier of your application.</p>
                </div>
                <div class="dashboard-info-item">
                    <label class="label">Cookie name</label>
                    <div class="dashboard-info-field">
                        <p class="control has-icons-left">
                            <input class="input" type="text" disabled value="__Host-karakal-auth">
                            <span class="icon is-left"><i class="fas fa-cookie"></i></span>
                        </p>
                        <button type="button" class="copy-btn" aria-label="Copy to clipboard" title="Copy" data-copy-value="__Host-karakal-auth">
                            <i class="fas fa-copy"></i>
                        </button>
                    </div>
                    <p class="help">The cookie name used for the auth session.</p>
                </div>
                <div class="dashboard-info-item">
                    <label class="label">URL</label>
                    <div class="dashboard-info-field">
                        <p class="control has-icons-left">
                            <input class="input" type="text" disabled value="${app.url}">
                            <span class="icon is-left"><i class="fas fa-globe"></i></span>
                        </p>
                        <button type="button" class="copy-btn" aria-label="Copy to clipboard" title="Copy" data-copy-value="${attrEsc(app.url!)}">
                            <i class="fas fa-copy"></i>
                        </button>
                    </div>
                    <p class="help">The URL your app is accessible at. Used for Cookies, JWT and CORS verifications.</p>
                </div>
                <div class="dashboard-info-item">
                    <label class="label">JWKS URL</label>
                    <div class="dashboard-info-field">
                        <p class="control has-icons-left">
                            <input class="input" type="text" disabled value="${url}/api/v1/app/${app.appId}/jwks.json">
                            <span class="icon is-left"><i class="fas fa-key"></i></span>
                        </p>
                        <button type="button" class="copy-btn" aria-label="Copy to clipboard" title="Copy" data-copy-value="${attrEsc(url + '/api/v1/app/' + app.appId + '/jwks.json')}">
                            <i class="fas fa-copy"></i>
                        </button>
                    </div>
                    <p class="help">The URL where you can fetch the public key to validate the JWT from Karakal.</p>
                </div>
                <div class="dashboard-info-item">
                    <label class="label">Auth Div-Element</label>
                    <div class="dashboard-info-field">
                        <p class="control has-icons-left">
                            <input class="input" type="text" disabled value="&lt;div id=&quot;karakal-auth&quot;&gt;&lt;/div&gt;">
                            <span class="icon is-left"><i class="fas fa-code"></i></span>
                        </p>
                        <button type="button" class="copy-btn" aria-label="Copy to clipboard" title="Copy" data-copy-value="${attrEsc(authDivValue)}">
                            <i class="fas fa-copy"></i>
                        </button>
                    </div>
                    <p class="help">The required Div-Element to put in the body of your application login page.</p>
                </div>
                <div class="dashboard-info-item">
                    <label class="label">Script URL</label>
                    <div class="dashboard-info-field">
                        <p class="control has-icons-left">
                            <input class="input" type="text" disabled value="${url}/api/v1/assets/${app.appId}/karakal.min.js">
                            <span class="icon is-left"><i class="fas fa-file-code"></i></span>
                        </p>
                        <button type="button" class="copy-btn" aria-label="Copy to clipboard" title="Copy" data-copy-value="${attrEsc(url + '/api/v1/assets/' + app.appId + '/karakal.min.js')}">
                            <i class="fas fa-copy"></i>
                        </button>
                    </div>
                    <p class="help">The required JavaScript for WebAuthn to be included in your application login page.</p>
                </div>
            </div>
        </div>
    </section>
</div>
</@layout.myLayout>
