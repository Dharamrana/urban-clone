// requests.js — booking list: highlight, cancel, reschedule, rate.
document.addEventListener('DOMContentLoaded', function() {
    var params = new URLSearchParams(window.location.search);
    var highlight = params.get('highlight');
    if (highlight) {
        var el = document.getElementById('booking-' + highlight);
        if (el) {
            el.classList.add('highlight');
            el.scrollIntoView({ behavior: 'smooth', block: 'center' });
        }
    }
});

function refresh() {
    window.location.reload();
}

function showError(err) {
    console.error(err);
    showNotification((err && err.message) || 'Something went wrong.', 'error');
}

function parseError(response) {
    if (response.status === 401) {
        window.location.href = '/login?next=' + encodeURIComponent(window.location.pathname + window.location.search);
        throw new Error('Please log in first');
    }
    return response.json().then(function(body) {
        throw new Error(body.message || ('Request failed (' + response.status + ')'));
    });
}

function cancelBooking(id) {
    if (!confirm('Cancel booking #' + id + '?')) return;
    fetch('/api/requests/' + id + '/cancel', { method: 'PUT' })
        .then(function(response) {
            if (!response.ok) return parseError(response);
            return response.json();
        })
        .then(function() {
            showNotification('Booking cancelled', 'success');
            setTimeout(refresh, 800);
        })
        .catch(showError);
}

function rescheduleBooking(id) {
    var today = new Date().toISOString().split('T')[0];
    var date = prompt('New visit date (YYYY-MM-DD, today or later):', today);
    if (!date) return;
    fetch('/api/requests/slots?date=' + date)
        .then(function(response) {
            if (!response.ok) return parseError(response);
            return response.json();
        })
        .then(function(slots) {
            var slot = prompt('Pick a slot:\n' + slots.join('\n'), slots[1] || slots[0]);
            if (!slot) return;
            return fetch('/api/requests/' + id + '/reschedule?date=' + date + '&slot=' + encodeURIComponent(slot), { method: 'PUT' })
                .then(function(response) {
                    if (!response.ok) return parseError(response);
                    return response.json();
                })
                .then(function() {
                    showNotification('Booking rescheduled', 'success');
                    setTimeout(refresh, 800);
                });
        })
        .catch(showError);
}

function rateBooking(id) {
    var rating = prompt('Rate your professional (1-5 stars):', '5');
    if (!rating) return;
    var review = prompt('Add a short review (optional):', '');
    var url = '/api/requests/' + id + '/rate?rating=' + encodeURIComponent(rating);
    if (review) {
        url += '&review=' + encodeURIComponent(review);
    }
    fetch(url, { method: 'PUT' })
        .then(function(response) {
            if (!response.ok) return parseError(response);
            return response.json();
        })
        .then(function() {
            showNotification('Thanks for your rating!', 'success');
            setTimeout(refresh, 800);
        })
        .catch(showError);
}
