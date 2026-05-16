import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getOrder, cancelOrder } from '../api';
import { useUser } from '../context/UserContext';

const STATUS_LABEL = {
  PENDING: 'Aguardando pagamento',
  PAID: 'Pagamento confirmado',
  SHIPPED: 'Em transporte',
  DELIVERED: 'Entregue',
  CANCELLED: 'Cancelado',
};

export default function OrderConfirmation() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useUser();
  const [order, setOrder] = useState(null);
  const [error, setError] = useState('');
  const [cancelling, setCancelling] = useState(false);

  useEffect(() => {
    getOrder(id)
      .then(setOrder)
      .catch(() => setError('Pedido não encontrado.'));
  }, [id]);

  async function handleCancel() {
    if (!confirm('Tem certeza que deseja cancelar este pedido?')) return;
    setCancelling(true);
    try {
      await cancelOrder(order.id, user.id);
      setOrder(o => ({ ...o, status: 'CANCELLED' }));
    } catch (err) {
      setError(err.message);
    } finally {
      setCancelling(false);
    }
  }

  if (error) return <main className="container"><p className="error">{error}</p></main>;
  if (!order) return <main className="container"><p className="loading">Carregando pedido...</p></main>;

  return (
    <main className="container" style={{ maxWidth: 620 }}>
      <div className="form-card" style={{ marginTop: '2rem' }}>
        <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
          <div style={{ fontSize: '2.5rem', marginBottom: '0.5rem' }}>
            {order.status === 'PAID' ? '✓' : order.status === 'PENDING' ? '⏳' : '📦'}
          </div>
          <h2 style={{ margin: 0 }}>Pedido #{order.id}</h2>
          <span className={`order-status order-status-${order.status?.toLowerCase()}`}>
            {STATUS_LABEL[order.status] || order.status}
          </span>
        </div>

        <h4>Itens</h4>
        {order.items?.map((item, i) => (
          <div key={i} className="checkout-item">
            <span>{item.productName}{item.size ? ` — Tam: ${item.size}` : ''}</span>
            <span>R$ {Number(item.productPrice).toFixed(2)}</span>
          </div>
        ))}

        <div className="checkout-item" style={{ borderTop: '1px solid #eee', paddingTop: '0.5rem', marginTop: '0.5rem' }}>
          <span>Subtotal</span>
          <span>R$ {Number(order.subtotal).toFixed(2)}</span>
        </div>
        <div className="checkout-item">
          <span>Frete ({order.shippingMethod === 'SEDEX' ? 'SEDEX' : 'PAC'})</span>
          <span>R$ {Number(order.shippingCost).toFixed(2)}</span>
        </div>
        <div className="checkout-total">
          <strong>Total: R$ {Number(order.total).toFixed(2)}</strong>
        </div>

        {order.trackingCode && (
          <div style={{ background: '#f5f5f5', borderRadius: 8, padding: '1rem', margin: '1rem 0', textAlign: 'center' }}>
            <p style={{ margin: '0 0 0.5rem', color: '#666', fontSize: '0.85rem' }}>Código de rastreio Correios</p>
            <strong style={{ fontSize: '1.3rem', letterSpacing: 3 }}>{order.trackingCode}</strong>
            <div style={{ marginTop: '0.75rem' }}>
              <a
                href={`https://rastreamento.correios.com.br/app/index.php?objeto=${order.trackingCode}`}
                target="_blank" rel="noreferrer"
                style={{ background: '#111', color: '#fff', padding: '0.6rem 1.5rem', textDecoration: 'none', fontSize: '0.85rem' }}
              >
                Rastrear encomenda
              </a>
            </div>
          </div>
        )}

        <h4 style={{ marginTop: '1.5rem' }}>Endereço de entrega</h4>
        <p style={{ lineHeight: 1.6, color: '#444' }}>
          {order.rua}, {order.numero}{order.complemento ? `, ${order.complemento}` : ''}<br />
          {order.bairro} — {order.cidade}/{order.estado}<br />
          CEP: {order.cep}
        </p>

        <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1.5rem', flexWrap: 'wrap' }}>
          <button className="btn" onClick={() => navigate('/')} style={{ flex: 1, background: '#111', color: '#fff', border: 'none', padding: '0.9rem' }}>
            Continuar comprando
          </button>
          <button className="btn btn-secondary" onClick={() => navigate('/meus-pedidos')} style={{ flex: 1 }}>
            Meus pedidos
          </button>
        </div>

        {['PENDING', 'PAID'].includes(order.status) && (
          <button
            onClick={handleCancel}
            disabled={cancelling}
            style={{ marginTop: '0.75rem', width: '100%', background: 'none', border: '1px solid #e63946', color: '#e63946', padding: '0.7rem', cursor: 'pointer', fontSize: '0.88rem' }}
          >
            {cancelling ? 'Cancelando...' : 'Cancelar pedido'}
          </button>
        )}
      </div>
    </main>
  );
}
