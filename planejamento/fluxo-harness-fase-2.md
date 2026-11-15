# Fluxo do harness — Fase 2: escrever o PRD de produto

A Fase 2 transforma as decisões da Fase 1 no PRD, com a skill `tlc-prd`. O PRD fica em `.prd/sales-copilot.md`, só de produto e sem tecnologia. As telas e regras de negócio ficam em `especificacao/design-sales-copilot.md`. O PRD atual, `especificacao/prd-sales-copilot.md`, é substituído pelos dois. Na Fase 3, a `tlc-plan` recebe esses arquivos.

## Como a `tlc-prd` trabalha

- Entrevista na ordem situação, problema, veredito e arquivo, e para antes de qualquer desenho técnico.
- Quando a decisão de construir já foi tomada, ela registra quem decidiu e quando, e gasta a sessão no problema e na jornada, sem abrir um segundo veredito. Aqui, o veredito de construir é do Rodrigo, em 14/11/2026.
- Cada pergunta traz a resposta recomendada. O que só um teste responde vai para "Open questions", sem virar decisão.
- A saída é só o arquivo; a skill não publica nada.

## Entradas

| Tipo | Arquivos | Como entra |
|---|---|---|
| Decisões | `especificacao/decisoes-levantamento.md` | Decisão já tomada. Não se reabre; o que estiver ambíguo ou faltar vira pergunta |
| Telas | PNGs e HTML de `especificacao/prototipos/` | Fonte obrigatória das telas, pela regra do `AGENTS.md`. Todo desvio é perguntado e registrado |
| Detalhe | `especificacao/prd-sales-copilot.md` e `especificacao/diagramas/03-jornada.*` | Só fonte de detalhe. Onde contradiz o arquivo de decisões, vale o arquivo de decisões |
| Fora desta fase | `especificacao/notas-tecnicas.md` e os diagramas de arquitetura, fluxo ao vivo e roadmap | Tecnologia; a Fase 3 os revê |

## Desvios esperados entre protótipo e decisões

O protótipo foi desenhado para o PRD atual, que tinha a EPC como piloto, o SDR e o pós-call completo. Estes pontos mudam com as decisões, e cada um precisa ser confirmado com o Rodrigo e registrado no documento de telas:

| Tela | Decisão que afeta |
|---|---|
| `tela-1-antes` | Antes da call, o closer informa ou importa só os dados básicos do lead. Agenda e SDR ficam fora |
| `tela-3-depois` | O fim da call tem ata, próximo passo, resultado da venda e marcação das dicas como úteis ou inúteis. A ficha de CRM fica fora |
| `tela-4-retornos` | Lembretes de follow-up ficam com as ferramentas de cada negócio |
| `tela-6-metodo` | O playbook é escrito, importado ou rascunhado a partir de gravações, sem programar |
| `tela-2-durante` e `tela-2a-na-call` | A ajuda ao vivo ganha o pedido de ajuda, o alerta de fala demais e o termômetro da venda, se o protótipo ainda não os tiver |

A lista vem das decisões e dos nomes das telas. O conteúdo de cada tela é conferido na sessão.

## Passos

### 1. Abrir a sessão

Abrir uma sessão do Claude Code em `/Users/rodrigo/Estudos/vendas/sales-copilot` e invocar:

```
/tlc-prd Escreva o PRD do Sales Copilot em .prd/sales-copilot.md. Trate especificacao/decisoes-levantamento.md como decisão já tomada: não reabra nenhuma linha dele; se algo estiver ambíguo ou faltar, pergunte. O veredito de construir já foi dado pelo Rodrigo em 14/11/2026. Use especificacao/prd-sales-copilot.md só como fonte de detalhe, nunca de decisão: onde ele contradiz o arquivo de decisões (EPC como piloto, SDR, pipeline, ficha de CRM, follow-up), vale o arquivo de decisões. Os protótipos em especificacao/prototipos/ são fonte obrigatória das telas: cada tela que as decisões tiraram do escopo ou mudaram é um desvio, que você me pergunta e registra; a lista dos desvios esperados está em planejamento/fluxo-harness-fase-2.md. Escreva também especificacao/design-sales-copilot.md com as telas e regras de negócio que ficam. O PRD não cita nome nem dado pessoal de lead e não fala de tecnologia; a hipótese do sistema agêntico fica para a Fase 3. As perguntas de tela e as hipóteses encaminhadas para teste continuam abertas, sem virar requisito. Me faça no máximo 2 perguntas por vez.
```

### 2. Responder as perguntas

- Aceite a recomendação com "sim" ou responda com valor concreto. Resposta vaga volta como pergunta.
- Se a pergunta reabre uma decisão da Fase 1, diga se a decisão muda. Se mudar, o agente da sessão atualiza o arquivo de decisões na mesma sessão, para os dois não divergirem.

### 3. Os arquivos

- `.prd/sales-copilot.md` segue o formato da `tlc-prd`: títulos de seção em inglês, texto em PT-BR. Passa pelo `deslop`, porque só `.specs/` fica de fora, e a revisão não pode mudar os títulos de seção em inglês.
- `especificacao/design-sales-copilot.md` traz as telas e as regras de negócio, com os desvios do protótipo registrados. Passa pelo `deslop`.
- `especificacao/prd-sales-copilot.md` sai do repositório depois que os dois arquivos estiverem prontos. O git guarda o histórico. O `AGENTS.md` deixa de citá-lo.

### 4. Versionar

Commit dos arquivos e push para `rodrigotrk/sales-copilot`, com confirmação do Rodrigo.

## Pronto quando

- [ ] `.prd/sales-copilot.md` cobre situação, problema, veredito, sucesso, escopo e jornada, sem tecnologia.
- [ ] Toda decisão do arquivo de decisões aparece no PRD ou no documento de telas, sem contradição.
- [ ] Cada desvio entre protótipo e decisões está confirmado e registrado.
- [ ] As perguntas de tela e as hipóteses de teste estão em "Open questions", com o destino.
- [ ] Nenhum dos arquivos tem nome ou dado pessoal de lead.
- [ ] O PRD atual saiu do repositório, e o `AGENTS.md` aponta para os arquivos novos.
- [ ] O PRD e o documento de telas passaram pelo `deslop`, e tudo está no GitHub.

## Fora desta fase

- Escolher a stack e decidir se o produto é um sistema agêntico (Fase 3, com a `tlc-plan`).
- Testar o protótipo e validar a ajuda ao vivo em call real.
- Refazer o protótipo. Esta fase só registra os desvios.
