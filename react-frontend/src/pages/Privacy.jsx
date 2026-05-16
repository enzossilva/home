import { Link } from 'react-router-dom';

export default function Privacy() {
  return (
    <main className="container" style={{ maxWidth: 760, padding: '2rem 1rem' }}>
      <h1>Política de Privacidade</h1>
      <p style={{ color: '#888', fontSize: '0.85rem', marginBottom: '2rem' }}>Última atualização: abril de 2025</p>

      <section className="legal-section">
        <h2>1. Quem somos</h2>
        <p>
          A <strong>Young Zone</strong> é uma loja de roupas e acessórios operada por{' '}
          <strong>[NOME COMPLETO / RAZÃO SOCIAL]</strong>, inscrita no CPF/CNPJ{' '}
          <strong>[CPF ou CNPJ]</strong>, com sede em <strong>[CIDADE/ESTADO]</strong>.
          Somos responsáveis pelo tratamento dos seus dados pessoais conforme descrito nesta política.
        </p>
      </section>

      <section className="legal-section">
        <h2>2. Quais dados coletamos</h2>
        <ul>
          <li><strong>Dados de cadastro:</strong> nome, email e senha (armazenada de forma criptografada).</li>
          <li><strong>Dados de entrega:</strong> endereço completo (rua, número, bairro, cidade, estado, CEP) coletados no momento da compra.</li>
          <li><strong>Dados de pagamento:</strong> CPF e informações de pagamento processadas diretamente pelo <strong>Mercado Pago</strong>. Não armazenamos dados de cartão de crédito.</li>
          <li><strong>Dados de uso:</strong> informações sobre suas compras e histórico de pedidos para fins de atendimento.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>3. Para que usamos seus dados</h2>
        <ul>
          <li>Processar e entregar seus pedidos.</li>
          <li>Enviar confirmações de compra e atualizações de rastreio por email.</li>
          <li>Permitir que você acesse seu histórico de pedidos.</li>
          <li>Cumprir obrigações legais e fiscais.</li>
          <li>Comunicar promoções (apenas se você concordar).</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>4. Compartilhamento de dados</h2>
        <p>Seus dados são compartilhados apenas com:</p>
        <ul>
          <li><strong>Mercado Pago:</strong> para processar pagamentos (PIX, boleto, cartão).</li>
          <li><strong>Correios / Melhor Envio:</strong> para entrega do pedido (nome e endereço).</li>
          <li><strong>Autoridades competentes:</strong> quando exigido por lei.</li>
        </ul>
        <p>Não vendemos nem compartilhamos seus dados com terceiros para fins comerciais.</p>
      </section>

      <section className="legal-section">
        <h2>5. Seus direitos (LGPD — Lei 13.709/2018)</h2>
        <p>Conforme a Lei Geral de Proteção de Dados, você tem direito a:</p>
        <ul>
          <li>Confirmar se tratamos seus dados.</li>
          <li>Acessar os dados que temos sobre você.</li>
          <li>Corrigir dados incompletos ou desatualizados.</li>
          <li>Solicitar a exclusão dos seus dados (exceto quando exigido por lei).</li>
          <li>Revogar consentimentos dados anteriormente.</li>
        </ul>
        <p>Para exercer seus direitos, entre em contato pelo email: <strong>[SEU EMAIL DE CONTATO]</strong></p>
      </section>

      <section className="legal-section">
        <h2>6. Cookies</h2>
        <p>
          Utilizamos cookies essenciais para manter sua sessão ativa e o carrinho de compras funcionando.
          Não utilizamos cookies de rastreamento publicitário de terceiros.
        </p>
      </section>

      <section className="legal-section">
        <h2>7. Segurança</h2>
        <p>
          Senhas são armazenadas com criptografia (BCrypt). As comunicações com nosso servidor
          utilizam HTTPS. Tokens de acesso expiram em 24 horas.
        </p>
      </section>

      <section className="legal-section">
        <h2>8. Contato</h2>
        <p>
          Dúvidas sobre esta política? Entre em contato:<br />
          Email: <strong>[SEU EMAIL DE CONTATO]</strong><br />
          WhatsApp: <strong>[SEU WHATSAPP]</strong>
        </p>
      </section>

      <div style={{ marginTop: '2rem', paddingTop: '1rem', borderTop: '1px solid #eee', display: 'flex', gap: '1rem', fontSize: '0.85rem' }}>
        <Link to="/">← Voltar à loja</Link>
        <Link to="/termos">Termos de Uso</Link>
      </div>
    </main>
  );
}
