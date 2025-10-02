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

const modal = document.getElementById('exampleModal');
const modalBody = document.getElementById('modalBody');

modal.addEventListener('click', function(e) {
    if (
        e.target.classList.contains('delete') ||
        e.target.classList.contains('modal-background') ||
        e.target.classList.contains('close-modal-btn')
    ) {
        closeModal();
    }
});
document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeModal(); });

function closeModal() {
    modal.classList.remove('is-active');
}