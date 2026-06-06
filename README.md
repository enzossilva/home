# Young Zone — Backend & Frontend

Loja de roupas streetwear com Spring Boot + React. Pagamentos via Mercado Pago, envio pelos Correios via Melhor Envio, banco de dados PostgreSQL.

---

## Tecnologias

| Camada | Tecnologia |
|--------|-----------|
| Backend | Spring Boot 4.0.3 + Java 21 |
| Frontend | React + Vite |
| Banco de dados | PostgreSQL |
| Pagamentos | Mercado Pago (PIX, Boleto, Cartão) |
| Envio | Melhor Envio (Correios PAC/SEDEX) |
| Autenticação | JWT |
| Hospedagem | Railway |

---

## Estrutura do projeto

```
young-zone-back-end/
├── demo/                          # Backend Spring Boot
│   └── src/main/
│       ├── java/com/example/demo/
│       │   ├── config/            # JWT, Security, CORS
│       │   ├── controller/        # Endpoints REST
│       │   ├── model/             # Entidades JPA
│       │   ├── repository/        # Repositórios
│       │   └── service/           # Regras de negócio
│       └── resources/
│           ├── application.properties          # Config local
│           └── application-prod.properties     # Config produção
└── react-frontend/                # Frontend React
    ├── public/                    # Arquivos estáticos (logo.png)
    └── src/
        ├── components/            # Header, CartDrawer, etc.
        ├── context/               # UserContext, CartDrawerContext
        └── pages/                 # Home, Checkout, Admin, etc.
```

---

## Rodar localmente

### Pré-requisitos

- Java 21
- Maven
- Node.js 20+
- PostgreSQL

### 1. Banco de dados

```bash
# Iniciar PostgreSQL (Mac com Homebrew)
brew services start postgresql@14

# Criar banco (só na primeira vez)
psql postgres -c "CREATE DATABASE youngzone;"
```

### 2. Backend

```bash
cd demo
./mvnw spring-boot:run
```

O backend sobe em `http://localhost:8080`.

Na primeira execução, o sistema cria automaticamente o usuário admin:
- **Email:** `admin@youngzone.com`
- **Senha:** `admin123`

### 3. Frontend

```bash
cd react-frontend
npm install    # só na primeira vez
npm run dev
```

O frontend sobe em `http://localhost:3000`.

---

## Variáveis de configuração

O arquivo `demo/src/main/resources/application.properties` contém as configurações locais. **Este arquivo não vai para o GitHub** (está no `.gitignore`).

| Variável | Descrição |
|----------|-----------|
| `spring.datasource.url` | URL do banco PostgreSQL |
| `mercadopago.access-token` | Token do Mercado Pago |
| `mercadopago.public-key` | Chave pública do Mercado Pago |
| `jwt.secret` | Chave secreta para assinar JWT |
| `spring.mail.username` | Email Gmail para envio |
| `spring.mail.password` | Senha de app do Gmail |
| `melhorenvio.token` | Token da API Melhor Envio |
| `loja.*` | Dados da loja (nome, CEP, endereço) |

---

## Funcionalidades

### Loja
- Catálogo de produtos com categorias, tamanhos e estoque por tamanho
- Carrinho deslizante com separação por tamanho
- Checkout em 2 etapas: endereço + pagamento
- Auto-preenchimento de endereço via ViaCEP
- Cálculo de frete PAC/SEDEX via Melhor Envio

### Pagamentos
- PIX (QR Code)
- Boleto bancário
- Cartão de crédito/débito (tokenizado via Mercado Pago SDK)

### Pós-compra
- Email de confirmação do pedido
- Página de acompanhamento do pedido
- Email com código de rastreio ao ser enviado
- Rastreamento via Correios

### Admin (`/admin`)
- Gerenciar produtos (cadastro com tamanhos e estoque individual por tamanho)
- Upload de imagem via Cloudinary
- Painel de pedidos com status e endereço do cliente
- Geração de etiqueta Melhor Envio com um clique
- Dashboard com métricas (receita, ticket médio, pedidos do dia)
- Gerenciamento do Lookbook

### Segurança
- Autenticação JWT
- IDOR prevenido — userId sempre extraído do token
- Rate limiting no login (5 tentativas por 15 min)
- Rotas admin protegidas por role no backend
- Headers de segurança (HSTS, X-Frame-Options, etc.)

---

## Deploy (Railway)

O projeto está configurado para deploy automático no Railway via `railway.toml` e `Dockerfile`.

### Variáveis de ambiente no Railway

Configure as seguintes variáveis no painel do Railway:

```
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=${{Postgres.DATABASE_URL}}
MP_ACCESS_TOKEN=
MP_PUBLIC_KEY=
JWT_SECRET=
MAIL_USERNAME=
MAIL_PASSWORD=
ME_TOKEN=
ME_SANDBOX=false
APP_FRONTEND_URL=https://seudominio.up.railway.app
LOJA_NOME=
LOJA_CPF=
LOJA_TELEFONE=
LOJA_EMAIL=
LOJA_CEP=
LOJA_RUA=
LOJA_NUMERO=
LOJA_BAIRRO=
LOJA_CIDADE=
LOJA_ESTADO=SP
PACOTE_PESO=0.5
PACOTE_ALTURA=3
PACOTE_LARGURA=25
PACOTE_COMPRIMENTO=35
```

### Build

O Dockerfile faz um build multi-stage:
1. Node 20 builda o React
2. Maven + Java 21 builda o Spring Boot com o React embutido
3. Imagem final leve com apenas o JAR

---

## Fluxo de compra

```
1. Cliente adiciona produto ao carrinho (com tamanho)
2. Abre o carrinho → "Finalizar a compra"
3. Preenche endereço → CEP auto-preenchido via ViaCEP
4. Seleciona frete (PAC ou SEDEX) → valor calculado pelo Melhor Envio
5. Escolhe forma de pagamento → PIX / Boleto / Cartão
6. Pedido criado no banco com status PENDING
7. Pagamento aprovado → status vira PAID, carrinho limpo, estoque decrementado
8. Cliente recebe email de confirmação
9. Admin gera etiqueta → Melhor Envio cria envio e debita da carteira
10. Admin posta nos Correios → cliente recebe email com código de rastreio
```

---

## Páginas legais

- `/privacidade` — Política de Privacidade (LGPD)
- `/termos` — Termos de Uso

> **Atenção:** Preencha os campos `[NOME]`, `[CNPJ]`, `[EMAIL]` e `[WHATSAPP]` nessas páginas antes de abrir a loja.
