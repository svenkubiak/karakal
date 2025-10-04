<#import "../layout.ftl" as layout>
<@layout.myLayout "Layout">
<div class="container">
    <div class="columns mt-6">
        <div class="column is-half">
            <h1 class="title">Add a new Application</h1>
        </div>
    </div>
    <div class="columns is-centered is-multiline">
        <div class="column">
            <form action="/dashboard/app" method="POST" class="box has-text-centered">
                <div class="field has-text-left">
                    <label class="label">
                        Name*
                    </label>
                    <p class="control has-icons-left ">
                        <input class="input <#if form.hasError("name")>is-danger</#if>" type="text" name="name" value="<#if (form.name)??>${form.name}<#elseif (app.name)??>${app.name}</#if>"
                               placeholder="Application name (e.g. MyApp)">
                        <span class="icon is-left"><i class="fas fa-tag"></i></span>
                    </p>
                    <p class="help">The name of your application.</p>
                    <#if form.hasError("name")>
                        <p class="help is-danger">${form.getError("name")}</p>
                    </#if>
                </div>
                <div class="field has-text-left">
                    <label class="label">
                        Login redirect*
                    </label>
                    <p class="control has-icons-left">
                        <input class="input <#if form.hasError("redirect")>is-danger</#if>" type="text" name="redirect" value="<#if (form.redirect)??>${form.redirect}<#elseif (app.redirect)??>${app.redirect}</#if>"
                               placeholder="Login redirect URL (e.g. https://myapp.com/callback)">
                        <span class="icon is-left"><i class="fas fa-share"></i></span>
                    </p>
                    <p class="help">The URL to redirect to after a successful login.</p>
                    <#if form.hasError("redirect")>
                        <p class="help is-danger">${form.getError("redirect")}</p>
                    </#if>
                </div>
                <div class="field has-text-left">
                    <label class="label">
                        URL*
                    </label>
                    <p class="control has-icons-left">
                        <input class="input <#if form.hasError("domain")>is-danger</#if>" type="text" name="url" value="<#if (form.url)??>${form.url}<#elseif (app.url)??>${app.url}</#if>"
                               placeholder="URL address (e.g. https://myapp.com)">
                        <span class="icon is-left"><i class="fas fa-server"></i></span>
                    </p>
                    <p class="help">The url of your application.</p>
                    <#if form.hasError("url")>
                        <p class="help is-danger">${form.getError("url")}</p>
                    </#if>
                </div>
                <div class="field has-text-left">
                    <label class="label">
                        Cookie ttl*
                    </label>
                    <p class="control has-icons-left">
                        <input class="input <#if form.hasError("ttl")>is-danger</#if>" type="text" name="ttl" value="<#if (form.ttl)??>${form.ttl}<#elseif (app.ttl)??>${app.ttl}<#else>600</#if>"
                               placeholder="Lifetime in seconds (e.g. 600)">
                        <span class="icon is-left"><i class="fas fa-clock"></i></span>
                    </p>
                    <p class="help">The lifetime of the cookie and JWT in seconds.</p>
                    <#if form.hasError("ttl")>
                        <p class="help is-danger">${form.getError("ttl")}</p>
                    </#if>
                </div>
                <div class="field has-text-left">
                    <label class="label">
                        Audience
                    </label>
                    <p class="control has-icons-left">
                        <input class="input <#if form.hasError("audience")>is-danger</#if>" type="text" name="audience" value="<#if (form.audience)??>${form.audience}<#elseif (app.audience)??>${app.audience}</#if>"
                               placeholder="Comma seperated list of hosts (e.g. localhost, api.myapp.com)">
                        <span class="icon is-left"><i class="fas fa-list"></i></span>
                    </p>
                    <p class="help">The required audience(s9 that will be set into the JWT.</p>
                    <#if form.hasError("audience")>
                        <p class="help is-danger">${form.getError("audience")}</p>
                    </#if>
                </div>
                <div class="field has-text-left">
                    <label class="label">
                        Allowed e-mail domains
                    </label>
                    <p class="control has-icons-left">
                        <input class="input <#if form.hasError("email")>is-danger</#if>" type="text" name="email" value="<#if (form.email)??>${form.email}<#elseif (app.email)??>${app.email}</#if>"
                               placeholder="Comma seperated list of @mydomain, @foo.de, @bar.de">
                        <span class="icon is-left"><i class="fas fa-envelope"></i></span>
                    </p>
                    <p class="help">The allowed domain endings that are enabled to register for this application. Leave blank for unrestriced.</p>
                    <#if form.hasError("email")>
                        <p class="help is-danger">${form.getError("email")}</p>
                    </#if>
                </div>
                <div class="field has-text-left">
                    <label class="checkbox">
                        <input type="checkbox" name="registration" value="true" <#if (app.registration)?? && app.registration>checked</#if>>
                        <span class="icon is-left"><i class="fas fa-user-plus"></i></span>
                        Allow new registrations
                    </label>
                    <p class="help">Wether or not users a allowed to register to this App.</p>
                </div>
                <div class="field has-text-left">
                    <div class="control">
                        <button class="button is-primary is-fullwidth" type="submit">
                            Save
                        </button>
                    </div>
                </div>
                <#if app??>
                <input type="hidden" value="${app.appId}" name="appId">
                </#if>
                <@csrfform/>
            </form>
        </div>
    </div>
</div>

</@layout.myLayout>