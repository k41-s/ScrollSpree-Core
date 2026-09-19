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