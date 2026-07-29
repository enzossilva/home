import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getProducts, addProduct, deleteProduct, updateProduct, getAllOrders, markAsShipped, getAdminStats, gerarEtiqueta, getLookbook, addLookbookItem, deleteLookbookItem, markOrderAsPaid, syncOrderPayment } from '../api';
import { getVideos, addVideo, deleteVideo } from '../api';
import { useUser } from '../context/UserContext';
import ImageUpload from '../components/ImageUpload';

const EMPTY = { name: '', price: '', originalPrice: '', stock: '', category: '', description: '', images: [], sizeStocks: {}, sizeChartUrl: '' };
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
  const [videos, setVideos] = useState([]);
  const [vidForm, setVidForm] = useState({ youtubeUrl: '', title: '', description: '', ordem: '' });
  const [products, setProducts] = useState([]);
  const [orders, setOrders] = useState([]);
  const [orderFilter, setOrderFilter] = useState('ship'); // ship | pending | shipped | all
  const [syncingId, setSyncingId] = useState(null);
  const [form, setForm] = useState(EMPTY);
  const [editId, setEditId] = useState(null);
  const [message, setMessage] = useState({ text: '', type: '' });
  const [loading, setLoading] = useState(false);
  const [trackingInputs, setTrackingInputs] = useState({});
  const [gerandoEtiqueta, setGerandoEtiqueta] = useState({});

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') { navigate('/'); return; }
    load();
    loadOrders({ syncPending: true });
    getAdminStats().then(setStats).catch(() => {});
    getLookbook().then(setLookbook).catch(() => {});
    getVideos().then(setVideos).catch(() => {});
  }, [user]);

  // Atualiza pedidos a cada 20s enquanto a aba estiver aberta
  useEffect(() => {
    if (tab !== 'pedidos' || !user || user.role !== 'ADMIN') return;
    const id = setInterval(() => { loadOrders(); getAdminStats().then(setStats).catch(() => {}); }, 20000);
    return () => clearInterval(id);
  }, [tab, user]);

  async function load() {
    try {
      setProducts(await getProducts());
    } catch {
      showMsg('Erro ao carregar produtos', 'error');
    }
  }

  async function loadOrders(opts = {}) {
    const { syncPending = false } = opts;
    try {
      let list = await getAllOrders();
      setOrders(list);
      if (syncPending) {
        const pending = (list || []).filter(o => o.status === 'PENDING').slice(0, 10);
        if (pending.length) {
          await Promise.allSettled(pending.map(o => syncOrderPayment(o.id)));
          list = await getAllOrders();
          setOrders(list);
        }
      }
    } catch {
      showMsg('Erro ao carregar pedidos', 'error');
    }
  }

  async function handleSyncPayment(orderId) {
    setSyncingId(orderId);
    try {
      const result = await syncOrderPayment(orderId);
      if (result?.updated || result?.status === 'PAID') {
        showMsg(`Pedido #${orderId} atualizado: ${STATUS_LABEL[result.status] || result.status}`);
      } else {
        showMsg(`Pedido #${orderId} ainda aguardando pagamento no Mercado Pago`);
      }
      setOrders(await getAllOrders());
    } catch (err) {
      showMsg(err.message, 'error');
    } finally {
      setSyncingId(null);
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
      originalPrice: p.originalPrice ?? '',
      stock: p.stock,
      category: p.category || '',
      description: p.description || '',
      images: p.images && p.images.length > 0 ? p.images : (p.imageUrl ? [p.imageUrl] : []),
      sizeStocks,
      sizeChartUrl: p.sizeChartUrl || '',
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
      originalPrice: form.originalPrice !== '' && form.originalPrice != null
        ? parseFloat(form.originalPrice)
        : null,
      stock: totalStock,
      category: form.category,
      description: form.description,
      imageUrl: images[0] || null,
      images: images.length > 0 ? images : null,
      sizeStocks: hasSizes ? sizeStocks : null,
      sizeChartUrl: form.sizeChartUrl?.trim() ? form.sizeChartUrl.trim() : null,
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
        <button className={`payment-tab ${tab === 'videos' ? 'active' : ''}`} onClick={() => setTab('videos')}>Videos</button>
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

      {/* ABA VIDEOS */}
      {tab === 'videos' && (
        <div>
          {message.text && <p className={message.type === 'error' ? 'error' : 'success'}>{message.text}</p>}
          <div className="form-card" style={{ maxWidth: 500, marginBottom: '1.5rem' }}>
            <h3>Adicionar vídeo</h3>
            <label>URL do YouTube *</label>
            <input value={vidForm.youtubeUrl} onChange={e => setVidForm(f => ({ ...f, youtubeUrl: e.target.value }))} placeholder="https://www.youtube.com/watch?v=..." />
            <label>Título</label>
            <input value={vidForm.title} onChange={e => setVidForm(f => ({ ...f, title: e.target.value }))} placeholder="Nome do vídeo" />
            <label>Descrição</label>
            <textarea rows={3} value={vidForm.description} onChange={e => setVidForm(f => ({ ...f, description: e.target.value }))} placeholder="Descrição opcional..." style={{ resize: 'vertical' }} />
            <label>Ordem</label>
            <input type="number" value={vidForm.ordem} onChange={e => setVidForm(f => ({ ...f, ordem: e.target.value }))} placeholder="1, 2, 3..." />
            <button className="btn" style={{ marginTop: '0.75rem', background: '#111', color: '#fff', border: 'none' }}
              onClick={async () => {
                if (!vidForm.youtubeUrl) { showMsg('URL obrigatória', 'error'); return; }
                try {
                  const v = await addVideo({ youtubeUrl: vidForm.youtubeUrl, title: vidForm.title, description: vidForm.description, ordem: vidForm.ordem ? parseInt(vidForm.ordem) : null });
                  setVideos(vs => [...vs, v]);
                  setVidForm({ youtubeUrl: '', title: '', description: '', ordem: '' });
                  showMsg('Vídeo adicionado!');
                } catch (err) { showMsg(err.message, 'error'); }
              }}>
              Adicionar
            </button>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem' }}>
            {videos.map(v => (
              <div key={v.id} className="form-card" style={{ display: 'flex', alignItems: 'center', gap: '1rem', padding: '0.75rem 1rem' }}>
                <div style={{ flex: 1 }}>
                  <strong style={{ fontSize: '0.88rem' }}>{v.title || v.youtubeUrl}</strong>
                  <p style={{ fontSize: '0.78rem', color: '#888', marginTop: '0.2rem' }}>{v.youtubeUrl}</p>
                </div>
                <button onClick={async () => {
                  await deleteVideo(v.id);
                  setVideos(vs => vs.filter(x => x.id !== v.id));
                }} style={{ background: '#e63946', color: '#fff', border: 'none', borderRadius: 4, padding: '0.3rem 0.6rem', cursor: 'pointer', fontSize: '0.75rem' }}>✕</button>
              </div>
            ))}
            {videos.length === 0 && <p className="empty">Nenhum vídeo cadastrado ainda.</p>}
          </div>
        </div>
      )}

      {/* ABA PEDIDOS */}
      {tab === 'pedidos' && (() => {
        const statusRank = { PAID: 0, PENDING: 1, SHIPPED: 2, DELIVERED: 3, CANCELLED: 4 };
        const sorted = [...orders].sort((a, b) => {
          const ra = statusRank[a.status] ?? 9;
          const rb = statusRank[b.status] ?? 9;
          if (ra !== rb) return ra - rb;
          const ta = a.createdAt ? new Date(a.createdAt).getTime() : 0;
          const tb = b.createdAt ? new Date(b.createdAt).getTime() : 0;
          return tb - ta;
        });
        const counts = {
          ship: sorted.filter(o => o.status === 'PAID').length,
          pending: sorted.filter(o => o.status === 'PENDING').length,
          shipped: sorted.filter(o => o.status === 'SHIPPED' || o.status === 'DELIVERED').length,
        };
        const filtered = sorted.filter(o => {
          if (orderFilter === 'all') return true;
          if (orderFilter === 'ship') return o.status === 'PAID';
          if (orderFilter === 'pending') return o.status === 'PENDING';
          if (orderFilter === 'shipped') return o.status === 'SHIPPED' || o.status === 'DELIVERED';
          return true;
        });

        return (
        <div>
          {message.text && <p className={message.type === 'error' ? 'error' : 'success'}>{message.text}</p>}

          <div className="admin-order-filters">
            <button type="button" className={orderFilter === 'ship' ? 'active' : ''} onClick={() => setOrderFilter('ship')}>
              Para enviar {counts.ship > 0 && <span>{counts.ship}</span>}
            </button>
            <button type="button" className={orderFilter === 'pending' ? 'active' : ''} onClick={() => setOrderFilter('pending')}>
              Aguardando pgto {counts.pending > 0 && <span>{counts.pending}</span>}
            </button>
            <button type="button" className={orderFilter === 'shipped' ? 'active' : ''} onClick={() => setOrderFilter('shipped')}>
              Enviados {counts.shipped > 0 && <span>{counts.shipped}</span>}
            </button>
            <button type="button" className={orderFilter === 'all' ? 'active' : ''} onClick={() => setOrderFilter('all')}>
              Todos ({sorted.length})
            </button>
            <button type="button" className="admin-order-refresh" onClick={() => { loadOrders({ syncPending: true }); getAdminStats().then(setStats).catch(() => {}); showMsg('Pedidos atualizados'); }}>
              Atualizar + verificar pagamentos
            </button>
          </div>

          {orderFilter === 'ship' && counts.ship === 0 && (
            <p className="admin-order-hint">Nenhum pedido pago aguardando envio. Quando o cliente pagar, aparece aqui automaticamente.</p>
          )}

          {filtered.length === 0 ? (
            <p className="empty">Nenhum pedido neste filtro.</p>
          ) : (
            filtered.map(order => (
              <div key={order.id} className={`form-card admin-order-card admin-order-${order.status?.toLowerCase()}`} style={{ marginBottom: '1rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', flexWrap: 'wrap', gap: '0.5rem' }}>
                  <div>
                    <strong>Pedido #{order.id}</strong>
                    <span style={{ marginLeft: '0.75rem' }} className={`order-status order-status-${order.status?.toLowerCase()}`}>
                      {STATUS_LABEL[order.status] || order.status}
                    </span>
                    <p style={{ margin: '0.25rem 0 0', fontSize: '0.83rem', color: '#666' }}>
                      {order.createdAt ? new Date(order.createdAt).toLocaleString('pt-BR') : ''}
                      {' — '}{order.user?.email}
                      {order.buyerCpf ? ` — CPF ${order.buyerCpf}` : ''}
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
                    <span key={i}>{item.productName}{item.size ? ` (${item.size})` : ''} ×{item.quantity || 1}{i < order.items.length - 1 ? ', ' : ''}</span>
                  ))}
                </div>

                <div style={{ marginTop: '0.5rem', fontSize: '0.83rem', color: '#666' }}>
                  Frete: {order.shippingMethod === 'SEDEX' ? 'SEDEX' : 'PAC'} — R$ {Number(order.shippingCost || 0).toFixed(2)}
                </div>

                {order.trackingCode && (
                  <div style={{ marginTop: '0.5rem', fontSize: '0.85rem' }}>
                    <strong>Rastreio:</strong>{' '}
                    <a href={`https://rastreamento.correios.com.br/app/index.php?objeto=${order.trackingCode}`} target="_blank" rel="noreferrer">
                      {order.trackingCode}
                    </a>
                  </div>
                )}

                {order.status === 'PENDING' && (
                  <div className="admin-order-pending-box">
                    <p>
                      Aguardando pagamento do cliente. O status muda para <strong>Pago</strong> sozinho após confirmação do Mercado Pago.
                    </p>
                    <div className="admin-order-pending-actions">
                      <button type="button" disabled={syncingId === order.id} onClick={() => handleSyncPayment(order.id)}>
                        {syncingId === order.id ? 'Verificando…' : 'Verificar no Mercado Pago'}
                      </button>
                      <button
                        type="button"
                        className="admin-order-manual"
                        onClick={async () => {
                          if (!confirm('Confirmar pagamento manualmente? Use só se o cliente já pagou e a verificação automática falhou.')) return;
                          try {
                            await markOrderAsPaid(order.id);
                            showMsg('Pedido marcado como pago!');
                            setOrders(await getAllOrders());
                          } catch (err) { showMsg(err.message, 'error'); }
                        }}
                      >
                        Confirmar manualmente
                      </button>
                    </div>
                  </div>
                )}

                {order.status === 'PAID' && !order.trackingCode && (
                  <div style={{ marginTop: '0.75rem', padding: '0.75rem', background: '#e8f5e9', borderRadius: 6, display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    <p style={{ margin: 0, fontSize: '0.82rem', color: '#0a3622' }}>
                      <strong>Pronto para envio</strong> — gere a etiqueta ou informe o rastreio.
                    </p>
                    {!order.buyerCpf && (
                      <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                        <input
                          placeholder="CPF do comprador (só números)"
                          value={trackingInputs[`cpf_${order.id}`] || ''}
                          onChange={e => setTrackingInputs(t => ({ ...t, [`cpf_${order.id}`]: e.target.value }))}
                          style={{ flex: 1, padding: '0.4rem 0.6rem', border: '1px solid #ddd', fontSize: '0.83rem' }}
                        />
                        <button
                          onClick={async () => {
                            const cpf = trackingInputs[`cpf_${order.id}`];
                            if (!cpf?.trim()) { showMsg('Informe o CPF', 'error'); return; }
                            try {
                              const res = await fetch(`/orders/${order.id}/set-cpf`, {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${localStorage.getItem('yz_token')}` },
                                body: JSON.stringify({ cpf: cpf.trim() }),
                              });
                              if (res.ok) { showMsg('CPF salvo!'); loadOrders(); }
                              else showMsg('Erro ao salvar CPF', 'error');
                            } catch { showMsg('Erro ao salvar CPF', 'error'); }
                          }}
                          style={{ background: '#555', color: '#fff', border: 'none', padding: '0.4rem 0.75rem', cursor: 'pointer', fontSize: '0.83rem', whiteSpace: 'nowrap' }}
                        >
                          Salvar CPF
                        </button>
                      </div>
                    )}
                    <button
                      className="btn"
                      onClick={() => handleGerarEtiqueta(order.id)}
                      disabled={gerandoEtiqueta[order.id] || !order.buyerCpf}
                      style={{ background: order.buyerCpf ? '#111' : '#aaa', color: '#fff', border: 'none', padding: '0.6rem 1rem', fontSize: '0.88rem' }}
                    >
                      {gerandoEtiqueta[order.id] ? 'Gerando etiqueta...' : !order.buyerCpf ? 'Salve o CPF primeiro' : 'Gerar etiqueta (Melhor Envio)'}
                    </button>
                    <details style={{ fontSize: '0.82rem' }}>
                      <summary style={{ cursor: 'pointer', color: '#666' }}>Inserir código manualmente</summary>
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
        );
      })()}

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
                <label>Preço de (R$)</label>
                <input
                  name="originalPrice"
                  type="number"
                  step="0.01"
                  min="0"
                  value={form.originalPrice}
                  onChange={handleChange}
                  placeholder="opcional — promoção"
                />
                <span style={{ fontSize: '0.75rem', color: '#888', display: 'block', marginTop: '0.25rem' }}>
                  Valor riscado na loja (ex.: De R$ 120)
                </span>
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

            <label style={{ marginTop: '1rem' }}>
              Guia de tamanhos (opcional)
              <span style={{ fontWeight: 400, fontSize: '0.78rem', color: '#888', display: 'block' }}>
                Deixe vazio para usar o guia padrão de camiseta
              </span>
            </label>
            <ImageUpload
              value={form.sizeChartUrl}
              onChange={url => setForm(f => ({ ...f, sizeChartUrl: url }))}
            />

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
                <span>
                  {p.category && `${p.category} — `}
                  {p.originalPrice != null && Number(p.originalPrice) > Number(p.price)
                    ? <>De R$ {Number(p.originalPrice).toFixed(2)} → </>
                    : null}
                  R$ {Number(p.price).toFixed(2)} — Estoque: {p.stock}
                </span>
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
