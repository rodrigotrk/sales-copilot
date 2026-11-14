# Análise — playbook-sales-copilot (Victor Baggio / Playbook Lab)

- Repositório: https://github.com/felipenunes07/playbook-sales-copilot (clonado em `pesquisas/repos/playbook-sales-copilot`, commit `2f1df34`, 22 commits)
- Origem: fork em português de `moritzkremb/jev-sales-copilot`
- Vídeo relacionado: `videos/Victor Baggio/… - transcricao.md` (3º caso de uso, a partir de 13:22)
- Data da análise: 2026-11-14

## 1. Ideia central

O **Jev** (TypeSafe, "System One") não escreve texto. Ele só responde perguntas tipadas:

| Tipo | Retorno | Uso no copiloto |
| --- | --- | --- |
| `noul` (sim/não) | probabilidade 0–1 | sinais por fala ("prospect está objetando?") |
| `choice` | opção + confiança + distribuição | etapa da call, tipo de objeção, próxima jogada, melhor frase |
| `score` | nível numa rubrica (3 níveis) | rapport, engajamento, urgência |

**Toda a lógica fica no código.** O Jev só responde às perguntas. Probabilidade, memória de fatos, estado da objeção, proporção de fala e roteiro são calculados em Python (`copilot/engine.py`). O autor resume assim: *"change a coefficient, not a prompt"*.

## 2. Fluxo por fala

```
fala transcrita ─► é "uhum/legal/perfeito" (≤3 palavras, sem "?")? ─► sim: repete a leitura anterior, sem chamar o Jev
                 └► não: monta o state ─► 1 requisição ao Jev com ~40 perguntas ─► engine
state = { call_facts: {duração, talk_ratio, histórico de etapas, known_facts, objeção}, 
          recent_transcript: últimas 12 falas, latest_utterance }
```

Arquivos: `copilot/constants.py` (todas as perguntas, pesos e limiares) → `jev_client.py` (provedores + cache em disco) → `engine.py` (estado e contas) → `server.py` (FastAPI + WebSocket) → `static/index.html` (tela).

### Perguntas feitas a cada fala (`constants.QUESTIONS`)

- **8 sinais da fala do prospect:** buying_signal, commitment, next_step_agreed, prospect_objecting, prospect_accepts, prospect_disengaging, buyer_confused, prospect_asked_price.
- **3 sinais da fala do vendedor:** rep_discovery_question, rep_pitching, next_step_proposed.
- **8 "fatos" lidos sobre a janela de 12 falas:** pain_identified, situation_understood, pain_quantified, need_payoff, budget_discussed, decision_maker_identified, timeline_known, competitor_mentioned. Na prática é uma checklist SPIN/BANT.
- **3 scores:** rapport, engagement, urgency.
- **3 choices:** stage (8 etapas), objection_type (price/timing/authority/need/trust/competitor/none), next_move (15 jogadas do playbook).
- **15 choices especulativas `phrasing::<jogada>`:** para cada jogada possível, o Jev já escolhe qual das 3 frases prontas encaixa melhor. Assim, quando a jogada sai, a frase já veio na mesma requisição, sem um segundo round-trip.

O app manda **todas as perguntas sempre** (fan-out especulativo) e descarta no código as que não valem para quem falou. Por exemplo, "buying_signal" vira 0 se quem falou foi o vendedor. O modo `COPILOT_ECONOMY=1` corta as perguntas inúteis e reduz a janela para 6 falas.

Cada pergunta traz critérios `true`/`false` com exemplos concretos. As jogadas e etapas trazem `what` e `not_for`. **Todo o texto que o modelo lê está em inglês, mesmo com a call em português.** Só os textos mostrados na tela estão em PT.

## 3. Probabilidade de fechamento

```
inst = STAGE_PRIOR[etapa] + Σ peso_i · sinal_i     (scores centralizados: nível do meio = 0)
p_t  = 0,40 · inst + 0,60 · p_{t-1}                (média móvel exponencial, clamp 3%–95%)
```

