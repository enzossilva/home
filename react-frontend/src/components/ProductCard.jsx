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
  const soldOut = !(Number(product.stock) > 0);

  return (
    <div
      className={`card card-clickable${soldOut ? ' card-sold-out' : ''}`}
      onClick={() => navigate(`/product/${product.id}`)}
    >
      {product.imageUrl
        ? (
          <div className="card-img-wrap">
            <ProductImage src={product.imageUrl} alt={product.name} className="card-img" />
            {soldOut && <span className="card-soldout-badge">SOLD OUT</span>}
            {!soldOut && promo && <span className="card-promo-badge">PROMO</span>}
          </div>
        )
        : (
          <div className="card-no-img">
            Sem imagem
            {soldOut && <span className="card-soldout-badge">SOLD OUT</span>}
          </div>
        )
      }
      <div className="card-body">
        <h3 className="card-title">{product.name}</h3>
        <div className="card-price-row">
          {promo && !soldOut && (
            <span className="card-price-old">R$ {Number(product.originalPrice).toFixed(2)}</span>
          )}
          <p className={`card-price ${promo && !soldOut ? 'card-price-sale' : ''}`}>
            {soldOut ? 'Esgotado' : `R$ ${Number(product.price).toFixed(2)}`}
          </p>
        </div>
      </div>
    </div>
  );
}
