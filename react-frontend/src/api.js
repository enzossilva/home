const BASE = '/';

function authHeaders(extra = {}) {
  const token = localStorage.getItem('yz_token');
  return {
    'Content-Type': 'application/json',
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
    ...extra,
  };
}

function forceLogout() {
  localStorage.removeItem('yz_token');
  localStorage.removeItem('yz_user');
  const path = window.location.pathname + window.location.search;
  if (path && !path.startsWith('/login')) {
    sessionStorage.setItem('yz_login_redirect', path);
  }
  window.dispatchEvent(new Event('yz:logout'));
}

let refreshPromise = null;

/** Renova o JWT (válido ou recentemente expirado). */
export async function refreshSession() {
  const token = localStorage.getItem('yz_token');
  if (!token) throw new Error('Sem sessão');

  if (!refreshPromise) {
    refreshPromise = (async () => {
      const res = await fetch(`${BASE}users/refresh`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
      });
      const text = await res.text();
      let json = null;
      try {
        json = text ? JSON.parse(text) : null;
      } catch {
        throw new Error('Falha ao renovar sessão');
      }
      if (!res.ok || !json?.success) {
        throw new Error(json?.message || 'Sessão expirada');
      }
      const data = json.data;
      if (data?.token) localStorage.setItem('yz_token', data.token);
      if (data?.user) {
        localStorage.setItem('yz_user', JSON.stringify(data.user));
        window.dispatchEvent(new CustomEvent('yz:user', { detail: data.user }));
      }
      return data;
    })().finally(() => {
      refreshPromise = null;
    });
  }
  return refreshPromise;
}

/**
 * fetch autenticado: em 401 tenta renovar a sessão uma vez e repete.
 * Assim o cliente não precisa deslogar/logar só porque o token antigo expirou.
 */
export async function authFetch(url, options = {}, retried = false) {
  const headers = { ...authHeaders(), ...(options.headers || {}) };
  const res = await fetch(url, { ...options, headers });

  if (res.status !== 401) return res;

  if (!retried && localStorage.getItem('yz_token')) {
    try {
      await refreshSession();
      return authFetch(url, options, true);
    } catch {
      forceLogout();
      throw new Error('Sua sessão expirou. Faça login novamente para continuar.');
    }
  }

  forceLogout();
  throw new Error('Sua sessão expirou. Faça login novamente para continuar.');
}

async function parseResponse(res) {
  const text = await res.text();
  let json;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    throw new Error(text?.trim() || `Erro HTTP ${res.status}`);
  }
  if (!res.ok || !json?.success) {
    throw new Error(json?.message || text || 'Erro na requisição');
  }
  return json.data;
}

// ── Products ──────────────────────────────────────────────
export async function getProducts() {
  const res = await fetch(`${BASE}products`);
  return parseResponse(res);
}

export async function addProduct(product) {
  const res = await authFetch(`${BASE}products`, {
    method: 'POST',
    body: JSON.stringify(product),
  });
  return parseResponse(res);
}

export async function updateProduct(id, product) {
  const res = await authFetch(`${BASE}products/${id}`, {
    method: 'PUT',
    body: JSON.stringify(product),
  });
  return parseResponse(res);
}

export async function deleteProduct(id) {
  const res = await authFetch(`${BASE}products/${id}`, {
    method: 'DELETE',
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
  const res = await authFetch(`${BASE}users/${userId}/profile`, {
    method: 'PUT',
    body: JSON.stringify(data),
  });
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
    body: JSON.stringify({ token, newPassword: password }),
  });
  return parseResponse(res);
}

// ── Cart ───────────────────────────────────────────────────
export async function getCart(userId) {
  const res = await authFetch(`${BASE}cart/${userId}`);
  return parseResponse(res);
}

export async function addToCart(userId, productId, quantity = 1, size = null) {
  const res = await authFetch(`${BASE}cart`, {
    method: 'POST',
    body: JSON.stringify({ userId, productId, quantity, size }),
  });
  return parseResponse(res);
}

export async function removeFromCart(cartItemId) {
  const res = await authFetch(`${BASE}cart/${cartItemId}`, {
    method: 'DELETE',
  });
  return parseResponse(res);
}

