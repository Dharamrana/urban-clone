// main.js — navigation, search, geolocation, toast notifications, hamburger menu, location bar, address book.
document.addEventListener('DOMContentLoaded', function() {
    initSearchForm();
    autoLocateHero();
    initAddressBook();
    initLocationBar();
    initHamburgerMenu();
});

// ── search ──
function initSearchForm() {
    var form = document.getElementById('serviceSearchForm');
    if (form) {
        form.addEventListener('submit', function(e) {
            e.preventDefault();
            var q = (document.getElementById('searchInput').value || '').trim();
            window.location.href = '/services' + (q ? '?q=' + encodeURIComponent(q) : '');
        });
    }
}

function autoLocateHero() {
    if (!navigator.geolocation) return;
    var latEl = document.getElementById('latInput');
    var lngEl = document.getElementById('lngInput');
    if (!latEl || !lngEl) return;
    var saved = getLocation();
    if (saved) {
        latEl.value = saved.lat;
        lngEl.value = saved.lng;
        return;
    }
    navigator.geolocation.getCurrentPosition(function(pos) {
        latEl.value = pos.coords.latitude;
        lngEl.value = pos.coords.longitude;
    }, function() {});
}

// ── toast notifications ──
function showNotification(message, type) {
    var existing = document.querySelector('.toast-notification');
    if (existing) existing.remove();
    var el = document.createElement('div');
    el.className = 'toast-notification toast-' + (type || 'info');
    el.textContent = message;
    document.body.appendChild(el);
    el.offsetHeight;
    el.classList.add('toast-visible');
    setTimeout(function() {
        el.classList.remove('toast-visible');
        setTimeout(function() { el.remove(); }, 300);
    }, 2500);
}

// ── loading overlay ──
function showLoading(msg) {
    hideLoading();
    var overlay = document.createElement('div');
    overlay.id = 'loadingOverlay';
    overlay.innerHTML = '<div class="loading-spinner"></div>'
        + '<div class="loading-text">' + (msg || 'Loading...') + '</div>';
    document.body.appendChild(overlay);
}

function hideLoading() {
    var el = document.getElementById('loadingOverlay');
    if (el) el.remove();
}

// ══════════════════════════════════════════
// HAMBURGER MENU
// ══════════════════════════════════════════
function initHamburgerMenu() {
    // Close menu on Escape
    document.addEventListener('keydown', function(e) {
        if (e.key === 'Escape') closeSideMenu();
    });
}

function toggleSideMenu() {
    var menu = document.getElementById('sideMenu');
    var overlay = document.getElementById('sideMenuOverlay');
    if (!menu || !overlay) return;
    var isOpen = menu.classList.contains('open');
    if (isOpen) {
        closeSideMenu();
    } else {
        menu.classList.add('open');
        overlay.classList.add('open');
        document.body.style.overflow = 'hidden';
        renderAddressBook();
    }
}

function closeSideMenu() {
    var menu = document.getElementById('sideMenu');
    var overlay = document.getElementById('sideMenuOverlay');
    if (menu) menu.classList.remove('open');
    if (overlay) overlay.classList.remove('open');
    document.body.style.overflow = '';
}

// ══════════════════════════════════════════
// ADDRESS BOOK (localStorage)
// ══════════════════════════════════════════
function getAddresses() {
    try { return JSON.parse(localStorage.getItem('uc_addresses') || '[]'); }
    catch(e) { return []; }
}

function saveAddresses(list) {
    localStorage.setItem('uc_addresses', JSON.stringify(list));
}

function renderAddressBook() {
    var list = document.getElementById('addressBookList');
    if (!list) return;
    var addrs = getAddresses();
    if (!addrs.length) {
        list.innerHTML = '<div class="address-book-empty">No saved addresses yet</div>';
        return;
    }
    list.innerHTML = '';
    addrs.forEach(function(a, i) {
        var div = document.createElement('div');
        div.className = 'address-item';
        div.innerHTML = '<div><div class="address-item-label">' + esc(a.label) + '</div>'
            + '<div class="address-item-text">' + esc(a.text) + '</div></div>'
            + '<button class="address-item-delete" onclick="event.stopPropagation(); deleteAddress(' + i + ')" title="Remove"><i class="fas fa-times"></i></button>';
        div.onclick = function() { selectAddress(i); };
        list.appendChild(div);
    });
}

function showAddAddressForm() {
    var existing = document.querySelector('.address-form');
    if (existing) { existing.classList.toggle('show'); return; }
    var list = document.getElementById('addressBookList');
    var form = document.createElement('div');
    form.className = 'address-form show';
    form.innerHTML = '<input type="text" id="addrLabel" placeholder="Label (e.g. Home, Office)">'
        + '<input type="text" id="addrText" placeholder="Full address">'
        + '<div class="address-form-actions">'
        + '<button class="btn btn-primary btn-sm" onclick="saveNewAddress()">Save</button>'
        + '<button class="btn btn-outline btn-sm" onclick="this.closest(\'.address-form\').remove()">Cancel</button>'
        + '</div>';
    list.parentNode.insertBefore(form, list.nextSibling);
}

