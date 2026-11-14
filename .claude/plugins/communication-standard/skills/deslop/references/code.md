# Checklist de código

## Conteúdo
- [Antes de listar cortes](#antes-de-listar-cortes)
- [Padrões](#padrões)
- [Comentários](#comentários)
- [Não sinalize](#não-sinalize)
- [Repositório inteiro](#repositório-inteiro)

## Antes de listar cortes

Leia o código que o alvo toca, não só as linhas do alvo: encontre quem chama o que você apagaria ou faria inline, e procure no repositório helpers existentes antes de dizer que algo foi reinventado. As convenções do repositório vencem este checklist. Cada linha é um sinal, não uma prova.

Julgue um corte por o leitor ter menos para guardar na cabeça depois, não pela contagem de linhas. Mover complexidade para outro lugar não é corte.

## Padrões

| Padrão | Sinal | Ação |
|---|---|---|
| Desnecessário | Código para um caso que a tarefa não tem: parâmetros não usados, opções que ninguém define, estrutura "para depois" | Apagar |
| Helper reinventado | Lógica que uma função existente no repositório já faz | Reescrever chamando o helper existente |
| Biblioteca padrão reinventada | Loops manuais, parsers, tratamento de datas ou caminhos que a linguagem já fornece | Reescrever com a chamada da biblioteca padrão correta nos casos de borda |
| Recurso da plataforma ignorado | Código próprio para o que a plataforma faz nativamente (um seletor de data em vez de `<input type="date">`, código da aplicação em vez de uma constraint do banco) | Reescrever com o recurso nativo |
| Dependência nova para poucas linhas | Um pacote adicionado para o que poucas linhas ou uma dependência já instalada resolvem | Reescrever; remover a dependência |
| Abstração especulativa | Interface com uma implementação, factory com um produto, strategy com uma estratégia, classe base com uma subclasse | Fazer inline; reintroduzir quando surgir um segundo uso |
| Repasse | Um wrapper ou método que só repassa para outro; apagá-lo não faz complexidade reaparecer em outro lugar | Apagar; chamar o destino diretamente |
| Configuração para uma constante | Uma configuração, flag ou parâmetro cujo valor nunca muda | Substituir pelo valor |
| Lógica duplicada | O mesmo bloco em vários lugares | Fundir numa função, só se as cópias precisarem mudar juntas |
| Remendo de sintoma | A mesma proteção repetida em vários chamadores | Mover uma proteção para a função compartilhada por onde os chamadores passam |
| Ramo defensivo sobre dado confiável | Uma verificação de um valor que a mesma função acabou de produzir, ou que seu tipo já garante | Apagar nesses dois casos; nos demais, perguntar, porque o valor pode cruzar uma fronteira de confiança |
| Falha silenciada | Catch genérico, cast ou supressão de tipo que esconde um erro em vez de tratá-lo | Perguntar, não cortar: removê-lo muda o comportamento |
| Código morto | Ramos inalcançáveis, exports não usados, nomes `_unused`, shims internos sem chamador | Apagar |
| Aninhamento profundo | Vários níveis de condicionais que retornos antecipados achatariam | Reescrever com retornos antecipados |
| Densidade esperta | Um one-liner que precisa de comentário para ser entendido | Reescrever na forma simples, mesmo que mais longa |

## Comentários

| Padrão | Sinal | Ação |
|---|---|---|
| Narrar o quê | Um comentário que repete a próxima linha | Apagar |
| Docstring em helper privado trivial | Uma docstring de várias linhas que repete o nome e a assinatura de uma função privada curta | Apagar, ou reduzir a uma linha de porquê |
| Banner de seção | Comentários divisores decorativos num arquivo curto | Apagar |
| Histórico | "refatorado de", "movido para cá de", "agora usa", "anteriormente" | Reescrever com o motivo atual, ou apagar |
| ID de tracker ou processo | IDs de ticket, PR, especificação, ADR ou achado de revisão em código, comentários, docstrings, testes ou campos `description` de infraestrutura | Manter o raciocínio, apagar o ID; apagar o comentário se ele só citava o ID |
| Referência à tarefa | "adicionado para este ticket", "conforme o pedido", "para o novo fluxo" | Reescrever como o motivo duradouro, ou apagar |
| Código comentado | Código desativado guardado "por via das dúvidas" | Apagar; o git o guarda |
| TODO de fachada | "TODO: adicionar tratamento de erro" | Perguntar: fazer o trabalho ou apagar o comentário |
| Atalho sem gatilho | Um comentário `atalho:` que não nomeia o limite, ou o ponto em que evoluir | Reescrever nomeando os dois, ou perguntar ao autor |

## Não sinalize

- Validação de entrada em fronteiras de confiança, e tratamento de erro que evita perda de dados ou falha silenciosa.
- Verificações de segurança, autorização, tratamento de segredos e atributos de acessibilidade.
- Um comentário que explica por que um código não óbvio está correto.
- Uma abstração com dois ou mais usos reais, ou que existe como ponto de injeção para testes que os testes usam.
- Um helper cujo nome dá nome a um conceito, mesmo com um só chamador.
- Um adaptador de compatibilidade mantido para chamadores fora do repositório, como uma API pública ou uma migração em andamento.
- O teste pequeno ou a verificação executável que falha se uma lógica não trivial quebrar.
- Um valor de calibração ou configuração que o mundo físico ou a operação de fato ajusta.
- Um comentário `atalho:` que nomeia um limite deliberado e quando evoluir, como `# atalho: um lock global; locks por conta se o throughput importar`.
- Numa auditoria de diff, IDs e comentários em linhas que a mudança não tocou.
- Código que o usuário pediu explicitamente.

## Repositório inteiro

Para um repositório ou diretório grande demais para ler por inteiro, mapeie primeiro a árvore e os manifestos de dependências, depois procure estes itens e leia por inteiro só os arquivos que eles apontarem: dependências que a biblioteca padrão ou a plataforma já fornecem (compare cada dependência do manifesto com o que o código chama dela e com a biblioteca padrão da versão da linguagem em uso), interfaces com uma implementação, factories com um produto, wrappers que só delegam, arquivos que exportam uma única coisa, flags e configurações mortas, e código caseiro que a biblioteca padrão já resolve.

Ordene os cortes do maior para o menor e termine o relatório com o total: `líquido: -<N> linhas, -<M> dependências possíveis.`
