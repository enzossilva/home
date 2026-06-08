import { useEffect, useState } from 'react';

export default function Lookbook() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);

  useEffect(() => {
    fetch('/lookbook')
      .then(r => r.json())
      .then(setItems)
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    function onKey(e) { if (e.key === 'Escape') setSelected(null); }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  if (loading) return <main className="lookbook-page"><p className="loading">Carregando...</p></main>;

  return (
    <main className="lookbook-page">
      {items.length === 0 ? (
        <p className="empty" style={{ textAlign: 'center', padding: '4rem' }}>Em breve.</p>
      ) : (
        <div className="lookbook-grid">
          {items.map(item => (
            <div key={item.id} className="lookbook-item" onClick={() => setSelected(item)}>
              <div className="lookbook-img-wrap">
                <img src={item.imageUrl} alt={item.title || ''} />
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Lightbox */}
      {selected && (
        <div className="lookbook-lightbox" onClick={() => setSelected(null)}>
          <img
            src={selected.imageUrl}
            alt={selected.title || ''}
            onClick={e => e.stopPropagation()}
          />
          {selected.title && <p className="lookbook-lightbox-title">{selected.title}</p>}
        </div>
      )}
    </main>
  );
}
