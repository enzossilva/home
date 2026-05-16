import { useEffect, useState, useMemo } from 'react';
import { getProducts } from '../api';
import ProductCard from '../components/ProductCard';
import { useSearch } from '../context/SearchContext';

export default function Home() {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [activeCategory, setActiveCategory] = useState('Todos');
  const { search } = useSearch();

  useEffect(() => {
    getProducts()
      .then(setProducts)
      .catch(() => setError('Erro ao conectar com o servidor. O backend está rodando?'))
      .finally(() => setLoading(false));
  }, []);

  const categories = useMemo(() => {
    const cats = [...new Set(products.map(p => p.category).filter(Boolean))];
    return ['Todos', ...cats.sort()];
  }, [products]);

  const filtered = products.filter(p => {
    const matchSearch = p.name.toLowerCase().includes(search.toLowerCase());
    const matchCat = activeCategory === 'Todos' || p.category === activeCategory;
    return matchSearch && matchCat;
  });

  return (
    <main className="container">
      {categories.length > 1 && (
        <div className="category-filters">
          {categories.map(cat => (
            <button
              key={cat}
              className={`category-btn ${activeCategory === cat ? 'active' : ''}`}
              onClick={() => setActiveCategory(cat)}
            >
              {cat}
            </button>
          ))}
        </div>
      )}

      {loading && <p className="loading">Carregando...</p>}
      {error && <p className="error">{error}</p>}
      {!loading && !error && filtered.length === 0 && (
        <p className="empty">Nenhum produto encontrado.</p>
      )}

      <div className="grid">
        {filtered.map(p => <ProductCard key={p.id} product={p} />)}
      </div>
    </main>
  );
}
