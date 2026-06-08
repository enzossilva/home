import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getProducts, addProduct, deleteProduct, updateProduct, getAllOrders, markAsShipped, getAdminStats, gerarEtiqueta, getLookbook, addLookbookItem, deleteLookbookItem } from '../api';
import { useUser } from '../context/UserContext';
import ImageUpload from '../components/ImageUpload';

const EMPTY = { name: '', price: '', stock: '', category: '', description: '', images: [], sizeStocks: {} };
const ALL_SIZES = ['PP', 'P', 'M', 'G', 'GG', 'XGG'];

const STATUS_LABEL = {
  PENDING: 'Aguardando pagamento',
  PAID: 'Pago',
  SHIPPED: 'Enviado',
  DELIVERED: 'Entregue',
  CANCELLED: 'Cancelado',
};

export default function Admin() {
  const { user } = useUser();
  const navigate = useNavigate();
  const [tab, setTab] = useState('dashboard');
  const [stats, setStats] = useState(null);
  const [lookbook, setLookbook] = useState([]);
  const [lbForm, setLbForm] = useState({ imageUrl: '', title: '', ordem: '' });
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [form, setForm] = useState(EMPTY);
  const [editId, setEditId] = useState(null);
  const [message, setMessage] = useState({ text: '', type: '' });
  const [loading, setLoading] = useState(false);
  const [trackingInputs, setTrackingInputs] = useState({});
  const [gerandoEtiqueta, setGerandoEtiqueta] = useState({});

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') { navigate('/'); return; }
    load();
    loadOrders();
    getAdminStats().then(setStats).catch(() => {});
    getLookbook().then(setLookbook).catch(() => {});
  }, [user]);

  async function load() {
    try {
      setProducts(await getProducts());
    } catch {
      showMsg('Erro ao carregar produtos', 'error');
    }
  }

  async function loadOrders() {
    try {
      setOrders(await getAllOrders());
    } catch {
      showMsg('Erro ao carregar pedidos', 'error');
    }
  }

  async function handleShip(orderId) {
    const code = trackingInputs[orderId];
    if (!code?.trim()) { showMsg('Informe o código de rastreio', 'error'); return; }
    try {
      await markAsShipped(orderId, code.trim());
      showMsg('Pedido marcado como enviado! Email enviado ao cliente.');
      loadOrders();
    } catch (err) {
      showMsg(err.message, 'error');
    }
  }

  async function handleGerarEtiqueta(orderId) {
    setGerandoEtiqueta(g => ({ ...g, [orderId]: true }));
    try {
      const result = await gerarEtiqueta(orderId);
      showMsg('Etiqueta gerada! Email enviado ao cliente.');
      loadOrders();
      if (result.labelUrl) window.open(result.labelUrl, '_blank');
    } catch (err) {
      showMsg(err.message, 'error');
    } finally {
      setGerandoEtiqueta(g => ({ ...g, [orderId]: false }));
    }
  }

  function showMsg(text, type = 'success') {
    setMessage({ text, type });
    setTimeout(() => setMessage({ text: '', type: '' }), 3000);
  }

  function handleChange(e) {
    setForm(f => ({ ...f, [e.target.name]: e.target.value }));
  }

  function handleSizeStock(size, value) {
    setForm(f => ({
      ...f,
      sizeStocks: { ...f.sizeStocks, [size]: value === '' ? '' : parseInt(value) || 0 },
    }));
  }

  function startEdit(p) {
    setEditId(p.id);
    const sizeStocks = {};
    (p.productSizes || []).forEach(ps => { sizeStocks[ps.size] = ps.stock; });
    setForm({
      name: p.name,
      price: p.price,
      stock: p.stock,
      category: p.category || '',
      description: p.description || '',
      images: p.images && p.images.length > 0 ? p.images : (p.imageUrl ? [p.imageUrl] : []),
      sizeStocks,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  function cancelEdit() {
    setEditId(null);
    setForm(EMPTY);
  }

  async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true);
    // Filtra tamanhos com estoque > 0
    const sizeStocks = {};
    Object.entries(form.sizeStocks || {}).forEach(([s, v]) => {
      if (v !== '' && v !== undefined) sizeStocks[s] = parseInt(v) || 0;
    });
    const hasSizes = Object.keys(sizeStocks).length > 0;
    const totalStock = hasSizes ? Object.values(sizeStocks).reduce((a, b) => a + b, 0) : parseInt(form.stock) || 0;

    const images = (form.images || []).filter(u => u && u.trim());
    const payload = {
      name: form.name,
      price: parseFloat(form.price),
      stock: totalStock,
      category: form.category,
      description: form.description,
      imageUrl: images[0] || null,
      images: images.length > 0 ? images : null,
      sizeStocks: hasSizes ? sizeStocks : null,
    };
    try {
      if (editId) {
        await updateProduct(editId, payload);
        showMsg('Produto atualizado!');
        setEditId(null);
      } else {
        await addProduct(payload);
        showMsg('Produto cadastrado!');
      }
      setForm(EMPTY);
      load();
    } catch (err) {
      showMsg(err.message, 'error');
    } finally {
      setLoading(false);
    }
  }

  async function handleDelete(id) {
    if (!confirm('Deletar este produto?')) return;
    try {
      await deleteProduct(id);
      showMsg('Produto deletado!');
      load();
    } catch (err) {
      showMsg(err.message, 'error');
    }
  }

  return (
    <main className="container">
      <h2>Painel Admin</h2>

      <div className="payment-tabs" style={{ marginBottom: '1.5rem' }}>
        <button className={`payment-tab ${tab === 'dashboard' ? 'active' : ''}`} onClick={() => setTab('dashboard')}>Dashboard</button>
        <button className={`payment-tab ${tab === 'produtos' ? 'active' : ''}`} onClick={() => setTab('produtos')}>Produtos</button>
        <button className={`payment-tab ${tab === 'lookbook' ? 'active' : ''}`} onClick={() => setTab('lookbook')}>Lookbook</button>
        <button className={`payment-tab ${tab === 'pedidos' ? 'active' : ''}`} onClick={() => setTab('pedidos')}>
          Pedidos {orders.filter(o => o.status === 'PAID').length > 0 && (
            <span style={{ background: '#e63946', color: '#fff', borderRadius: '10px', padding: '1px 7px', fontSize: '0.75rem', marginLeft: '6px' }}>
              {orders.filter(o => o.status === 'PAID').length}
            </span>
          )}
        </button>
      </div>

      {/* ABA DASHBOARD */}
      {tab === 'dashboard' && (
        <div>
          {stats ? (
            <div className="admin-stats-grid">
              <div className="stat-card">
                <span className="stat-label">Receita total</span>
                <strong className="stat-value">R$ {Number(stats.receitaTotal).toFixed(2)}</strong>
              </div>
              <div className="stat-card">
                <span className="stat-label">Ticket médio</span>
                <strong className="stat-value">R$ {Number(stats.ticketMedio).toFixed(2)}</strong>
              </div>
              <div className="stat-card">
                <span className="stat-label">Pedidos hoje</span>
                <strong className="stat-value">{stats.pedidosHoje}</strong>
              </div>
              <div className="stat-card">
                <span className="stat-label">Total de pedidos</span>
                <strong className="stat-value">{stats.totalPedidos}</strong>
              </div>
              <div className="stat-card stat-alert">
                <span className="stat-label">Aguardando envio</span>
                <strong className="stat-value">{stats.aguardandoEnvio}</strong>
              </div>
              <div className="stat-card">
                <span className="stat-label">Pagamentos pendentes</span>
                <strong className="stat-value">{stats.pendentes}</strong>
              </div>
            </div>
          ) : (
            <p className="loading">Carregando...</p>
          )}

          {/* Produtos com estoque baixo */}
          {products.length > 0 && (() => {
            const baixo = products.filter(p => p.stock <= 5 && p.stock > 0);
            const esgotado = products.filter(p => p.stock === 0);
            if (baixo.length === 0 && esgotado.length === 0) return null;
            return (
              <div style={{ marginTop: '1.5rem' }}>
                {esgotado.length > 0 && (
                  <div className="stock-alert stock-alert-danger">
                    <strong>⚠ Esgotados ({esgotado.length})</strong>
                    <div className="stock-alert-list">
                      {esgotado.map(p => (
                        <span key={p.id} className="stock-alert-tag"
                          onClick={() => { setTab('produtos'); }}>
                          {p.name}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
                {baixo.length > 0 && (
                  <div className="stock-alert stock-alert-warn" style={{ marginTop: '0.75rem' }}>
                    <strong>📦 Estoque baixo (&le;5 unidades) ({baixo.length})</strong>
                    <div className="stock-alert-list">
                      {baixo.map(p => (
                        <span key={p.id} className="stock-alert-tag"
                          onClick={() => { setTab('produtos'); }}>
                          {p.name} — {p.stock} un.
                        </span>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            );
          })()}
        </div>
      )}

      {/* ABA LOOKBOOK */}
      {tab === 'lookbook' && (
        <div>
          {message.text && <p className={message.type === 'error' ? 'error' : 'success'}>{message.text}</p>}
          <div className="form-card" style={{ maxWidth: 500, marginBottom: '1.5rem' }}>
            <h3>Adicionar foto</h3>
            <label>URL da imagem *</label>
            <input value={lbForm.imageUrl} onChange={e => setLbForm(f => ({ ...f, imageUrl: e.target.value }))} placeholder="https://..." />
            <label>Legenda</label>
            <input value={lbForm.title} onChange={e => setLbForm(f => ({ ...f, title: e.target.value }))} placeholder="Ex: São Paulo, 2025" />
            <label>Ordem (número)</label>
            <input type="number" value={lbForm.ordem} onChange={e => setLbForm(f => ({ ...f, ordem: e.target.value }))} placeholder="1, 2, 3..." />
            <button className="btn" style={{ marginTop: '0.75rem', background: '#111', color: '#fff', border: 'none' }}
              onClick={async () => {
                if (!lbForm.imageUrl) { showMsg('URL da imagem obrigatória', 'error'); return; }
                try {
                  const item = await addLookbookItem({ imageUrl: lbForm.imageUrl, title: lbForm.title, ordem: lbForm.ordem ? parseInt(lbForm.ordem) : null });
                  setLookbook(l => [...l, item]);
                  setLbForm({ imageUrl: '', title: '', ordem: '' });
                  showMsg('Foto adicionada!');
                } catch (err) { showMsg(err.message, 'error'); }
              }}>
              Adicionar
            </button>
          </div>

          <div className="lookbook-grid">
            {lookbook.map(item => (
              <div key={item.id} className="lookbook-item" style={{ position: 'relative' }}>
                <div className="lookbook-img-wrap">
                  <img src={item.imageUrl} alt={item.title} onError={e => e.target.style.display = 'none'} />
                </div>
                {item.title && <p className="lookbook-title">{item.title}</p>}
                <button onClick={async () => {
                  await deleteLookbookItem(item.id);
                  setLookbook(l => l.filter(i => i.id !== item.id));
                }} style={{ position: 'absolute', top: 4, right: 4, background: '#e63946', color: '#fff', border: 'none', borderRadius: 4, padding: '2px 6px', cursor: 'pointer', fontSize: '0.75rem' }}>✕</button>
              </div>
            ))}
          </div>
          {lookbook.length === 0 && <p className="empty">Nenhuma foto no lookbook ainda.</p>}
        </div>
      )}

      {/* ABA PEDIDOS */}
      {tab === 'pedidos' && (
        <div>
          {message.text && <p className={message.type === 'error' ? 'error' : 'success'}>{message.text}</p>}
          {orders.length === 0 ? (
            <p className="empty">Nenhum pedido ainda.</p>
          ) : (
            orders.map(order => (
              <div key={order.id} className="form-card" style={{ marginBottom: '1rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
                  <div>
                    <strong>Pedido #{order.id}</strong>
                    <span style={{ marginLeft: '0.75rem' }} className={`order-status order-status-${order.status?.toLowerCase()}`}>
                      {STATUS_LABEL[order.status] || order.status}
                    </span>
                    <p style={{ margin: '0.25rem 0 0', fontSize: '0.83rem', color: '#666' }}>
                      {order.createdAt ? new Date(order.createdAt).toLocaleString('pt-BR') : ''}
                      {' — '}{order.user?.email}
                    </p>
                  </div>
                  <strong>R$ {Number(order.total).toFixed(2)}</strong>
                </div>

                <div style={{ marginTop: '0.75rem', fontSize: '0.85rem', color: '#444' }}>
                  <strong>Endereço:</strong> {order.rua}, {order.numero}{order.complemento ? `, ${order.complemento}` : ''} — {order.bairro}, {order.cidade}/{order.estado} — CEP {order.cep}
                </div>

                <div style={{ marginTop: '0.5rem', fontSize: '0.85rem' }}>
                  <strong>Itens:</strong>{' '}
                  {order.items?.map((item, i) => (
                    <span key={i}>{item.productName}{item.size ? ` (${item.size})` : ''}{i < order.items.length - 1 ? ', ' : ''}</span>
                  ))}
                </div>

                <div style={{ marginTop: '0.5rem', fontSize: '0.83rem', color: '#666' }}>
                  Frete: {order.shippingMethod === 'SEDEX' ? 'SEDEX' : 'PAC'} — R$ {Number(order.shippingCost).toFixed(2)}
                </div>

                {order.trackingCode && (
                  <div style={{ marginTop: '0.5rem', fontSize: '0.85rem' }}>
                    <strong>Rastreio:</strong>{' '}
                    <a href={`https://rastreamento.correios.com.br/app/index.php?objeto=${order.trackingCode}`} target="_blank" rel="noreferrer">
                      {order.trackingCode}
                    </a>
                  </div>
                )}

                {order.status === 'PAID' && !order.trackingCode && (
                  <div style={{ marginTop: '0.75rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {/* Botão principal — gera etiqueta no Melhor Envio */}
                    <button
                      className="btn"
                      onClick={() => handleGerarEtiqueta(order.id)}
                      disabled={gerandoEtiqueta[order.id]}
                      style={{ background: '#111', color: '#fff', border: 'none', padding: '0.6rem 1rem', fontSize: '0.88rem' }}
                    >
                      {gerandoEtiqueta[order.id] ? 'Gerando etiqueta...' : '📦 Gerar etiqueta (Melhor Envio)'}
                    </button>

                    {/* Fallback — inserir código manual */}
                    <details style={{ fontSize: '0.82rem' }}>
                      <summary style={{ cursor: 'pointer', color: '#888' }}>Inserir código manualmente</summary>
                      <div style={{ display: 'flex', gap: '0.5rem', marginTop: '0.5rem' }}>
                        <input
                          placeholder="Ex: AA123456789BR"
                          value={trackingInputs[order.id] || ''}
                          onChange={e => setTrackingInputs(t => ({ ...t, [order.id]: e.target.value }))}
                          style={{ flex: 1, padding: '0.4rem 0.6rem', border: '1px solid #ddd', fontSize: '0.83rem' }}
                        />
                        <button className="btn btn-secondary" onClick={() => handleShip(order.id)} style={{ whiteSpace: 'nowrap', fontSize: '0.83rem' }}>
                          Confirmar envio
                        </button>
                      </div>
                    </details>
                  </div>
                )}
              </div>
            ))
          )}
        </div>
      )}

      {/* ABA PRODUTOS */}
      {tab === 'produtos' && (
      <div className="admin-layout">

        {/* Formulário */}
        <div className="form-card admin-form">
          <h3>{editId ? 'Editar Produto' : 'Novo Produto'}</h3>

          {message.text && <p className={message.type === 'error' ? 'error' : 'success'}>{message.text}</p>}

          <form onSubmit={handleSubmit}>
            <label>Nome *</label>
            <input name="name" value={form.name} onChange={handleChange} placeholder="Ex: Camiseta Básica" required />

            <div className="form-row">
              <div>
                <label>Preço (R$) *</label>
                <input name="price" type="number" step="0.01" min="0" value={form.price} onChange={handleChange} placeholder="0.00" required />
              </div>
              <div>
                <label>Estoque *</label>
                <input name="stock" type="number" min="0" value={form.stock} onChange={handleChange} placeholder="0" required />
              </div>
            </div>

            <label>Categoria</label>
            <input name="category" value={form.category} onChange={handleChange} placeholder="Ex: Camiseta, Calça" />

            <label>Tamanhos e estoque</label>
            <div className="size-manager">
              {ALL_SIZES.map(s => {
                const active = s in (form.sizeStocks || {});
                const qty = form.sizeStocks?.[s] ?? 0;
                return (
                  <div key={s} className={`size-manager-row ${active ? 'active' : ''}`}>
                    <button
                      type="button"
                      className={`size-manager-toggle ${active ? 'on' : ''}`}
                      onClick={() => {
                        setForm(f => {
                          const next = { ...f.sizeStocks };
                          if (active) delete next[s];
                          else next[s] = 0;
                          return { ...f, sizeStocks: next };
                        });
                      }}
                    >
                      {s}
                    </button>
                    {active && (
                      <div className="size-manager-qty">
                        <button type="button" className="qty-btn"
                          onClick={() => handleSizeStock(s, Math.max(0, qty - 1))}>−</button>
                        <input
                          type="number" min="0"
                          value={qty}
                          onChange={e => handleSizeStock(s, parseInt(e.target.value) || 0)}
                          className="qty-input"
                        />
                        <button type="button" className="qty-btn"
                          onClick={() => handleSizeStock(s, qty + 1)}>+</button>
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
            <p style={{ fontSize: '0.78rem', color: '#999', marginTop: '0.4rem' }}>
              Clique no tamanho para ativar, depois defina a quantidade em estoque.
            </p>
            {Object.keys(form.sizeStocks || {}).length === 0 && (
              <>
                <label style={{ marginTop: '0.75rem' }}>Estoque geral *</label>
                <input name="stock" type="number" min="0" value={form.stock} onChange={handleChange} placeholder="0" required />
              </>
            )}

            <label>Descrição</label>
            <textarea name="description" value={form.description} onChange={handleChange} rows={3} placeholder="Descrição do produto..." />

            <label>Fotos do produto <span style={{ fontWeight: 400, fontSize: '0.82rem', color: '#888' }}>(primeira = capa)</span></label>
            {(form.images || []).map((url, i) => (
              <div key={i} style={{ display: 'flex', gap: '0.5rem', alignItems: 'center', marginBottom: '0.4rem' }}>
                <ImageUpload
                  value={url}
                  onChange={newUrl => setForm(f => {
                    const imgs = [...(f.images || [])];
                    imgs[i] = newUrl;
                    return { ...f, images: imgs };
                  })}
                />
                <button type="button" onClick={() => setForm(f => ({ ...f, images: f.images.filter((_, j) => j !== i) }))}
                  style={{ background: '#e63946', color: '#fff', border: 'none', borderRadius: 4, padding: '0.3rem 0.6rem', cursor: 'pointer', flexShrink: 0 }}>✕</button>
              </div>
            ))}
            <button type="button" className="btn btn-secondary btn-sm" style={{ marginTop: '0.25rem' }}
              onClick={() => setForm(f => ({ ...f, images: [...(f.images || []), ''] }))}>
              + Adicionar foto
            </button>

            <div style={{ display: 'flex', gap: '0.5rem' }}>
              <button type="submit" className="btn" disabled={loading}>
                {loading ? 'Salvando...' : editId ? 'Salvar' : 'Cadastrar'}
              </button>
              {editId && (
                <button type="button" className="btn btn-secondary" onClick={cancelEdit}>
                  Cancelar
                </button>
              )}
            </div>
          </form>
        </div>

        {/* Lista */}
        <div className="admin-list">
          <h3>Produtos ({products.length})</h3>
          {products.length === 0 && <p className="empty">Nenhum produto cadastrado.</p>}
          {products.map(p => (
            <div key={p.id} className="product-row">
              {p.imageUrl
                ? <img src={p.imageUrl} alt={p.name} className="row-img" onError={e => e.target.style.display = 'none'} />
                : <div className="row-no-img">Sem img</div>
              }
              <div className="row-info">
                <strong>{p.name}</strong>
                <span>{p.category && `${p.category} — `}R$ {Number(p.price).toFixed(2)} — Estoque: {p.stock}</span>
              </div>
              <div style={{ display: 'flex', gap: '0.4rem' }}>
                <button className="btn btn-secondary btn-sm" onClick={() => startEdit(p)}>Editar</button>
                <button className="btn btn-danger btn-sm" onClick={() => handleDelete(p.id)}>Deletar</button>
              </div>
            </div>
          ))}
        </div>

      </div>
      )}
    </main>
  );
}
