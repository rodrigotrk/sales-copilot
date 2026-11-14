# Análise — Deal Advisor (Kelvin Cleto / Acelera 360)

- Produto: **Deal Advisor**, módulo do "Growth AI OS" da Acelera 360. **Código fechado, sem repositório.**
- Fonte: vídeo "JEV: É Assim que Vou Fazer Dinheiro de VERDADE com IA" (`videos/Kelvin Cleto/`): a transcrição e **os quadros da tela**, principalmente o documento "Arquitetura do Sales Copilot" que ele mostra entre 06:40 e 08:45.
- Complementa `videos/Kelvin Cleto/JEV - engenharia reversa.md`. Os quadros confirmam e corrigem várias inferências feitas lá (veja a seção 9).
- Data da análise: 2026-11-14

Legenda: **[tela]** = lido num quadro do vídeo · **[dito]** = falado · **[inferido]** = dedução minha

## 1. Ideia central

É uma **cascata de modelos com política no código**. O Jev (o mesmo `jev-1.13` do playbook-sales-copilot) faz a triagem barata e decide. Uma biblioteca de cards prontos responde a maior parte das situações. O modelo caro (GPT-5.4) só escreve quando nenhum card serve, e o Jev ainda revisa o que ele escreveu antes de mostrar ao closer.

O documento dele resume assim **[tela]**: *"A cada fala nova, o app decide se fica quieto, se usa uma pergunta pronta do banco ou se chama o modelo pesado. O JEV, barato, faz quase toda a triagem; o GPT-5.4 entra só quando precisa escrever."*

A diferença para o playbook-sales-copilot é o objetivo. O playbook calcula uma **probabilidade de fechamento** e sugere a próxima jogada. O Deal Advisor não mostra probabilidade: ele é um **sistema operacional do closer**, com dicas ao vivo, CRM preenchido sozinho, apresentação gerada a partir da call e treino com lead simulado.

## 2. Produto (o que o closer vê)

App desktop **Electron** **[tela: ícone "Electron" no dock]**, dentro do "Growth AI OS" com Agenda, Conversas, Pipeline, Contatos, Calls e Coach.

| Módulo | O que faz | Quando |
| --- | --- | --- |
| **Calls (copiloto ao vivo)** | Detecta a reunião no Meet, Zoom ou Teams, transcreve e mostra a etapa, a objeção detectada e os cards de dica | Durante a call |
| **Dados da reunião** | Extrai pessoas, empresas e descobertas "com a origem de cada informação" e um histórico de alterações; exporta JSON | Durante e depois |
| **Assessment** | Apresentação de 38 slides montada a partir da call, com citações reais do lead | Logo após a descoberta |
| **Coach** | Roleplay por voz com um lead gerado a partir das calls reais, seguido de avaliação | Fora da call |

Detalhes da tela **[tela]**:
- Tela inicial: *"Aguardando uma call… Escuta conectada · detecção automática ativa"*, com as permissões Acessibilidade, Microfone e Áudio do sistema, e *"Modo discreto"*.
- *"Uma memória para cada conversa. Transcrição e anotações ficam salvas neste computador."* Cada call tem os botões **Simular** (vira cenário no Coach) e **Só neste computador**.
- Etapas fixas: **Introdução → Diagnóstico → Apresentação → Valor → Decisão**.
- Abas da call: Copiloto, Transcrição (Host/Guest com timestamps, busca, download .txt), Anotações e Dados.
- Call real de exemplo: 1h28, 1.356 trechos, 17.536 palavras.

## 3. Ciclo ao vivo (diagrama "1. O ciclo ao vivo, dentro do app")

