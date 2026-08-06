import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useUser } from '../context/UserContext';
import { useCartDrawer } from '../context/CartDrawerContext';
import { useSearch } from '../context/SearchContext';
import { useState, useRef, useEffect } from 'react';

export default function Header() {
  const { user, setUser } = useUser();
  const { openCart, cartCount } = useCartDrawer();
  const { setSearch } = useSearch();
  const navigate = useNavigate();
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchVal, setSearchVal] = useState('');
  const [menuOpen, setMenuOpen] = useState(false);
  const [navOpen, setNavOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const searchRef = useRef(null);
  const mobileSearchRef = useRef(null);
  const menuRef = useRef(null);
  const navRef = useRef(null);
  const location = useLocation();
  const isShop = location.pathname === '/produtos';

  function toggleSearch(mobile = false) {
    setSearchOpen(o => {
      if (!o) {
        setTimeout(() => (mobile ? mobileSearchRef : searchRef).current?.focus(), 50);
      } else {
        setSearchVal('');
        setSearch('');
      }
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

  useEffect(() => {
    function handleClickOutside(e) {
      if (menuRef.current && !menuRef.current.contains(e.target)) setMenuOpen(false);
      if (navRef.current && !navRef.current.contains(e.target)) setNavOpen(false);
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  useEffect(() => {
    setNavOpen(false);
    setMenuOpen(false);
  }, [location.pathname]);

  const searchField = (ref, mobile) => (
    <div className={`search-wrapper ${mobile ? 'search-wrapper-mobile' : 'search-wrapper-desktop'}`}>
      <button
        className="header-icon-btn"
        onClick={() => toggleSearch(mobile)}
        title="Buscar"
        type="button"
      >
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="11" cy="11" r="8" />
          <line x1="21" y1="21" x2="16.65" y2="16.65" />
        </svg>
      </button>
      <input
        ref={ref}
        className={`search-input ${searchOpen ? 'search-open' : ''}`}
        type="text"
        placeholder="Buscar..."
        value={searchVal}
        onChange={handleSearchChange}
      />
    </div>
  );

  return (
    <header className={`header ${isShop && !scrolled ? 'header-hero' : ''}`}>
      <nav className="header-nav-left">
        <Link to="/produtos">Produtos</Link>
        <Link to="/fotografia">Fotografia</Link>
        <Link to="/videos">Videos</Link>
      </nav>

      <div className="header-mobile-left">
        <div className="mobile-nav-wrapper" ref={navRef}>
          <button
            className="header-icon-btn mobile-nav-btn"
            onClick={() => setNavOpen(o => !o)}
            title="Menu"
            type="button"
            aria-expanded={navOpen}
            aria-label="Abrir menu"
          >
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </svg>
          </button>
          {navOpen && (
            <div className="mobile-nav-menu">
              <Link to="/produtos" className="mobile-nav-item" onClick={() => setNavOpen(false)}>Produtos</Link>
              <Link to="/fotografia" className="mobile-nav-item" onClick={() => setNavOpen(false)}>Fotografia</Link>
              <Link to="/videos" className="mobile-nav-item" onClick={() => setNavOpen(false)}>Videos</Link>
            </div>
          )}
        </div>
        {searchField(mobileSearchRef, true)}
      </div>

      <Link to="/" className="header-logo">
        <img src="/logo.png" alt="Young Zone" className="header-logo-img" />
      </Link>

      <nav className="header-nav">
        {searchField(searchRef, false)}

        {user ? (
          <>
            <div className="user-menu-wrapper" ref={menuRef}>
              <button
                className="header-icon-btn"
                onClick={() => setMenuOpen(o => !o)}
                title={user.name || 'Conta'}
                type="button"
              >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                  <circle cx="12" cy="7" r="4" />
                </svg>
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

            <button className="header-icon-btn cart-icon-btn" onClick={openCart} title="Carrinho" type="button">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
                <line x1="3" y1="6" x2="21" y2="6" />
                <path d="M16 10a4 4 0 0 1-8 0" />
              </svg>
              {cartCount > 0 && <span className="cart-badge">{cartCount}</span>}
            </button>
          </>
        ) : (
          <Link className="header-icon-btn" to="/login" title="Login">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
              <circle cx="12" cy="7" r="4" />
            </svg>
          </Link>
        )}
      </nav>
    </header>
  );
}
