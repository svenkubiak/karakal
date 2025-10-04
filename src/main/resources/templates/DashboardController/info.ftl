<#import "../layout.ftl" as layout>
<@layout.myLayout "Layout">
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
                    URL
                </label>
                <p class="control has-icons-left ">
                    <input class="input" type="text" name="name" disabled value="${app.url}">
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
                    <input class="input" type="text" name="name" disabled value="${url}/api/v1/assets/${app.appId}/karakal.min.css">
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
                    <input class="input" type="text" name="name" disabled value="${url}/api/v1/assets/${app.appId}/karakal.min.js">
                    <span class="icon is-left"><i class="fas fa-scroll"></i></span>
                </p>
            </div>
        </div>
    </div>
</div>
</@layout.myLayout>