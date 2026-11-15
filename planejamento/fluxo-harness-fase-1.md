# Fluxo do harness — Fase 1: rever o projeto desde a ideia

A Fase 1 reabre o Sales Copilot desde a ideia com a skill `grilling`. O PRD atual, as notas técnicas e o protótipo entram como hipóteses, não como decisões. As pesquisas e as 12 calls do piloto são a evidência. A fase termina com as decisões registradas em `especificacao/decisoes-levantamento.md`, incluindo o veredito sobre construir, em que forma e para quem. Na Fase 2, a `tlc-prd` recebe esse arquivo como decisão já tomada e escreve o PRD.

A fase terminou em 15/11/2026. O veredito, de 14/11/2026, é construir um produto próprio de copiloto e treino para o closer de venda consultiva de qualquer nicho, com playbook configurável sem programar. A EPC fica fora do produto: as 12 gravações servem só como material de treino e de ajuste do playbook. O plano da fase seguinte está em `planejamento/fluxo-harness-fase-2.md`.

## Como o `grilling` trabalha

- Monta uma árvore de decisões e pergunta em rodadas. Cada rodada traz todas as perguntas cujos pré-requisitos já estão respondidos, numeradas e com a resposta recomendada.
- Fatos ele busca sozinho, com subagentes. Decisões são suas.
- Termina quando nenhuma pergunta respondível sobra e você confirma o entendimento comum.

## Entradas

| Tipo | Arquivos |
|---|---|
| Evidência do piloto | Atas e `CRM.txt` das 12 sessões de 05 a 13/11/2026, em `pesquisas/videos/Diagnosticos/`. Ficam só na máquina, fora do git |
| Evidência das referências | `pesquisas/analise-playbook-sales-copilot.md` e `pesquisas/analise-deal-advisor-kelvin-cleto.md` |
| Hipóteses | `especificacao/prd-sales-copilot.md`, `especificacao/notas-tecnicas.md`, `especificacao/prototipos/` e `especificacao/diagramas/` |

## A árvore prevista

O `grilling` recalcula a árvore a cada rodada. As camadas vêm nesta ordem porque cada uma depende da anterior.

| Camada | Perguntas | O que a evidência já diz |
|---|---|---|
| 1. Situação | O produto é para os closers da mentoria, um produto para vender a outros negócios ou as duas coisas em sequência? O que já está comprometido: prazo, dinheiro, pessoas? Quanto custa errar? | Nada comprometido por escrito. O PRD assume as duas coisas: piloto na EPC e um segundo negócio depois |
| 2. Problema | Qual dor vem primeiro: vender mais na call, não perder o lead depois, acabar com o trabalho manual ou preço errado? Quanto cada uma custa, em R$ ou horas por semana? | Em 12 calls: 1 fechou com pagamento (R$ 6.000), 1 fechou R$ 10.000 com R$ 100 pagos e 1 desistiu depois de pagar R$ 2.000 de entrada. Dinheiro pesou em 9 a 10 calls, "começar depois" em 5 e decisor ausente em 5. O plano de 6 meses saiu por R$ 12.000, R$ 7.000, 6x R$ 1.100 e 6x R$ 1.000. Em três calls, o prazo que o lead deu para responder venceu sem registro de follow-up, e as fichas têm campos faltando |
| 3. Substitutos | O que resolve isso hoje sem o produto: planilha, CRM, roteiro impresso, a Luana? Por que não basta? | O pós-call é manual: gravar, transcrever, escrever a ata e montar a ficha. Parte já é automatizada pelas skills de ata desta máquina |
| 4. Caminhos mais baratos | Um roteiro com tabela de preços e um checklist de follow-up resolveria a maior parte? Dá para comprar ou adaptar o que existe? | O Deal Advisor é fechado. O playbook-sales-copilot é aberto, mas não tem pós-call e o playbook está no código |
| 5. Veredito | Construir agora, menor ou diferente, depois de quê, ou não construir? Qual parte vem primeiro: ao vivo, pós-call ou treino? | O PRD propõe as três partes. As notas técnicas estimam menos de US$ 0,30 por call |
| 6. Sucesso | O que torna isto um acerto, em quanto tempo e quem revisa? | O PRD mira 30% de fechamento em 60 dias, ficha pronta em 10 minutos e zero erro de proposta |
| 7. Escopo e jornada | Quem usa (Rodrigo, Luana, outros closers)? Quais etapas da jornada entram no primeiro corte? O que fica de fora? | As 15 perguntas do §11 do PRD entram aqui. As de tela vão para teste com o protótipo ou call real, não viram decisão |