// ── Orders ──────────────────────────────────────────────────
export async function createOrder(userId, address, shippingMethod, shippingCost) {
  let res;
  try {
    res = await authFetch(`${BASE}orders`, {
      method: 'POST',
      body: JSON.stringify({ userId, address, shippingMethod, shippingCost }),
    });
  } catch (err) {
    if (err?.message?.includes('sessão')) throw err;
    throw new Error('Falha de conexão ao criar o pedido. Tente novamente.');
  }
  return parseResponse(res);
}

export async function getOrder(id) {
  const res = await authFetch(`${BASE}orders/${id}`);
  return parseResponse(res);
}

export async function getOrdersByUser(userId) {
  const res = await authFetch(`${BASE}orders/user/${userId}`);
  return parseResponse(res);
}

export async function cancelOrder(orderId) {
  const res = await authFetch(`${BASE}orders/${orderId}/cancel`, {
    method: 'POST',
    body: JSON.stringify({}),
  });
  return parseResponse(res);
}

export async function getAdminStats() {
  const res = await authFetch(`${BASE}orders/admin/stats`);
  return parseResponse(res);
}

export async function getAllOrders() {
  const res = await authFetch(`${BASE}orders/admin/all`);
  return parseResponse(res);
}

// ── Videos ──────────────────────────────────────────────────
export async function getVideos() {
  const res = await fetch(`${BASE}videos`);
  return parseResponse(res);
}

export async function addVideo(video) {
  const res = await authFetch(`${BASE}videos`, {
    method: 'POST',
    body: JSON.stringify(video),
  });
  return parseResponse(res);
}

export async function deleteVideo(id) {
  const res = await authFetch(`${BASE}videos/${id}`, {
    method: 'DELETE',
  });
  return parseResponse(res);
}

// ── Lookbook ────────────────────────────────────────────────
export async function getLookbook() {
  const res = await fetch(`${BASE}lookbook`);
  return parseResponse(res);
}

export async function addLookbookItem(item) {
  const res = await authFetch(`${BASE}lookbook`, {
    method: 'POST',
    body: JSON.stringify(item),
  });
  return parseResponse(res);
}

export async function deleteLookbookItem(id) {
  const res = await authFetch(`${BASE}lookbook/${id}`, {
    method: 'DELETE',
  });
  return parseResponse(res);
}

export async function markOrderAsPaid(orderId) {
  const res = await authFetch(`${BASE}orders/${orderId}/mark-paid`, {
    method: 'POST',
  });
  return parseResponse(res);
}

export async function syncOrderPayment(orderId) {
  const res = await authFetch(`${BASE}orders/${orderId}/sync-payment`, {
    method: 'POST',
  });
  return parseResponse(res);
}

export async function gerarEtiqueta(orderId) {
  const res = await authFetch(`${BASE}orders/${orderId}/etiqueta`, {
    method: 'POST',
  });
  return parseResponse(res);
}

export async function markAsShipped(orderId, trackingCode) {
  const res = await authFetch(`${BASE}orders/${orderId}/ship`, {
    method: 'POST',
    body: JSON.stringify({ trackingCode }),
  });
  return parseResponse(res);
}

// ── Payment ─────────────────────────────────────────────────
export async function gerarPix(userId, orderId, email, cpf, firstName, lastName) {
  const res = await authFetch(`${BASE}payment/pix`, {
    method: 'POST',
    body: JSON.stringify({ userId, orderId, email, cpf, firstName, lastName }),
  });
  return parseResponse(res);
}

export async function gerarBoleto(userId, orderId, email, cpf, firstName, lastName) {
  const res = await authFetch(`${BASE}payment/boleto`, {
    method: 'POST',
    body: JSON.stringify({ userId, orderId, email, cpf, firstName, lastName }),
  });
  return parseResponse(res);
}

export async function pagarCartao(userId, orderId, token, paymentMethodId, installments, email, cpf, firstName, lastName, cardType) {
  const res = await authFetch(`${BASE}payment/card`, {
    method: 'POST',
    body: JSON.stringify({
      userId, orderId, token, paymentMethodId, installments, email, cpf, firstName, lastName, cardType,
    }),
  });
  return parseResponse(res);
}