```
Captura da call ──falas──► CallStore ──fala nova──► Agenda da sessão
 Recall Desktop SDK          calls.sqlite local        espera 1,2 s a fala assentar
 + AssemblyAI (transcrição)  tabelas: calls,           ≥ 15 s entre dicas · ≤ 4 dicas/min
                             transcript, copilot_v1    objeção ou "Pedir ajuda" furam a fila
                                                              │
                     a cada 60 s, sem dica pendente ◄─────────┤
                                 ▼                            ▼
                     Memória da call                 1º JEV · triagem
                     resumo + dados da reunião       lê o momento: fase, objeção, resposta dada
                     GPT-5.4 · tarefa "memory"       jev-1.13 · 2,5 s
                                                              ▼
                                                    Política decide (código)
                                                    conduction/policy.decide
                          ┌───────────────────────────────┼───────────────────────┐
                    há card que cabe              sem card ou objeção nova        nada a fazer
                          ▼                               ▼                         ▼
              Banco de intervenções               Modelo pesado                   Pular
              72 cards · pré-seleção local        GPT-5.4                         (custo zero)
                          ▼ ≤ 4 opções            (Gemini Flash no diagnóstico)
              2º JEV · escolhe o card                     ▼
              entre até 4 candidatos ──nenhum cabe──► 3º JEV · revisa o plano
                          │                       aplicado, rejeitado ou plano inválido
                          ▼                               ▼ só se aprovado
                       Tela da call ◄─────────────────────┘
                       "Agora", objeção detectada, fases, cards e resumo
                          │ closer pergunta
                          ▼
                       Pedir ajuda (GPT-5.4 · copilot-agent, lê resumo e dados)
```

Pontos que merecem atenção:

- **A captura não é própria.** Usa o **Recall.ai Desktop Recording SDK**, que grava o áudio do sistema e do microfone sem bot na reunião, e a **AssemblyAI** para transcrever. Eu tinha chutado ScreenCaptureKit e Scribe na engenharia reversa.
- **O gate tem números concretos:** espera 1,2 s para a fala "assentar", deixa no mínimo 15 s entre dicas e mostra no máximo 4 dicas por minuto. Objeção ou o botão "Pedir ajuda" passam na frente. Isso explica a média de uma chamada ao Jev a cada 16 s.
- **A decisão fica no código** (`conduction/policy.decide`), não no modelo. É a mesma filosofia do playbook: o Jev classifica e o código decide.
- **Os cards passam por uma pré-seleção local** (por etapa e gatilho, sem IA) que reduz os 72 a no máximo 4, e o Jev escolhe entre eles. É um `choice` do Jev com poucas opções.
- **A memória longa é um resumo** gerado pelo GPT-5.4 a cada 60 s, quando não há dica pendente. É a solução dele para o contexto de calls de 60 a 90 minutos.
- **O 3º Jev barra a dica antes da tela** e pode marcá-la como "aplicado", "rejeitado" ou "plano inválido". O texto do slide admite o problema: *"quando ele rejeita, a chamada já foi paga."*

## 4. Servidor ("2. O que passa pelo servidor do Growth")

**[tela]** *"O desktop nunca guarda chave de IA: tudo passa pela API do Growth, que registra o consumo de cada chamada."* O relay manda tudo ao **OpenRouter**.

| Rota | Função |
| --- | --- |
| `/precall/context` | Contexto antes da call: 1 chamada "fast", com cache |
| `/calls/:id/revisions` | Envia a call ao CRM como atividade e coloca numa **fila de projeção**: 1 chamada "smart" por fato novo |
| `/coach/session` | Abre a sessão de voz. **Fora do relay:** conecta direto na OpenAI, `gpt-live-1` |

O CRM próprio ("CRM do Growth") guarda reunião, contato e oportunidade. A atualização dos campos é incremental: no máximo uma chamada de IA por fato novo.

## 5. Custo por reunião ("3. Onde o dinheiro vai")

Estimativa para uma call de 73 min, extrapolada de um replay de 21/09 e do assessment medido em 01/10 **[tela]**:

| Item | Chamadas | Custo | Custo/chamada |
| --- | --- | --- | --- |
| Jev (triagem e revisões) | ~268 | US$ 0,12 | ~US$ 0,0004 |
| Modelo pesado (dicas, plano e memória) | ~61 | **US$ 1,96** | ~US$ 0,032 |
| Assessment | 7 | US$ 0,44 (medido) | ~US$ 0,063 |
| **Total** | | **≈ US$ 2,52 → "R$ 15"** (câmbio de R$ 6 com impostos) | |

- **78% do custo está no modelo pesado.** O Jev, mesmo com 4,4 vezes mais chamadas, representa menos de 5%.
- **Dado mais importante do slide** **[tela]**: *"No replay medido, só 7 de 45 chamadas ao modelo pesado viraram dica publicada; as outras foram rejeitadas pelo 3º JEV ou vieram com plano inválido."* Ou seja, **cerca de 84% do gasto com dicas vira lixo.** O slide diz que o app atual usa metade dos tokens por chamada e que *"falta medir uma call inteira no pipeline novo"*. O próprio número de custo ainda é estimativa.
- Antes do Jev, ele gastava R$ 20 a 30 por reunião **[dito]**. A redução veio de trocar o classificador próprio pelo Jev e de fazer o modelo pesado entrar menos.

