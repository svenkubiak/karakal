<#macro myLayout title="Layout example">
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
    <header class="dashboard-top">
      <div class="dashboard-top-inner">
        <a href="/dashboard" class="dashboard-logo">KARAKAL</a>
        <nav class="dashboard-nav" aria-label="Dashboard">
          <a href="/dashboard/logout" role="button" class="danger dashboard-logout">
            <i class="fas fa-sign-out-alt" aria-hidden="true"></i>
            <span>Log out</span>
          </a>
        </nav>
      </div>
    </header>
    <main class="dashboard-content">
      <#nested>
    </main>
  </body>
  <script src="/assets/js/dashboard.min.js"></script>
</html>
</#macro>
