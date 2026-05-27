import { useEffect, useState } from 'react';

export default function Lookbook() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetch('/lookbook')
      .then(r => r.json())
      .then(setItems)
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <main className="lookbook-page"><p className="loading">Carregando...</p></main>;

  return (
    <main className="lookbook-page">
      {items.length === 0 ? (
        <p className="empty" style={{ textAlign: 'center', padding: '4rem' }}>Em breve.</p>
      ) : (
        <div className="lookbook-grid">
          {items.map(item => (
            <div key={item.id} className="lookbook-item">
              <div className="lookbook-img-wrap">
                <img src={item.imageUrl} alt={item.title} />
              </div>
              {item.title && <p className="lookbook-title">{item.title}</p>}
            </div>
          ))}
        </div>
      )}
    </main>
  );
}