- **Base por etapa:** opening 25%, discovery 30%, qualification 35%, pitch 35%, objection 30%, pricing 38%, closing 45%, next_steps 42%.
- **Pesos maiores:** commitment +0,15, buying_signal +0,10, next_step_agreed +0,09, engagement ±0,08. Os negativos: prospect_disengaging −0,16, objeção em aberto −0,14, vendedor falando demais −0,10.
- **Fatos com memória:** quando o Jev dá ≥ 0,70, o código trava o fato em 1,0 pelo resto da call. Isso contorna a janela de só 12 falas.
- **Objeção como máquina de estados:** abre com prospect_objecting ≥ 0,6. Fecha com aceitação, sinal de compra ou compromisso ≥ 0,6, mas nunca se o prospect estiver se desengajando. Expira após 8 falas.
- **"Vendedor falando demais" é calculado no código:** proporção de palavras do vendedor entre 65% e 90%, ou um monólogo com mais de 70 palavras.
- **Painel "o que mexeu o número":** mostra as 3 maiores variações de contribuição entre uma fala e a anterior.
- **Pesos ajustáveis na tela sem chamar o Jev de novo:** as respostas brutas ficam guardadas em cada `Step` e `recompute()` refaz a conta.

## 4. Coaching (próxima jogada + frase)

1. O Jev escolhe `next_move`. Se a confiança ficar abaixo de 0,35, a tela mostra "ouvindo…".
2. "Pare de falar" só aparece depois de o vendedor segurar a palavra por 60 s ou mais. Antes disso, entra a segunda jogada mais provável.
3. A frase genérica vem de `phrasing::<jogada>` e aparece na hora.
4. **Personalização opcional, fora do caminho crítico** (`personalize.py`): um LLM pequeno reescreve a frase para esta conversa. Pode ser Claude Haiku 4.5 via `ANTHROPIC_API_KEY` ou `qwen/qwen3.7-flash` via OpenRouter, com timeout de 3 s e no máximo 32 palavras. Ele só é chamado quando a jogada muda, quando surge um fato novo ou a cada 8 falas.
5. **O Jev serve de guardrail da frase gerada:** `invents_fact` ≥ 0,5 ou `on_move` < 0,5 descartam a frase, e a tela fica com a genérica.

Na tela também aparecem:
- **Roteiro SPIN** (`data/roteiro.json`): Situação → Problema → Implicação → Necessidade → Proposta → Fechamento. Os itens são marcados pelos fatos e as lacunas puladas são listadas.
- **Checklist de qualificação:** Dor, Decisor, Prazo, Orçamento e Próximo passo, cada um com a pergunta pronta.

## 5. Infra

- **Provedores do Jev, nesta ordem:** OpenRouter (`/api/alpha/decisions`, modelo `typesafe/jev-1.13`), BeatAPI, jevmodel.org (máx. 8 perguntas por requisição, o client divide em lotes paralelos) e o SDK oficial `typesafe_sdk` (modelo fixado em `jev-1.13.0`).
- **Cache em disco por hash da requisição:** repetir uma call não custa nada.
- **Custo declarado:** cerca de US$ 0,042 por milhão de tokens de entrada (os de saída são grátis). Uma call de 30 min sai por menos de US$ 0,05 de Jev. No vídeo, uma call custou US$ 0,0083.
- **Transcrição:** o reconhecimento de fala grátis do Chrome (sem identificar quem fala) ou `openai/gpt-audio-mini` via OpenRouter (cerca de US$ 0,05/h). Na aba Chamada de vídeo, quem fala é identificado pelo canal: microfone = vendedor, áudio remoto = prospect.
- **Chamada de vídeo:** WebRTC próprio (`static/sala.html`), com sinalização por polling HTTP. Na Vercel, a fila fica no Upstash Redis. Localmente, o link público sai de um túnel `cloudflared`. Salas assinadas com HMAC.
- **Deploy:** Vercel (FastAPI) com login por senha ou `OPEN_ACCESS=1`. Quando a Vercel derruba o WebSocket (cerca de 5 min), o navegador reenvia as respostas brutas e `restore()` reconstrói a call sem chamar o Jev.
- **Avaliação:** `eval/integration_report.json` mostra 56/56 checagens passando com o Jev real. **Todos os casos estão em inglês** (calls fictícias "Summit HVAC", "Bluewater Plumbing").

