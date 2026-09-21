window.ScrollSpreeCart = (function () {
    'use strict';

    const GUEST_CART_KEY = 'guestCart';

    let cartItems = [];
    const listeners = [];

    function subscribe(listener) {
        listeners.push(listener);
        listener(cartItems);
        return () => {
            const i = listeners.indexOf(listener);
            if (i > -1) listeners.splice(i, 1);
        };
    }

    function setItems(items) {
        cartItems = items || [];
        listeners.forEach(listener => listener(cartItems));
    }

    function getItems() {
        return cartItems.slice();
    }

    function getCount() {
        return cartItems.reduce((sum, item) => sum + item.quantity, 0);
    }

    function getTotal() {
        return cartItems.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    }

    function isLoggedIn() {
        const token = localStorage.getItem('token');
        if (!token) return false;

        // auth.js's parseJwt() is already loaded by the time cart.js runs.
        // A token can outlive a browser session in localStorage; without this
        // check an expired-but-present token still read as "logged in" here,
        // which is why the header could look signed-in when the person no
        // longer had a valid session. fetchWithAuth() still owns the actual
        // refresh-or-log-out flow for API calls; this only keeps the header
        // and cart source (guest vs server) consistent with that.
        const claims = typeof parseJwt === 'function' ? parseJwt(token) : null;
        if (claims && claims.exp && claims.exp * 1000 < Date.now()) {
            return false;
        }
        return true;
    }

    function readGuestCart() {
        try {
            const raw = localStorage.getItem(GUEST_CART_KEY);
            const parsed = raw ? JSON.parse(raw) : [];
            return Array.isArray(parsed) ? parsed : [];
        } catch (error) {
            console.warn('Guest basket was unreadable, starting fresh.', error);
            return [];
        }
    }

    function writeGuestCart(items) {
        try {
            localStorage.setItem(GUEST_CART_KEY, JSON.stringify(items));
        } catch (error) {
            console.warn('Could not save the guest basket.', error);
        }
    }

    function clearGuestCart() {
        localStorage.removeItem(GUEST_CART_KEY);
    }

    function mapServerCart(cartDto) {
        const items = (cartDto && cartDto.items) || [];
        return items.map(item => ({
            productId: item.product.id,
            name: item.product.name,
            price: Number(item.product.price),
            imageId: (item.product.imageIds && item.product.imageIds[0]) || null,
            quantity: item.quantity
        }));
    }

    async function apiCall(url, options) {
        const response = await fetchWithAuth(url, options);
        if (!response.ok) {
            throw new Error('Cart request failed with status ' + response.status);
        }
        return mapServerCart(await response.json());
    }

    /* ---------------------------------------------------------
       Public operations
       --------------------------------------------------------- */

    async function fetchCart() {
        if (!isLoggedIn()) {
            setItems(readGuestCart());
            return getItems();
        }

        try {
            setItems(await apiCall('/api/cart', { method: 'GET' }));
        } catch (error) {
            console.error('Could not load the basket:', error);
        }
        return getItems();
    }

    async function addToCart(product, quantity = 1) {
        if (isLoggedIn()) {
            setItems(await apiCall('/api/cart/items', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ productId: product.id, quantity: quantity })
            }));
            return getItems();
        }

        const items = readGuestCart();
        const existing = items.find(item => item.productId === product.id);

        if (existing) {
            existing.quantity += quantity;
        } else {
            items.push({
                productId: product.id,
                name: product.name,
                price: Number(product.price),
                imageId: product.imageId || null,
                quantity: quantity
            });
        }

        writeGuestCart(items);
        setItems(items);
        return getItems();
    }

    async function updateQuantity(productId, change) {
        const current = cartItems.find(item => item.productId === productId);
        if (!current) return getItems();

        const newQuantity = current.quantity + change;
        if (newQuantity <= 0) {
            return removeFromCart(productId);
        }

        if (isLoggedIn()) {
            setItems(await apiCall('/api/cart/items/' + productId, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ productId: productId, quantity: newQuantity })
            }));
            return getItems();
        }

        const items = readGuestCart();
        const target = items.find(item => item.productId === productId);
        if (target) target.quantity = newQuantity;

        writeGuestCart(items);
        setItems(items);
        return getItems();
    }

    async function removeFromCart(productId) {
        if (isLoggedIn()) {
            setItems(await apiCall('/api/cart/items/' + productId, { method: 'DELETE' }));
            return getItems();
        }

        const items = readGuestCart().filter(item => item.productId !== productId);
        writeGuestCart(items);
        setItems(items);
        return getItems();
    }

    async function clearCart() {
        if (isLoggedIn()) {
            setItems(await apiCall('/api/cart', { method: 'DELETE' }));
            return getItems();
        }

        clearGuestCart();
        setItems([]);
        return getItems();
    }

    async function syncGuestCartToApi() {
        const guestItems = readGuestCart();
        if (!isLoggedIn() || guestItems.length === 0) {
            clearGuestCart();
            return;
        }

        for (const item of guestItems) {
            try {
                await fetchWithAuth('/api/cart/items', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ productId: item.productId, quantity: item.quantity })
                });
            } catch (error) {
                console.error('Could not transfer product ' + item.productId, error);
            }
        }

        clearGuestCart();
        await fetchCart();
    }

    /* ---------------------------------------------------------
       Shared UI helpers
       --------------------------------------------------------- */

    let toastTimer = null;

    function showToast(message, action) {
        const toastEl = document.querySelector('[data-toast]');
        if (!toastEl) return;

        toastEl.textContent = '';
        toastEl.append(message);

        if (action) {
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'toast-action';
            button.textContent = action.label;
            button.addEventListener('click', action.onClick);
            toastEl.append(button);
        }

        toastEl.hidden = false;
        clearTimeout(toastTimer);
        toastTimer = setTimeout(() => { toastEl.hidden = true; }, action ? 5000 : 2600);
    }

    function requireLogin(message) {
        showToast(message || 'Log in required to perform this action.', {
            label: 'Log in',
            onClick: () => { window.location.href = '/auth/login'; }
        });
    }

    function formatPrice(value) {
        return '$' + Number(value).toFixed(2);
    }

    function renderBadges(items) {
        const total = items.reduce((sum, item) => sum + item.quantity, 0);
        document.querySelectorAll('[data-cart-count]').forEach(badge => {
            badge.textContent = total;
            badge.hidden = total === 0;
        });
    }

    /**
     * Swaps a button's icon for a spinner while an async action is in flight.
     * icon.className is saved and restored, so this works with any FA icon
     * without the caller needing to know or repeat the original classes.
     */
    function setButtonBusy(button, isBusy) {
        const icon = button.querySelector('i');
        if (!icon) return;

        if (isBusy) {
            if (!icon.dataset.originalClass) {
                icon.dataset.originalClass = icon.className;
            }
            icon.className = 'spinner';
        } else if (icon.dataset.originalClass) {
            icon.className = icon.dataset.originalClass;
            delete icon.dataset.originalClass;
        }
    }

    /* ---------------------------------------------------------
       Boot
       --------------------------------------------------------- */

    // Resolves once the initial fetchCart() below has settled, so a page
    // can await real data instead of rendering subscribe()'s first,
    // necessarily-empty emission as if it were "the basket is empty".
    let readyResolve;
    const ready = new Promise(resolve => { readyResolve = resolve; });

    document.addEventListener('DOMContentLoaded', () => {
        subscribe(renderBadges);

        const authLink = document.querySelector('[data-auth-link]');
        const logoutBtn = document.querySelector('[data-logout-btn]');

        if (isLoggedIn()) {
            if (authLink) authLink.hidden = true;
            if (logoutBtn) logoutBtn.hidden = false;
        }

        if (logoutBtn) {
            logoutBtn.addEventListener('click', () => {
                // logout() is defined in auth.js: clears both tokens and
                // sends the browser to /auth/login.
                logout();
            });
        }

        fetchCart().finally(readyResolve);
    });

    return {
        subscribe,
        getItems,
        getCount,
        getTotal,
        isLoggedIn,
        fetchCart,
        addToCart,
        updateQuantity,
        removeFromCart,
        clearCart,
        syncGuestCartToApi,
        showToast,
        requireLogin,
        formatPrice,
        setButtonBusy,
        ready
    };
})();