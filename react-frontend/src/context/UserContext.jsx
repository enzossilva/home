import { createContext, useContext, useState, useEffect } from 'react';
import { refreshSession } from '../api';

const UserContext = createContext(null);

export function UserProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem('yz_user');
      return stored ? JSON.parse(stored) : null;
    } catch {
      return null;
    }
  });

  useEffect(() => {
    function handleLogout() { setAndStore(null); }
    function handleUser(e) {
      if (e.detail) setAndStore(e.detail);
    }
    window.addEventListener('yz:logout', handleLogout);
    window.addEventListener('yz:user', handleUser);
    return () => {
      window.removeEventListener('yz:logout', handleLogout);
      window.removeEventListener('yz:user', handleUser);
    };
  }, []);

  // Ao abrir o site: se tem token, renova a sessão (mesmo se já estiver “velha”)
  useEffect(() => {
    const token = localStorage.getItem('yz_token');
    if (!token) return;
    let cancelled = false;
    refreshSession()
      .then((data) => {
        if (!cancelled && data?.user) setAndStore(data.user);
      })
      .catch(() => {
        if (!cancelled) setAndStore(null);
      });

    // Renova periodicamente enquanto a aba estiver aberta
    const id = setInterval(() => {
      if (localStorage.getItem('yz_token')) {
        refreshSession().catch(() => {});
      }
    }, 30 * 60 * 1000);

    return () => {
      cancelled = true;
      clearInterval(id);
    };
  }, []);

  function setAndStore(u) {
    setUser(u);
    if (u) {
      localStorage.setItem('yz_user', JSON.stringify(u));
    } else {
      localStorage.removeItem('yz_user');
      localStorage.removeItem('yz_token');
    }
  }

  return (
    <UserContext.Provider value={{ user, setUser: setAndStore }}>
      {children}
    </UserContext.Provider>
  );
}

export function useUser() {
  return useContext(UserContext);
}

export function getToken() {
  return localStorage.getItem('yz_token');
}

export function saveToken(token) {
  localStorage.setItem('yz_token', token);
}
