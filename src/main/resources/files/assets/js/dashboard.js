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
    const $navbarBurgers = Array.prototype.slice.call(document.querySelectorAll('.navbar-burger'), 0);

    $navbarBurgers.forEach( el => {
        el.addEventListener('click', () => {
            const target = el.dataset.target;
            const $target = document.getElementById(target);
            el.classList.toggle('is-active');
            $target.classList.toggle('is-active');
        });
    });
});