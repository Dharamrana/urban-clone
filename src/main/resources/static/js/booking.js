var selectedSlot = null;
var slotOptions = [];
var preselectedProviderId = null;

document.addEventListener('DOMContentLoaded', function() {
    if (window.location.pathname.includes('/request')) {
        initBookingForm();
    }
});

function initBookingForm() {
    var urlParams = new URLSearchParams(window.location.search);
    var serviceId = urlParams.get('serviceId');
    var providerId = urlParams.get('providerId');
    if (providerId) {
        preselectedProviderId = providerId;
    }
    if (serviceId) {
        document.getElementById('serviceId').value = serviceId;
        loadProviders();
    }

    var dateInput = document.getElementById('slotDate');
    if (dateInput) {
        var tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        var iso = tomorrow.toISOString().split('T')[0];
        dateInput.min = new Date().toISOString().split('T')[0];
        dateInput.value = iso;
        dateInput.addEventListener('change', loadSlots);
    }
    loadSlots();
    tryAutoLocate();
    prefillProfile();
    syncBookingMode();
}

// Logged-in profile pre-fills the address step (UC-style); logged-out users are sent to login.
function prefillProfile() {
    fetch('/api/auth/me')
        .then(function(response) {
            if (response.status === 401) {
                window.location.href = '/login?next=' + encodeURIComponent('/request' + window.location.search);
                return null;
            }
            return response.json();
        })
        .then(function(me) {
            if (!me) return;
            if (me.name && !document.getElementById('customerName').value) {
                document.getElementById('customerName').value = me.name;
            }
            if (me.phone && !document.getElementById('customerPhone').value) {
                document.getElementById('customerPhone').value = me.phone;
            }
            if (me.email && !document.getElementById('customerEmail').value) {
                document.getElementById('customerEmail').value = me.email;
            }
        })
        .catch(function() { /* quote/checkout still works; booking will 401 with a message */ });
}

// ---------- multi-service cart mode (UC: one visit, many services) ----------

function isCartMode() {
    return typeof getCart === 'function' && getCart().length > 0;
}

function cartFirstServiceId() {
    var cart = getCart();
    return cart.length > 0 ? cart[0].serviceId : null;
}

function syncBookingMode() {
    var cartMode = isCartMode();
    var cartBlock = document.getElementById('cartLinesBlock');
    var singleGroup = document.getElementById('singleServiceGroup');
    if (cartBlock) cartBlock.style.display = cartMode ? 'block' : 'none';
    if (singleGroup) singleGroup.style.display = cartMode ? 'none' : 'block';
    if (cartMode) {
        renderCartLines();
        loadProviders();
    }
}

function renderCartLines() {
    var box = document.getElementById('cartLines');
    if (!box) return;
    var cart = getCart();
    box.innerHTML = '';
    if (cart.length === 0) {
        box.innerHTML = '<p>Your cart is empty.</p>';
        return;
    }
    cart.forEach(function(line) {
        var row = document.createElement('div');
        row.className = 'cart-line';
        row.innerHTML = '<div class="cart-line-info"><strong></strong>'
            + '<span class="cart-line-price"></span></div>'
            + '<div class="qty-stepper">'
            + '<button type="button" data-act="dec">−</button>'
            + '<span class="qty"></span>'
            + '<button type="button" data-act="inc">+</button>'
            + '<button type="button" class="link" data-act="rm">Remove</button>'
            + '</div>';
        row.querySelector('strong').textContent = line.name;
        row.querySelector('.cart-line-price').textContent = '₹' + line.basePrice + ' each';
        row.querySelector('.qty').textContent = 'Qty ' + line.qty;
        row.querySelector('[data-act="dec"]').onclick = function() { setCartQty(line.serviceId, line.qty - 1); };
        row.querySelector('[data-act="inc"]').onclick = function() { setCartQty(line.serviceId, line.qty + 1); };
        row.querySelector('[data-act="rm"]').onclick = function() { removeFromCart(line.serviceId); };
        box.appendChild(row);
    });
    var note = document.createElement('p');
    note.className = 'checkout-note';
    note.textContent = 'One professional visits for all services in a single slot.';
    box.appendChild(note);
}

// ---------- wizard navigation (Urban Company style steps) ----------

function wizardGo(step) {
    document.querySelectorAll('.wizard-pane').forEach(function(p) {
        p.classList.toggle('active', p.getAttribute('data-pane') === String(step));
    });
    document.querySelectorAll('#wizardSteps li').forEach(function(li) {
        li.classList.toggle('active', parseInt(li.getAttribute('data-step'), 10) === step);
        li.classList.toggle('done', parseInt(li.getAttribute('data-step'), 10) < step);
    });
    window.scrollTo(0, 0);
}

