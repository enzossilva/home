const BASE = '/';

function authHeaders(extra = {}) {
  const token = localStorage.getItem('yz_token');
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...extra,
  };
}

function handleResponse(res) {
  if (res.status === 401) {
    localStorage.removeItem('yz_token');
    localStorage.removeItem('yz_user');
    window.dispatchEvent(new Event('yz:logout'));
  }
  return res;
}

async function parseResponse(res) {
  const json = await res.json();
  if (!res.ok || !json.success) {
    throw new Error(json.message || 'Erro na requisição');
  }
  return json.data;
}

// ── Products ──────────────────────────────────────────────
export async function getProducts() {
  const res = await fetch(`${BASE}products`);
  return parseResponse(res);
}

export async function addProduct(product) {
  const res = await fetch(`${BASE}products`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(product),
  });
  return parseResponse(res);
}

export async function updateProduct(id, product) {
  const res = await fetch(`${BASE}products/${id}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(product),
  });
  return parseResponse(res);
}

export async function deleteProduct(id) {
  const res = await fetch(`${BASE}products/${id}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  return parseResponse(res);
}

// ── Users ──────────────────────────────────────────────────
export async function register(name, email, password) {
  const res = await fetch(`${BASE}users/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, password }),
  });
  const data = await parseResponse(res);
  if (data.token) localStorage.setItem('yz_token', data.token);
  return data;
}

export async function login(email, password) {
  const res = await fetch(`${BASE}users/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  const data = await parseResponse(res);
  if (data.token) localStorage.setItem('yz_token', data.token);
  return data;
}

export async function updateProfile(userId, data) {
  const res = handleResponse(await fetch(`${BASE}users/${userId}/profile`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(data),
  }));
  const result = await parseResponse(res);
  if (result.token) localStorage.setItem('yz_token', result.token);
  return result;
}

export async function requestPasswordReset(email) {
  const res = await fetch(`${BASE}users/reset-request`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  });
  return parseResponse(res);
}

export async function resetPassword(token, password) {
  const res = await fetch(`${BASE}users/reset-password`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token, password }),
  });
  return parseResponse(res);
}

// ── Cart ───────────────────────────────────────────────────
export async function getCart(userId) {
  const res = handleResponse(await fetch(`${BASE}cart/${userId}`, { headers: authHeaders() }));
  return parseResponse(res);
}

export async function addToCart(userId, productId, quantity = 1, size = null) {
  const res = await fetch(`${BASE}cart`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ userId, productId, quantity, size }),
  });
  return parseResponse(res);
}

export async function removeFromCart(cartItemId) {
  const res = await fetch(`${BASE}cart/${cartItemId}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  return parseResponse(res);
}

// ── Orders ──────────────────────────────────────────────────
export async function createOrder(userId, address, shippingMethod, shippingCost) {
  const res = await fetch(`${BASE}orders`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ userId, address, shippingMethod, shippingCost }),
  });
  return parseResponse(res);
}

export async function getOrder(id) {
  const res = await fetch(`${BASE}orders/${id}`, { headers: authHeaders() });
  return parseResponse(res);
}

export async function getOrdersByUser(userId) {
  const res = await fetch(`${BASE}orders/user/${userId}`, { headers: authHeaders() });
  return parseResponse(res);
}

export async function cancelOrder(orderId) {
  const res = handleResponse(await fetch(`${BASE}orders/${orderId}/cancel`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({}),
  }));
  return parseResponse(res);
}

export async function getAdminStats() {
  const res = handleResponse(await fetch(`${BASE}orders/admin/stats`, { headers: authHeaders() }));
  return parseResponse(res);
}

export async function getAllOrders() {
  const res = await fetch(`${BASE}orders/admin/all`, { headers: authHeaders() });
  return parseResponse(res);
}

// ── Videos ──────────────────────────────────────────────────
export async function getVideos() {
  const res = await fetch(`${BASE}videos`);
  return parseResponse(res);
}

export async function addVideo(video) {
  const res = await fetch(`${BASE}videos`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(video),
  });
  return parseResponse(res);
}

export async function deleteVideo(id) {
  const res = await fetch(`${BASE}videos/${id}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  return parseResponse(res);
}

// ── Lookbook ────────────────────────────────────────────────
export async function getLookbook() {
  const res = await fetch(`${BASE}lookbook`);
  return parseResponse(res);
}

export async function addLookbookItem(item) {
  const res = await fetch(`${BASE}lookbook`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(item),
  });
  return parseResponse(res);
}

export async function deleteLookbookItem(id) {
  const res = await fetch(`${BASE}lookbook/${id}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  return parseResponse(res);
}

export async function markOrderAsPaid(orderId) {
  const res = handleResponse(await fetch(`${BASE}orders/${orderId}/mark-paid`, {
    method: 'POST',
    headers: authHeaders(),
  }));
  return parseResponse(res);
}

export async function gerarEtiqueta(orderId) {
  const res = handleResponse(await fetch(`${BASE}orders/${orderId}/etiqueta`, {
    method: 'POST',
    headers: authHeaders(),
  }));
  return parseResponse(res);
}

export async function markAsShipped(orderId, trackingCode) {
  const res = await fetch(`${BASE}orders/${orderId}/ship`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ trackingCode }),
  });
  return parseResponse(res);
}

// ── Payment ─────────────────────────────────────────────────
export async function gerarPix(userId, orderId, email, cpf, firstName, lastName) {
  const res = await fetch(`${BASE}payment/pix`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ userId, orderId, email, cpf, firstName, lastName }),
  });
  return parseResponse(res);
}
