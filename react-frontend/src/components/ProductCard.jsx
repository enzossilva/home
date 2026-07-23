import { useNavigate } from 'react-router-dom';
import ProductImage from './ProductImage';

export default function ProductCard({ product }) {
  const navigate = useNavigate();

  return (
    <div className="card card-clickable" onClick={() => navigate(`/product/${product.id}`)}>
      {product.imageUrl
        ? <ProductImage src={product.imageUrl} alt={product.name} className="card-img" />
        : <div className="card-no-img">Sem imagem</div>
      }
      <div className="card-body">
        <h3 className="card-title">{product.name}</h3>
        <p className="card-price">R$ {Number(product.price).toFixed(2)}</p>
      </div>
    </div>
  );
}
