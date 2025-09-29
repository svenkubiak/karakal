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

const copyIcons = document.querySelectorAll('.copy');
copyIcons.forEach(icon => {
    icon.addEventListener('click', (event) => {
        const target = event.currentTarget;
        const textToCopy = target.value;

        const status = target.nextElementSibling;
        navigator.clipboard.writeText(textToCopy)
            .then(() => {
                if (status && status.classList.contains('copy-status')) {
                    status.textContent = 'Copied!';
                    setTimeout(() => { status.textContent = ''; }, 2000);
                }
            });
    });
});