## 6. Assessment e Coach

**Assessment [tela]:** gerado por blocos, com 23 blocos e 38 slides. Cada bloco aparece como "Pronto" e tem botão **Refazer**, além de "Refazer tudo" e "Abrir janela". Os blocos vistos são Capa, A oportunidade, Custo de não agir, O que você me contou, O gargalo real, O que você já tentou e Riscos de tentar sozinho. Os blocos institucionais (história do Kelvin, números da Acelera e cases) são fixos. Os slides citam frases literais do lead, por exemplo *"Eu já comprei outras mentorias, mas não tenho tempo para estudar." — Luiz*, num gráfico "Onde você está em 12 meses?" marcado como *"Projeção ilustrativa, sem valores"*.

**Coach [tela]:**
- Três modos: "Uma call minha" (refazer uma call real), "Call aleatória" (*"A IA cria um lead novo misturando o que aparece nas suas calls: perfis, jeito de falar, dores e objeções"*) e "Gerar um lead".
- Dá para treinar a call inteira ou uma etapa só (Introdução, Diagnóstico, Apresentação, Valor, Decisão), em três dificuldades: Receptivo, Realista e Exigente. A sessão dura até 10 min.
- Ao final há uma **avaliação**: nota (ou "Sem nota" quando há pouca evidência), "O que funcionou" e "O que melhorar", com citações, e um **próximo exercício** (por exemplo: *"Treine um bloco de 5 minutos só de descoberta…"*). O que ainda falta, segundo ele, é a gamificação e o ranking dos closers **[dito]**.
- O lead simulado encerra a call se o closer for agressivo. A persona tem critério de abandono.

## 7. Pontos fortes (vale copiar)

1. **Biblioteca de cards antes de gerar.** Situações recorrentes, como objeções clássicas e perguntas de diagnóstico, saem por quase nada e com texto revisado por humano.
2. **Gate com limites explícitos** (1,2 s, 15 s, 4 por minuto, fura-fila para objeção). Isso controla o custo e evita encher a tela do closer de dicas.
3. **Revisão das dicas geradas pelo Jev**, o mesmo padrão de guardrail barato do playbook (`invents_fact` / `on_move`).
4. **Memória por resumo a cada 60 s** resolve calls longas, que é a limitação nº 4 do playbook.
5. **Relay no servidor:** nenhuma chave no desktop e custo medido por chamada. É o que permite mostrar "US$ 1,96 por call".
6. **Produto fechado no ciclo:** antes da call (contexto), durante (dicas), logo depois (CRM e assessment) e fora da call (coach treinando com as calls reais). O assessment com citações do lead é o maior diferencial comercial.
7. **Captura comprada pronta** (Recall Desktop SDK): ele não reinventou a parte mais difícil, a de áudio e diarização por canal.
8. **Dados com origem:** cada informação extraída aponta o trecho de onde veio, o que permite auditar.

## 8. Limitações e riscos

1. **84% das chamadas ao modelo pesado são descartadas** e pagas mesmo assim. A revisão acontece depois da geração. Um filtro antes, com o Jev perguntando "vale gerar algo aqui?" ou com um card genérico de fallback, cortaria a maior parte do custo.
2. **Não há métrica de resultado.** A meta declarada é a conversão por reunião realizada, mas o vídeo não mostra antes e depois. O "R$ 15 se paga" é afirmação, não medição.
3. **Custo ainda estimado:** o número vem de um replay extrapolado, e o slide diz que falta medir uma call inteira.
4. **Dependências e LGPD:** Recall.ai, AssemblyAI, OpenRouter e OpenAI recebem o áudio ou a transcrição de clientes. Também há risco de consentimento: o app grava sem bot visível e tem "Modo discreto".
5. **O Jev tem latência de 2,5 s na triagem** (no playbook ficava abaixo de 1 s com uma única requisição). Somado ao 1,2 s de espera e à geração do GPT-5.4, a dica gerada chega vários segundos depois da fala.
6. **Código fechado e preso ao ecossistema** dele (CRM do Growth, Acelera). Não há nada para clonar: dá para copiar a arquitetura, não o código. Os prompts, o schema dos 72 cards e a política exata não aparecem.
7. **O assessment pode exagerar:** o gráfico "Onde você está em 12 meses?" é uma projeção ilustrativa feita por IA para um lead específico. Isso exige revisão do closer para não virar promessa.
8. **O vídeo é peça de marketing:** CTA para a Acelera repetido três vezes, workshop e prova social. Os números técnicos são reais na tela, mas foram escolhidos para vender.

