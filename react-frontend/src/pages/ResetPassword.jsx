import { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { resetPassword } from '../api';

export default function ResetPassword() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [password, setPassword] = useState('');
  const [confirm, setConfirm] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);

  const token = searchParams.get('token');

  useEffect(() => {
    if (!token) setError('Link inválido. Solicite um novo.');
  }, [token]);

  async function handleSubmit(e) {
    e.preventDefault();
    if (password !== confirm) { setError('As senhas não coincidem.'); return; }
    if (password.length < 6) { setError('A senha deve ter pelo menos 6 caracteres.'); return; }
    setLoading(true);
    setError('');
    try {
      await resetPassword(token, password);
      setSuccess(true);
      setTimeout(() => navigate('/login'), 3000);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="container center">
      <div className="form-card">
        <h2>Nova senha</h2>

        {success ? (
          <div style={{ textAlign: 'center', padding: '1rem 0' }}>
            <p style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>✓</p>
            <p>Senha redefinida com sucesso!</p>
            <p style={{ fontSize: '0.85rem', color: '#888' }}>Redirecionando para o login...</p>
          </div>
        ) : (
          <>
            {error && <p className="error">{error}</p>}
            {token && (
              <form onSubmit={handleSubmit}>
                <label>Nova senha</label>
                <input
                  type="password"
                  value={password}
                  onChange={e => setPassword(e.target.value)}
                  placeholder="Mínimo 6 caracteres"
                  required
                />
                <label>Confirmar senha</label>
                <input
                  type="password"
                  value={confirm}
                  onChange={e => setConfirm(e.target.value)}
                  placeholder="Repita a senha"
                  required
                />
                <button type="submit" className="btn" disabled={loading} style={{ marginTop: '0.5rem' }}>
                  {loading ? 'Salvando...' : 'Salvar nova senha'}
                </button>
              </form>
            )}
            {!token && (
              <Link to="/esqueci-senha" className="btn" style={{ display: 'inline-block', marginTop: '1rem' }}>
                Solicitar novo link
              </Link>
            )}
          </>
        )}
      </div>
    </main>
  );
}
