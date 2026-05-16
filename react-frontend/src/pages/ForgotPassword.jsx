import { useState } from 'react';
import { Link } from 'react-router-dom';
import { requestPasswordReset } from '../api';

export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [loading, setLoading] = useState(false);
  const [sent, setSent] = useState(false);
  const [error, setError] = useState('');

  async function handleSubmit(e) {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      await requestPasswordReset(email);
      setSent(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="container center">
      <div className="form-card">
        <h2>Esqueci minha senha</h2>

        {sent ? (
          <div style={{ textAlign: 'center', padding: '1rem 0' }}>
            <p style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>📧</p>
            <p>Se o email estiver cadastrado, você receberá um link de redefinição em breve.</p>
            <p style={{ fontSize: '0.85rem', color: '#888', marginTop: '0.5rem' }}>Verifique também a caixa de spam.</p>
            <Link to="/login" className="btn" style={{ display: 'inline-block', marginTop: '1.5rem', background: '#111', color: '#fff', padding: '0.75rem 2rem', textDecoration: 'none' }}>
              Voltar ao login
            </Link>
          </div>
        ) : (
          <>
            <p style={{ color: '#666', fontSize: '0.9rem', marginBottom: '1rem' }}>
              Informe seu email e enviaremos um link para redefinir sua senha.
            </p>
            {error && <p className="error">{error}</p>}
            <form onSubmit={handleSubmit}>
              <label>Email</label>
              <input
                type="email"
                value={email}
                onChange={e => setEmail(e.target.value)}
                placeholder="seu@email.com"
                required
              />
              <button type="submit" className="btn" disabled={loading} style={{ marginTop: '0.5rem' }}>
                {loading ? 'Enviando...' : 'Enviar link de redefinição'}
              </button>
            </form>
            <p className="form-footer">
              <Link to="/login">← Voltar ao login</Link>
            </p>
          </>
        )}
      </div>
    </main>
  );
}
