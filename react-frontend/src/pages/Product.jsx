import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { getProducts, addToCart } from '../api';
import { useUser } from '../context/UserContext';
import ProductImage from '../components/ProductImage';
import SizeChart from '../components/SizeChart';


export default function Product() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { user } = useUser();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [added, setAdded] = useState(false);
  const [cartError, setCartError] = useState('');
  const [selectedSize, setSelectedSize] = useState(null);
  const [descOpen, setDescOpen] = useState(false);
  const [lightbox, setLightbox] = useState(null);

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

  if (loading) return <main className="container"><p className="loading">Carregando...</p></main>;
  if (error)   return <main className="container"><p className="error">{error}</p></main>;

  const hasSizes = product.productSizes && product.productSizes.length > 0;
  const needsSize = hasSizes && !selectedSize;
  const outOfStock = product.stock === 0;

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
              <div className="product-img-stack">
                {imgs.map((url, i) => (
                  <ProductImage
                    key={i}
                    src={url}
                    alt={`${product.name} ${i + 1}`}
                    className="product-detail-photo"
                    onClick={() => setLightbox(url)}
                  />
                ))}
              </div>
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
          <p className="product-detail-price">
            {product.originalPrice != null && Number(product.originalPrice) > Number(product.price) && (
              <span className="product-detail-price-old">R$ {Number(product.originalPrice).toFixed(2)}</span>
            )}
            <span className={
              product.originalPrice != null && Number(product.originalPrice) > Number(product.price)
                ? 'product-detail-price-sale'
                : undefined
            }>
              R$ {Number(product.price).toFixed(2)}
            </span>
          </p>
          {outOfStock && <p className="product-detail-stock">Esgotado</p>}

          {hasSizes && (
            <div>
              <p className="sizes-label">Tamanho</p>
              <div className="sizes-buttons">
                {product.productSizes.map(ps => (
                  <button
                    key={ps.size}
                    className={`size-btn ${selectedSize === ps.size ? 'size-btn-selected' : ''} ${ps.stock === 0 ? 'size-btn-esgotado' : ''}`}
                    onClick={() => ps.stock > 0 && setSelectedSize(ps.size)}
                    disabled={ps.stock === 0}
                    title={ps.stock === 0 ? 'Esgotado' : undefined}
                  >
                    {ps.size}
                  </button>
                ))}
              </div>
            </div>
          )}

          {cartError && <p className="error">{cartError}</p>}

          <div className="product-add-wrap">
            <button
              className={`btn ${added ? 'btn-success' : ''}`}
              onClick={handleAddToCart}
              disabled={outOfStock || needsSize}
              style={{ padding: '1rem', fontSize: '0.95rem', width: '100%', letterSpacing: '0.05em', background: '#111', color: '#fff', borderRadius: '0', border: 'none' }}
            >
              {outOfStock ? 'ESGOTADO' : added ? 'ADICIONADO!' : needsSize ? 'SELECIONE UM TAMANHO' : 'ADICIONAR AO CARRINHO'}
            </button>
          </div>

          <div className="size-guide-block">
            <SizeChart imageUrl={product.sizeChartUrl || null} />
          </div>
        </div>
      </div>
      {lightbox && (
        <div className="lb-lightbox" onClick={() => setLightbox(null)} role="dialog" aria-modal="true">
          <button type="button" className="lb-lightbox-close" onClick={() => setLightbox(null)}>✕</button>
          <img src={lightbox} alt={product.name} onClick={e => e.stopPropagation()} />
        </div>
      )}
    </main>
  );
}
