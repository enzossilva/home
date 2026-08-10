# Setup — API oficial dos Correios (etiquetas)

O botão **Gerar etiqueta (Correios)** no admin cria pré-postagem + PDF + rastreio.
Sem contrato isso **não funciona** — a API recusa.

## 1. O que você precisa nos Correios (antes do Railway)

1. Conta **Meu Correios** / Correios Empresas (PJ recomenda-se).
2. **Contrato comercial** + **cartão de postagem** (gerente comercial / portal).
3. No **CWS** (Correios Web Services), liberar APIs:
   - Token
   - Pré-postagem
   - (opcional) Preço / Prazo
4. Anotar:
   - usuário Meu Correios
   - senha/código de acesso da API
   - número do cartão de postagem
   - número do contrato (e DR/SE se existir)
5. Confirmar no CWS os **códigos de serviço** PAC e SEDEX do seu cartão
   (podem diferir dos padrões `03298` / `03220`).

Homologação: `https://apihom.correios.com.br`  
Produção: `https://api.correios.com.br`

## 2. Variáveis no Railway

```
CORREIOS_USERNAME=...
CORREIOS_ACCESS_CODE=...
CORREIOS_CARTAO_POSTAGEM=...
CORREIOS_CONTRATO=...          # opcional
CORREIOS_DR=...                # opcional
CORREIOS_AMBIENTE=hom          # use hom até validar; depois prod
CORREIOS_CODIGO_PAC=03298      # confirme no contrato
CORREIOS_CODIGO_SEDEX=03220

LOJA_NOME=Youngs Zone Envio
LOJA_CPF=00000000000000
LOJA_TELEFONE=11999999999
LOJA_EMAIL=pedidos@youngszone.com.br
LOJA_CEP=00000000
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

## 3. Teste

1. Deixe `CORREIOS_AMBIENTE=hom` e faça redeploy.
2. No admin, pedido **PAID** com CPF do comprador.
3. Clique **Gerar etiqueta (Correios)** — deve baixar PDF e salvar rastreio.
4. Só então mude para `CORREIOS_AMBIENTE=prod`.

## 4. Se der erro

- Auth 401 → usuário/senha/cartão errados ou API não liberada no CWS.
- “serviço não disponível no cartão” → ajuste `CORREIOS_CODIGO_PAC` / `SEDEX`.
- Dados da loja incompletos → preencha todos os `LOJA_*`.
- Sem CPF no pedido → use “Salvar CPF” no admin antes.
