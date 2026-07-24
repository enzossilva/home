import { useEffect, useState, useRef, useCallback } from 'react';

export default function Lookbook() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selected, setSelected] = useState(null);
  const [current, setCurrent] = useState(0);
  const slideRefs = useRef([]);
  const wrapRef = useRef(null);

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
        if (e.isIntersecting) {
          e.target.classList.add('in-view');
          const idx = slideRefs.current.indexOf(e.target);
          if (idx >= 0) setCurrent(idx);
        } else {
          // Remove para o efeito de rolagem repetir ao voltar na imagem
          e.target.classList.remove('in-view');
        }
      }),
      { threshold: 0.45 }
    );
    slideRefs.current.forEach(s => s && observer.observe(s));
    return () => observer.disconnect();
  }, [items]);

  const goTo = useCallback((index) => {
    const el = slideRefs.current[index];
    if (!el) return;
    el.scrollIntoView({ behavior: 'smooth', inline: 'start', block: 'nearest' });
    setCurrent(index);
  }, []);

  const goPrev = useCallback((e) => {
    e?.stopPropagation();
    if (current > 0) goTo(current - 1);
  }, [current, goTo]);

  const goNext = useCallback((e) => {
    e?.stopPropagation();
    if (current < items.length - 1) goTo(current + 1);
  }, [current, items.length, goTo]);

  useEffect(() => {
    function onKey(e) {
      if (selected) {
        if (e.key === 'Escape') setSelected(null);
        return;
      }
      if (e.key === 'ArrowLeft') goPrev();
      if (e.key === 'ArrowRight') goNext();
      if (e.key === 'Escape') setSelected(null);
    }
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [selected, goPrev, goNext]);

  if (loading) return (
    <div className="lb-loading">
      <span>YOUNGS ZONE</span>
    </div>
  );

  return (
    <>
      <div className="lb-wrap" ref={wrapRef}>
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

      {items.length > 1 && (
        <div className="lb-nav-arrows" aria-hidden="false">
          <button
            type="button"
            className="lb-arrow lb-arrow-prev"
            onClick={goPrev}
            disabled={current <= 0}
            aria-label="Imagem anterior"
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4">
              <polyline points="15 18 9 12 15 6" />
            </svg>
          </button>
          <button
            type="button"
            className="lb-arrow lb-arrow-next"
            onClick={goNext}
            disabled={current >= items.length - 1}
            aria-label="Próxima imagem"
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.4">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </button>
        </div>
      )}

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
