// Urban Company style multi-service cart (one visit, single checkout).
// Stored in localStorage so it survives navigation between Services -> Book.
var UC_CART_KEY = 'uc_cart_v1';

function getCart() {
    try {
        var raw = localStorage.getItem(UC_CART_KEY);
        var cart = raw ? JSON.parse(raw) : [];
        return Array.isArray(cart) ? cart : [];
    } catch (e) {
        return [];
    }
}

function saveCart(cart) {
    localStorage.setItem(UC_CART_KEY, JSON.stringify(cart));
    updateCartBadges();
    renderCartBar();
}

function addToCart(serviceId, name, basePrice) {
    var cart = getCart();
    var id = parseInt(serviceId, 10);
    var found = null;
    cart.forEach(function(l) { if (l.serviceId === id) found = l; });
    if (found) {
        if (found.qty < 10) found.qty += 1;
    } else {
        if (cart.length >= 20) {
            showNotification('Maximum 20 different services per booking.', 'error');
            return;
        }
        cart.push({ serviceId: id, name: name, basePrice: basePrice || 0, qty: 1 });
    }
    saveCart(cart);
    showNotification(name + ' added to cart', 'success');
}

function setCartQty(serviceId, qty) {
    var cart = getCart();
    var id = parseInt(serviceId, 10);
    cart.forEach(function(l) { if (l.serviceId === id) l.qty = Math.min(10, Math.max(1, qty)); });
    saveCart(cart);
    if (typeof renderCartLines === 'function' && document.getElementById('cartLines')) {
        renderCartLines();
    }
}

function removeFromCart(serviceId) {
    var id = parseInt(serviceId, 10);
    saveCart(getCart().filter(function(l) { return l.serviceId !== id; }));
    if (typeof renderCartLines === 'function' && document.getElementById('cartLines')) {
        renderCartLines();
    }
    if (typeof syncBookingMode === 'function' && window.location.pathname.includes('/request')) {
        syncBookingMode();
    }
}

function clearCart() {
    saveCart([]);
    if (typeof renderCartLines === 'function' && document.getElementById('cartLines')) {
        renderCartLines();
    }
}

function cartCount() {
    return getCart().reduce(function(n, l) { return n + (l.qty || 0); }, 0);
}

function cartTotal() {
    return getCart().reduce(function(n, l) { return n + (l.basePrice || 0) * (l.qty || 0); }, 0);
}

function updateCartBadges() {
    document.querySelectorAll('[data-cart-count]').forEach(function(el) {
        var n = cartCount();
        el.textContent = n;
        el.style.display = n > 0 ? 'inline-flex' : 'none';
    });
}

// Floating "N items · ₹X · Book" bar (UC style), hidden on the booking page itself.
function renderCartBar() {
    var onBookingPage = window.location.pathname.includes('/request');
    var bar = document.getElementById('cartBar');
    var cart = getCart();
    if (onBookingPage || cart.length === 0) {
        if (bar) bar.style.display = 'none';
        return;
    }
    if (!bar) {
        bar = document.createElement('div');
        bar.id = 'cartBar';
        bar.innerHTML = '<span id="cartBarText"></span>'
            + '<a href="/request?cart=1" class="btn btn-primary btn-sm">Book Visit →</a>';
        document.body.appendChild(bar);
    }
    bar.style.display = 'flex';
    document.getElementById('cartBarText').textContent =
        cartCount() + ' item(s) · ₹' + cartTotal() + ' (+ ₹49 visit)';
}

document.addEventListener('DOMContentLoaded', function() {
    updateCartBadges();
    renderCartBar();
});
