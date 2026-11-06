<#import "../layout.ftl" as layout>
<#function attrEsc str>
  <#if str??><#return str?replace('&','&amp;')?replace('"','&quot;')?replace('<','&lt;')?replace('>','&gt;')><#else><#return ''></#if>
</#function>
<#assign authDivValue='&lt;div id=&quot;karakal-auth&quot;&gt;&lt;/div&gt;' />
<@layout.myLayout "App information">
<div class="dashboard-page">
    <header class="dashboard-page-header">
        <div>
            <a href="/dashboard" class="dashboard-page-back"><i class="fas fa-arrow-left" aria-hidden="true"></i> Applications</a>
            <h1 class="dashboard-page-title">App information for '${app.name}'</h1>
            <p class="dashboard-page-subtitle">Use these values to integrate Karakal into your application.</p>
        </div>
        <a class="contrast" role="button" href="/dashboard/app/${app.appId}">
            <i class="fas fa-edit" aria-hidden="true"></i>
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
                    <label for="info-app-id">App id</label>
                    <div class="dashboard-info-field">
                        <div class="dashboard-control">
                            <i class="fas fa-fingerprint" aria-hidden="true"></i>
                            <input id="info-app-id" type="text" disabled value="${app.appId}">
                        </div>
                        <button type="button" class="copy-btn secondary" aria-label="Copy to clipboard" title="Copy" data-copy-value="${attrEsc(app.appId!)}">
                            <i class="fas fa-copy"></i>
                        </button>
                    </div>
                    <small>The unique identifier of your application.</small>
                </div>
                <div class="dashboard-info-item">
                    <label for="info-cookie">Cookie name</label>
                    <div class="dashboard-info-field">
                        <div class="dashboard-control">
                            <i class="fas fa-cookie" aria-hidden="true"></i>
                            <input id="info-cookie" type="text" disabled value="__Host-karakal-auth">
                        </div>
                        <button type="button" class="copy-btn secondary" aria-label="Copy to clipboard" title="Copy" data-copy-value="__Host-karakal-auth">
                            <i class="fas fa-copy"></i>
                        </button>
                    </div>
                    <small>The cookie name used for the auth session.</small>
                </div>
                <div class="dashboard-info-item">
                    <label for="info-url">URL</label>
                    <div class="dashboard-info-field">
                        <div class="dashboard-control">
                            <i class="fas fa-globe" aria-hidden="true"></i>
                            <input id="info-url" type="text" disabled value="${app.url}">
                        </div>
                        <button type="button" class="copy-btn secondary" aria-label="Copy to clipboard" title="Copy" data-copy-value="${attrEsc(app.url!)}">
                            <i class="fas fa-copy"></i>
                        </button>
                    </div>
                    <small>The URL your app is accessible at. Used for Cookies, JWT and CORS verifications.</small>
                </div>
                <div class="dashboard-info-item">
                    <label for="info-jwks">JWKS URL</label>
                    <div class="dashboard-info-field">
                        <div class="dashboard-control">
                            <i class="fas fa-key" aria-hidden="true"></i>
                            <input id="info-jwks" type="text" disabled value="${url}/api/v1/app/${app.appId}/jwks.json">
                        </div>
                        <button type="button" class="copy-btn secondary" aria-label="Copy to clipboard" title="Copy" data-copy-value="${attrEsc(url + '/api/v1/app/' + app.appId + '/jwks.json')}">
                            <i class="fas fa-copy"></i>
                        </button>
                    </div>
                    <small>The URL where you can fetch the public key to validate the JWT from Karakal.</small>
                </div>
                <div class="dashboard-info-item">
                    <label for="info-auth-div">Auth Div-Element</label>
                    <div class="dashboard-info-field">
                        <div class="dashboard-control">
                            <i class="fas fa-code" aria-hidden="true"></i>
                            <input id="info-auth-div" type="text" disabled value="&lt;div id=&quot;karakal-auth&quot;&gt;&lt;/div&gt;">
                        </div>
                        <button type="button" class="copy-btn secondary" aria-label="Copy to clipboard" title="Copy" data-copy-value="${attrEsc(authDivValue)}">
                            <i class="fas fa-copy"></i>
                        </button>
                    </div>
                    <small>The required Div-Element to put in the body of your application login page.</small>
                </div>
                <div class="dashboard-info-item">
                    <label for="info-script">Script URL</label>
                    <div class="dashboard-info-field">
                        <div class="dashboard-control">
                            <i class="fas fa-file-code" aria-hidden="true"></i>
                            <input id="info-script" type="text" disabled value="${url}/api/v1/assets/${app.appId}/karakal.min.js">
                        </div>
                        <button type="button" class="copy-btn secondary" aria-label="Copy to clipboard" title="Copy" data-copy-value="${attrEsc(url + '/api/v1/assets/' + app.appId + '/karakal.min.js')}">
                            <i class="fas fa-copy"></i>
                        </button>
                    </div>
                    <small>The required JavaScript for WebAuthn to be included in your application login page.</small>
                </div>
            </div>
        </div>
    </section>
</div>
</@layout.myLayout>
