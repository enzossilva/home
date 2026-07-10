import { useEffect, useState, useRef } from 'react';

export default function Lookbook() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);
  const slideRefs = useRef([]);

  useEffect(() => {
    fetch('/lookbook')
      .then(r => r.json())
      .then(res => setItems(res.data ?? []))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    document.body.classList.add('lookbook-mode');
    return () => document.body.classList.remove('lookbook-mode');
  }, []);

  useEffect(() => {
    if (!items.length) return;
    const observer = new IntersectionObserver(
      entries => entries.forEach(e => {
        if (e.isIntersecting) e.target.classList.add('in-view');
      }),
      { threshold: 0.45 }
    );
    slideRefs.current.forEach(s => s && observer.observe(s));
    return () => observer.disconnect();
  }, [items]);

  useEffect(() => {
    function onKey(e) { if (e.key === 'Escape') setSelected(null); }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, []);

  if (loading) return (
    <div className="lb-loading">
      <span>YOUNGS ZONE</span>
    </div>
  );

  return (
    <>
      <div className="lb-wrap">
        {items.length === 0 ? (
          <div className="lb-empty">Em breve.</div>
        ) : items.map((item, i) => (
          <section
            key={item.id}
            className="lb-slide"
            ref={el => (slideRefs.current[i] = el)}
            onClick={() => setSelected(item)}
          >
            <img src={item.imageUrl} alt={item.title || `Lookbook ${i + 1}`} />
            <div className="lb-overlay" />

            <div className="lb-caption">
              <span className="lb-label">YOUNGS ZONE</span>
              {item.title && <p className="lb-title">{item.title}</p>}
            </div>

            <div className="lb-counter">
              {String(i + 1).padStart(2, '0')}&nbsp;/&nbsp;{String(items.length).padStart(2, '0')}
            </div>

            {i === 0 && (
              <div className="lb-scroll-hint">
                <div className="lb-scroll-line" />
                <span>SCROLL</span>
              </div>
            )}
          </section>
        ))}
      </div>

      {selected && (
        <div className="lb-lightbox" onClick={() => setSelected(null)}>
          <button className="lb-lightbox-close" onClick={() => setSelected(null)}>✕</button>
          <img
            src={selected.imageUrl}
            alt={selected.title || ''}
            onClick={e => e.stopPropagation()}
          />
          {selected.title && <p className="lb-lightbox-title">{selected.title}</p>}
        </div>
      )}
    </>
  );
}