## 9. Correções à engenharia reversa anterior

| Na engenharia reversa | Na tela |
| --- | --- |
| Captura nativa com ScreenCaptureKit/Core Audio (inferido) | **Recall Desktop SDK** + **AssemblyAI**, app em **Electron** |
| Gate: fim de turno + ~15 s (inferido) | **1,2 s** de assentamento, **≥ 15 s** entre dicas, **≤ 4/min**, objeção e "Pedir ajuda" furam a fila |
| Tamanho do banco de cards desconhecido | **72 cards**, pré-seleção local para **≤ 4** candidatos |
| Contexto longo desconhecido | **Resumo da call (GPT-5.4) a cada 60 s** |
| Custo do Jev "centavos, não informado" | **US$ 0,12** por call |
| Avaliação do closer "não implementada" | **Já existe** (nota, funcionou/melhorar, próximo exercício). Falta a gamificação |
| Identidade do JEV desconhecida | **`jev-1.13`**, o mesmo modelo usado pelo playbook-sales-copilot, via OpenRouter |

## 10. Comparação com o playbook-sales-copilot

| | playbook-sales-copilot | Deal Advisor |
| --- | --- | --- |
| Saída ao vivo | Probabilidade de fechamento + próxima jogada + frase | Etapa, objeção e card de dica |
| Uso do Jev | 1 requisição com ~40 perguntas por fala | 3 chamadas pequenas (triagem, card, revisão) a cada ~16 s |
| Texto gerado | Frase pronta, com LLM pequeno opcional (Haiku) | GPT-5.4 quando não há card |
| Custo da call | < US$ 0,05 | ~US$ 2,50 |
| Contexto longo | Fatos travados + janela de 12 falas | Resumo a cada 60 s |
| Captura | Chrome STT / sala WebRTC própria | Recall Desktop SDK + AssemblyAI |
| Pós-call | Nada | CRM, assessment e coach |
| Idioma | Perguntas em inglês | Português de ponta a ponta |
| Disponível | Open source | Fechado |

**Resumo:** o playbook tem o **motor** mais elegante e barato (sinais tipados, pesos e máquina de estados). O Deal Advisor tem o **produto** mais completo (captura robusta, memória, pós-call e treino). O custo 50 vezes maior vem quase todo do modelo pesado escrevendo dicas que depois são descartadas.

## 11. Ideias para o nosso sales-copilot

- **Juntar os dois:** o motor de sinais do playbook (fan-out em 1 requisição, máquina de estados da objeção, fatos com memória) com a **biblioteca de cards** e o **gate** do Deal Advisor. Gerar texto só quando nenhum card servir.
- **Revisar antes de pagar:** perguntar ao Jev se vale gerar antes de chamar o modelo caro, e usar um modelo médio (Haiku ou Gemini Flash) em vez do GPT-5.4 para dicas de até 30 palavras. A meta é ficar abaixo de US$ 0,30 por call.
- **Começar com 30 a 50 cards em PT-BR** escritos a partir das nossas objeções reais, com `etapa`, `gatilho`, `texto` e `pergunta`. Medir a taxa de "nenhum card cabe" para saber onde escrever cards novos.
- **Avaliar o Recall.ai Desktop SDK** para a captura. Resolve diarização e Meet/Zoom/Teams sem bot (é a limitação nº 5 do playbook). Comparar custo e LGPD com captura própria.
- **Memória por resumo** a cada 60 s ou a cada mudança de etapa, somada aos fatos travados.
- **Relay no servidor com log de custo por chamada** desde o primeiro dia, para ter números reais como os dele.
- **Pós-call primeiro**, se for preciso priorizar: extração para CRM com origem do trecho, resumo e follow-up. Tem mais valor e menos risco de latência que as dicas ao vivo. O assessment personalizado vem depois.
- **Coach:** reaproveitar as transcrições como base de personas. O padrão de avaliação ("Sem nota" quando há pouca evidência, com próximo exercício) é bom de copiar.
- **Medir o que ele não mediu:** conversão por reunião com e sem copiloto, e a porcentagem de dicas aplicadas, já que o 3º Jev registra isso.
