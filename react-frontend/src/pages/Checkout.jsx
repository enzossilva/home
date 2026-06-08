import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { getCart, createOrder } from '../api';
import { useUser } from '../context/UserContext';

const BASE = '/';

function authH() {
  const t = localStorage.getItem('yz_token');
  return { 'Content-Type': 'application/json', ...(t ? { Authorization: `Bearer ${t}` } : {}) };
}

async function gerarPix(userId, orderId, email, cpf, firstName, lastName) {
  const res = await fetch(`${BASE}payment/pix`, {
    method: 'POST', headers: authH(),
    body: JSON.stringify({ userId, orderId, email, cpf, firstName, lastName }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.detalhes || data.erro || 'Erro ao gerar PIX');
  return data;
}

async function gerarBoleto(userId, orderId, email, cpf, firstName, lastName) {
  const res = await fetch(`${BASE}payment/boleto`, {
    method: 'POST', headers: authH(),
    body: JSON.stringify({ userId, orderId, email, cpf, firstName, lastName }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.detalhes || data.erro || 'Erro ao gerar boleto');
  return data;
}

async function pagarCartao(userId, orderId, token, paymentMethodId, installments, email, cpf, firstName, lastName, cardType) {
  const res = await fetch(`${BASE}payment/card`, {
    method: 'POST', headers: authH(),
    body: JSON.stringify({ userId, orderId, token, paymentMethodId, installments, email, cpf, firstName, lastName, cardType }),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.detalhes || data.erro || 'Erro ao processar cartão');
  return data;
}

async function getPublicKey() {
  const res = await fetch(`${BASE}payment/public-key`);
  const data = await res.json();
  return data.publicKey;
}

function detectBrand(number) {
  const n = number.replace(/\s/g, '');
  if (/^4/.test(n)) return 'visa';
  if (/^5[1-5]/.test(n)) return 'master';
  if (/^3[47]/.test(n)) return 'amex';
  if (/^(636368|438935|504175|451416|636297|5067|4576|4011)/.test(n)) return 'elo';
  if (/^(606282|3841)/.test(n)) return 'hipercard';
  return 'master';
}

function loadMpScript(publicKey) {
  return new Promise((resolve) => {
    if (window.MercadoPago) { resolve(new window.MercadoPago(publicKey)); return; }
    const script = document.createElement('script');
    script.src = 'https://sdk.mercadopago.com/js/v2';
    script.onload = () => resolve(new window.MercadoPago(publicKey));
    document.body.appendChild(script);
  });
}

async function buscarOpcoesFrete(cep) {
  try {
    const res = await fetch(`/frete/calcular?cep=${cep.replace(/[^0-9]/g, '')}`);
    if (!res.ok) return null;
    return await res.json(); // [{ service, name, price, days }]
  } catch {
    return null;
  }
}

const EMPTY_ADDRESS = { cep: '', rua: '', numero: '', complemento: '', bairro: '', cidade: '', estado: '' };

export default function Checkout() {
  const { user } = useUser();
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [subtotal, setSubtotal] = useState(0);
  const [loading, setLoading] = useState(true);
  const [step, setStep] = useState(1); // 1=endereço, 2=pagamento
  const [address, setAddress] = useState(EMPTY_ADDRESS);
  const [cepLoading, setCepLoading] = useState(false);
  const [shippingMethod, setShippingMethod] = useState('PAC');
  const [shippingCost, setShippingCost] = useState(null);
  const [freteOptions, setFreteOptions] = useState([]);
  const [orderId, setOrderId] = useState(null);
  const [creatingOrder, setCreatingOrder] = useState(false);
  const [paying, setPaying] = useState(false);
  const [result, setResult] = useState(null);
  const [copied, setCopied] = useState(false);
  const [error, setError] = useState('');
  const [tab, setTab] = useState('pix');
  const [form, setForm] = useState({ firstName: '', lastName: '', cpf: '', email: '' });
  const [card, setCard] = useState({ number: '', name: '', expiry: '', cvv: '', installments: 1, cardType: 'credit_card' });

  const total = shippingCost != null ? subtotal + shippingCost : subtotal;

  useEffect(() => {
    if (!user) { navigate('/login'); return; }
    setForm(f => ({ ...f, email: user.email || '' }));
    getCart(user.id)
      .then(data => { setItems(data.items || []); setSubtotal(data.total || 0); })
      .catch(() => setError('Erro ao carregar carrinho.'))
      .finally(() => setLoading(false));
  }, [user]);

  function handleAddr(e) {
    const { name, value } = e.target;
    setAddress(a => ({ ...a, [name]: value }));
  }

  async function buscarCep() {
    const digits = address.cep.replace(/[^0-9]/g, '');
    if (digits.length !== 8) return;
    setCepLoading(true);
    try {
      const [viacepRes, opcoes] = await Promise.all([
        fetch(`https://viacep.com.br/ws/${digits}/json/`).then(r => r.json()),
        buscarOpcoesFrete(digits),
      ]);
      if (!viacepRes.erro) {
        setAddress(a => ({
          ...a,
          rua: viacepRes.logradouro || a.rua,
          bairro: viacepRes.bairro || a.bairro,
          cidade: viacepRes.localidade || a.cidade,
          estado: viacepRes.uf || a.estado,
        }));
      }
      if (opcoes && opcoes.length > 0) {
        setFreteOptions(opcoes);
        const selected = opcoes.find(o => o.service === shippingMethod) || opcoes[0];
        setShippingMethod(selected.service);
        setShippingCost(selected.price);
      }
    } catch {}
    finally { setCepLoading(false); }
  }

  function handleShippingMethod(method) {
    setShippingMethod(method);
    const opt = freteOptions.find(o => o.service === method);
    if (opt) setShippingCost(opt.price);
  }

  async function handleProceedToPayment(e) {
    e.preventDefault();
    const required = ['cep', 'rua', 'numero', 'bairro', 'cidade', 'estado'];
    for (const f of required) {
      if (!address[f]) { setError('Preencha todos os campos obrigatórios do endereço.'); return; }
    }
    setError('');
    setCreatingOrder(true);
    try {
      const order = await createOrder(user.id, address, shippingMethod, shippingCost);
      setOrderId(order.id);
      setShippingCost(order.shippingCost);
      setStep(2);
      window.scrollTo({ top: 0 });
    } catch (err) {
      setError(err.message);
    } finally {
      setCreatingOrder(false);
    }
  }

  function handleChange(e) { setForm(f => ({ ...f, [e.target.name]: e.target.value })); }
  function handleCard(e) { setCard(c => ({ ...c, [e.target.name]: e.target.value })); }

  async function handlePix(e) {
    e.preventDefault(); setPaying(true); setError('');
    try {
      const data = await gerarPix(user.id, orderId, form.email, form.cpf, form.firstName, form.lastName);
      setResult({ type: 'pix', ...data });
    } catch (err) { setError(err.message); }
    finally { setPaying(false); }
  }

  async function handleBoleto(e) {
    e.preventDefault(); setPaying(true); setError('');
    try {
      const data = await gerarBoleto(user.id, orderId, form.email, form.cpf, form.firstName, form.lastName);
      setResult({ type: 'boleto', ...data });
    } catch (err) { setError(err.message); }
    finally { setPaying(false); }
  }

  async function handleCartao(e) {
    e.preventDefault(); setPaying(true); setError('');
    try {
      const publicKey = await getPublicKey();
      const mp = await loadMpScript(publicKey);

      const [expMonthRaw, expYearRaw] = card.expiry.split('/');
      const expMonth = expMonthRaw?.trim().padStart(2, '0');
      const expYear = expYearRaw?.trim().length === 2 ? '20' + expYearRaw.trim() : expYearRaw?.trim();
      const cardNumber = card.number.replace(/\D/g, '');
      const cpfNumbers = form.cpf.replace(/\D/g, '');

      console.log('Tokenizando cartão:', { cardNumber, expMonth, expYear, cvv: card.cvv, cpf: cpfNumbers });

      const tokenData = await mp.createCardToken({
        cardNumber,
        cardholderName: card.name,
        cardExpirationMonth: expMonth,
        cardExpirationYear: expYear,
        securityCode: card.cvv,
        identificationType: 'CPF',
        identificationNumber: cpfNumbers,
      });

      console.log('Token gerado:', tokenData);
      const paymentMethodId = tokenData.payment_method_id
        || tokenData.bin_attributes?.brand?.code
        || detectBrand(card.number);
      console.log('paymentMethodId:', paymentMethodId);
      const data = await pagarCartao(user.id, orderId, tokenData.id, paymentMethodId,
        parseInt(card.installments), form.email, form.cpf, form.firstName, form.lastName, card.cardType);
      setResult({ type: 'card', ...data });
      if (data.orderId && (data.status === 'paid' || data.status === 'approved' || data.status === 'processed')) {
        setTimeout(() => navigate(`/pedido/${data.orderId}`), 2000);
      }
    } catch (err) {
      console.error('Erro cartão completo:', err);
      const msg = err?.cause?.message || err?.message || JSON.stringify(err);
      setError('Erro: ' + msg);
    }
    finally { setPaying(false); }
  }

  function copiar() {
    navigator.clipboard.writeText(result.qr_code);
    setCopied(true); setTimeout(() => setCopied(false), 2500);
  }

  if (loading) return <main className="container"><p className="loading">Carregando...</p></main>;

  return (
    <main className="container">
      <button className="btn-back" onClick={() => step === 2 && !result ? setStep(1) : navigate(-1)}>← Voltar</button>
      <h2>Finalizar compra</h2>

      {/* Indicador de etapas */}
      <div className="checkout-steps">
        <span className={`checkout-step ${step >= 1 ? 'active' : ''}`}>1. Entrega</span>
        <span className="checkout-step-sep">›</span>
        <span className={`checkout-step ${step >= 2 ? 'active' : ''}`}>2. Pagamento</span>
      </div>

      <div className="checkout-layout">
        {/* Resumo */}
        <div className="checkout-summary form-card">
          <h3>Resumo do pedido</h3>
          {items.map(item => (
            <div key={item.id} className="checkout-item">
              <span>{item.product?.name}{item.size ? ` (${item.size})` : ''}</span>
              <span>R$ {Number(item.product?.price).toFixed(2)}</span>
            </div>
          ))}
          <div className="checkout-item" style={{ borderTop: '1px solid #eee', paddingTop: '0.5rem' }}>
            <span>Subtotal</span>
            <span>R$ {Number(subtotal).toFixed(2)}</span>
          </div>
          <div className="checkout-item">
            <span>Frete ({freteOptions.find(o => o.service === shippingMethod)?.name || shippingMethod})</span>
            <span>{shippingCost != null ? `R$ ${shippingCost.toFixed(2)}` : '—'}</span>
          </div>
          <div className="checkout-total">
            <strong>Total: R$ {shippingCost != null ? total.toFixed(2) : subtotal.toFixed(2)}</strong>
          </div>
        </div>

        {/* Etapa 1: Endereço */}
        {step === 1 && (
          <div className="form-card checkout-form">
            <h3>Endereço de entrega</h3>
            {error && <p className="error">{error}</p>}
            <form onSubmit={handleProceedToPayment}>
              <div className="form-row">
                <div style={{ flex: 2 }}>
                  <label>CEP *</label>
                  <input
                    name="cep" value={address.cep} onChange={handleAddr}
                    placeholder="00000-000" maxLength={9} required
                    onBlur={buscarCep}
                  />
                </div>
                <div style={{ flex: 1, display: 'flex', alignItems: 'flex-end' }}>
                  <button type="button" className="btn btn-secondary" onClick={buscarCep} disabled={cepLoading} style={{ width: '100%' }}>
                    {cepLoading ? '...' : 'Buscar'}
                  </button>
                </div>
              </div>

              <label>Rua *</label>
              <input name="rua" value={address.rua} onChange={handleAddr} placeholder="Rua, Avenida..." required />

              <div className="form-row">
                <div>
                  <label>Número *</label>
                  <input name="numero" value={address.numero} onChange={handleAddr} placeholder="123" required />
                </div>
                <div>
                  <label>Complemento</label>
                  <input name="complemento" value={address.complemento} onChange={handleAddr} placeholder="Apto, Bloco..." />
                </div>
              </div>

              <label>Bairro *</label>
              <input name="bairro" value={address.bairro} onChange={handleAddr} placeholder="Bairro" required />

              <div className="form-row">
                <div style={{ flex: 2 }}>
                  <label>Cidade *</label>
                  <input name="cidade" value={address.cidade} onChange={handleAddr} placeholder="São Paulo" required />
                </div>
                <div style={{ flex: 1 }}>
                  <label>Estado *</label>
                  <input name="estado" value={address.estado} onChange={handleAddr} placeholder="SP" maxLength={2} required />
                </div>
              </div>

              {freteOptions.length > 0 && (
                <div className="shipping-options">
                  <label>Modalidade de envio</label>
                  <div className="shipping-methods">
                    {freteOptions.map(opt => (
                      <label key={opt.service} className={`shipping-option ${shippingMethod === opt.service ? 'selected' : ''}`}>
                        <input type="radio" name="shipping" value={opt.service}
                          checked={shippingMethod === opt.service}
                          onChange={() => handleShippingMethod(opt.service)} />
                        <div>
                          <strong>{opt.name}</strong>
                          <span>{opt.days}</span>
                        </div>
                        <span>R$ {Number(opt.price).toFixed(2)}</span>
                      </label>
                    ))}
                  </div>
                </div>
              )}

              <button className="btn" type="submit" disabled={creatingOrder} style={{ marginTop: '1.5rem', width: '100%', padding: '1rem', background: '#111', color: '#fff', border: 'none', letterSpacing: '0.05em' }}>
                {creatingOrder ? 'Criando pedido...' : 'CONTINUAR PARA O PAGAMENTO →'}
              </button>
            </form>
          </div>
        )}

        {/* Etapa 2: Pagamento */}
        {step === 2 && (
          <div className="form-card checkout-form">
            {result ? (
              <div className="pix-container">
                {result.type === 'pix' && (
                  <>
                    <h3>Pague via PIX</h3>
                    <p className="pix-total">Total: <strong>R$ {Number(result.total).toFixed(2)}</strong></p>
                    {result.ticket_url && (
                      <a href={result.ticket_url} target="_blank" rel="noreferrer" className="btn" style={{ display: 'block', textAlign: 'center', marginBottom: '1rem' }}>
                        Ver QR Code PIX
                      </a>
                    )}
                    {result.qr_code && (
                      <>
                        <p className="pix-hint">Pix Copia e Cola:</p>
                        <div className="pix-code">{result.qr_code}</div>
                        <button className={`btn ${copied ? 'btn-success' : ''}`} onClick={copiar}>
                          {copied ? 'Copiado!' : 'Copiar código PIX'}
                        </button>
                      </>
                    )}
                    {result.orderId && (
                      <button className="btn btn-secondary" onClick={() => navigate(`/pedido/${result.orderId}`)} style={{ marginTop: '0.75rem', width: '100%' }}>
                        Ver pedido #{result.orderId}
                      </button>
                    )}
                  </>
                )}
                {result.type === 'boleto' && (
                  <>
                    <h3>Boleto gerado!</h3>
                    <p className="pix-total">Total: <strong>R$ {Number(result.total).toFixed(2)}</strong></p>
                    {result.boleto_url && (
                      <a href={result.boleto_url} target="_blank" rel="noreferrer" className="btn" style={{ marginTop: '1rem' }}>
                        Abrir boleto
                      </a>
                    )}
                    {result.orderId && (
                      <button className="btn btn-secondary" onClick={() => navigate(`/pedido/${result.orderId}`)} style={{ marginTop: '0.75rem', width: '100%' }}>
                        Ver pedido #{result.orderId}
                      </button>
                    )}
                  </>
                )}
                {result.type === 'card' && (
                  <>
                    <h3>{result.status === 'paid' || result.status === 'approved' ? '✓ Pagamento aprovado!' : 'Pagamento processado'}</h3>
                    <p className="pix-total">Status: <strong>{result.status}</strong></p>
                    {result.orderId && (
                      <button className="btn" onClick={() => navigate(`/pedido/${result.orderId}`)} style={{ marginTop: '1rem', width: '100%' }}>
                        Ver pedido #{result.orderId}
                      </button>
                    )}
                  </>
                )}
                <p className="pix-status">Status: <strong>{result.status}</strong></p>
              </div>
            ) : (
              <>
                <h3>Forma de pagamento</h3>

                <div className="payment-tabs">
                  {['pix', 'boleto', 'cartao'].map(t => (
                    <button key={t} className={`payment-tab ${tab === t ? 'active' : ''}`} onClick={() => { setTab(t); setError(''); }}>
                      {t === 'pix' ? 'PIX' : t === 'boleto' ? 'Boleto' : 'Cartão'}
                    </button>
                  ))}
                </div>

                {error && <p className="error">{error}</p>}

                <div className="form-row" style={{ marginTop: '1rem' }}>
                  <div>
                    <label>Nome *</label>
                    <input name="firstName" value={form.firstName} onChange={handleChange} placeholder="João" required />
                  </div>
                  <div>
                    <label>Sobrenome *</label>
                    <input name="lastName" value={form.lastName} onChange={handleChange} placeholder="Silva" required />
                  </div>
                </div>
                <label>Email *</label>
                <input name="email" type="email" value={form.email} onChange={handleChange} placeholder="seu@email.com" required />
                <label>CPF *</label>
                <input name="cpf" value={form.cpf} onChange={handleChange} placeholder="000.000.000-00" required />

                {tab === 'pix' && (
                  <form onSubmit={handlePix}>
                    <button className="btn" type="submit" disabled={paying} style={{ marginTop: '1.25rem' }}>
                      {paying ? 'Gerando PIX...' : 'Gerar QR Code PIX'}
                    </button>
                  </form>
                )}

                {tab === 'boleto' && (
                  <form onSubmit={handleBoleto}>
                    <button className="btn" type="submit" disabled={paying} style={{ marginTop: '1.25rem' }}>
                      {paying ? 'Gerando boleto...' : 'Gerar Boleto'}
                    </button>
                  </form>
                )}

                {tab === 'cartao' && (
                  <form onSubmit={handleCartao}>
                    <label>Número do cartão *</label>
                    <input name="number" value={card.number} onChange={handleCard} placeholder="0000 0000 0000 0000" maxLength={19} required />
                    <label>Nome no cartão *</label>
                    <input name="name" value={card.name} onChange={handleCard} placeholder="JOÃO SILVA" required />
                    <div className="form-row">
                      <div>
                        <label>Validade *</label>
                        <input name="expiry" value={card.expiry} onChange={handleCard} placeholder="MM/AA" maxLength={5} required />
                      </div>
                      <div>
                        <label>CVV *</label>
                        <input name="cvv" value={card.cvv} onChange={handleCard} placeholder="123" maxLength={4} required />
                      </div>
                    </div>
                    <label>Tipo de cartão</label>
                    <select name="cardType" value={card.cardType} onChange={handleCard}>
                      <option value="credit_card">Crédito</option>
                      <option value="debit_card">Débito</option>
                    </select>
                    <label>Parcelas</label>
                    <select name="installments" value={card.installments} onChange={handleCard} disabled={card.cardType === 'debit_card'}>
                      {[1,2,3,4,5,6].map(n => (
                        <option key={n} value={n}>{n}x de R$ {(total / n).toFixed(2)}</option>
                      ))}
                    </select>
                    <button className="btn" type="submit" disabled={paying} style={{ marginTop: '1.25rem' }}>
                      {paying ? 'Processando...' : 'Pagar com cartão'}
                    </button>
                  </form>
                )}
              </>
            )}
          </div>
        )}
      </div>
    </main>
  );
}
