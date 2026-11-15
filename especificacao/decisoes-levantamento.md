# Decisões do levantamento

Decisões da revisão do Sales Copilot desde a ideia, tomadas em sessões de `grilling` a partir das análises em `pesquisas/` e das 12 sessões diagnósticas da EPC/MEPC de 05 a 13/11/2026, que entram só como fato da EPC. O PRD atual, as notas técnicas e os protótipos entram como hipóteses. Este arquivo traz só regras e números agregados, sem dado pessoal de lead.

## 1. Situação

| Decisão | Valor | Decidido por | Data |
|---|---|---|---|
| O que é o produto | Ferramenta genérica de treino e copiloto para o closer de venda consultiva, que fecha a call com ata, próximo passo e resultado da venda | Rodrigo | 14/11/2026 |
| Para quem é o produto | Negócios de venda consultiva de qualquer nicho. Por isso o playbook de cada negócio é configurável | Rodrigo | 14/11/2026 |
| Papel da EPC/MEPC | Não é base de requisitos, primeiro cliente nem piloto. As gravações dela entram só como material, conforme a linha "Gravações de calls" | Rodrigo | 14/11/2026 |
| Dados de cada negócio | Prazos, preços, volume de calls e objeções de um negócio servem para calibrar os parâmetros e as objeções dele. Não são decisões de produto | Rodrigo | 14/11/2026 |
| Quem usa como closer | Qualquer closer. O produto não depende de o Rodrigo ser quem vende | Rodrigo | 14/11/2026 |
| Primeiro negócio | Nenhum definido | Rodrigo | 14/11/2026 |
| Tempo e pessoas para construir | Não definido, por não ser relevante neste momento. O veredito sai sem limite de capacidade | Rodrigo | 14/11/2026 |
| Prazo | Nenhum para o produto. A janela de vendas de janeiro de 2027 é dado da EPC, não do produto | Rodrigo | 14/11/2026 |
| Custo de errar ao vivo | Não se aplica como pergunta de situação | Rodrigo | 14/11/2026 |

Teto de gasto mensal e aprender como motivo ficam sem definir. O veredito saiu sem depender deles.

## 2. Problema

| Decisão | Valor | Decidido por | Data |
|---|---|---|---|
| Dor do closer | Seguir o playbook da empresa em tempo real durante a call, para vender mais | Rodrigo | 14/11/2026 |
| Problema | Durante a call, o closer não tem o playbook da empresa à mão. Por isso: (1) não sabe em que etapa da call está; (2) pula perguntas de descoberta e qualificação, e descobre tarde quem decide junto com o lead; (3) não sabe o que dizer agora nem a resposta que a empresa aprovou para a objeção do momento; (4) improvisa a proposta: preço fora da tabela, conta das parcelas errada, plano menor não oferecido quando o orçamento do lead cabia nele; (5) deixa passar sinais de compra sem pedir o fechamento; (6) fala demais; (7) não sabe se a venda está indo bem; (8) perde o contexto numa call longa e erra dados do próprio lead, como o nome; (9) fica sem ajuda quando a situação não está prevista no playbook; (10) termina a call sem próximo passo com responsável e data. Fora da call, o closer só melhora vendendo para lead real. A empresa não consegue pôr o próprio playbook numa ferramenta sem programar | Rodrigo | 14/11/2026 |
| Empresa sem playbook escrito | O produto atende quem já tem playbook escrito e quem não tem. Para quem não tem, o produto ajuda a escrever um | Rodrigo | 14/11/2026 |
| Gravações de calls | As gravações que a empresa tiver dos closers servem para o treino e para ajustar a configuração do playbook. As 12 gravações da EPC entram assim, anonimizadas, por serem as únicas calls reais em português disponíveis | Rodrigo | 14/11/2026 |
| Pós-call | Entram a transcrição da call, a ata, o resultado da venda (fechou, perdeu ou ficou em aberto, com o motivo) e o próximo passo com responsável e data. O resultado da venda pertence à negociação, que é o lead com as calls dele, e conta uma vez mesmo com várias calls; ata e próximo passo pertencem a cada call. Toda call com o copiloto, inclusive de venda perdida, tem transcrição e ata. A call sem copiloto não tem transcrição nem ata: o closer registra à mão o resultado e o próximo passo ou o motivo da perda. Ficha de CRM e lembretes de follow-up ficam com as ferramentas que cada negócio já usa. O resultado da venda alimenta o ajuste do playbook: mostra quais objeções, planos e falas aparecem nas vendas fechadas e nas perdidas | Rodrigo | 15/11/2026 |

## 3. Substitutos e caminhos mais baratos

| Decisão | Valor | Decidido por | Data |
|---|---|---|---|
| Comprar ou adaptar uma ferramenta pronta | Descartado | Rodrigo | 14/11/2026 |
| Referências | O produto se baseia no playbook-sales-copilot e no Deal Advisor, analisados em `pesquisas/` | Rodrigo | 14/11/2026 |

## 4. Veredito

| Decisão | Valor | Decidido por | Data |
|---|---|---|---|
| Construir | Sim, um produto próprio | Rodrigo | 14/11/2026 |
| Ordem de construção | (1) playbook configurável, escrito ou importado pela empresa; (2) ajuda ao vivo durante a call, com a transcrição; (3) fim da call: ata, próximo passo e resultado da venda; (4) treino com lead simulado; (5) rascunho de playbook a partir de calls gravadas, para quem não tem playbook escrito | Rodrigo | 15/11/2026 |
| Uso das referências | Como ideias e desenho. O produto tem código próprio e não copia o do playbook-sales-copilot, mesmo que o autor permita | Rodrigo | 14/11/2026 |

