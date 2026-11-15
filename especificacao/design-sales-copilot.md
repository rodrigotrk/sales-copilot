# Telas e regras de negócio do Sales Copilot

O PRD, em `.prd/sales-copilot.md`, define o problema, o escopo e a jornada; aqui ficam a forma de cada tela e as regras que valem em todas.

As telas seguem o protótipo (os PNGs e o HTML navegável). Os originais trazem nomes de leads e ficam fora do git, em `pesquisas/prototipos-originais/`; cada plano copia para o próprio `captures/` a tela que usa, sem dados pessoais, e essa cópia é a fonte da tela. O protótipo foi desenhado com dados de exemplo da EPC/MEPC, que não fazem parte do produto. Cada ponto em que esta especificação difere do protótipo aparece na tabela de desvios da tela, confirmado pelo Rodrigo em 15/11/2026. O que não aparece como desvio vale como está no protótipo.

## Termos

| Termo | Significado |
|---|---|
| Playbook | O jeito de vender de um negócio: etapas da call, o que descobrir com a pergunta sugerida, objeções com a resposta aprovada e tabela de preços com desconto máximo. O protótipo chama de "Método de venda" |
| Gestor | Quem configura o playbook: o dono do negócio ou o gestor de vendas |
| Closer | Quem conduz a call e fecha a venda |
| Dica | Tudo o que o painel sugere ao closer durante a call: resposta a objeção, pergunta do que falta descobrir, próxima possibilidade, alerta ou resposta a um pedido de ajuda |
| Resultado da venda | Fechou, perdeu ou em aberto, com o motivo. Pertence à negociação, não a cada call |
| Negociação | O lead com as calls dele no produto, sem etapas nem funil. Ata e próximo passo pertencem a cada call; o resultado da venda, à negociação |
| Próximo passo | Ação combinada, com responsável e data |
| Call de retorno | Call com um lead que já teve call registrada no produto |

## Navegação

O menu lateral de ícones fica como no protótipo, com o visual claro e o destaque em roxo. Os itens são:

| Item | Tela |
|---|---|
| Nova call | Antes da call |
| Ao vivo | Painel durante a call |
| Calls | Lista de calls e, ao abrir uma, o fim da call |
| Coach | Treino |
| Playbook | Configuração do playbook |

| Desvio | Decisão que o causa |
|---|---|
| "Agenda" vira "Nova call" | Agenda fica com as ferramentas de cada negócio |
| "Pipeline" sai; a lista de calls entra em "Calls" | Pipeline e lembretes de follow-up ficam fora |
| "Método" vira "Playbook", no menu e no título da tela | Um termo por conceito, o mesmo do PRD |
| Entra "Trocar de negócio" no menu, só para quem participa de mais de um negócio; ele abre o endereço do outro negócio | Uma pessoa pode participar de mais de um negócio, e cada aba mostra o negócio do próprio endereço |

## 1. Antes da call

Protótipo: `tela-1-antes.png`.

O closer abre uma call nova com um lead novo ou com um lead que já teve call.

Fica do protótipo:

- Os dados do lead, com "a confirmar" no que o closer não tem certeza, como o nome completo.
- "O que descobrir nesta call": cada informação do playbook com a pergunta sugerida e a contagem do que já foi descoberto. As informações que mais travam vendas no playbook levam um destaque.
- "Agora · para abrir a call": a sugestão de abertura, tirada do playbook.
- "O que o perfil já mostra": o closer pode informar o link do perfil profissional público do lead, e o produto resume o perfil e aponta um gancho para o diagnóstico.

| Desvio | Decisão que o causa |
|---|---|
| Os dados do lead se limitam aos básicos, informados ou importados pelo closer, como o nome e as respostas do formulário | Antes da call, o closer informa ou importa só os dados básicos do lead |
| Saem o horário agendado, a duração e "Agendado por" | Agenda e SDR ficam fora |
| Sai o checklist "Preparação" com as tarefas da SDR | SDR fica fora |
| Em call de retorno, a tela mostra a ata, o resultado e o próximo passo da call anterior, e o que já foi descoberto vem marcado | O closer não recomeça do zero uma negociação em aberto |