function wizardNext(step) {
    if (step === 2) {
        if (isCartMode()) {
            if (getCart().length === 0) {
                showNotification('Your cart is empty. Add services first.', 'error');
                return;
            }
        } else if (!document.getElementById('serviceId').value) {
            showNotification('Please select a service first.', 'error');
            return;
        }
        if (!document.getElementById('providerId').value) {
            showNotification('Please select a professional (or Auto-select Nearest).', 'error');
            return;
        }
    }
    if (step === 3) {
        if (!document.getElementById('slotDate').value || !selectedSlot) {
            showNotification('Please pick a visit date and time slot.', 'error');
            return;
        }
    }
    if (step === 4) {
        var name = document.getElementById('customerName').value.trim();
        var phone = document.getElementById('customerPhone').value.trim();
        var addr = document.getElementById('customerAddress').value.trim();
        if (!name || !phone || !addr || !document.getElementById('lat').value || !document.getElementById('lng').value) {
            showNotification('Please fill name, phone, address and location.', 'error');
            return;
        }
        renderCheckout();
    }
    wizardGo(step);
}

// ---------- steps data ----------

function tryAutoLocate() {
    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(function(position) {
            document.getElementById('lat').value = position.coords.latitude.toFixed(6);
            document.getElementById('lng').value = position.coords.longitude.toFixed(6);
        }, function() {
            console.log('Geolocation not available');
        });
    }
}

function loadProviders() {
    var serviceId = isCartMode() ? cartFirstServiceId() : document.getElementById('serviceId').value;
    var lat = document.getElementById('lat').value || 28.6139;
    var lng = document.getElementById('lng').value || 77.2090;

    if (!serviceId) {
        document.getElementById('providerSelectionGroup').style.display = 'none';
        return;
    }

    fetch('/api/providers/nearest?serviceId=' + serviceId + '&lat=' + lat + '&lng=' + lng + '&limit=20')
        .then(function(response) { return response.json(); })
        .then(function(providers) {
            var select = document.getElementById('providerId');
            select.innerHTML = '<option value="">-- Select a Professional --</option>';
            providers.forEach(function(pd) {
                var option = document.createElement('option');
                option.value = pd.provider.id;
                option.textContent = pd.provider.name + ' - ' + Math.round(pd.distance) + ' km away - Rating: ' + pd.provider.rating;
                select.appendChild(option);
            });
            document.getElementById('providerSelectionGroup').style.display = 'block';
            if (preselectedProviderId) {
                select.value = preselectedProviderId;
                if (!select.value) {
                    // provider list arrived before pre-select id was known to exist; keep for retry
                    autoSelectNearest();
                }
                preselectedProviderId = null;
            }
        })
        .catch(function(error) {
            console.error('Error loading providers:', error);
        });
}

function autoSelectNearest() {
    var lat = document.getElementById('lat').value || 28.6139;
    var lng = document.getElementById('lng').value || 77.2090;
    var serviceId = isCartMode() ? cartFirstServiceId() : document.getElementById('serviceId').value;

    if (!serviceId) return;

    fetch('/api/providers/nearest?serviceId=' + serviceId + '&lat=' + lat + '&lng=' + lng + '&limit=1')
        .then(function(response) { return response.json(); })
        .then(function(providers) {
            if (providers.length > 0) {
                document.getElementById('providerId').value = providers[0].provider.id;
                showNotification('Nearest professional selected', 'success');
            }
        });
}

function loadSlots() {
    var dateInput = document.getElementById('slotDate');
    var date = dateInput ? dateInput.value : '';
    var url = '/api/requests/slots' + (date ? '?date=' + date : '');
    fetch(url)
        .then(function(response) { return response.json(); })
        .then(function(slots) {
            slotOptions = slots;
            if (selectedSlot && slots.indexOf(selectedSlot) === -1) {
                selectedSlot = null;
            }
            renderSlotGrid();
        })
        .catch(function() {
            slotOptions = ['08:00-10:00', '10:00-12:00', '12:00-14:00', '14:00-16:00', '16:00-18:00', '18:00-20:00'];
            renderSlotGrid();
        });
}

function renderSlotGrid() {
    var grid = document.getElementById('slotGrid');
    if (!grid) return;
    grid.innerHTML = '';
    slotOptions.forEach(function(slot) {
        var btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'slot-btn' + (slot === selectedSlot ? ' selected' : '');
        btn.textContent = slot;
        btn.onclick = function() {
            selectedSlot = slot;
            renderSlotGrid();
        };
        grid.appendChild(btn);
    });
}

function checkedPayment() {
    var el = document.querySelector('input[name="paymentMethod"]:checked');
    return el ? el.value : 'UPI';
}

