import { createContext, useContext, useState } from 'react';

const CartDrawerContext = createContext(null);

export function CartDrawerProvider({ children }) {
  const [open, setOpen] = useState(false);
  const [cartCount, setCartCount] = useState(0);
  return (
    <CartDrawerContext.Provider value={{ open, openCart: () => setOpen(true), closeCart: () => setOpen(false), cartCount, setCartCount }}>
      {children}
    </CartDrawerContext.Provider>
  );
}

export function useCartDrawer() {
  return useContext(CartDrawerContext);
}