### Iniciar a call

O closer inicia a call pela própria tela. O produto lembra de avisar o lead que a call é gravada, e o closer escolhe "Entrar com o copiloto" ou "Sem copiloto", como no cartão "Antes de entrar" do protótipo (`tela-2a-na-call.png`). Sem copiloto, o produto não ouve nem transcreve a call; ao encerrar, o closer registra o resultado à mão, como na seção 3.

| Desvio | Decisão que o causa |
|---|---|
| O aviso não chega sozinho na hora marcada; o closer inicia a call | Agenda fica fora |

## 2. Painel durante a call

Protótipos: `tela-2a-na-call.png` (painel compacto, ao lado da reunião) e `tela-2-durante.png` (painel expandido).

A ajuda ao vivo cobre calls no Google Meet, no Chrome e no Edge. O painel abre numa janela flutuante sobre a reunião, e o lead não o vê quando o closer compartilha uma aba ou janela. Antes de entrar na call, o produto lembra o closer de compartilhar só a aba ou a janela da apresentação, porque na tela inteira o painel aparece. Nenhum participante extra entra na reunião.

Fica do protótipo:

- Etapas da call, com a atual em destaque e as anteriores marcadas.
- Alerta quando quem decide não está na call, com o trecho, o minuto e a sugestão do que fazer.
- "Agora": a resposta aprovada para a objeção do momento, com o tipo da objeção, o que ouvir em seguida e a próxima etapa.
- "Próximas possibilidades": até duas falas para as respostas mais prováveis do lead.
- "O que falta descobrir": marca sozinho o que já foi dito, com o minuto, e mostra a pergunta sugerida do que falta.
- Proposta: no compacto, o resumo; no expandido, a calculadora com plano, entrada e parcelas, preço de tabela, desconto, a conta conferida e o desconto máximo.
- "Pedir ajuda", nos dois painéis.
- No expandido, a transcrição com quem falou e o minuto, e as abas "Anotações" e "Dados".
- O botão que reduz o painel a uma barra fina com o alerta e a dica do momento, e o botão que alterna entre compacto e expandido.

| Desvio | Decisão que o causa |
|---|---|
| Saem os botões "Usei" e "Não serve" das dicas | O closer marca as dicas como úteis ou inúteis ao fim da call, sem tirar a atenção do lead |
| Entra o alerta de sinal de compra, no mesmo formato do alerta de decisor, com a sugestão de pedir o fechamento | O closer deixa passar sinais de compra |
| Entram dois indicadores pequenos no topo do painel compacto: fala demais e se a venda está indo bem | O closer fala demais e não sabe se a venda está indo bem |
| O expandido mostra o tempo de fala do closer e do lead | O closer fala demais |

A posição final desses itens depende das perguntas de tela abertas no PRD.

### Pedir ajuda

Quando a situação não está no playbook, o closer toca em "Pedir ajuda" e recebe uma sugestão curta, montada a partir do que já foi dito na call. A sugestão chega enquanto o assunto está em pauta e nunca traz preço fora da tabela.

## 3. Fim da call

Protótipo: `tela-3-depois.png`.

Fica do protótipo:

- Cabeçalho com a data, a duração e a contagem de itens a confirmar.
- Abas "Transcrição" e "Anotações".
- Resumo da call, que é a ata.
- Próximos passos em tabela: quem, o quê e até quando.
- Cada dado incerto aparece como "a confirmar", com o trecho e o minuto de onde veio, e um campo para o closer corrigir.

| Desvio | Decisão que o causa |
|---|---|
| Sai "Dados do lead · vai para o CRM" e o botão "Aprovar e salvar no CRM"; o "a confirmar" com trecho e minuto passa para a ata | Ficha de CRM fica com as ferramentas de cada negócio |
| Sai a "Mensagem sugerida · WhatsApp" | Follow-up fica com as ferramentas de cada negócio |
| O selo de status vira o resultado da negociação: fechou, perdeu ou em aberto, com o motivo, editável depois | Pós-call inclui o resultado da venda, que pertence à negociação |
| Entra a lista das dicas mostradas na call, cada uma para marcar como útil ou inútil | O closer marca cada dica ao fim da call |
| Entra um botão para copiar a ata e o próximo passo, para colar na ferramenta que o negócio já usa | Ficha de CRM e follow-up ficam com as ferramentas de cada negócio, e a ata precisa chegar até elas |

