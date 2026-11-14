# Checklist de prosa

## Conteúdo
- [Estrutura](#estrutura)
- [Frases](#frases)
- [Não sinalize](#não-sinalize)

Cada linha é um sinal, não uma prova. Confirme que o texto não carrega nada antes de cortá-lo. Uma linha marcada como *fraco* justifica um corte só quando outro sinal aparece no mesmo trecho.

## Estrutura

| Padrão | Sinal | Ação |
|---|---|---|
| Narração de changelog | "foi alterado para", "agora usa", "anteriormente", "atualizado para", "corrigido: a versão anterior dizia", notas de correção datadas | Reescrever para o estado atual; apagar o histórico. Isentos: ADRs, changelogs, notas de versão, guias de migração |
| Ponteiro como explicação | Uma frase que só faz sentido depois de abrir um link, arquivo ou ID, ou de conhecer uma sigla ou rótulo inventado que nunca foi apresentado ao leitor; vários ponteiros num parágrafo | Reescrever: nomear o conceito ou dar o contexto em poucas palavras; manter o ponteiro depois, se útil |
| Fato repetido | O mesmo requisito, número ou decisão declarado em dois lugares | Fundir no nível que é dono dele; nos outros lugares, referir-se a ele pelo nome |
| Contexto repetido | Contexto que o leitor já tem diante de si: a pergunta que acabou de fazer, o texto poucas linhas acima, o arquivo que está editando | Apagar |
| Detalhe derivável | Listas de arquivos, métodos, valores de configuração ou convenções do repositório que o leitor conseguiria ler no código, repetidas por seção | Nomear a convenção uma vez; apagar as listas |
| Plano organizado por tipo de trabalho | Todo o trabalho de API, depois todo o de UI, depois todos os testes, de modo que nenhuma seção descreve algo entregável | Reescrever por fatia entregue |
| Opção rejeitada longa | Parágrafos sobre uma alternativa que não foi escolhida | Reescrever em uma frase: por que não e, se o texto disser, quando ela venceria |
| Seção vazia ou inflada | Um título seguido de "N/A", "Nenhum" ou texto genérico escrito para preencher um espaço do template | Apagar a seção |
| Seção superdimensionada | Uma seção mais longa do que aquilo que realmente se sabe sobre o assunto | Reescrever para o conteúdo conhecido |
| Ramificações em prosa | Opções, casos ou passos escritos como parágrafo | Reescrever como lista ou tabela |
| Listas só de rótulos | Itens com rótulo cujo texto repete o rótulo ("**Desempenho:** o desempenho melhora") | Reescrever como frases simples |
| Preâmbulo e recapitulação | Abertura que anuncia o que vem; fechamento que resume o que acabou de ser dito; uma pergunta que o próprio texto responde em seguida, ou um suspense ou franqueza encenada antes de uma afirmação rotineira | Apagar a moldura; reescrever a preparação como a afirmação direta |
| Título ecoado | A primeira frase sob um título repete o título | Apagar a frase |
| Trilha de raciocínio | Como o autor chegou a uma conclusão, quando o leitor precisa só da conclusão | Apagar, a menos que o raciocínio justifique uma decisão em que o leitor precisa confiar |
| Formatação decorativa | Negrito em frases comuns, emoji em títulos, títulos numa resposta curta, bullets para um raciocínio encadeado que se lê melhor como prosa, apartes rotulados ou uma expressão com dois-pontos encenando uma revelação ("Insight principal:") | Reescrever como texto simples |

## Frases

| Padrão | Sinal | Ação |
|---|---|---|
| Frase sem efeito | Removê-la não muda nada do que o leitor sabe ou faz. Em instruções para um agente (skills, prompts, `AGENTS.md`), removê-la não muda nada do que o modelo faria por padrão | Apagar a frase inteira, não aparar palavras |
| Moldura de contraste | "não X, mas Y"; "não é só X, é Y"; uma cauda truncada ", sem X"; ou o mesmo contraste dividido numa frase de negação e numa de afirmação. Só quando nenhum leitor acredita em X | Reescrever afirmando Y |
| Metadiscurso | Uma palavra, linha ou oração inicial sobre o texto em vez do assunto: dizer ao leitor o que notar ou quanto peso dar, apresentar um ponto comum como verdade oculta, ou montar um cenário (uma era, um campo) antes do ponto ("vale notar que", "o ponto-chave é", "importante", "em resumo") | Apagar a moldura, manter o conteúdo |
| Ressalva vazia | Uma palavra de cautela que não acrescenta informação ("talvez", "pode ser", "possivelmente poderia"), sozinha ou empilhada numa afirmação | Apagar; manter a ressalva que carrega incerteza real, porque apagá-la fabrica confiança |
| Discutir com ninguém | Refutar uma objeção que o leitor nunca levantou ("uma abordagem tentadora seria…") | Apagar |
| Afirmação sem fonte | "especialistas concordam", "é sabido que", "as boas práticas dizem" sem nada por trás | Atribuir a fonte ou apagar |
| Benefício vago ou linguagem de vendas | "melhora a manutenibilidade", "mais limpo", "mais robusto", ou adjetivos que fazem propaganda da coisa em vez de descrevê-la, sem nada específico | Substituir pelo efeito específico ou pelo que a coisa é, ou apagar |
| Inflação de importância | "desempenha um papel crucial", "um passo fundamental", "significativo" sem nada medido | Apagar a inflação, manter o fato |
| Oração final no gerúndio | ", garantindo…", ", destacando…", ", refletindo…" acrescentado para dar peso | Apagar, ou transformar numa frase com sujeito real |
| Enchimento portátil | Uma frase, aforismos incluídos, que poderia ir sem mudança para um documento sem relação | Apagar |
| Cerimônia de chat | Aberturas ("Ótima pergunta", "Claro!"), ofertas de encerramento ("Me avise se…"), elogio ao pedido | Apagar |
| Tríade forçada (*fraco*) | Ideias chegam de três em três, quer o sentido tenha três partes ou não: três itens numa frase, três exemplos paralelos, ou três fatos curtos seguidos de uma lição | Fundir ou apagar os itens que não trazem ideia distinta; manter três quando o sentido precisa de três |
| Rodízio de sinônimos (*fraco*) | Um conceito chamado por vários termos alternadamente | Reescrever com um termo só |
| Palavreado inflado (*fraco*) | "a fim de", "utilizar", "atua como", "alavancar" onde "para", "usar", "é" funcionam; um verbo mais longo no lugar de "é" ou "tem"; um substantivo com verbo fraco onde um verbo basta ("realizar a análise" em vez de "analisar") | Reescrever com a palavra simples |
| Agente oculto (*fraco*) | Uma frase na voz passiva num plano ou especificação em que o leitor precisa saber quem age | Reescrever nomeando o agente que o texto ou o contexto fornece; se nenhum for dado, listar em Perguntas |
| Fecho de slogan | Uma linha final que repete o ponto como slogan ou metáfora, ou o mesmo fecho depois de várias seções | Apagar em vez de polir |

## Não sinalize

- Uma ressalva que carrega dúvida real ("não verificado", "provável, com base em uma execução"). Removê-la faz o texto afirmar uma certeza que não tem. Uma palavra como "geralmente" que declara uma frequência real é informação, não ressalva.
- Uma ressalva, limitação, restrição ou questão em aberto, mesmo breve e sem brilho.
- Um contraste de que o leitor precisa, porque ele acredita na coisa contrastada ou porque as duas metades carregam informação.
- Uma objeção que o texto atribui ou responde, e uma alternativa que um leitor de fato consideraria, como as opções rejeitadas de um ADR.
- Uma expressão dentro de uma citação, de um título ou de um trecho que discute a própria expressão, incluindo checklists como este.
- Um termo técnico usado no sentido técnico ("estimador robusto", "idempotente").
- Repetição deliberada em instruções que o leitor executará passo a passo.
- A voz do autor: frases curtas e incisivas, um estilo pessoal consistente ou palavras que o usuário pediu.
- Gramática correta, registro formal ou layout de carta, por si sós. Isso não é evidência de slop.
- Conteúdo que o usuário pediu explicitamente, como um passo a passo completo ou uma seção de template.
