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

// ── Products ──────────────────────────────────────────────
export async function getProducts() {
  const res = await fetch(`${BASE}products`);
  if (!res.ok) throw new Error('Erro ao buscar produtos');
  return res.json();
}

export async function addProduct(product) {
  const res = await fetch(`${BASE}products`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(product),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.erro || 'Erro ao cadastrar produto');
  return data;
}

export async function updateProduct(id, product) {
  const res = await fetch(`${BASE}products/${id}`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(product),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.erro || 'Erro ao atualizar produto');
  return data;
}

export async function deleteProduct(id) {
  const res = await fetch(`${BASE}products/${id}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error('Erro ao deletar produto');
}

// ── Users ──────────────────────────────────────────────────
export async function register(name, email, password) {
  const res = await fetch(`${BASE}users/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, password }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.erro || 'Erro ao cadastrar');
  // Salva token automaticamente
  if (data.token) localStorage.setItem('yz_token', data.token);
  return data;
}

export async function login(email, password) {
  const res = await fetch(`${BASE}users/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.erro || 'Email ou senha inválidos');
  // Salva token automaticamente
  if (data.token) localStorage.setItem('yz_token', data.token);
  return data;
}

export async function updateProfile(userId, data) {
  const res = handleResponse(await fetch(`${BASE}users/${userId}/profile`, {
    method: 'PUT',
    headers: authHeaders(),
    body: JSON.stringify(data),
  }));
  const json = await res.json();
  if (!res.ok) throw new Error(json.erro || 'Erro ao atualizar perfil');
  if (json.token) localStorage.setItem('yz_token', json.token);
  return json;
}

export async function requestPasswordReset(email) {
  const res = await fetch(`${BASE}users/reset-request`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.erro || 'Erro ao solicitar reset');
  return data;
}

export async function resetPassword(token, password) {
  const res = await fetch(`${BASE}users/reset-password`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token, password }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.erro || 'Erro ao redefinir senha');
  return data;
}

// ── Cart ───────────────────────────────────────────────────
export async function getCart(userId) {
  const res = handleResponse(await fetch(`${BASE}cart/${userId}`, { headers: authHeaders() }));
  if (!res.ok) throw new Error('Erro ao buscar carrinho');
  return res.json();
}

export async function addToCart(userId, productId, quantity = 1, size = null) {
  const res = await fetch(`${BASE}cart`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ userId, productId, quantity, size }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.erro || 'Erro ao adicionar ao carrinho');
  return data;
}

export async function removeFromCart(cartItemId) {
  const res = await fetch(`${BASE}cart/${cartItemId}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error('Erro ao remover item');
}

// ── Orders ──────────────────────────────────────────────────
export async function createOrder(userId, address, shippingMethod, shippingCost) {
  const res = await fetch(`${BASE}orders`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ userId, address, shippingMethod, shippingCost }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.erro || 'Erro ao criar pedido');
  return data;
}

export async function getOrder(id) {
  const res = await fetch(`${BASE}orders/${id}`, { headers: authHeaders() });
  if (!res.ok) throw new Error('Erro ao buscar pedido');
  return res.json();
}

export async function getOrdersByUser(userId) {
  const res = await fetch(`${BASE}orders/user/${userId}`, { headers: authHeaders() });
  if (!res.ok) throw new Error('Erro ao buscar pedidos');
  return res.json();
}

export async function cancelOrder(orderId) {
  const res = handleResponse(await fetch(`${BASE}orders/${orderId}/cancel`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({}),
  }));
  const data = await res.json();
  if (!res.ok) throw new Error(data.erro || 'Erro ao cancelar pedido');
  return data;
}

export async function getAdminStats() {
  const res = handleResponse(await fetch(`${BASE}orders/admin/stats`, { headers: authHeaders() }));
  if (!res.ok) throw new Error('Erro ao buscar estatísticas');
  return res.json();
}

export async function getAllOrders() {
  const res = await fetch(`${BASE}orders/admin/all`, { headers: authHeaders() });
  if (!res.ok) throw new Error('Erro ao buscar pedidos');
  return res.json();
}

// ── Lookbook ────────────────────────────────────────────────
export async function getLookbook() {
  const res = await fetch(`${BASE}lookbook`);
  if (!res.ok) throw new Error('Erro ao buscar lookbook');
  return res.json();
}

export async function addLookbookItem(item) {
  const res = await fetch(`${BASE}lookbook`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(item),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.erro || 'Erro ao adicionar foto');
  return data;
}

export async function deleteLookbookItem(id) {
  const res = await fetch(`${BASE}lookbook/${id}`, {
    method: 'DELETE',
    headers: authHeaders(),
  });
  if (!res.ok) throw new Error('Erro ao remover foto');
}

export async function markOrderAsPaid(orderId) {
  const res = handleResponse(await fetch(`${BASE}orders/${orderId}/mark-paid`, {
    method: 'POST',
    headers: authHeaders(),
  }));
  const data = await res.json();
  if (!res.ok) throw new Error(data.erro || 'Erro ao marcar pedido como pago');
  return data;
}

export async function gerarEtiqueta(orderId) {
  const res = handleResponse(await fetch(`${BASE}orders/${orderId}/etiqueta`, {
    method: 'POST',
    headers: authHeaders(),
  }));
  const data = await res.json();
  if (!res.ok) throw new Error(data.erro || 'Erro ao gerar etiqueta');
  return data;
}

export async function markAsShipped(orderId, trackingCode) {
  const res = await fetch(`${BASE}orders/${orderId}/ship`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ trackingCode }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.erro || 'Erro ao marcar como enviado');
  return data;
}

// ── Payment ─────────────────────────────────────────────────
export async function gerarPix(userId, orderId, email, cpf, firstName, lastName) {
  const res = await fetch(`${BASE}payment/pix`, {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({ userId, orderId, email, cpf, firstName, lastName }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.detalhes || data.erro || 'Erro ao gerar PIX');
  return data;
}