Numa call com o copiloto, inclusive de venda perdida, a tela traz a ata e a transcrição; a venda perdida troca o próximo passo pelo motivo da perda. Numa call sem copiloto, a tela não tem ata, transcrição nem lista de dicas: o closer preenche o resultado da negociação e o próximo passo com responsável e data, ou o motivo da perda, e pode escrever anotações.

## 4. Lista de calls

Protótipo: `tela-4-retornos.png`, que mostra o pipeline.

A tela lista as calls, cada uma com data, lead, closer, resultado atual da negociação com o motivo e próximo passo com responsável e data. Abrir uma call leva ao fim da call, onde o closer atualiza o resultado da negociação quando o lead responde. No topo, o resumo do período: calls realizadas e negociações fechadas, perdidas e em aberto, cada negociação contada uma vez.

| Desvio | Decisão que o causa |
|---|---|
| A tela deixa de ser pipeline de leads e vira lista de calls | Pipeline fica fora; o resultado precisa ser atualizável depois |
| Saem o destaque "atrasado", o "verificar" e o cartão "Atrasados" | Lembretes de follow-up ficam com as ferramentas de cada negócio |
| O destaque passa a marcar a call que ficou "em aberto" sem motivo, até o closer completar o resultado | Toda negociação tem o resultado da venda registrado |
| O resumo do topo troca "Retornos agendados" e "Fechadas com pagamento" por calls realizadas e negociações fechadas, perdidas e em aberto | Agenda e pagamento ficam fora; uma venda com várias calls conta uma vez |

## 5. Treino

Protótipo: `tela-5-treino.png`.

Fica do protótipo:

- Três fontes de cenário: "Recomendados", montados a partir das objeções que mais travam vendas no playbook; "Uma call minha", que refaz uma call gravada do closer; e "Gerar um lead".
- Trecho: call inteira ou uma etapa do playbook.
- Dificuldade: Receptivo, Realista ou Exigente.
- Conversa por voz, até 10 minutos, com "Encerrar".
- O lead simulado levanta as objeções cadastradas, reage às respostas do closer e encerra a call se o closer for agressivo ou ignorar a objeção.
- Avaliação com nota (ou "sem nota" quando a simulação teve pouco material), o que funcionou e o que melhorar com citações, e um próximo exercício. Botões "Novo treino" e "Treinar de novo".

| Desvio | Decisão que o causa |
|---|---|
| O closer escolhe treinar com ou sem o painel da ajuda ao vivo; com ele, o painel aparece ao lado da simulação como numa call real | O treino é uma simulação da ajuda ao vivo; sem o painel, o closer pratica também sem ajuda |
| A avaliação lista os erros em quatro tipos: etapa pulada, pergunta de qualificação esquecida, objeção mal respondida e proposta fora da tabela, cada um com o trecho | A avaliação aponta os mesmos erros que o gestor apontaria |
| Os cenários mostram a objeção e quantas calls ela travou, sem nome de lead | Nenhum dado pessoal de lead fora da call dele |

## 6. Playbook

Protótipo: `tela-6-metodo.png`.

Fica do protótipo:

- Etapas da call, em ordem, com "+ Etapa".
- "O que precisa ser descoberto na call": a informação e a pergunta sugerida.
- "Objeções e sugestões de fala": a objeção, como o lead costuma dizer, a resposta aprovada e em quantas calls ela apareceu, com "+ Objeção".
- Tabela de preços: plano, à vista e parcelado, com o desconto máximo na call.
- O teste com uma call já gravada: roda a call com o playbook e mostra o que o painel teria mostrado em cada momento.