## 5. Sucesso

| Parte | Funciona quando | Decidido por | Data |
|---|---|---|---|
| Playbook configurável | A empresa configura o playbook inteiro sem escrever uma linha de código. Obrigatório | Rodrigo | 14/11/2026 |
| Ajuda ao vivo | Medida em calls reais, porque as gravações não servem para medi-la: (1) a dica aparece enquanto o assunto ainda está em pauta; (2) ao fim da call, o closer marca cada dica como útil ou inútil, e a maioria sai útil; (3) toda proposta mostrada sai da tabela de preços cadastrada, com a conta certa; (4) toda call termina com próximo passo, responsável e data, menos a venda perdida, que termina com o motivo da perda. Os critérios (1) e (3) são revisados ao fim da parte 2; os critérios (2) e (4), ao fim da parte 3, porque marcar as dicas e registrar o próximo passo são telas dela | Rodrigo | 15/11/2026 |
| Fim da call | (1) Numa call com o copiloto, inclusive de venda perdida, a ata e o próximo passo ficam prontos ao fim da call sem o closer escrever nada; ele só confere e corrige. (2) Toda negociação tem o resultado da venda registrado, como definido na linha "Pós-call", e ele pode ser atualizado depois, quando o lead responder | Rodrigo | 15/11/2026 |
| Treino | (1) Um gestor que ouve uma simulação a julga parecida com uma call real do playbook dele: o lead simulado levanta as objeções cadastradas e reage às respostas do closer. (2) A avaliação aponta os mesmos erros que o gestor apontaria na mesma simulação: etapa pulada, pergunta de qualificação esquecida, objeção mal respondida ou proposta fora da tabela | Rodrigo | 15/11/2026 |
| Rascunho de playbook | A partir das gravações, o produto gera um playbook que a empresa só revisa, sem escrever do zero | Rodrigo | 14/11/2026 |

Revisão: o Rodrigo revisa cada parte contra o critério dela antes de começar a parte seguinte da ordem de construção. A ajuda ao vivo é revisada nas primeiras calls reais de um negócio que use o produto, em duas etapas: critérios (1) e (3) ao fim da parte 2, critérios (2) e (4) ao fim da parte 3. A operação de vendas do próprio Rodrigo pode ser esse primeiro uso, sem ser cliente. Decidido pelo Rodrigo em 14/11/2026; revisão em duas etapas decidida em 15/11/2026.

## 6. Escopo e jornada

| Decisão | Valor | Decidido por | Data |
|---|---|---|---|
| Usuários | Dois: quem configura o playbook (dono ou gestor de vendas) e o closer, que treina e usa a ajuda ao vivo. O SDR fica fora | Rodrigo | 15/11/2026 |
| Pedir ajuda | Durante a call, o closer pede ajuda quando a situação não está no playbook, e o produto devolve uma sugestão curta a partir do que já foi dito. A resposta segue os critérios (1) e (3) da ajuda ao vivo: chega enquanto o assunto está em pauta e nunca traz preço fora da tabela | Rodrigo | 15/11/2026 |
| Jornada | (1) Configurar, pelo gestor: escreve ou importa o playbook, ou sobe gravações e revisa o rascunho gerado; ajusta o playbook com as gravações e com os resultados de venda. (2) Treinar, pelo closer: pratica numa simulação da ajuda ao vivo, com lead simulado, e recebe avaliação. (3) Antes da call, pelo closer: informa ou importa só os dados básicos do lead, como o nome e as respostas do formulário. (4) Call ao vivo: o produto transcreve e mostra a etapa, o que falta descobrir, a resposta aprovada para a objeção do momento, a proposta montada pela tabela, os sinais de compra, o alerta de fala demais e se a venda está indo bem; o closer pode pedir ajuda. (5) Fim da call: numa call com o copiloto, o closer confere a ata e o próximo passo com responsável e data, registra o resultado da negociação e marca cada dica como útil ou inútil; numa call sem copiloto, registra à mão o resultado e o próximo passo ou o motivo da perda; depois atualiza o resultado da negociação quando o lead responder. Ficam fora agenda, ficha de CRM e lembretes de follow-up | Rodrigo | 15/11/2026 |

## Hipóteses encaminhadas para teste

| Hipótese | Destino |
|---|---|
| Os desvios do playbook listados no problema acontecem com closers de outros nichos, não só na EPC | Validar com closers de um negócio real fora da EPC |
| A ajuda em tempo real aumenta as vendas | Calls reais, comparando o resultado da venda registrado em calls com e sem o produto |
| O produto deve ser um sistema agêntico | Decidido na etapa de plano (`tlc-plan`): híbrido, como o Deal Advisor (AD-001 em `.specs/STATE.md`) |

## Perguntas de tela encaminhadas para teste com o protótipo

Estas perguntas do PRD atual não viram decisão aqui.

| Pergunta | Destino |
|---|---|
| O painel durante a call mostra o que o closer precisa? O que tirar e o que falta? | Teste com o protótipo |
| O closer usa um monitor (Meet e painel lado a lado) ou dois? | Teste com o protótipo e call real |
| Ao compartilhar a tela, o painel lateral basta ou deve virar uma barra? | Teste com o protótipo e call real |
| Quantas sugestões o closer vê ao mesmo tempo sem se distrair? | Call real |
