import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCart, removeFromCart } from '../api';
import { useUser } from '../context/UserContext';

export default function Cart() {
  const { user } = useUser();
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (!user) { navigate('/login'); return; }
    load();
  }, [user]);

  async function load() {
    setLoading(true);
    try {
      const data = await getCart(user.id);
      setItems(data.items || []);
      setTotal(data.total || 0);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function handleRemove(id) {
    try {
      await removeFromCart(id);
      load();
    } catch (e) {
      setError(e.message);
    }
  }

  if (loading) return <main className="container"><p className="loading">Carregando carrinho...</p></main>;

  return (
    <main className="container">
      <h2>Carrinho</h2>
      {error && <p className="error">{error}</p>}

      {items.length === 0 ? (
        <p className="empty">Seu carrinho está vazio.</p>
      ) : (
        <>
          <div className="cart-list">
            {items.map(item => (
              <div key={item.id} className="cart-item">
                {item.product?.imageUrl
                  ? <img src={item.product.imageUrl} alt={item.product?.name} className="cart-img" />
                  : <div className="cart-no-img">Sem imagem</div>
                }
                <div className="cart-info">
                  <strong>{item.product?.name}</strong>
                  <span>Qtd: {item.quantity}</span>
                  <span>R$ {Number(item.product?.price * item.quantity).toFixed(2)}</span>
                </div>
                <button className="btn btn-danger" onClick={() => handleRemove(item.id)}>
                  Remover
                </button>
              </div>
            ))}
          </div>
          <div className="cart-total">
            <strong>Total: R$ {Number(total).toFixed(2)}</strong>
          </div>
          <button className="btn" style={{ marginTop: '1rem' }} onClick={() => navigate('/checkout')}>
            Finalizar compra
          </button>
        </>
      )}
    </main>
  );
}
