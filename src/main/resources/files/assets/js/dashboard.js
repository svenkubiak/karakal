document.querySelectorAll('.deleteBtn').forEach(function(button){
    button.addEventListener('click', function(event){
        const appId = event.currentTarget.dataset.id;
        const name = event.currentTarget.dataset.name;
        console.log(appId);
        if (confirm('Are you sure you want to delete the app "'+  name + '"? This can not be undone.')) {
            console.log("deleting fe");
            fetch('/dashboard/app/' + appId, {
                method: 'DELETE',
                headers: {'Content-Type': 'application/json'},
            })
                .then(data => {
                    console.log('Item deleted');
                    window.location.replace("/dashboard");
                })
                .catch(error => {
                    console.error('There was a problem with the delete request:', error);
                });
        }
    });
});

document.addEventListener('DOMContentLoaded', () => {
    const $navbarBurgers = Array.prototype.slice.call(document.querySelectorAll('.navbar-burger, .dashboard-sidebar-toggle'), 0);
    const $sidebar = document.getElementById('dashboardSidebar');

    $navbarBurgers.forEach(el => {
        el.addEventListener('click', () => {
            const target = el.dataset.target;
            const $target = document.getElementById(target);
            if ($target) {
                el.classList.toggle('is-active');
                $target.classList.toggle('is-active');
            }
        });
    });

    // On mobile, close sidebar when a nav link is clicked
    if ($sidebar) {
        $sidebar.querySelectorAll('.dashboard-sidebar-item[href]').forEach(link => {
            link.addEventListener('click', () => {
                if (window.innerWidth < 1024) {
                    $sidebar.classList.remove('is-active');
                    const burger = document.querySelector('.dashboard-sidebar-toggle.is-active, .navbar-burger.is-active');
                    if (burger) burger.classList.remove('is-active');
                }
            });
        });
    }
});