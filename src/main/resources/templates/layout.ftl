<#macro myLayout title="Layout example" section="applications">
<!DOCTYPE html>
<html lang="en" data-theme="light">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>${title} — Karakal</title>
    <meta name="csrf-token" content="<@csrftoken/>">
    <link rel="stylesheet" href="/assets/css/blades.min.css">
    <link rel="stylesheet" href="/assets/css/all.min.css">
    <link rel="stylesheet" href="/assets/css/dashboard.min.css">
  </head>
  <body class="dashboard-body">
    <div class="dashboard-layout">
      <aside class="dashboard-sidebar" id="dashboardSidebar">
        <div class="dashboard-sidebar-brand">
          <a href="/dashboard" class="dashboard-sidebar-logo">
            <span class="dashboard-sidebar-logo-text">KARAKAL</span>
          </a>
        </div>
        <nav class="dashboard-sidebar-nav">
          <p class="dashboard-sidebar-menu-label">Menu</p>
          <a href="/dashboard" class="dashboard-sidebar-item <#if section == "applications">is-active</#if>"<#if section == "applications"> aria-current="page"</#if>>
            <i class="fas fa-th-large" aria-hidden="true"></i>
            <span>Applications</span>
          </a>
        </nav>
        <div class="dashboard-sidebar-footer">
          <a href="/dashboard/logout" role="button" class="danger">
            <i class="fas fa-sign-out-alt" aria-hidden="true"></i>
            <span>Log out</span>
          </a>
        </div>
      </aside>
      <div class="dashboard-main">
        <header class="dashboard-header">
          <button type="button" class="dashboard-sidebar-toggle" aria-label="menu" aria-expanded="false" data-target="dashboardSidebar">
            <span aria-hidden="true"></span>
            <span aria-hidden="true"></span>
            <span aria-hidden="true"></span>
          </button>
        </header>
        <main class="dashboard-content">
          <#nested>
        </main>
      </div>
    </div>
  </body>
  <script src="/assets/js/dashboard.min.js"></script>
</html>
</#macro>
