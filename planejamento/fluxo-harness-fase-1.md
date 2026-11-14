# Fluxo do harness — Fase 1: fechar as perguntas em aberto

A Fase 1 fecha as 15 perguntas do §11 do PRD (`especificacao/prd-sales-copilot.md`) com a skill `grilling`. Ela termina quando cada pergunta tem uma resposta registrada na seção do PRD a que pertence, ou um destino declarado, como teste com o protótipo ou call real. O PRD sai desta fase pronto para a `tlc-prd` reescrevê-lo na Fase 2.

## Como o `grilling` trabalha

- Monta uma árvore de decisões e pergunta em rodadas. Cada rodada traz todas as perguntas cujos pré-requisitos já estão respondidos, numeradas e com a resposta recomendada.
- Fatos ele busca sozinho, com subagentes. Decisões são suas.
- Termina quando nenhuma pergunta respondível sobra e você confirma o entendimento comum.

## Entradas

- O §11 do PRD, com 15 perguntas em três grupos: produto, piloto e telas.
- O §12 do PRD, os próximos passos do levantamento.
- As atas e os `CRM.txt` das 12 sessões, em `pesquisas/videos/Diagnosticos/`. Ficam só na máquina, fora do git.
- O protótipo, em `especificacao/prototipos/`.

## A árvore prevista

O `grilling` recalcula a árvore a cada rodada.

| Rodada | Perguntas | Tipo | Observação |
|---|---|---|---|
| 1 | Uso próprio dos closers da mentoria ou produto para vender a outros negócios | Decisão | Raiz: muda o segundo negócio, o CRM e quem usa o treino |
| 1 | Tabela de 3, 6 e 12 meses; desconto máximo e Pix de compromisso; diferença entre EPC e MEPC; campos obrigatórios da ficha | Fato | Subagentes leem as atas. Você só confirma |
| 1 | A Luana usa o produto ou só recebe as tarefas | Decisão | Independente da raiz |
| 2 | Segundo negócio para validar; CRM próprio ou existente; treino só do Rodrigo ou de outros closers | Decisão | Dependem da raiz |
| 3 | O que tirar ou acrescentar no painel da call; um ou dois monitores; painel lateral ou barra ao compartilhar a tela; quantas sugestões ao mesmo tempo | Teste | Conversa não responde. Vão para o teste do protótipo com Rodrigo e Luana e para a validação com uma call real (§12) |

As atas já trazem a tabela, com uma inconsistência a confirmar: no plano de 6 meses, "7 mil ou 6x 1.100" dá R$ 6.600 parcelado, menos que o valor à vista. Desconto aparece em 4 atas e Pix em 10.

## Passos

### 1. Abrir a sessão

Abrir uma sessão do Claude Code em `/Users/rodrigo/Estudos/vendas/sales-copilot` e invocar:

```
/grilling Feche as perguntas do §11 de especificacao/prd-sales-copilot.md. Fatos sobre o piloto (preços, descontos, Pix, EPC × MEPC, campos da ficha) vêm das atas e CRM.txt em pesquisas/videos/Diagnosticos/: busque com subagentes e me traga para confirmar. Perguntas que só um teste com o protótipo ou uma call real respondem não viram decisão: registre o destino. Ao fim de cada rodada, grave as respostas no §11 do PRD, marcando [x] e escrevendo a resposta ao lado.
```

Gravar a cada rodada protege as respostas de uma compactação da conversa. A Fase 1 pode levar mais de uma sessão, porque os testes com o protótipo dependem do Rodrigo e da Luana.

### 2. Responder as rodadas

- Aceite a recomendação com "sim" ou responda com valor concreto: um número, um nome, uma data. Resposta vaga volta como pergunta.
- Se um fato das atas estiver errado ou desatualizado, corrija. Vale a sua resposta.
- Os dados pessoais dos leads ficam nas atas. O PRD vai para o git e recebe só regras e números agregados, sem nome de lead.

### 3. Registrar no PRD

Quando você confirmar o entendimento comum, cada resposta sai do §11 e entra na seção a que pertence:

| Resposta | Seção do PRD |
|---|---|
| Uso próprio ou produto; segundo negócio | §1 Em uma frase e §5 Quem usa |
| Tabela, desconto, Pix, EPC × MEPC | §8 Regras de negócio |
| CRM; campos obrigatórios da ficha | §7.1 e §7.3 |
| Luana; treino | §5 Quem usa e §7.5 |

O §11 fica só com as perguntas encaminhadas para teste, cada uma com o destino. O hook de `Stop` passa o PRD pelo `deslop` ao fim da sessão.

### 4. Versionar

Commit da versão nova do PRD e push para `rodrigotrk/sales-copilot`, com sua confirmação.

## Pronto quando

- [ ] As perguntas de produto e de piloto do §11 têm resposta na seção a que pertencem.
- [ ] A inconsistência do plano de 6 meses está resolvida.
- [ ] As perguntas de tela estão no §11 com destino: teste do protótipo ou call real.
- [ ] O PRD não tem nome nem dado pessoal de lead.
- [ ] O PRD passou pelo `deslop` e está no GitHub.

## Fora desta fase

- Os testes em si: Rodrigo e Luana navegarem pelo protótipo e validar a jornada com uma call real (§12). As respostas deles fecham as perguntas de tela antes ou durante a Fase 2.
- Escrever as falas para as 4 objeções principais (§12). É conteúdo do playbook, não decisão de produto.
- Reescrever o PRD no formato da `tlc-prd` e separar telas e regras em `especificacao/design-sales-copilot.md` (Fase 2).
