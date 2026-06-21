import { Link, useNavigate } from 'react-router-dom';
import { useUser } from '../context/UserContext';
import { useCartDrawer } from '../context/CartDrawerContext';
import { useSearch } from '../context/SearchContext';
import { useState, useRef, useEffect } from 'react';
import { useLocation } from 'react-router-dom';

export default function Header() {
  const { user, setUser } = useUser();
  const { openCart, cartCount } = useCartDrawer();
  const { setSearch } = useSearch();
  const navigate = useNavigate();
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchVal, setSearchVal] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const searchRef = useRef(null);
  const menuRef = useRef(null);
  const location = useLocation();
  const isHome = location.pathname === '/';

  function toggleSearch() {
    setSearchOpen(o => {
      if (!o) setTimeout(() => searchRef.current?.focus(), 50);
      else { setSearchVal(''); setSearch(''); }
      return !o;
    });
  }

  function handleSearchChange(e) {
    setSearchVal(e.target.value);
    setSearch(e.target.value);
  }

  function logout() {
    setUser(null);
    setMenuOpen(false);
    navigate('/');
  }

  useEffect(() => {
    function onScroll() { setScrolled(window.scrollY > 60); }
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  // Fecha o menu ao clicar fora
  useEffect(() => {
    function handleClickOutside(e) {
      if (menuRef.current && !menuRef.current.contains(e.target)) {
        setMenuOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  return (
    <header className={`header ${isHome && !scrolled ? 'header-hero' : ''}`}>
      <nav className="header-nav-left">
        <Link to="/">Shop</Link>
        <Link to="/lookbook">Lookbook</Link>
        <Link to="/videos">Videos</Link>
      </nav>
      <Link to="/" className="header-logo">
        <img src="/logo.svg" alt="Young Zone" className="header-logo-img" />
      </Link>
      <nav className="header-nav">
        {/* Busca */}
        <div className="search-wrapper">
          <button className="header-icon-btn" onClick={toggleSearch} title="Buscar">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          </button>
          <input
            ref={searchRef}
            className={`search-input ${searchOpen ? 'search-open' : ''}`}
            type="text"
            placeholder="Buscar..."
            value={searchVal}
            onChange={handleSearchChange}
          />
        </div>

        {user ? (
          <>
            {/* Perfil — dropdown com opções */}
            <div className="user-menu-wrapper" ref={menuRef}>
              <button
                className="header-icon-btn"
                onClick={() => setMenuOpen(o => !o)}
                title={user.name || 'Conta'}
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
              </button>

              {menuOpen && (
                <div className="user-menu">
                  <div className="user-menu-header">
                    <strong>{user.name}</strong>
                    <span>{user.email}</span>
                  </div>
                  {user.role === 'ADMIN' && (
                    <button className="user-menu-item" onClick={() => { navigate('/admin'); setMenuOpen(false); }}>
                      Painel Admin
                    </button>
                  )}
                  <button className="user-menu-item" onClick={() => { navigate('/perfil'); setMenuOpen(false); }}>
                    Meu perfil
                  </button>
                  <button className="user-menu-item" onClick={() => { navigate('/meus-pedidos'); setMenuOpen(false); }}>
                    Meus pedidos
                  </button>
                  <button className="user-menu-item user-menu-logout" onClick={logout}>
                    Sair da conta
                  </button>
                </div>
              )}
            </div>

            {/* Carrinho com badge */}
            <button className="header-icon-btn cart-icon-btn" onClick={openCart} title="Carrinho">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>
              {cartCount > 0 && <span className="cart-badge">{cartCount}</span>}
            </button>
          </>
        ) : (
          <Link className="header-icon-btn" to="/login" title="Login">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          </Link>
        )}
      </nav>
    </header>
  );
}