| Desvio | Decisão que o causa |
|---|---|
| O botão "Testar o método" vira "Testar o playbook" | Um termo por conceito |
| Entram três jeitos de começar: escrever, importar um documento, ou subir gravações e revisar o rascunho gerado | O playbook é escrito, importado ou rascunhado a partir de gravações |
| Entra um bloco de ajuste: objeções, planos e falas que aparecem nas vendas fechadas e nas perdidas, e a taxa de dicas marcadas como úteis por fala | O resultado da venda e a marcação das dicas alimentam o ajuste do playbook |
| Sai a narração do protótipo: o subtítulo sobre a mentoria EPC, o selo "Novo", o texto "Protótipo · dados reais", o selo "Configuração" e o cronômetro. O subtítulo fica "Cada negócio cadastra o próprio jeito de vender, sem programar.", e o topo mostra "Playbook: <nome> · versão <N>" | A narração explica o protótipo, não o produto |
| A coluna "Nas calls" só aparece quando o negócio tem call registrada | Sem call registrada não há o que contar |
| A coluna "Encontros" vira uma coluna opcional, com o nome que o gestor escreve | Outros nichos não vendem por encontros |
| O desconto máximo na call é um valor único em reais para todos os planos; sem valor, aparece "a definir" e o produto não oferece desconto | O gestor define o limite uma vez para a tabela inteira |
| Entram "+ Informação" e "+ Plano", edição no lugar, "Remover" em cada linha, setas para reordenar as etapas, "Importar documento" e "Publicar" | O gestor configura o playbook inteiro sem programar |
| O playbook tem rascunho e versões: "Publicar" grava a versão seguinte, e só a versão publicada vale para as calls | As calls guardam a versão que usaram |
| Planos e desconto trazidos pela importação chegam marcados "Importado · conferir", com o trecho do documento de onde vieram, e "Conferido" tira a marca | Nenhum preço importado chega à call sem que o gestor confira |

Regras da tela:

- "Publicar" exige ao menos uma etapa, uma informação a descobrir e uma objeção, todas as linhas completas e nenhum valor marcado "Importado · conferir". A tabela de preços pode ficar vazia.
- "Publicar" fica bloqueado enquanto algum campo estiver salvando ou com a gravação falha; o campo com falha oferece "Tentar de novo" e "Desfazer".
- A importação acrescenta etapas, informações, objeções e planos ao fim de cada bloco, sem mudar o que já existe. Desconto máximo e nome da coluna só são preenchidos quando estão vazios; quando o documento traz outro valor, o atual fica e o resultado da importação avisa a divergência.
- Durante a importação, o playbook fica só para leitura. Se outra aba gravou antes, a gravação é recusada e a tela pede para recarregar.

## Regras de negócio

1. Toda call com o copiloto começa com o lembrete de avisar o lead que a call é gravada.
2. Toda proposta e toda resposta do painel usam só os planos e preços da tabela cadastrada, com a conta conferida. O produto não monta proposta com desconto acima do máximo do playbook.
3. Sem tabela de preços cadastrada, o produto não monta proposta nem cita preço.
4. Toda call termina com próximo passo, responsável e data ou, na venda perdida, com o motivo da perda. Toda call com o copiloto termina também com ata e transcrição, inclusive a de venda perdida.
5. Quando o closer sai sem registrar o resultado, a negociação fica "em aberto", sem motivo, e a call fica destacada na lista até o closer completar.
6. O resultado da venda pertence à negociação e pode ser atualizado a qualquer momento depois da call. O resumo do período conta cada negociação uma vez.
7. O produto não envia mensagens, cobranças nem contratos.
8. Os dados de um negócio não aparecem para outro.
9. As gravações da EPC/MEPC entram no treino e no ajuste do playbook anonimizadas.
10. Só cria um negócio quem o operador do produto liberou.
11. Uma pessoa pode participar de mais de um negócio. Cada aba trabalha no negócio do próprio endereço, e trocar de negócio numa aba não muda as outras.

## Perguntas de tela em aberto

Ficam para o teste com o protótipo e para call real, sem virar requisito: o que tirar ou acrescentar no painel, um ou dois monitores, painel lateral ou barra ao compartilhar a tela, e quantas dicas o closer vê ao mesmo tempo sem se distrair.
