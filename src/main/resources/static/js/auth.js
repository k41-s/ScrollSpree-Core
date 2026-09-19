async function fetchWithAuth(url, options = {}) {
    let token = localStorage.getItem('token');

    if (!options.headers) {
        options.headers = {};
    }

    if (token) {
        options.headers['Authorization'] = `Bearer ${token}`;
    }

    let response = await fetch(url, options);

    if (response.status === 401) {
        console.warn("Access token expired. Attempting to refresh...");

        const refreshed = await attemptTokenRefresh();

        if (refreshed) {
            options.headers['Authorization'] = `Bearer ${localStorage.getItem('token')}`;

            response = await fetch(url, options);
        } else {
            localStorage.removeItem('token');
            localStorage.removeItem('refreshToken');
            window.location.href = '/auth/login';
        }
    }

    return response;
}

async function attemptTokenRefresh() {
    const refreshToken = localStorage.getItem('refreshToken');
    if (!refreshToken) return false;

    try {
        const response = await fetch('/api/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken: refreshToken })
        });

        if (response.ok) {
            const data = await response.json();
            localStorage.setItem('token', data.token);
            localStorage.setItem('refreshToken', data.refreshToken);
            return true;
        }
    } catch (error) {
        console.error("Network error during token refresh:", error);
    }

    return false;
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    window.location.href = '/auth/login';
}

function parseJwt(token) {
    try {
        const base64Url = token.split('.')[1];
        const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
        const jsonPayload = decodeURIComponent(
            atob(base64)
                .split('')
                .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
                .join('')
        );
        return JSON.parse(jsonPayload);
    } catch (e) {
        return null;
    }
}

function redirectBasedOnRole(accessToken) {
    const claims = parseJwt(accessToken);

    if (!claims || claims.token_type !== 'access') {
        localStorage.removeItem('token');
        return false;
    }

    if (claims.exp * 1000 < Date.now()) {
        localStorage.removeItem('token');
        return false;
    }

    const roles = claims.roles ? claims.roles.split(',') : [];

    if (roles.includes('ROLE_ADMIN') || roles.includes('ADMIN')) {
        window.location.href = '/admin/logs';
    } else {
        window.location.href = '/';
    }
    return true;
}