function saveNewAddress() {
    var label = (document.getElementById('addrLabel').value || '').trim();
    var text = (document.getElementById('addrText').value || '').trim();
    if (!text) { showNotification('Please enter an address', 'error'); return; }
    var addrs = getAddresses();
    addrs.push({ label: label || 'Address ' + (addrs.length + 1), text: text, lat: 28.6139, lng: 77.2090 });
    saveAddresses(addrs);
    renderAddressBook();
    var form = document.querySelector('.address-form');
    if (form) form.remove();
    showNotification('Address saved', 'success');
}

function deleteAddress(idx) {
    var addrs = getAddresses();
    addrs.splice(idx, 1);
    saveAddresses(addrs);
    renderAddressBook();
}

function selectAddress(idx) {
    var addrs = getAddresses();
    if (!addrs[idx]) return;
    var a = addrs[idx];
    setLocation(a.lat || 28.6139, a.lng || 77.2090, a.text);
    closeSideMenu();
    showNotification('Location set to: ' + a.label, 'success');
}

// ══════════════════════════════════════════
// LOCATION BAR
// ══════════════════════════════════════════
function initLocationBar() {
    var saved = getLocation();
    if (saved) {
        var el = document.getElementById('locationText');
        if (el) el.textContent = saved.label || formatCoords(saved.lat, saved.lng);
    }
    // Close panel on outside click
    document.addEventListener('click', function(e) {
        var panel = document.getElementById('locationPanel');
        var display = document.getElementById('locationDisplay');
        if (panel && panel.classList.contains('open')
            && !panel.contains(e.target) && !display.contains(e.target)) {
            panel.classList.remove('open');
        }
    });
}

function toggleLocationPanel() {
    var panel = document.getElementById('locationPanel');
    if (panel) panel.classList.toggle('open');
    renderSavedLocations();
}

function useCurrentLocation() {
    if (!navigator.geolocation) {
        showNotification('Geolocation not supported', 'error');
        return;
    }
    showNotification('Detecting location...', 'info');
    navigator.geolocation.getCurrentPosition(function(pos) {
        var lat = pos.coords.latitude;
        var lng = pos.coords.longitude;
        var label = formatCoords(lat, lng);
        setLocation(lat, lng, label);
        document.getElementById('locationPanel').classList.remove('open');
        showNotification('Location detected', 'success');
        updateHeroCoords(lat, lng);
    }, function() {
        showNotification('Could not detect location. Enter manually.', 'error');
    });
}

function setManualLocation() {
    var input = document.getElementById('manualLocationInput');
    var text = (input.value || '').trim();
    if (!text) { showNotification('Enter an address', 'error'); return; }
    // Save to saved locations
    var addrs = getAddresses();
    addrs.push({ label: text.split(',')[0] || text, text: text, lat: 28.6139, lng: 77.2090 });
    saveAddresses(addrs);
    setLocation(28.6139, 77.2090, text);
    input.value = '';
    document.getElementById('locationPanel').classList.remove('open');
    showNotification('Location set', 'success');
    renderSavedLocations();
}

function renderSavedLocations() {
    var box = document.getElementById('savedLocationsList');
    if (!box) return;
    var addrs = getAddresses();
    if (!addrs.length) { box.innerHTML = ''; return; }
    var html = '';
    addrs.forEach(function(a, i) {
        html += '<div class="saved-loc-item" onclick="pickSavedLocation(' + i + ')">'
            + '<i class="fas fa-map-pin"></i><span>' + esc(a.text) + '</span>'
            + '<button class="delete-loc" onclick="event.stopPropagation(); deleteAddress(' + i + '); renderSavedLocations();" title="Remove"><i class="fas fa-times"></i></button>'
            + '</div>';
    });
    box.innerHTML = html;
}

function pickSavedLocation(idx) {
    var addrs = getAddresses();
    if (!addrs[idx]) return;
    var a = addrs[idx];
    setLocation(a.lat || 28.6139, a.lng || 77.2090, a.text);
    document.getElementById('locationPanel').classList.remove('open');
    showNotification('Location set to: ' + (a.label || a.text), 'success');
}

// ── location storage helpers ──
function setLocation(lat, lng, label) {
    localStorage.setItem('uc_location', JSON.stringify({ lat: lat, lng: lng, label: label }));
    var el = document.getElementById('locationText');
    if (el) el.textContent = label || formatCoords(lat, lng);
    // Update hero hidden inputs
    updateHeroCoords(lat, lng);
}

function getLocation() {
    try { return JSON.parse(localStorage.getItem('uc_location')); }
    catch(e) { return null; }
}

function updateHeroCoords(lat, lng) {
    var latEl = document.getElementById('latInput');
    var lngEl = document.getElementById('lngInput');
    if (latEl) latEl.value = lat;
    if (lngEl) lngEl.value = lng;
}

function formatCoords(lat, lng) {
    return lat.toFixed(2) + ', ' + lng.toFixed(2);
}

function esc(s) {
    var d = document.createElement('div');
    d.textContent = s;
    return d.innerHTML;
}