## 6. Pontos fortes (vale copiar)

1. **Separação "modelo classifica, código decide".** Dá para auditar, testar e ajustar pesos sem mexer em prompt, e o custo é previsível.
2. **Fan-out especulativo numa única requisição.** Até a escolha da frase vem junto, e a latência fica abaixo de 1 s.
3. **Fatos com memória e objeção como máquina de estados.** Resolvem a janela curta e evitam o número "pular" a cada fala.
4. **Ignorar "uhum/legal".** Economiza chamadas e estabiliza o gráfico.
5. **LLM gerador fora do caminho crítico, verificado por um classificador.** É um padrão anti-alucinação barato.
6. **Respostas brutas guardadas.** Permitem recalcular, reconectar e, sobretudo, **calibrar os pesos depois com dados reais** (veja abaixo).

## 7. Limitações e riscos

1. **Os pesos são chutes calibrados à mão,** e o próprio README diz que "não é uma previsão calibrada". A soma é linear, com base fixa por etapa e EMA de 0,4.
2. **Não há avaliação em PT-BR.** As perguntas e critérios estão em inglês e os testes de integração usam calls em inglês, então a qualidade em calls brasileiras não está medida.
3. **O playbook é específico da Playbook Lab:** Sales OS, squad de automação, LinkedIn. Frases e pesos precisam ser reescritos para outro negócio.
4. **O Jev só vê as últimas 12 falas** mais a lista de fatos. Contexto longo (o que foi dito aos 5 min de uma call de 1 h) se perde, a menos que tenha virado fato.
5. **No modo microfone não há diarização.** Fica um "aperte S a cada troca", inviável numa call real. Só a sala própria resolve, e o Meet e o Zoom dependem do modo "Usar com Meet/Zoom", que capta áudio do sistema ou da aba.
6. **O endpoint do OpenRouter é `alpha`** e há dependência de provedores pequenos (BeatAPI, jevmodel.org). Risco de quebra e de enviar transcrições de clientes a terceiros (LGPD).
7. **Nada acontece depois da call:** não há resumo, CRM, follow-up nem histórico entre calls do mesmo lead.
8. **Não consegui rodar os testes nesta sessão:** a execução de código de terceiros foi bloqueada pelo modo de permissão. A análise é só de leitura do código.

## 8. Ideias para o nosso sales-copilot

- **Reaproveitar a arquitetura**, com perguntas e critérios **reescritos e testados em PT-BR** e com o nosso ICP, as nossas objeções e o nosso playbook.
- **Calibrar os pesos com dados reais.** Gravar as respostas brutas do Jev por fala e o resultado final da oportunidade (ganhou ou perdeu, do CRM). Depois, ajustar uma regressão logística sobre os sinais. Isso troca os pesos à mão por pesos aprendidos, usando o próprio `recompute()`.
- **Criar uma camada pós-call**, que o original não tem. O Jev classifica em lote (objeções, fatos BANT, nota da call) e um LLM gera o resumo, o follow-up e a atualização do CRM. É o mesmo padrão dos casos 1 e 2 do vídeo: o Jev faz o volume barato e o Claude escreve.
- **Ampliar o contexto do Jev.** Manter um "estado da conta" acumulado (fatos e objeções já tratadas), além da janela de 12 falas.
- **Montar um conjunto de avaliação próprio:** 20 a 30 trechos de calls reais em PT, rotulados, no formato de `tests/test_integration_jev.py`, antes de confiar nos números.
- **Resolver a diarização.** Usar captura por canal (bot no Meet/Zoom ou sistema + microfone separados) ou um STT com diarização, como o ElevenLabs Scribe que já usamos.
