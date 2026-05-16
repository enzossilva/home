import { createContext, useContext, useState, useEffect } from 'react';

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
    window.addEventListener('yz:logout', handleLogout);
    return () => window.removeEventListener('yz:logout', handleLogout);
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

// Helpers usados pelo api.js
export function getToken() {
  return localStorage.getItem('yz_token');
}

export function saveToken(token) {
  localStorage.setItem('yz_token', token);
}
