<#import "../layout.ftl" as layout>
<@layout.myLayout "Layout">
<#assign styleTag = "<script src=\"/api/v1/assets/" + app.appId + "/karakal.min.css\"></script>">
<#assign scriptTag = "<script src=\"/api/v1/assets/" + app.appId + "/karakal.min.js\"></script>">
<div class="container">
    <div class="columns mt-6">
        <div class="column is-half">
            <h1 class="title">Add Info for ${app.name}</h1>
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
            </div>
            <div class="field has-text-left">
                <label class="label">
                    Domain
                </label>
                <p class="control has-icons-left ">
                    <input class="input" type="text" name="name" disabled value="${app.domain}">
                    <span class="icon is-left"><i class="fas fa-globe"></i></span>
                </p>
            </div>
            <div class="field has-text-left">
                <label class="label">
                    Public Key
                </label>
                <p class="control has-icons-left ">
                    <input class="input" type="text" name="name" disabled value="${url}/api/v1/app/${app.appId}/.well-known/jwks.json">
                    <span class="icon is-left"><i class="fas fa-globe"></i></span>
                </p>
            </div>
            <div class="field has-text-left">
                <label class="label">
                    CSS URL
                </label>
                <p class="control has-icons-left ">
                    <input class="input" type="text" name="name" disabled value="${styleTag}">
                    <span class="icon is-left"><i class="fas fa-scroll"></i></span>
                </p>
            </div>
            <div class="field has-text-left">
                <label class="label">
                    Auth Div-Element
                </label>
                <p class="control has-icons-left ">
                    <input class="input" type="text" name="name" disabled value="&lt;div id=&quot;karakal-auth&quot;&gt;&lt;/div&gt;">
                    <span class="icon is-left"><i class="fas fa-scroll"></i></span>
                </p>
            </div>
            <div class="field has-text-left">
                <label class="label">
                    Script URL
                </label>
                <p class="control has-icons-left ">
                    <input class="input" type="text" name="name" disabled value="${scriptTag}">
                    <span class="icon is-left"><i class="fas fa-scroll"></i></span>
                </p>
            </div>
        </div>
    </div>
</div>
</@layout.myLayout>