Se o veredito da camada 5 for "não agora" ou "não construir", as camadas 6 e 7 não rodam. O arquivo de decisões registra então o que precisaria mudar para reabrir.

## Passos

### 1. Abrir a sessão

Abrir uma sessão do Claude Code em `/Users/rodrigo/Estudos/vendas/sales-copilot` e invocar:

```
/grilling Quero rever o Sales Copilot desde a ideia. Trate especificacao/prd-sales-copilot.md, notas-tecnicas.md e os protótipos como hipóteses, não como decisões. A evidência são as 12 atas e CRM.txt em pesquisas/videos/Diagnosticos/ e as análises em pesquisas/: busque os fatos com subagentes e me traga para confirmar. Siga a ordem: situação, problema, substitutos, caminhos mais baratos, veredito, sucesso, escopo e jornada. Não proponha tecnologia antes do veredito. Perguntas que só um teste com o protótipo ou uma call real responde não viram decisão: registre o destino. Ao fim de cada rodada, grave as decisões em especificacao/decisoes-levantamento.md.
```

Gravar a cada rodada protege as decisões de uma compactação da conversa. A fase pode levar mais de uma sessão.

### 2. Responder as rodadas

- Aceite a recomendação com "sim" ou responda com valor concreto: um número, um nome, uma data. Resposta vaga volta como pergunta.
- Se um fato das atas ou das pesquisas estiver errado, corrija. Vale a sua resposta.
- Um número que ninguém mede, como horas gastas no pós-call, é registrado como "não medido", com quem pode medir. Não vira estimativa.

### 3. O arquivo de decisões

`especificacao/decisoes-levantamento.md` tem uma seção por camada da árvore. Cada decisão traz o valor concreto e quem decidiu, e o veredito traz a data. As perguntas encaminhadas para teste ficam numa seção própria, com o destino.

O arquivo vai para o git e recebe só regras e números agregados, sem nome nem dado pessoal de lead. O PRD atual cita leads pelo primeiro nome (§3 e outras seções). Isso sai na reescrita da Fase 2.

O hook de `Stop` passa o arquivo pelo `deslop` ao fim de cada sessão.

### 4. Versionar

Commit do arquivo de decisões e push para `rodrigotrk/sales-copilot`, com sua confirmação.

## Pronto quando

- [x] As camadas 1 a 5 estão decididas, com veredito datado.
- [x] Se o veredito é construir: as camadas 6 e 7 estão decididas e as perguntas de tela têm destino.
- [x] Se o veredito é "não agora" ou "não construir": o arquivo diz o que precisaria mudar para reabrir. Não se aplica, porque o veredito é construir.
- [x] O arquivo não tem nome nem dado pessoal de lead.
- [x] O arquivo passou pelo `deslop` e está no GitHub.

## Fora desta fase

- Os testes com o protótipo e a validação com uma call real (§12 do PRD).
- Escrever as falas para as objeções. É conteúdo do playbook, não decisão de produto.
- Escrever o PRD no formato da `tlc-prd` e o documento de telas e regras, `especificacao/design-sales-copilot.md` (Fase 2).
- Rever as notas técnicas. Só depois do veredito e do PRD, na escolha da stack (Fase 3).
