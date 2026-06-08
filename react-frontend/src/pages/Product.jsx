import { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getProducts, addToCart } from '../api';
import { useUser } from '../context/UserContext';
import { useCartDrawer } from '../context/CartDrawerContext';


export default function Product() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useUser();
  const { openCart } = useCartDrawer();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [added, setAdded] = useState(false);
  const [cartError, setCartError] = useState('');
  const [selectedSize, setSelectedSize] = useState(null);
  const [descOpen, setDescOpen] = useState(false);
  const [activeImg, setActiveImg] = useState(0);

  useEffect(() => {
    getProducts()
      .then(products => {
        const found = products.find(p => p.id === parseInt(id));
        if (!found) setError('Produto não encontrado.');
        else setProduct(found);
      })
      .catch(() => setError('Erro ao carregar produto.'))
      .finally(() => setLoading(false));
  }, [id]);

  async function handleAddToCart() {
    if (!user) { navigate('/login'); return; }
    try {
      await addToCart(user.id, product.id, 1, selectedSize);
      setAdded(true);
      setTimeout(() => setAdded(false), 2500);
    } catch (e) {
      setCartError(e.message);
    }
  }

  async function handleBuy() {
    if (!user) { navigate('/login'); return; }
    try {
      await addToCart(user.id, product.id, 1, selectedSize);
      openCart();
    } catch (e) {
      setCartError(e.message);
    }
  }

  if (loading) return <main className="container"><p className="loading">Carregando...</p></main>;
  if (error)   return <main className="container"><p className="error">{error}</p></main>;

  return (
    <main className="container">
      <button className="btn-back" onClick={() => navigate(-1)}>← Voltar</button>

      <div className="product-detail">
        <div className="product-detail-img">
          {(() => {
            const imgs = product.images && product.images.length > 0
              ? product.images
              : product.imageUrl ? [product.imageUrl] : [];
            if (imgs.length === 0) return <div className="product-detail-no-img">Sem imagem</div>;
            return (
              <>
                <img src={imgs[activeImg]} alt={product.name} onError={e => e.target.style.display = 'none'} />
                {imgs.length > 1 && (
                  <div className="product-thumbnails">
                    {imgs.map((url, i) => (
                      <img key={i} src={url} alt={`foto ${i+1}`}
                        className={`product-thumb ${activeImg === i ? 'active' : ''}`}
                        onClick={() => setActiveImg(i)}
                        onError={e => e.target.style.display = 'none'} />
                    ))}
                  </div>
                )}
              </>
            );
          })()}
        </div>

        <div className="product-detail-info">
          {product.category && <span className="card-category">{product.category}</span>}
          <h1>{product.name}</h1>
          {product.description && (
            <div className="product-dropdown">
              <button className="product-dropdown-btn" onClick={() => setDescOpen(o => !o)}>
                Descrição <span>{descOpen ? '▲' : '▼'}</span>
              </button>
              {descOpen && <p className="product-dropdown-body">{product.description}</p>}
            </div>
          )}
          <p className="product-detail-price">R$ {Number(product.price).toFixed(2)}</p>
          <p className="product-detail-stock">
            {product.stock > 0 ? `${product.stock} em estoque` : 'Esgotado'}
          </p>

          {product.productSizes && product.productSizes.length > 0 && (
            <div>
              <p className="sizes-label">Tamanho</p>
              <div className="sizes-buttons">
                {product.productSizes.map(ps => (
                  <button
                    key={ps.size}
                    className={`size-btn ${selectedSize === ps.size ? 'size-btn-selected' : ''} ${ps.stock === 0 ? 'size-btn-esgotado' : ''}`}
                    onClick={() => ps.stock > 0 && setSelectedSize(ps.size)}
                    disabled={ps.stock === 0}
                    title={ps.stock === 0 ? 'Esgotado' : `${ps.stock} disponíveis`}
                  >
                    {ps.size}
                  </button>
                ))}
              </div>
            </div>
          )}

          {cartError && <p className="error">{cartError}</p>}

          {(() => {
            const hasSizes = product.productSizes && product.productSizes.length > 0;
            const needsSize = hasSizes && !selectedSize;
            const outOfStock = product.stock === 0;
            return (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '0.75rem', marginTop: '1.5rem' }}>
                <button
                  className={`btn ${added ? 'btn-success' : ''}`}
                  onClick={handleAddToCart}
                  disabled={outOfStock || needsSize}
                  style={{ padding: '1rem', fontSize: '0.95rem', width: '100%', letterSpacing: '0.05em', background: '#111', color: '#fff', borderRadius: '0', border: 'none' }}
                >
                  {outOfStock ? 'ESGOTADO' : added ? 'ADICIONADO!' : needsSize ? 'SELECIONE UM TAMANHO' : 'ADICIONAR AO CARRINHO'}
                </button>
                {!outOfStock && (
                  <button
                    className="btn"
                    onClick={handleBuy}
                    disabled={needsSize}
                    style={{ padding: '1rem', fontSize: '0.95rem', width: '100%', letterSpacing: '0.05em', background: '#111', color: '#fff', borderRadius: '0', border: 'none' }}
                  >
                    {needsSize ? 'SELECIONE UM TAMANHO' : 'COMPRE JÁ'}
                  </button>
                )}
              </div>
            );
          })()}
        </div>
      </div>
    </main>
  );
}