function renderCheckout() {
    var summary = document.getElementById('checkoutSummary');
    var providerSelect = document.getElementById('providerId');
    var providerName = providerSelect.options[providerSelect.selectedIndex]
        ? providerSelect.options[providerSelect.selectedIndex].text : '';
    var baseHtml = '<div class="summary-row"><span>Professional</span><strong>' + providerName + '</strong></div>'
        + '<div class="summary-row"><span>Visit</span><strong>' + document.getElementById('slotDate').value + ' · ' + selectedSlot + '</strong></div>'
        + '<div class="summary-row"><span>Payment</span><strong>' + checkedPayment() + '</strong></div>';
    if (isCartMode()) {
        // Server-computed price truth for the whole cart.
        var payload = {
            items: getCart().map(function(l) {
                return { service: { id: l.serviceId }, quantity: l.qty };
            })
        };
        fetch('/api/requests/quote', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        })
            .then(function(response) { return response.json(); })
            .then(function(quote) {
                var html = baseHtml;
                quote.lines.forEach(function(line) {
                    html += '<div class="summary-row"><span>' + line.name + ' × ' + line.quantity
                        + '</span><strong>₹' + line.lineTotal + '</strong></div>';
                });
                html += '<div class="summary-row"><span>Visiting fee</span><strong>₹' + quote.visitingFee + '</strong></div>'
                    + '<div class="summary-row total"><span>To pay</span><strong>₹' + quote.total + '</strong></div>';
                summary.innerHTML = html;
            })
            .catch(function() {
                summary.innerHTML = baseHtml;
            });
        return;
    }
    var serviceId = document.getElementById('serviceId').value;
    if (!serviceId) {
        summary.innerHTML = baseHtml;
        return;
    }
    fetch('/api/services/' + serviceId)
        .then(function(response) { return response.json(); })
        .then(function(service) {
            var base = service.basePrice || 0;
            var fee = 49;
            summary.innerHTML = baseHtml
                + '<div class="summary-row"><span>' + service.name + '</span><strong>₹' + base + '</strong></div>'
                + '<div class="summary-row"><span>Visiting fee</span><strong>₹' + fee + '</strong></div>'
                + '<div class="summary-row total"><span>To pay</span><strong>₹' + (base + fee) + '</strong></div>';
        })
        .catch(function() {
            summary.innerHTML = baseHtml;
        });
}

function submitBooking() {
    var form = document.getElementById('bookingForm');
    var formData = new FormData(form);
    var cartMode = isCartMode();
    var cart = cartMode ? getCart() : [];
    var data = {
        user: {
            name: formData.get('customerName'),
            email: formData.get('customerEmail') || '',
            phone: formData.get('customerPhone'),
            location: {
                latitude: parseFloat(formData.get('lat')),
                longitude: parseFloat(formData.get('lng')),
                address: formData.get('customerAddress')
            }
        },
        service: cartMode
            ? { id: cart[0].serviceId }
            : { id: parseInt(formData.get('serviceId'), 10) },
        items: cartMode
            ? cart.map(function(l) { return { service: { id: l.serviceId }, quantity: l.qty }; })
            : undefined,
        provider: formData.get('providerId') ? { id: parseInt(formData.get('providerId'), 10) } : null,
        description: formData.get('description') || '',
        address: formData.get('customerAddress'),
        scheduledDate: document.getElementById('slotDate').value,
        scheduledSlot: selectedSlot,
        paymentMethod: checkedPayment()
    };

    fetch('/api/requests', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(data)
    })
        .then(function(response) {
            if (response.status === 401) {
                window.location.href = '/login?next=' + encodeURIComponent('/request' + window.location.search);
                throw new Error('Please log in to complete your booking');
            }
            if (!response.ok) {
                return response.json().then(function(err) {
                    throw new Error(err.message || 'Booking failed');
                });
            }
            return response.json();
        })
        .then(function(result) {
            if (typeof clearCart === 'function') clearCart();
            // Online payments go through the mock gateway first (UC checkout).
            if (result.paymentMethod && result.paymentMethod !== 'CASH') {
                window.location.href = '/pay/' + result.id;
                return;
            }
            document.getElementById('bookingForm').style.display = 'none';
            document.getElementById('wizardSteps').style.display = 'none';
            var resultDiv = document.getElementById('bookingResult');
            resultDiv.style.display = 'block';
            document.getElementById('resultMessage').textContent =
                'Your service request #' + result.id + ' is confirmed for ' + result.scheduledDate + ' (' + result.scheduledSlot + ').';
            document.getElementById('resultMeta').textContent =
                'Total ₹' + result.finalPrice + ' · Pay via ' + result.paymentMethod + '. The professional will arrive at your address.';
            document.getElementById('trackLink').href = '/requests?highlight=' + result.id;
        })
        .catch(function(error) {
            console.error('Error submitting booking:', error);
            showNotification(error.message || 'There was an error submitting your request.', 'error');
        });
}
