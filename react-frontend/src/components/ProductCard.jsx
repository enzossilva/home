import { useNavigate } from 'react-router-dom';
import ProductImage from './ProductImage';

function hasPromo(product) {
  const price = Number(product.price);
  const original = Number(product.originalPrice);
  return product.originalPrice != null && original > price;
}

export default function ProductCard({ product }) {
  const navigate = useNavigate();
  const promo = hasPromo(product);

  return (
    <div className="card card-clickable" onClick={() => navigate(`/product/${product.id}`)}>
      {product.imageUrl
        ? (
          <div className="card-img-wrap">
            <ProductImage src={product.imageUrl} alt={product.name} className="card-img" />
            {promo && <span className="card-promo-badge">PROMO</span>}
          </div>
        )
        : <div className="card-no-img">Sem imagem</div>
      }
      <div className="card-body">
        <h3 className="card-title">{product.name}</h3>
        <div className="card-price-row">
          {promo && (
            <span className="card-price-old">R$ {Number(product.originalPrice).toFixed(2)}</span>
          )}
          <p className={`card-price ${promo ? 'card-price-sale' : ''}`}>
            R$ {Number(product.price).toFixed(2)}
          </p>
        </div>
      </div>
    </div>
  );
}
