document.querySelectorAll('.deleteBtn').forEach(function(button){
    button.addEventListener('click', function(event){
        const appId = event.currentTarget.dataset.id;
        const name = event.currentTarget.dataset.name;
        if (confirm('Are you sure you want to delete the app "'+  name + '"? This can not be undone.')) {
            fetch('/dashboard/app/' + appId, {
                method: 'DELETE',
                headers: {'Content-Type': 'application/json'},
            })
                .then(function() {
                    window.location.replace("/dashboard");
                })
                .catch(function(error) {
                    console.error('There was a problem with the delete request:', error);
                });
        }
    });
});

document.addEventListener('DOMContentLoaded', function() {
    const $navbarBurgers = Array.prototype.slice.call(document.querySelectorAll('.navbar-burger, .dashboard-sidebar-toggle'), 0);
    const $sidebar = document.getElementById('dashboardSidebar');

    $navbarBurgers.forEach(function(el) {
        el.addEventListener('click', function() {
            const target = el.dataset.target;
            const $target = document.getElementById(target);
            if ($target) {
                el.classList.toggle('is-active');
                $target.classList.toggle('is-active');
            }
        });
    });

    if ($sidebar) {
        $sidebar.querySelectorAll('.dashboard-sidebar-item[href]').forEach(function(link) {
            link.addEventListener('click', function() {
                if (window.innerWidth < 1024) {
                    $sidebar.classList.remove('is-active');
                    const burger = document.querySelector('.dashboard-sidebar-toggle.is-active, .navbar-burger.is-active');
                    if (burger) burger.classList.remove('is-active');
                }
            });
        });
    }

    // Copy to clipboard (integration page)
    document.querySelectorAll('.copy-btn').forEach(function(btn) {
        btn.addEventListener('click', function(e) {
            e.preventDefault();
            var text = btn.getAttribute('data-copy-value') || '';
            if (!text) return;
            var icon = btn.querySelector('i');
            function showCopied() {
                if (icon) { icon.classList.remove('fa-copy'); icon.classList.add('fa-check'); }
                setTimeout(function() {
                    if (icon) { icon.classList.remove('fa-check'); icon.classList.add('fa-copy'); }
                }, 2000);
            }
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(text).then(showCopied).catch(function() {
                    fallbackCopy(text, showCopied);
                });
            } else {
                fallbackCopy(text, showCopied);
            }
        });
    });
});

function fallbackCopy(text, onDone) {
    var el = document.createElement('textarea');
    el.value = text;
    el.setAttribute('readonly', '');
    el.style.cssText = 'position:fixed;top:0;left:0;width:2em;height:2em;padding:0;border:0;outline:none;boxShadow:none;background:transparent;';
    document.body.appendChild(el);
    el.focus();
    el.select();
    el.setSelectionRange(0, text.length);
    try {
        if (document.execCommand('copy') && onDone) onDone();
    } catch (err) {}
    document.body.removeChild(el);
}