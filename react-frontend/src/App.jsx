import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { UserProvider } from './context/UserContext';
import { SearchProvider } from './context/SearchContext';
import { CartDrawerProvider } from './context/CartDrawerContext';
import Header from './components/Header';
import CartDrawer from './components/CartDrawer';
import CookieBanner from './components/CookieBanner';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import Admin from './pages/Admin';
import Product from './pages/Product';
import Checkout from './pages/Checkout';
import OrderConfirmation from './pages/OrderConfirmation';
import Orders from './pages/Orders';
import Profile from './pages/Profile';
import ForgotPassword from './pages/ForgotPassword';
import ResetPassword from './pages/ResetPassword';
import Privacy from './pages/Privacy';
import Terms from './pages/Terms';
import NotFound from './pages/NotFound';
import Lookbook from './pages/Lookbook';

export default function App() {
  return (
    <SearchProvider>
      <UserProvider>
        <CartDrawerProvider>
          <BrowserRouter>
            <Header />
            <CartDrawer />
            <CookieBanner />
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route path="/admin" element={<Admin />} />
              <Route path="/product/:id" element={<Product />} />
              <Route path="/checkout" element={<Checkout />} />
              <Route path="/pedido/:id" element={<OrderConfirmation />} />
              <Route path="/meus-pedidos" element={<Orders />} />
              <Route path="/perfil" element={<Profile />} />
              <Route path="/esqueci-senha" element={<ForgotPassword />} />
              <Route path="/reset-senha" element={<ResetPassword />} />
              <Route path="/privacidade" element={<Privacy />} />
              <Route path="/termos" element={<Terms />} />
              <Route path="/lookbook" element={<Lookbook />} />
              <Route path="*" element={<NotFound />} />
            </Routes>
          </BrowserRouter>
        </CartDrawerProvider>
      </UserProvider>
    </SearchProvider>
  );
}
