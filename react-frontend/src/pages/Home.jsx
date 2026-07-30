import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';

export default function Home() {
  const [heroUrl, setHeroUrl] = useState('');

  useEffect(() => {
    document.body.classList.add('home-landing');
    return () => document.body.classList.remove('home-landing');
  }, []);

  useEffect(() => {
    fetch('/lookbook')
      .then(r => r.json())
      .then(res => {
        const items = res.data ?? [];
        if (items.length) setHeroUrl(items[0].imageUrl);
      })
      .catch(() => {});
  }, []);

  return (
    <div
      className="home-hero"
      style={heroUrl ? { backgroundImage: `url(${heroUrl})` } : {}}
    >
      <div className="home-hero-overlay" />
      <div className="home-hero-content">
        <img src="/logo.png" alt="Young Zone" className="home-hero-logo" />
        <nav className="home-hero-nav">
          <Link to="/produtos">PRODUTOS</Link>
          <Link to="/fotografia">FOTOGRAFIA</Link>
          <Link to="/videos">VIDEOS</Link>
        </nav>
      </div>
    </div>
  );
}
