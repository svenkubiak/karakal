<#import "../layout.ftl" as layout>
<@layout.myLayout "Applications">
<div class="dashboard-page">
    <header class="dashboard-page-header">
        <div>
            <h1 class="dashboard-page-title">Applications</h1>
            <p class="dashboard-page-subtitle">Manage your registered applications and their settings.</p>
        </div>
        <a class="contrast" role="button" href="/dashboard/app">
            <i class="fas fa-plus" aria-hidden="true"></i>
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
                        <span class="tag">${app.url}</span>
                        <#if app.registration>
                            <span class="tag tag-info">Registration enabled</span>
                        <#else>
                            <span class="tag tag-warning">Registration disabled</span>
                        </#if>
                    </div>
                </div>
                <footer class="dashboard-card-footer">
                    <a href="/dashboard/app/${app.appId}/info" class="dashboard-card-action" title="Info">
                        <i class="fas fa-info-circle" aria-hidden="true"></i>
                    </a>
                    <a href="/dashboard/app/${app.appId}" class="dashboard-card-action" title="Edit">
                        <i class="fas fa-edit" aria-hidden="true"></i>
                    </a>
                    <a data-id="${app.appId}" data-name="${app.name}" class="dashboard-card-action deleteBtn" title="Delete">
                        <i class="fas fa-trash-alt" aria-hidden="true"></i>
                    </a>
                </footer>
            </article>
        </#list>
    </div>
</div>
</@layout.myLayout>