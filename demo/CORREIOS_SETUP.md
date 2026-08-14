# Setup — API oficial dos Correios (frete + etiquetas)

Com contrato + cartão, o site:
- **Cota frete** na API Preço/Prazo (origem = `LOJA_CEP`)
- **Gera etiqueta** na API Pré-postagem (PDF + rastreio)

Se a API Preço falhar (ex. GTW-012) ou faltar config, o frete cai na **tabela estimada** automaticamente.

## 1. Variáveis no Railway (produção)

```
CORREIOS_USERNAME=...                 # CNPJ / usuário Meu Correios
CORREIOS_ACCESS_CODE=...              # senha/código da API no CWS
CORREIOS_CARTAO_POSTAGEM=...
CORREIOS_CONTRATO=...                 # se tiver
CORREIOS_DR=...                       # se tiver
CORREIOS_AMBIENTE=prod                # use hom só para testes
CORREIOS_CODIGO_PAC=03298             # confirme no CWS / cartão
CORREIOS_CODIGO_SEDEX=03220

LOJA_NOME=Youngs Zone Envio
LOJA_CPF=...                          # CNPJ só números
LOJA_TELEFONE=11999999999
LOJA_EMAIL=pedidos@youngszone.com.br
LOJA_CEP=...                          # CEP de origem do frete/remetente
LOJA_RUA=...
LOJA_NUMERO=...
LOJA_COMPLEMENTO=
LOJA_BAIRRO=...
LOJA_CIDADE=...
LOJA_ESTADO=SP
LOJA_PACOTE_PESO=0.5
LOJA_PACOTE_ALTURA=10
LOJA_PACOTE_LARGURA=15
LOJA_PACOTE_COMPRIMENTO=20
```

No CWS, libere: **Token**, **Pré-postagem**, **Preço**, **Prazo**.

## 2. Teste rápido

1. Redeploy após salvar as vars.
2. Checkout: digite um CEP e confira se PAC/SEDEX vêm com valores parecidos com o balcão.
3. Admin: pedido PAID + CPF → **Gerar etiqueta (Correios)** → PDF + rastreio.

## 3. Erros comuns

- Auth 401 → usuário/código CWS errados, ou `CORREIOS_AMBIENTE` não é `prod`.
- “serviço não disponível no cartão” → ajuste `CORREIOS_CODIGO_PAC` / `SEDEX`.
- Frete com erro no checkout → falta `LOJA_CEP`/credenciais ou Preço/Prazo não liberados; veja o log.
- Sem CPF no pedido → “Salvar CPF” no admin antes da etiqueta.
