# Fluxo do harness — Fase 3: decidir a stack e planejar a parte 1

A Fase 3 transforma o PRD em plano de construção com a skill `tlc-plan`. Ela decide a stack e se o produto é um sistema agêntico, olhando as cinco partes do PRD, e escreve só o plano da parte 1, o playbook configurável. O plano sai aprovado por escrito pelo Rodrigo.

O corte segue o PRD, decidido pelo Rodrigo em 15/11/2026: ele revisa cada parte contra o critério dela antes da seguinte, e cada parte ganha o seu plano depois dessa revisão. A stack é decidida uma vez para as cinco, porque é a decisão mais difícil de reverter e precisa ver todas as partes juntas.

## Como a `tlc-plan` trabalha

- Lê todas as fontes antes de perguntar e pergunta toda área cinzenta até não sobrar decisão de produto para quem constrói.
- Escreve critérios no formato EARS, cada um com um valor concreto que uma execução confirma.
- Trata as capturas de tela como fonte obrigatória. Todo desvio vira uma decisão `AD-NNN` em `.specs/STATE.md` e aparece em `## Differences from the source` do plano.
- Decide só o que é difícil de reverter. Nomes, posição e tipos ficam para quem constrói.
- O plano só é apresentado depois que `validate_plan.py` passa, e a aprovação é a única parada.

## Entradas

| Tipo | Arquivos | Como entra |
|---|---|---|
| Produto | `.prd/sales-copilot.md` | Decisão tomada. Não se reabre; o que estiver ambíguo vira pergunta |
| Telas e regras | `especificacao/design-sales-copilot.md` | Decisão tomada, com os desvios do protótipo já confirmados |
| Capturas | `especificacao/prototipos/tela-6-metodo.png` e o HTML navegável | Fonte obrigatória da tela da parte 1. As outras telas entram só para a escolha da stack |
| Decisões do levantamento | `especificacao/decisoes-levantamento.md` | Contexto: hipóteses de teste e perguntas de tela com destino |
| Referências | `pesquisas/analise-playbook-sales-copilot.md` e `pesquisas/analise-deal-advisor-kelvin-cleto.md` | Ideia e desenho. O produto não copia código delas |
| Hipóteses técnicas | `especificacao/notas-tecnicas.md` e os diagramas de arquitetura, fluxo ao vivo e roadmap | Hipótese. O que sobreviver vai para `.specs/STATE.md` ou para o plano |

## O que a fase decide

### A stack, para as cinco partes

A stack precisa cobrir estas exigências do PRD, de todas as partes:

- transcrever uma call do Google Meet ao vivo, separando closer e lead, sem participante extra na reunião e com o painel invisível para o lead quando o closer compartilha a tela;
- entregar a dica enquanto o assunto ainda está em pauta;
- montar propostas só com a tabela de preços cadastrada, com a conta conferida;
- conversar por voz com um lead simulado no treino;
- gerar ata, próximo passo e rascunho de playbook a partir de transcrições e gravações;
- importar um playbook de um documento;
- separar os dados de cada negócio.

A fase também decide se o produto deve ser um sistema agêntico, hipótese que o levantamento encaminhou para cá. Cada escolha entra em `.specs/STATE.md` como `AD-NNN`, com o motivo. O teto de gasto mensal está indefinido; se uma escolha depender dele, a pergunta vai para o Rodrigo.

### O plano da parte 1

O plano fica em `.specs/features/playbook-configuravel/plan.md` e cobre as linhas da tabela Journey do PRD que são do gestor configurando o playbook sem gravações: escrever, importar e editar etapas, o que descobrir com a pergunta sugerida, objeções com a resposta aprovada e tabela de preços com desconto máximo. O critério de sucesso da parte é o do PRD: a empresa configura o playbook inteiro sem escrever uma linha de código.

O teste com call gravada, o ajuste com resultados de venda e o rascunho a partir de gravações dependem de partes posteriores e ficam em `## Out of scope` do plano, com a parte a que pertencem. Se a `tlc-plan` encontrar motivo para trazer algum deles para a parte 1, pergunta ao Rodrigo.

