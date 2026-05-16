import { useNavigate } from 'react-router-dom';

export default function NotFound() {
  const navigate = useNavigate();
  return (
    <main className="container center" style={{ minHeight: '60vh', flexDirection: 'column', gap: '1rem', textAlign: 'center' }}>
      <h1 style={{ fontSize: '5rem', margin: 0, fontWeight: 800, letterSpacing: '-2px' }}>404</h1>
      <p style={{ fontSize: '1.1rem', color: '#666' }}>Página não encontrada.</p>
      <button className="btn" onClick={() => navigate('/')}
        style={{ background: '#111', color: '#fff', border: 'none', padding: '0.8rem 2rem', cursor: 'pointer', marginTop: '0.5rem' }}>
        Voltar à loja
      </button>
    </main>
  );
}
