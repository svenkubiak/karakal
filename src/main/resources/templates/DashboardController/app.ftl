<#import "../layout.ftl" as layout>
<@layout.myLayout "Application details">
<div class="dashboard-page">
    <header class="dashboard-page-header">
        <div>
            <a href="/dashboard" class="dashboard-page-back"><span class="icon is-small"><i class="fas fa-arrow-left"></i></span> Applications</a>
            <h1 class="dashboard-page-title"><#if app??>${app.name}<#else>New application</#if></h1>
            <p class="dashboard-page-subtitle"><#if app??>Edit application settings.<#else>Create a new application.</#if></p>
        </div>
    </header>

    <section class="dashboard-section">
        <header class="dashboard-section-header">
            <h2 class="dashboard-section-title">Application details</h2>
        </header>
        <div class="dashboard-section-content">
            <form action="/dashboard/app" method="POST" class="dashboard-form">
                <div class="field">
                    <label class="label">Name *</label>
                    <p class="control has-icons-left">
                        <input class="input <#if (form?? && form.hasError("name"))>is-danger</#if>" type="text" name="name" value="<#if (form?? && form.name??)>${form.name}<#elseif (app.name)??>${app.name}</#if>"
                               placeholder="Application name (e.g. MyApp)">
                        <span class="icon is-left"><i class="fas fa-tag"></i></span>
                    </p>
                    <p class="help">The name of your application.</p>
                    <#if form?? && form.hasError("name")>
                        <p class="help is-danger">${form.getError("name")}</p>
                    </#if>
                </div>
                <div class="field">
                    <label class="label">Login redirect *</label>
                    <p class="control has-icons-left">
                        <input class="input <#if (form?? && form.hasError("redirect"))>is-danger</#if>" type="text" name="redirect" value="<#if (form?? && form.redirect??)>${form.redirect}<#elseif (app.redirect)??>${app.redirect}</#if>"
                               placeholder="Login redirect URL (e.g. https://myapp.com/callback)">
                        <span class="icon is-left"><i class="fas fa-share"></i></span>
                    </p>
                    <p class="help">The URL to redirect to after a successful login.</p>
                    <#if form?? && form.hasError("redirect")>
                        <p class="help is-danger">${form.getError("redirect")}</p>
                    </#if>
                </div>
                <div class="field">
                    <label class="label">URL *</label>
                    <p class="control has-icons-left">
                        <input class="input <#if (form?? && form.hasError("domain"))>is-danger</#if>" type="text" name="url" value="<#if (form?? && form.url??)>${form.url}<#elseif (app.url)??>${app.url}</#if>"
                               placeholder="URL address (e.g. https://myapp.com)">
                        <span class="icon is-left"><i class="fas fa-server"></i></span>
                    </p>
                    <p class="help">The URL of your application.</p>
                    <#if form?? && form.hasError("url")>
                        <p class="help is-danger">${form.getError("url")}</p>
                    </#if>
                </div>
                <div class="field">
                    <label class="label">Cookie ttl *</label>
                    <p class="control has-icons-left">
                        <input class="input <#if (form?? && form.hasError("ttl"))>is-danger</#if>" type="text" name="ttl" value="<#if (form?? && form.ttl??)>${form.ttl}<#elseif (app.ttl)??>${app.ttl}<#else>600</#if>"
                               placeholder="Lifetime in seconds (e.g. 600)">
                        <span class="icon is-left"><i class="fas fa-clock"></i></span>
                    </p>
                    <p class="help">The lifetime of the cookie and JWT in seconds.</p>
                    <#if form?? && form.hasError("ttl")>
                        <p class="help is-danger">${form.getError("ttl")}</p>
                    </#if>
                </div>
                <div class="field">
                    <label class="label">Audience</label>
                    <p class="control has-icons-left">
                        <input class="input <#if (form?? && form.hasError("audience"))>is-danger</#if>" type="text" name="audience" value="<#if (form?? && form.audience??)>${form.audience}<#elseif (app.audience)??>${app.audience}</#if>"
                               placeholder="Comma separated list of hosts (e.g. localhost, api.myapp.com)">
                        <span class="icon is-left"><i class="fas fa-list"></i></span>
                    </p>
                    <p class="help">The required audience(s) that will be set into the JWT.</p>
                    <#if form?? && form.hasError("audience")>
                        <p class="help is-danger">${form.getError("audience")}</p>
                    </#if>
                </div>
                <div class="field">
                    <label class="label">Allowed e-mail domains</label>
                    <p class="control has-icons-left">
                        <input class="input <#if (form?? && form.hasError("email"))>is-danger</#if>" type="text" name="email" value="<#if (form?? && form.email??)>${form.email}<#elseif (app.email)??>${app.email}</#if>"
                               placeholder="Comma separated list of @mydomain, @foo.de, @bar.de">
                        <span class="icon is-left"><i class="fas fa-envelope"></i></span>
                    </p>
                    <p class="help">The allowed domain endings that are enabled to register for this application. Leave blank for unrestricted.</p>
                    <#if form?? && form.hasError("email")>
                        <p class="help is-danger">${form.getError("email")}</p>
                    </#if>
                </div>
                <div class="field">
                    <label class="checkbox">
                        <input type="checkbox" name="registration" value="true" <#if (app.registration)?? && app.registration>checked</#if>>
                        Allow new registrations
                    </label>
                    <p class="help">Whether or not users are allowed to register to this App.</p>
                </div>
                <div class="field">
                    <button class="button is-dark is-fullwidth" type="submit">Save</button>
                </div>
                <#if app??>
                <input type="hidden" value="${app.appId}" name="appId">
                </#if>
                <@csrfform/>
            </form>
        </div>
    </section>
</div>
</@layout.myLayout>