## Passos

### 1. Abrir a sessão

Abrir uma sessão do Claude Code em `/Users/rodrigo/Estudos/vendas/sales-copilot` e invocar:

```
/tlc-plan Planeje a parte 1 do Sales Copilot, o playbook configurável, em .specs/features/playbook-configuravel/plan.md. Antes, decida a stack para as cinco partes do PRD e se o produto é um sistema agêntico, e registre cada escolha em .specs/STATE.md. Trate .prd/sales-copilot.md e especificacao/design-sales-copilot.md como decisão já tomada: não reabra; se algo estiver ambíguo ou faltar, pergunte. A captura obrigatória da parte 1 é especificacao/prototipos/tela-6-metodo.png; os desvios já confirmados no documento de telas viram AD-NNN e entram em Differences from the source. especificacao/notas-tecnicas.md e os diagramas de arquitetura, fluxo ao vivo e roadmap são hipóteses. O teste com call gravada, o ajuste com resultados de venda e o rascunho a partir de gravações ficam fora da parte 1. As gravações da EPC são só material, nunca requisito, e nenhum arquivo cita nome ou dado pessoal de lead. Me faça no máximo 2 perguntas por vez.
```

### 2. Responder as perguntas

- Aceite a recomendação com "sim" ou responda com valor concreto. Resposta vaga volta como pergunta.
- Se a pergunta reabre uma decisão do PRD ou do documento de telas, diga se a decisão muda. Se mudar, o agente da sessão atualiza o arquivo na mesma sessão, para os dois não divergirem.
- Aprovar o plano é escrever a aprovação com as suas palavras; o agente a registra sob o título do plano.

### 3. Os arquivos

- `.specs/STATE.md` e `.specs/features/playbook-configuravel/plan.md` seguem o formato da `tlc-plan` e não passam pelo `deslop`, porque os scripts de validação leem linhas literais de `.specs/`.
- `.specs/features/playbook-configuravel/captures/` recebe a captura da tela 6.
- O diagrama de jornada (`especificacao/diagramas/03-jornada.mmd`, `.png` e `.svg`) é redesenhado a partir da tabela Journey do PRD.
- Depois que o plano for aprovado, `especificacao/notas-tecnicas.md` e os diagramas de arquitetura, fluxo ao vivo e roadmap (`01-arquitetura`, `02-fluxo-ao-vivo` e `04-roadmap`, nos três formatos) saem do repositório. O git guarda o histórico.
- O `AGENTS.md` deixa de citar as notas técnicas e passa a apontar para `.specs/`.

### 4. Versionar

Commit dos arquivos e push para `rodrigotrk/sales-copilot`, com confirmação do Rodrigo.

## Pronto quando

- [ ] A stack e a decisão sobre sistema agêntico estão em `.specs/STATE.md`, cada uma com o motivo, e cobrem as cinco partes.
- [ ] O plano da parte 1 passa no `validate_plan.py` e tem a aprovação do Rodrigo registrada sob o título.
- [ ] Cada critério do plano tem um valor concreto, e toda linha da Journey do PRD que é da parte 1 aparece num critério.
- [ ] Cada desvio da tela 6 está como `AD-NNN` e em `## Differences from the source`.
- [ ] O diagrama de jornada reflete a tabela Journey do PRD.
- [ ] As notas técnicas e os três diagramas técnicos saíram do repositório, e o `AGENTS.md` aponta para `.specs/`.
- [ ] Nenhum arquivo tem nome ou dado pessoal de lead.
- [ ] O `AGENTS.md` passou pelo `deslop`, e tudo está no GitHub.

## Fora desta fase

- Construir a parte 1 (Fase 4, com a `tlc-implement`).
- Planejar as partes 2 a 5.
- Testar o protótipo e validar a ajuda ao vivo em call real.
- Refazer o protótipo.
