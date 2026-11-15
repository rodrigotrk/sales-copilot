# Sales Copilot

> Read this PRD and take the bet as settled. Do not re-derive it.
> Status: confirmado pelo Rodrigo, 14/11/2026

## Situation

- Effort: ainda não construído. Existem só um protótipo navegável das telas e as análises das referências.
- Decision: construir um produto próprio, decidido pelo Rodrigo em 14/11/2026, no levantamento da Fase 1.
- In flight: o produto aproveita como ideia e desenho o playbook-sales-copilot e o Deal Advisor, sem copiar código de nenhum dos dois. As telas e regras de negócio estão no documento de telas e regras do Sales Copilot, que registra também os desvios do protótipo.
- At stake: não avaliado. O veredito saiu sem limite de tempo, de pessoas ou de gasto mensal.

## Problem

Durante a call, o closer de venda consultiva não tem à mão o playbook da empresa. Por isso:

1. não sabe em que etapa da call está;
2. pula perguntas de descoberta e qualificação, e descobre tarde quem decide junto com o lead;
3. não sabe o que dizer agora nem a resposta que a empresa aprovou para a objeção do momento;
4. improvisa a proposta: preço fora da tabela, conta das parcelas errada, plano menor não oferecido quando o orçamento do lead cabia nele;
5. deixa passar sinais de compra sem pedir o fechamento;
6. fala demais;
7. não sabe se a venda está indo bem;
8. perde o contexto numa call longa e erra dados do próprio lead, como o nome;
9. fica sem ajuda quando a situação não está prevista no playbook;
10. termina a call sem próximo passo com responsável e data.

Fora da call, o closer só melhora vendendo para lead real, e cada treino gasta um lead. A empresa não consegue pôr o próprio playbook numa ferramenta sem programar: nas duas referências o playbook fica escrito no código, e uma delas serve a um único negócio. Quem não tem playbook escrito não tem nem por onde começar.

Quem sente: o closer, em qualquer nicho de venda consultiva, e o dono ou gestor de vendas que quer que o time siga o playbook.

Evidência: não medida fora de um negócio. Nas 12 sessões diagnósticas da mentoria EPC/MEPC, de 05 a 13/11/2026, o mesmo plano foi oferecido com três preços diferentes, quem decide junto com o lead estava fora da call em 4 de 12, e 3 de 12 terminaram sem próximo passo datado. Esses números são da EPC, e os mesmos desvios em outros nichos são hipótese a validar.

## Success

- Playbook configurável. Worked if: a empresa configura o playbook inteiro sem escrever uma linha de código. Obrigatório.
- Ajuda ao vivo, medida em calls reais, porque gravações não servem para medi-la. Worked if: (1) a dica aparece enquanto o assunto ainda está em pauta; (2) ao fim da call, a maioria das dicas sai marcada como útil; (3) toda proposta mostrada sai da tabela de preços cadastrada, com a conta certa; (4) toda call termina com próximo passo, responsável e data, menos a venda perdida, que termina com o motivo da perda.
- Fim da call. Worked if: (1) numa call com o copiloto, inclusive de venda perdida, a ata e o próximo passo ficam prontos sem o closer escrever nada, e ele só confere e corrige; (2) toda negociação tem o resultado da venda registrado e atualizável quando o lead responder, e cada venda conta uma vez, mesmo com várias calls.
- Treino. Worked if: (1) um gestor que ouve uma simulação a julga parecida com uma call real do playbook dele, com o lead simulado levantando as objeções cadastradas e reagindo às respostas do closer; (2) a avaliação aponta os mesmos erros que o gestor apontaria na mesma simulação: etapa pulada, pergunta de qualificação esquecida, objeção mal respondida ou proposta fora da tabela.
- Rascunho de playbook. Worked if: a partir das gravações, o produto gera um playbook que a empresa só revisa, sem escrever do zero.
- Going wrong: nas primeiras calls reais, o closer marca a maioria das dicas como inútil, uma dica chega depois que o assunto passou, uma proposta sai fora da tabela, ou o gestor precisa de alguém que programe para configurar o playbook.
- Review: o Rodrigo revisa cada parte contra o critério dela antes de começar a parte seguinte da ordem de construção. A ajuda ao vivo é revisada nas primeiras calls reais de um negócio que use o produto, em duas etapas: ao fim da parte 2, pelos critérios (1) e (3); ao fim da parte 3, antes da parte 4, pelos critérios (2) e (4), porque marcar as dicas e registrar o próximo passo são telas da parte 3. A operação de vendas do próprio Rodrigo pode ser esse primeiro uso, sem ser cliente.

