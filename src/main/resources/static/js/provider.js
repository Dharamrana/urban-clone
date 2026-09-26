// provider.js — partner portal: jobs, OTP start, completion, availability.
document.addEventListener('DOMContentLoaded', function() {
    paintAvailability();
});

function paintAvailability() {
    var btn = document.getElementById('availToggle');
    if (!btn) return;
    var on = (typeof PROVIDER_AVAILABLE !== 'undefined') ? PROVIDER_AVAILABLE : true;
    btn.textContent = on ? '● Available' : '○ Offline';
}

function api(path, method) {
    return fetch(path, { method: method || 'PUT' }).then(function(response) {
        if (response.status === 401 || response.status === 403) {
            window.location.href = '/login?next=' + encodeURIComponent('/provider-portal');
            throw new Error('Session expired. Please log in again.');
        }
        if (!response.ok) {
            return response.json().then(function(body) {
                throw new Error(body.message || ('Failed (' + response.status + ')'));
            }).catch(function(e) {
                if (e.message && e.message.indexOf('Failed (') === 0) throw e;
                throw new Error('Failed (' + response.status + ')');
            });
        }
        return response.json();
    });
}

function toggleAvailability() {
    var next = !((typeof PROVIDER_AVAILABLE !== 'undefined') ? PROVIDER_AVAILABLE : true);
    api('/api/provider/availability?available=' + next, 'PUT')
        .then(function() {
            showNotification(next ? 'You are now available' : 'You are now offline', 'success');
            setTimeout(function() { window.location.reload(); }, 800);
        })
        .catch(showError);
}

function acceptJob(id) {
    if (!confirm('Accept job #' + id + '?')) return;
    api('/api/provider/jobs/' + id + '/accept', 'PUT')
        .then(function() {
            showNotification('Job accepted', 'success');
            setTimeout(function() { window.location.reload(); }, 800);
        })
        .catch(showError);
}

function rejectJob(id) {
    if (!confirm('Reject job #' + id + '?')) return;
    api('/api/provider/jobs/' + id + '/reject', 'PUT')
        .then(function() {
            showNotification('Job rejected', 'success');
            setTimeout(function() { window.location.reload(); }, 800);
        })
        .catch(showError);
}

function startJob(id) {
    var otp = document.getElementById('otp-' + id).value.trim();
    if (!otp) {
        showNotification('Enter the 4-digit OTP from the customer.', 'error');
        return;
    }
    api('/api/provider/jobs/' + id + '/start?otp=' + encodeURIComponent(otp), 'PUT')
        .then(function() {
            showNotification('Job started', 'success');
            setTimeout(function() { window.location.reload(); }, 800);
        })
        .catch(showError);
}

function completeJob(id) {
    var amount = prompt('Amount collected (leave blank to keep quoted total):', '');
    var url = '/api/provider/jobs/' + id + '/complete';
    if (amount !== null && amount.trim() !== '') {
        url += '?finalPrice=' + encodeURIComponent(amount.trim());
    }
    api(url, 'PUT')
        .then(function() {
            showNotification('Job completed! Earnings updated.', 'success');
            setTimeout(function() { window.location.reload(); }, 800);
        })
        .catch(showError);
}

function showError(e) {
    console.error(e);
    showNotification(e.message || 'Something went wrong.', 'error');
}
