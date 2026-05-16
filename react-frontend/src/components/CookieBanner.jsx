import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';

export default function CookieBanner() {
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const consent = localStorage.getItem('yz_cookie_consent');
    if (!consent) setVisible(true);
  }, []);

  function accept() {
    localStorage.setItem('yz_cookie_consent', 'accepted');
    setVisible(false);
  }

  if (!visible) return null;

  return (
    <div className="cookie-banner">
      <p>
        Usamos cookies essenciais para manter o carrinho e sua sessão.{' '}
        <Link to="/privacidade">Saiba mais</Link>
      </p>
      <button onClick={accept} className="cookie-accept-btn">Entendi</button>
    </div>
  );
}
