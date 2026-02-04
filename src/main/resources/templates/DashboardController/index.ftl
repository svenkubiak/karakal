<#import "../layout.ftl" as layout>
<@layout.myLayout "Applications">
<div class="dashboard-page">
    <header class="dashboard-page-header">
        <div>
            <h1 class="dashboard-page-title">Applications</h1>
            <p class="dashboard-page-subtitle">Manage your registered applications and their settings.</p>
        </div>
        <a class="button is-dark" href="/dashboard/app">
            <span class="icon is-small"><i class="fas fa-plus"></i></span>
            <span>Add application</span>
        </a>
    </header>
    <div class="dashboard-cards">
        <#list apps as app>
            <article class="dashboard-card dashboard-card-app">
                <header class="dashboard-card-header">
                    <h2 class="dashboard-card-title">${app.name}</h2>
                </header>
                <div class="dashboard-card-content">
                    <div class="dashboard-card-tags">
                        <span class="tag is-light">${app.url}</span>
                        <#if app.registration>
                            <span class="tag is-info is-light">Registration enabled</span>
                        <#else>
                            <span class="tag is-warning is-light">Registration disabled</span>
                        </#if>
                    </div>
                </div>
                <footer class="dashboard-card-footer">
                    <a href="/dashboard/app/${app.appId}/info" class="dashboard-card-action" title="Info">
                        <span class="icon is-small"><i class="fas fa-info-circle"></i></span>
                    </a>
                    <a href="/dashboard/app/${app.appId}" class="dashboard-card-action" title="Edit">
                        <span class="icon is-small"><i class="fas fa-edit"></i></span>
                    </a>
                    <a data-id="${app.appId}" data-name="${app.name}" class="dashboard-card-action deleteBtn" title="Delete">
                        <span class="icon is-small"><i class="fas fa-trash-alt"></i></span>
                    </a>
                </footer>
            </article>
        </#list>
    </div>
</div>
</@layout.myLayout>