## Boundary

In: cinco partes, nesta ordem de construção:

1. playbook configurável, escrito ou importado pela empresa;
2. ajuda ao vivo durante a call, com a transcrição;
3. fim da call: ata, próximo passo e resultado da venda;
4. treino com lead simulado;
5. rascunho de playbook a partir de calls gravadas, para quem não tem playbook escrito.

Dois usuários: quem configura o playbook (dono ou gestor de vendas) e o closer, que treina, usa a ajuda ao vivo e fecha a call. Qualquer closer pode usar; o produto não depende de o Rodrigo ser quem vende. Uma pessoa pode participar de mais de um negócio. Só cria um negócio quem o operador do produto liberou, enquanto o modelo de negócio e o teto de gasto não estão definidos.

As gravações de calls que a empresa tiver servem para o treino e para ajustar o playbook. As 12 gravações da EPC entram assim, anonimizadas, como as únicas calls reais em português disponíveis.

Out:

- SDR - fora dos usuários.
- Agenda e agendamento de calls - ficam com as ferramentas de cada negócio.
- Ficha de CRM, pipeline de leads, lembretes e mensagens de follow-up - ficam com as ferramentas que cada negócio já usa.
- Envio de mensagens, cobranças ou contratos pelo produto - quem envia é a pessoa, na ferramenta dela.
- Calls fora do Google Meet - a ajuda ao vivo cobre o Meet; outra plataforma entra quando um negócio que a use aparecer.
- A EPC/MEPC como base de requisitos, primeiro cliente ou piloto - as gravações dela são só material.
- Comprar ou adaptar uma ferramenta pronta - descartado.
- Apresentação personalizada gerada a partir da call - fora desta aposta.

Unchanged: cada negócio continua dono do próprio playbook, do CRM e do follow-up. Prazos, preços, volume de calls e objeções de um negócio calibram o playbook dele e não mudam o produto.

## Journey

