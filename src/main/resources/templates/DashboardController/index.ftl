<#import "../layout.ftl" as layout>
<@layout.myLayout "Layout">
<div class="container">
    <div class="columns is-centered is-multiline">
        <div class="column is-one-quarter">
            <a href="/dashboard/app">
            <div class="card">
                <div class="card-content add-card">
                    +
                </div>
            </div>
            </a>
        </div>
        <#list apps as app>
            <div class="column is-one-quarter">
                <div class="card">
                    <header class="card-header">
                        <p class="card-header-title">${app.name}</p>
                    </header>
                    <div class="card-content">
                        <div class="tags">
                            <span class="tag is-info">${app.url}</span>
                            <#if app.registration>
                                <span class="tag is-info">Registration enabled</span>
                            <#else>
                                <span class="tag is-warning">Registration disabled</span>
                            </#if>
                        </div>
                    </div>
                    <footer class="card-footer is-justify-content-flex-end">
                        <a href="/dashboard/app/${app.appId}/info" class="card-footer-item has-text-right open-modal-btn" title="Info">
                            <span class="icon is-small">
                              <i class="fas fa-info-circle"></i>
                            </span>
                        </a>
                            <a href="/dashboard/app/${app.appId}" class="card-footer-item has-text-right" title="Edit">
                            <span class="icon is-small">
                              <i class="fas fa-edit"></i>
                            </span>
                        </a>
                        <a data-id="${app.appId}" data-name="${app.name}" class="card-footer-item has-text-right deleteBtn" title="Delete">
                            <span class="icon is-small">
                              <i class="fas fa-trash-alt"></i>
                            </span>
                        </a>
                    </footer>
                </div>
            </div>
        </#list>
    </div>
</div>
</@layout.myLayout>