import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCart, removeFromCart } from '../api';
import { useUser } from '../context/UserContext';
import { useCartDrawer } from '../context/CartDrawerContext';

export default function CartDrawer() {
  const { open, closeCart, setCartCount } = useCartDrawer();
  const { user } = useUser();
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (open && user) load();
  }, [open, user]);

  async function load() {
    setLoading(true);
    try {
      const data = await getCart(user.id);
      setItems(data.items || []);
      setTotal(data.total || 0);
      setCartCount((data.items || []).length);
    } finally {
      setLoading(false);
    }
  }

  async function handleRemove(id) {
    await removeFromCart(id);
    load();
  }

  function handleCheckout() {
    closeCart();
    navigate('/checkout');
  }

  return (
    <>
      {open && <div className="drawer-overlay" onClick={closeCart} />}
      <div className={`cart-drawer ${open ? 'cart-drawer-open' : ''}`}>
        <div className="cart-drawer-header">
          <span className="cart-drawer-title">Cart {items.length > 0 && <span className="cart-drawer-count">{items.length}</span>}</span>
          <button className="cart-drawer-close" onClick={closeCart}>✕</button>
        </div>

        <div className="cart-drawer-body">
          {loading ? (
            <p className="loading">Carregando...</p>
          ) : items.length === 0 ? (
            <p className="empty">Seu carrinho está vazio.</p>
          ) : (
            items.map(item => (
              <div key={item.id} className="drawer-item">
                {item.product?.imageUrl
                  ? <img src={item.product.imageUrl} alt={item.product.name} className="drawer-item-img" />
                  : <div className="drawer-item-no-img" />
                }
                <div className="drawer-item-info">
                  <strong>{item.product?.name}</strong>
                  {item.size && <span className="drawer-item-size">Tam: {item.size}</span>}
                  <span>R$ {Number(item.product?.price).toFixed(2)}</span>
                </div>
                <button className="drawer-item-remove" onClick={() => handleRemove(item.id)}>🗑</button>
              </div>
            ))
          )}
        </div>

        {items.length > 0 && (
          <div className="cart-drawer-footer">
            <div className="drawer-total">
              <span>Total estimado</span>
              <strong>R$ {Number(total).toFixed(2)}</strong>
            </div>
            <button className="btn drawer-checkout-btn" onClick={handleCheckout}>
              FINALIZAR A COMPRA
            </button>
          </div>
        )}
      </div>
    </>
  );
}
