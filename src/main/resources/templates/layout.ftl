<#macro myLayout title="Layout example">
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Karakal Dashboard</title>
    <link rel="stylesheet" href="/assets/css/bulma.min.css">
    <link rel="stylesheet" href="/assets/css/all.min.css">
    <link rel="stylesheet" href="/assets/css/dashboard.css">
  </head>
  <body>
  <nav class="navbar is-link" role="navigation" aria-label="main navigation">
      <div class="navbar-brand">
          <a class="navbar-item" href="/dashboard">
              <svg width="640" height="160" viewBox="0 0 640 160" xmlns="http://www.w3.org/2000/svg">
                  <text x="50" y="125" font-size="110" font-family="Arial, Helvetica, sans-serif" font-weight="bold" fill="#fff">KARAKAL</text>
              </svg>
          </a>
          <a role="button" class="navbar-burger" aria-label="menu" aria-expanded="false" data-target="navbarBasic">
              <span aria-hidden="true"></span>
              <span aria-hidden="true"></span>
              <span aria-hidden="true"></span>
              <span aria-hidden="true"></span>
          </a>
      </div>
      <div id="navbarBasic" class="navbar-menu">
          <div class="navbar-start">
              <a href="/dashboard" class="navbar-item">
                  Applications
              </a>
          </div>
          <div class="navbar-end">
              <div class="navbar-item">
                  <div class="buttons">
                      <a class="button is-danger" href="/dashboard/logout">
                          <strong>Logout</strong>
                      </a>
                  </div>
              </div>
          </div>
      </div>
  </nav>
  <#nested>
  </body>
  <script src="/assets/js/dashboard.js"></script>
</html>
</#macro>
