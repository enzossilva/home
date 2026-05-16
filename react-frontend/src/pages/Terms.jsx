import { Link } from 'react-router-dom';

export default function Terms() {
  return (
    <main className="container" style={{ maxWidth: 760, padding: '2rem 1rem' }}>
      <h1>Termos de Uso</h1>
      <p style={{ color: '#888', fontSize: '0.85rem', marginBottom: '2rem' }}>Última atualização: abril de 2025</p>

      <section className="legal-section">
        <h2>1. Aceitação dos termos</h2>
        <p>
          Ao acessar ou utilizar a loja Young Zone, você concorda com estes Termos de Uso.
          Se não concordar, não utilize nossos serviços.
        </p>
      </section>

      <section className="legal-section">
        <h2>2. Produtos e disponibilidade</h2>
        <ul>
          <li>Os produtos estão sujeitos à disponibilidade de estoque.</li>
          <li>As imagens são ilustrativas. Pode haver pequenas variações de cor dependendo do monitor.</li>
          <li>Nos reservamos o direito de corrigir preços incorretos antes da confirmação do pedido.</li>
          <li>Reservamos o direito de cancelar pedidos suspeitos de fraude.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>3. Preços e pagamento</h2>
        <ul>
          <li>Todos os preços são em Reais (R$) e incluem impostos aplicáveis.</li>
          <li>O frete é calculado no checkout com base no CEP de entrega.</li>
          <li>Pagamentos são processados pelo Mercado Pago (PIX, boleto bancário e cartão de crédito/débito).</li>
          <li>O pedido é confirmado somente após a aprovação do pagamento.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>4. Entrega</h2>
        <ul>
          <li>Entregas são realizadas pelos Correios (PAC ou SEDEX).</li>
          <li>Os prazos de entrega são estimados e contados a partir da postagem, não da compra.</li>
          <li>O código de rastreio será enviado por email após a postagem.</li>
          <li>Não nos responsabilizamos por atrasos causados pelos Correios ou por endereços incorretos informados pelo cliente.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>5. Trocas e devoluções</h2>
        <p>
          Conforme o <strong>Código de Defesa do Consumidor (Lei 8.078/1990)</strong>:
        </p>
        <ul>
          <li>
            <strong>Arrependimento:</strong> você tem até <strong>7 dias</strong> após o recebimento para
            desistir da compra sem justificativa, com devolução integral do valor pago.
          </li>
          <li>
            <strong>Defeito:</strong> produtos com defeito podem ser trocados ou ter o valor devolvido
            em até <strong>30 dias</strong> após o recebimento.
          </li>
          <li>Para solicitar troca ou devolução, entre em contato pelo email: <strong>[SEU EMAIL]</strong></li>
          <li>O produto deve ser devolvido sem uso, com etiquetas e na embalagem original.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>6. Conta do usuário</h2>
        <ul>
          <li>Você é responsável por manter a confidencialidade da sua senha.</li>
          <li>É proibido compartilhar ou ceder sua conta a terceiros.</li>
          <li>Podemos suspender contas que violem estes termos.</li>
        </ul>
      </section>

      <section className="legal-section">
        <h2>7. Propriedade intelectual</h2>
        <p>
          Todo o conteúdo do site (imagens, textos, logotipo, design) é de propriedade da Young Zone
          ou licenciado a nós. É proibida a reprodução sem autorização prévia.
        </p>
      </section>

      <section className="legal-section">
        <h2>8. Limitação de responsabilidade</h2>
        <p>
          A Young Zone não se responsabiliza por danos indiretos decorrentes do uso do site,
          como perda de dados ou interrupção do serviço, exceto nos casos previstos em lei.
        </p>
      </section>

      <section className="legal-section">
        <h2>9. Foro</h2>
        <p>
          Estes termos são regidos pelas leis brasileiras. Para resolução de conflitos,
          fica eleito o foro da comarca de <strong>[SUA CIDADE/ESTADO]</strong>.
        </p>
      </section>

      <section className="legal-section">
        <h2>10. Contato</h2>
        <p>
          Email: <strong>[SEU EMAIL DE CONTATO]</strong><br />
          WhatsApp: <strong>[SEU WHATSAPP]</strong>
        </p>
      </section>

      <div style={{ marginTop: '2rem', paddingTop: '1rem', borderTop: '1px solid #eee', display: 'flex', gap: '1rem', fontSize: '0.85rem' }}>
        <Link to="/">← Voltar à loja</Link>
        <Link to="/privacidade">Política de Privacidade</Link>
      </div>
    </main>
  );
}
