<#import "../layout.ftl" as layout>
<@layout.myLayout "Layout">

<div class="container">
    <div class="columns is-centered mt-6">
        <div class="column is-half">
            <div class="field has-addons is-justify-content-center">
                <div class="control is-expanded">
                    <input class="input" type="text" placeholder="Search..." />
                </div>
                <div class="control">
                    <a href="/dashboard/app" class="button is-primary" title="Add Card">
            <span class="icon">
              <i class="fas fa-plus"></i>
            </span>
                        <span>Add</span>
                    </a>
                </div>
            </div>
        </div>
    </div>

    <div class="columns is-centered is-multiline">
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
        <#else>
            <h1 class="title">No Apps yet.</h1>
        </#list>
    </div>
</div>

<div class="modal" id="exampleModal">
    <div class="modal-background"></div>
    <div class="modal-card">
        <header class="modal-card-head">
            <p class="modal-card-title">App information</p>
            <button class="delete" aria-label="close"></button>
        </header>
        <section class="modal-card-body" id="modalBody">
            <div class="has-text-grey">Loading...</div>
        </section>
        <footer class="modal-card-foot">
            <button class="button close-modal-btn">Close</button>
        </footer>
    </div>
</div>

<script>


</script>

</@layout.myLayout>