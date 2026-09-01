/**
 * API client for all HTTP requests to the Rentify backend.
 * Automatically attaches JWT token from localStorage.
 * Input: endpoint, options (method, body, etc.)
 * Output: parsed JSON response
 */

const RAW_BASE_URL = import.meta.env.VITE_API_URL || '/api';

// Normalize BASE_URL so it ALWAYS ends with /api (even if the user entered https://my-app.onrender.com)
function getNormalizedBaseUrl() {
  let url = RAW_BASE_URL.trim().replace(/\/+$/, '');
  if (!url.endsWith('/api')) {
    url = `${url}/api`;
  }
  return url;
}

const BASE_URL = getNormalizedBaseUrl();

async function request(endpoint, options = {}) {
  const token = localStorage.getItem('rentify_token');

  const config = {
    headers: {
      'Content-Type': 'application/json',
      ...(token && { Authorization: `Bearer ${token}` }),
      ...options.headers,
    },
    ...options,
  };

  if (config.body && typeof config.body === 'object' && !(config.body instanceof FormData)) {
    config.body = JSON.stringify(config.body);
  } else if (config.body instanceof FormData) {
    // Let the browser set the Content-Type automatically for boundary
    delete config.headers['Content-Type'];
  }

  // Handle leading/duplicate /api prefixes cleanly
  let finalEndpoint = endpoint.startsWith('/') ? endpoint : `/${endpoint}`;
  if (finalEndpoint.startsWith('/api/')) {
    finalEndpoint = finalEndpoint.replace(/^\/api/, '');
  }

  const response = await fetch(`${BASE_URL}${finalEndpoint}`, config);
  const data = await response.json();

  if (!response.ok) {
    const error = new Error(data.message || 'Something went wrong');
    error.status = response.status;
    
    // Global Auth Error Handling
    if (response.status === 401 || response.status === 403) {
      window.dispatchEvent(new CustomEvent('rentify-auth-error', { detail: { status: response.status } }));
    }
    
    throw error;
  }

  return data;
}

export const api = {
  get: (endpoint) => request(endpoint, { method: 'GET' }),
  post: (endpoint, body) => request(endpoint, { method: 'POST', body }),
  put: (endpoint, body) => request(endpoint, { method: 'PUT', body }),
  patch: (endpoint, body) => request(endpoint, { method: 'PATCH', body }),
  delete: (endpoint) => request(endpoint, { method: 'DELETE' }),
};

export default api;
