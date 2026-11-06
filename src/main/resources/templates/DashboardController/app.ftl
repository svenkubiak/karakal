<#import "../layout.ftl" as layout>
<@layout.myLayout "Application details">
<div class="dashboard-page">
    <header class="dashboard-page-header">
        <div>
            <a href="/dashboard" class="dashboard-page-back"><i class="fas fa-arrow-left" aria-hidden="true"></i> Applications</a>
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
                    <label for="app-name">Name *</label>
                    <div class="dashboard-control">
                        <i class="fas fa-tag" aria-hidden="true"></i>
                        <input id="app-name" type="text" name="name" <#if (app.dashboard)?? && app.dashboard>readonly</#if> value="<#if (form?? && form.name??)>${form.name}<#elseif (app.name)??>${app.name}</#if>"
                               placeholder="Application name (e.g. MyApp)"<#if form?? && form.hasError("name")> aria-invalid="true" aria-describedby="app-name-error"</#if>>
                    </div>
                    <small id="app-name-help">The name of your application.</small>
                    <#if form?? && form.hasError("name")>
                        <small id="app-name-error" class="is-error">${form.getError("name")}</small>
                    </#if>
                </div>
                <div class="field">
                    <label for="app-redirect">Login redirect *</label>
                    <div class="dashboard-control">
                        <i class="fas fa-share" aria-hidden="true"></i>
                        <input id="app-redirect" type="text" name="redirect" <#if (app.dashboard)?? && app.dashboard>readonly</#if> value="<#if (form?? && form.redirect??)>${form.redirect}<#elseif (app.redirect)??>${app.redirect}</#if>"
                               placeholder="Login redirect URL (e.g. https://myapp.com/callback)"<#if form?? && form.hasError("redirect")> aria-invalid="true" aria-describedby="app-redirect-error"</#if>>
                    </div>
                    <small id="app-redirect-help">The URL to redirect to after a successful login.</small>
                    <#if form?? && form.hasError("redirect")>
                        <small id="app-redirect-error" class="is-error">${form.getError("redirect")}</small>
                    </#if>
                </div>
                <div class="field">
                    <label for="app-url">URL *</label>
                    <div class="dashboard-control">
                        <i class="fas fa-server" aria-hidden="true"></i>
                        <input id="app-url" type="text" name="url" <#if (app.dashboard)?? && app.dashboard>readonly</#if> value="<#if (form?? && form.url??)>${form.url}<#elseif (app.url)??>${app.url}</#if>"
                               placeholder="URL address (e.g. https://myapp.com)"<#if form?? && form.hasError("domain")> aria-invalid="true" aria-describedby="app-url-error"</#if>>
                    </div>
                    <small id="app-url-help">The URL of your application.</small>
                    <#if form?? && form.hasError("url")>
                        <small id="app-url-error" class="is-error">${form.getError("url")}</small>
                    </#if>
                </div>
                <div class="field">
                    <label for="app-ttl">Cookie ttl *</label>
                    <div class="dashboard-control">
                        <i class="fas fa-clock" aria-hidden="true"></i>
                        <input id="app-ttl" type="text" name="ttl" value="<#if (form?? && form.ttl??)>${form.ttl}<#elseif (app.ttl)??>${app.ttl}<#else>600</#if>"
                               placeholder="Lifetime in seconds (e.g. 600)"<#if form?? && form.hasError("ttl")> aria-invalid="true" aria-describedby="app-ttl-error"</#if>>
                    </div>
                    <small id="app-ttl-help">The lifetime of the cookie and JWT in seconds.</small>
                    <#if form?? && form.hasError("ttl")>
                        <small id="app-ttl-error" class="is-error">${form.getError("ttl")}</small>
                    </#if>
                </div>
                <div class="field">
                    <label for="app-audience">Audience</label>
                    <div class="dashboard-control">
                        <i class="fas fa-list" aria-hidden="true"></i>
                        <input id="app-audience" type="text" name="audience" <#if (app.dashboard)?? && app.dashboard>readonly</#if> value="<#if (form?? && form.audience??)>${form.audience}<#elseif (app.audience)??>${app.audience}</#if>"
                               placeholder="The host of your application (e.g. localhost or api.myapp.com)"<#if form?? && form.hasError("audience")> aria-invalid="true" aria-describedby="app-audience-error"</#if>>
                    </div>
                    <small id="app-audience-help">The audience that will be set into the JWT.</small>
                    <#if form?? && form.hasError("audience")>
                        <small id="app-audience-error" class="is-error">${form.getError("audience")}</small>
                    </#if>
                </div>
                <div class="field">
                    <label for="app-email">Allowed e-mail domains</label>
                    <div class="dashboard-control">
                        <i class="fas fa-envelope" aria-hidden="true"></i>
                        <input id="app-email" type="text" name="email" value="<#if (form?? && form.email??)>${form.email}<#elseif (app.email)??>${app.email}</#if>"
                               placeholder="Comma separated list of @foo.de, bar.de, sub.example.com"<#if form?? && form.hasError("email")> aria-invalid="true" aria-describedby="app-email-error"</#if>>
                    </div>
                    <small id="app-email-help">The allowed e-mail domains that are enabled to register for this application. Sub domains of an allowed domain are included. Leave blank for unrestricted.</small>
                    <#if form?? && form.hasError("email")>
                        <small id="app-email-error" class="is-error">${form.getError("email")}</small>
                    </#if>
                </div>
                <div class="field">
                    <label>
                        <input type="checkbox" name="registration" value="true" <#if (app.registration)?? && app.registration>checked</#if>>
                        Allow new registrations
                    </label>
                    <small>Whether or not users are allowed to register to this App.</small>
                </div>
                <div class="field">
                    <button class="contrast" type="submit">Save</button>
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