| State | What should happen | Person sees |
|---|---|---|
| Gestor com playbook escrito | Escreve ou importa o playbook: etapas, o que descobrir com a pergunta sugerida, objeções com a resposta aprovada e a tabela de preços com desconto máximo, sem programar. Confere cada preço importado e publica uma versão; só a versão publicada vale para as calls | Gestor: o playbook organizado nesses blocos, editável, com os preços importados marcados para conferir |
| Gestor sem playbook escrito | Sobe gravações de calls e recebe um rascunho do playbook para revisar | Gestor: o rascunho nos mesmos blocos, pronto para revisão |
| Gestor testa o playbook | Roda uma call já gravada com o playbook e vê o que o painel teria mostrado em cada momento | Gestor: o painel da call gravada, momento a momento |
| Gestor ajusta o playbook | Vê quais objeções, planos e falas aparecem nas vendas fechadas e nas perdidas, e quais dicas os closers marcaram como úteis | Gestor: o comparativo e a taxa de dicas úteis por fala |
| Closer treina | Escolhe um cenário (recomendado pelas objeções que mais travam, uma call própria ou um lead gerado), o trecho e a dificuldade, e conversa por voz com um lead simulado, com ou sem o painel da ajuda ao vivo | Closer: o lead simulado, que levanta as objeções cadastradas e reage às respostas, e o painel se escolheu treinar com ele |
| Closer termina o treino | Recebe a avaliação com os erros por tipo (etapa pulada, pergunta esquecida, objeção mal respondida, proposta fora da tabela), cada um com o trecho, e um próximo exercício | Closer: a avaliação e o próximo exercício |
| Antes da primeira call com o lead | O closer informa ou importa os dados básicos do lead, como o nome e as respostas do formulário, e pode informar o perfil profissional público dele | Closer: os dados do lead, o resumo do perfil com um gancho para o diagnóstico, o que descobrir na call e a sugestão de abertura |
| Antes de uma call de retorno | O produto traz a ata, o resultado e o próximo passo da call anterior, e o que já foi descoberto vem marcado | Closer: o histórico da negociação e só o que ainda falta descobrir |
| Closer inicia a call | O closer inicia a call pelo produto, que lembra de avisar o lead que a call é gravada; ele escolhe entrar com ou sem o copiloto | Closer: o lembrete do aviso de gravação e a escolha |
| Call sem copiloto | O produto não ouve nem transcreve a call. Ao encerrar, o closer registra à mão o resultado da negociação e o próximo passo com responsável e data, ou o motivo da perda, e pode escrever anotações | Closer: nada do produto durante a call; ao encerrar, o fim da call sem ata, transcrição nem lista de dicas |
| Call ao vivo | O produto transcreve e mostra a etapa, o que falta descobrir (marcando o que já foi dito, com o minuto), a resposta aprovada para a objeção do momento, a proposta montada pela tabela com a conta certa, o alerta quando quem decide não está na call, o alerta de sinal de compra com a sugestão de pedir o fechamento, o alerta de fala demais e se a venda está indo bem | Closer: o painel ao lado da reunião; o lead não vê o painel quando o closer compartilha uma aba ou janela |
| Situação fora do playbook | O closer pede ajuda e recebe uma sugestão curta a partir do que já foi dito, enquanto o assunto está em pauta e nunca com preço fora da tabela | Closer: a sugestão no painel |
| Fim da call | Numa call com o copiloto, a ata e o próximo passo com responsável e data ficam prontos; o que não ficou claro aparece como "a confirmar", com o minuto de onde veio. O closer confere, corrige, registra o resultado da negociação (fechou, perdeu ou em aberto, com o motivo), marca cada dica da call como útil ou inútil e pode copiar a ata e o próximo passo para a ferramenta que já usa | Closer: a ata, o próximo passo, a transcrição, o resultado e a lista de dicas |
| Venda perdida | A call pode terminar sem próximo passo; o motivo da perda é obrigatório. Numa call com o copiloto, a ata e a transcrição ficam prontas como em qualquer call | Closer: o campo de motivo no lugar do próximo passo |
| Closer sai sem registrar o resultado | A negociação fica "em aberto", sem motivo, e a call aparece destacada na lista de calls até o resultado ser completado | Closer: a call destacada na lista |
| O lead responde depois da call | O closer abre a call na lista e atualiza o resultado da negociação, que vale para todas as calls daquele lead | Closer: a lista de calls com data, closer, resultado atual da negociação e próximo passo, e o resumo do período: calls realizadas e negociações fechadas, perdidas e em aberto, cada negociação contada uma vez |

## Open questions

1. O painel durante a call mostra o que o closer precisa? O que tirar e o que falta? - não decidido; teste com o protótipo. Define o conteúdo final do painel.
2. O closer usa um monitor (reunião e painel lado a lado) ou dois? - não decidido; teste com o protótipo e call real. Define se o painel compacto ou o expandido é o principal.
3. Ao compartilhar a tela, o painel lateral basta ou deve virar uma barra? - não decidido; teste com o protótipo e call real.
4. Quantas sugestões o closer vê ao mesmo tempo sem se distrair? - não decidido; call real. Define o ritmo das dicas no painel.
5. Os desvios do playbook listados no problema acontecem com closers de outros nichos, não só na EPC? - não decidido; validar com closers de um negócio real fora da EPC.
6. A ajuda em tempo real aumenta as vendas? - não decidido; calls reais, comparando o resultado registrado em calls com e sem o produto.
7. O produto deve ser um sistema agêntico? - decidido na etapa de plano; a decisão está em AD-001, em `.specs/STATE.md`.
8. Primeiro negócio a usar o produto - nenhum definido; a candidata para revisar a ajuda ao vivo está em Review.
9. Teto de gasto mensal - não definido.
10. Playbook sem tabela de preços cadastrada - default: o produto não monta proposta nem cita preço.
