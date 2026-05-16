import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { updateProfile } from '../api';
import { useUser } from '../context/UserContext';

export default function Profile() {
  const { user, setUser } = useUser();
  const navigate = useNavigate();

  const [form, setForm] = useState({ name: user?.name || '', email: user?.email || '', password: '', confirm: '' });
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState('');
  const [error, setError] = useState('');

  if (!user) { navigate('/login'); return null; }

  function handleChange(e) { setForm(f => ({ ...f, [e.target.name]: e.target.value })); }

  async function handleSubmit(e) {
    e.preventDefault();
    setError(''); setSuccess('');
    if (form.password && form.password !== form.confirm) {
      setError('As senhas não coincidem.'); return;
    }
    setLoading(true);
    try {
      const payload = { name: form.name, email: form.email };
      if (form.password) payload.password = form.password;

      const data = await updateProfile(user.id, payload);
      setUser(data.user);
      setForm(f => ({ ...f, password: '', confirm: '' }));
      setSuccess('Perfil atualizado com sucesso!');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="container center">
      <div className="form-card" style={{ minWidth: 340 }}>
        <h2>Meu perfil</h2>

        {success && <p className="success">{success}</p>}
        {error && <p className="error">{error}</p>}

        <form onSubmit={handleSubmit}>
          <label>Nome</label>
          <input name="name" value={form.name} onChange={handleChange} placeholder="Seu nome" required />

          <label>Email</label>
          <input name="email" type="email" value={form.email} onChange={handleChange} placeholder="seu@email.com" required />

          <hr style={{ margin: '1rem 0', border: 'none', borderTop: '1px solid #eee' }} />
          <p style={{ fontSize: '0.83rem', color: '#888', marginBottom: '0.5rem' }}>Deixe em branco para manter a senha atual</p>

          <label>Nova senha</label>
          <input name="password" type="password" value={form.password} onChange={handleChange} placeholder="Mínimo 6 caracteres" />

          <label>Confirmar nova senha</label>
          <input name="confirm" type="password" value={form.confirm} onChange={handleChange} placeholder="Repita a nova senha" />

          <button type="submit" className="btn" disabled={loading} style={{ marginTop: '1rem', width: '100%', background: '#111', color: '#fff', border: 'none', padding: '0.9rem' }}>
            {loading ? 'Salvando...' : 'Salvar alterações'}
          </button>
        </form>
      </div>
    </main>
  );
}
