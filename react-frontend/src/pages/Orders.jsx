import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getOrdersByUser } from '../api';
import { useUser } from '../context/UserContext';

const STATUS_LABEL = {
  PENDING: 'Aguardando pagamento',
  PAID: 'Pago',
  SHIPPED: 'Em transporte',
  DELIVERED: 'Entregue',
  CANCELLED: 'Cancelado',
};

export default function Orders() {
  const { user } = useUser();
  const navigate = useNavigate();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) { navigate('/login'); return; }
    getOrdersByUser(user.id)
      .then(setOrders)
      .finally(() => setLoading(false));
  }, [user]);

  if (loading) return <main className="container"><p className="loading">Carregando...</p></main>;

  return (
    <main className="container" style={{ maxWidth: 680 }}>
      <h2>Meus pedidos</h2>
      {orders.length === 0 ? (
        <p className="empty">Você ainda não fez nenhum pedido.</p>
      ) : (
        orders.map(order => (
          <div key={order.id} className="form-card order-card" onClick={() => navigate(`/pedido/${order.id}`)} style={{ cursor: 'pointer', marginBottom: '1rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div>
                <strong>Pedido #{order.id}</strong>
                <p style={{ margin: '0.25rem 0 0', fontSize: '0.85rem', color: '#666' }}>
                  {order.createdAt ? new Date(order.createdAt).toLocaleDateString('pt-BR') : ''}
                  {' — '}{order.items?.length || 0} ite{order.items?.length === 1 ? 'm' : 'ns'}
                </p>
              </div>
              <div style={{ textAlign: 'right' }}>
                <span className={`order-status order-status-${order.status?.toLowerCase()}`}>
                  {STATUS_LABEL[order.status] || order.status}
                </span>
                <p style={{ margin: '0.25rem 0 0', fontWeight: 600 }}>R$ {Number(order.total).toFixed(2)}</p>
              </div>
            </div>
          </div>
        ))
      )}
    </main>
  );
}
