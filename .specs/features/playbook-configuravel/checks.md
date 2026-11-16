# Playbook configurável checks

Build: `make build`; `make lint`
Run server: `make test-server`
Run web: `make test-web`
Plan: `.specs/features/playbook-configuravel/plan.md`

99 checks in 4 slices · 8 one-way doors · 0 open, of which 0 block

## Checks

### S1 - A pessoa entra e chega ao negócio certo

**C1** - Sem sessão, qualquer tela leva a `/entrar`, com "Entrar com Google" e o campo de e-mail (AC 1)
Proof: run web | ✓ [C01] sem sessão qualquer tela vai para /entrar com Google e campo de e-mail

**C2** - Pedir o link envia o e-mail por SMTP, responde igual para e-mail desconhecido, mostra "Enviamos um link de acesso para <e-mail>." e recusa e-mail sem "@" com "Informe um e-mail válido." (AC 2)
Proof: run server | PASSED [C02a] pedir o link envia o e-mail e responde igual para e-mail desconhecido
Proof: run web | ✓ [C02b] e-mail sem arroba mostra Informe um e-mail válido e não chama a API
Proof: run web | ✓ [C02c] pedido aceito mostra Enviamos um link de acesso para o e-mail

**C3** - Segundo pedido do mesmo e-mail em menos de 60 s responde `429` `too_many_requests`, não envia e mostra "Aguarde 1 minuto para pedir outro link." (AC 3)
Proof: run server | PASSED [C03a] segundo pedido em menos de 60 s responde 429 e não envia
Proof: run web | ✓ [C03b] resposta 429 mostra Aguarde 1 minuto para pedir outro link

**C4** - Abrir o link mostra "Entrar como <e-mail>?" com "Entrar" e não consome o token (AC 4)
Proof: run server | PASSED [C04a] ler o link duas vezes não consome o token
Proof: run web | ✓ [C04b] página do link mostra Entrar como o e-mail e o botão Entrar

**C5** - "Entrar" envia o token em `POST /login/ott`, que o consome e cria a sessão (AC 5)
Proof: run server | PASSED [C05] POST /login/ott consome o token e cria a sessão
Proof: run web | ✓ [C05b] Entrar envia o token por POST /login/ott

**C6** - Token consumido ou com mais de 15 minutos não cria sessão e mostra "Este link já foi usado ou expirou. Peça um novo." (AC 6)
Proof: run server | PASSED [C06a] token aos 14 min entra, reuso e 16 min são recusados
Proof: run web | ✓ [C06b] link recusado mostra Este link já foi usado ou expirou

**C7** - Conta Google sem e-mail verificado não cria sessão e mostra "Seu e-mail do Google não está verificado." (AC 7)
Proof: run server | PASSED [C07a] Google sem e-mail verificado não cria sessão
Proof: run web | ✓ [C07b] erro google_unverified mostra Seu e-mail do Google não está verificado

**C8** - Google com autoridade (`@gmail.com` ou `hd`) entra pelo `google_subject` ou pelo e-mail, gravando o `google_subject` (AC 8)
Proof: run server | PASSED [C08] Google com gmail ou hd entra e liga a identidade pelo e-mail

**C9** - Google sem autoridade grava o desafio, envia o link e mostra "Confirme seu e-mail: enviamos um link para <e-mail>.", sem sessão nem consulta a `allowed_signups` (AC 9)
Proof: run server | PASSED [C09a] Google fora do Gmail sem hd grava desafio, envia link e não cria sessão
Proof: run web | ✓ [C09b] confirmação pendente mostra Confirme seu e-mail

**C10** - Consumir o desafio grava o `google_subject` e cria a sessão na mesma transação; dois consumos simultâneos deixam um só vencedor (AC 10)
Proof: run server | PASSED [C10a] consumir o desafio grava google_subject e cria a sessão
Proof: run server | PASSED [C10b] dois consumos simultâneos do desafio têm um só vencedor

**C11** - Link comum para o mesmo e-mail não liga a identidade pendente; desafio vencido ou consumido é recusado sem gravar nada (AC 11)
Proof: run server | PASSED [C11] link comum não liga identidade pendente e desafio vencido é recusado

**C12** - Pessoa liberada e sem associação cria o negócio (nomes de 1 a 80 caracteres) como `gestor` e abre `/n/<businessId>/playbook` (AC 12)
Proof: run server | PASSED [C12a] criar negócio liberado cria gestor e playbook e valida 1 a 80 caracteres
Proof: run web | ✓ [C12b] formulário de criação abre /n/id/playbook

**C13** - Pessoa não liberada vê "Seu acesso ainda não foi liberado." e `POST /api/businesses` responde `403` `signup_not_allowed` (AC 13)
Proof: run server | PASSED [C13a] e-mail fora da lista responde 403 signup_not_allowed
Proof: run web | ✓ [C13b] sem liberação mostra Seu acesso ainda não foi liberado

**C14** - Quem já é membro recebe `409` `already_member` ao criar negócio (AC 14)
Proof: run server | PASSED [C14] membro que cria negócio recebe 409 already_member

**C15** - Duas criações simultâneas da mesma pessoa criam um negócio e um `409` (AC 15)
Proof: run server | PASSED [C15] duas criações simultâneas criam um negócio e um 409

**C16** - Ao entrar, o produto abre o último negócio usado ainda válido ou o único negócio (AC 16)
Proof: run server | PASSED [C16a] /api/me devolve o último negócio usado
Proof: run web | ✓ [C16b] entrada abre o último negócio válido ou o único

**C17** - Membro de mais de um negócio sem último válido vê a lista e abre o escolhido (AC 17)
Proof: run web | ✓ [C17] mais de um negócio sem último válido mostra a lista e abre o escolhido

**C18** - "Trocar de negócio" aparece só para membro de mais de um negócio e abre a lista (AC 18)
Proof: run web | ✓ [C18] Trocar de negócio aparece só com mais de um negócio e abre a lista

**C19** - Comando com `businessId` sem associação ou `playbookId` de outro negócio responde `404`; a gravação da aba 1 vai para o negócio do endereço dela (AC 19)
Proof: run server | PASSED [C19] businessId sem associação ou playbookId de outro negócio responde 404

**C20** - Com o papel `app`, uma consulta só vê o negócio de `app.business_id`; sem a variável, erro e 0 linhas (AC 20)
Proof: run server | PASSED [C20] RLS do papel app devolve só o negócio da variável

**C21** - Linha filha com `business_id` diferente do playbook é recusada pela chave composta (AC 21)
Proof: run server | PASSED [C21] chave composta recusa business_id divergente

**C22** - Membro `closer` recebe `403` nas rotas de escrita (AC 22)
Proof: run server | PASSED [C22] closer recebe 403 nas escritas

**C23** - Menu com Nova call, Ao vivo, Calls, Coach e Playbook, nessa ordem, Playbook em destaque e os quatro primeiros com "Disponível em breve" (AC 23)
Proof: run web | ✓ [C23] menu com cinco itens na ordem e Disponível em breve

**C24** - "Sair" invalida a sessão e abre `/entrar`; o cookie antigo recebe `401` (AC 24)
Proof: run server | PASSED [C24a] Sair invalida a sessão e o cookie antigo recebe 401
Proof: run web | ✓ [C24b] Sair abre /entrar

**C25** - Sessão sem uso por 7 dias ou com 30 dias desde o login é encerrada (AC 25)
Proof: run server | PASSED [C25] sessão expira com 7 dias sem uso ou 30 dias desde o login

**C26** - A sessão fica no Postgres e continua válida para um servidor novo (AC 26)
Proof: run server | PASSED [C26] sessão gravada no Postgres vale para outra instância do repositório

**C91** - Sem o cookie `XSRF-TOKEN`, o app web pede o token ao servidor antes do primeiro POST e o envia em `X-XSRF-TOKEN` (AC 2, AC 5; achado na construção: a tela `/entrar` não faz GET antes de pedir o link)
Proof: run web | ✓ [C91] sem cookie XSRF-TOKEN busca o token antes do primeiro POST e o envia

### S2 - O gestor escreve e edita o playbook

**C27** - Rascunho vazio sem versão mostra "Playbook", o subtítulo e "Escrever o playbook" e "Importar um documento" (AC 27)
Proof: run web | ✓ [C27] rascunho vazio mostra título, subtítulo e os dois jeitos de começar

**C28** - "Escrever o playbook" mostra os quatro blocos vazios com "+ Etapa", "+ Informação", "+ Objeção" e "+ Plano" (AC 28)
Proof: run web | ✓ [C28] Escrever o playbook mostra os quatro blocos vazios com os botões

**C29** - A tela com conteúdo corresponde a `.specs/features/playbook-configuravel/captures/tela-6-playbook.png`, menos as diferenças do plano (AC 29)
Proof: browser - abrir `/n/<businessId>/playbook` com um playbook de exemplo, janela 1440 x 1300, salvar `captures/tela-6-playbook.running.png`

**C30** - "+ Etapa" acrescenta a etapa ao fim com o número seguinte e salva ao sair do campo (AC 30)
Proof: run web | ✓ [C30] + Etapa acrescenta ao fim numerada e salva ao sair do campo

**C31** - As setas trocam a etapa com a vizinha, renumeram e salvam; as setas das pontas ficam desabilitadas (AC 31)
Proof: run web | ✓ [C31] setas trocam a etapa, renumeram, salvam e desabilitam nas pontas

**C32** - "+ Informação" acrescenta a linha com "Informação" e "Pergunta sugerida" ao fim do bloco (AC 32)
Proof: run web | ✓ [C32] + Informação acrescenta a linha ao fim do bloco

**C33** - "+ Objeção" acrescenta a linha com os três campos; "Como o lead costuma dizer" é opcional (AC 33)
Proof: run web | ✓ [C33] + Objeção acrescenta a linha com os três campos

**C34** - "+ Plano" acrescenta a linha com plano, coluna extra, à vista e parcelado ao fim da tabela (AC 34)
Proof: run web | ✓ [C34] + Plano acrescenta a linha ao fim da tabela

**C35** - Clicar numa linha a deixa editável com os valores atuais e cada campo salva ao sair (AC 35)
Proof: run web | ✓ [C35] clicar numa linha edita no lugar e salva cada campo ao sair

**C36** - "Remover" tira a linha na hora, sem confirmação, e renumera etapas (AC 36)
Proof: run web | ✓ [C36] Remover tira a linha sem confirmação e renumera as etapas

**C37** - Texto acima do limite (120 ou 500) responde `422` com o erro do campo, mostra "Máximo de <N> caracteres." e mantém o valor salvo (AC 37)
Proof: run server | PASSED [C37a] texto acima de 120 ou 500 caracteres responde 422 com o campo
Proof: run web | ✓ [C37b] erro de limite mostra Máximo de N caracteres e mantém o valor salvo

**C38** - Bloco no limite (30, 50, 50, 20) desabilita o "+" com "Limite de <N> itens neste bloco."; o servidor recusa com `422` (AC 38)
Proof: run server | PASSED [C38a] rascunho acima do limite de itens responde 422
Proof: run web | ✓ [C38b] bloco no limite desabilita o botão e mostra Limite de N itens

**C39** - Pergunta, fala do lead e sugestão aparecem entre aspas fora da edição e são guardadas sem aspas (AC 39)
Proof: run web | ✓ [C39] textos aparecem entre aspas e são guardados sem aspas

**C40** - Falha de gravação mantém o valor digitado, mostra "Não foi possível salvar. Tente de novo." e oferece "Tentar de novo" e "Desfazer" (AC 40)
Proof: run web | ✓ [C40] falha ao salvar mantém o valor e oferece Tentar de novo e Desfazer

**C41** - Revisão desatualizada responde `409` `revision_conflict`, não grava e mostra "O playbook foi alterado em outra aba. Recarregue para continuar." (AC 41)
Proof: run server | PASSED [C41a] gravação com revisão antiga responde 409 revision_conflict
Proof: run web | ✓ [C41b] revision_conflict mostra O playbook foi alterado em outra aba

**C42** - Com importação ativa, o rascunho fica só para leitura com "Importando…" e a gravação responde `409` `import_running` (AC 42)
Proof: run server | PASSED [C42a] gravação com importação ativa responde 409 import_running
Proof: run web | ✓ [C42b] importação ativa deixa o rascunho só para leitura

**C43** - Gravação e pedido de importação simultâneos nunca deixam uma gravação entrar depois da tarefa criada (AC 43)
Proof: run server | PASSED [C43] gravação e importação simultâneas nunca gravam depois da tarefa

**C44** - O nome da coluna extra vira o título da coluna entre "Plano" e "À vista" (AC 44)
Proof: run web | ✓ [C44] nome da coluna extra vira o título entre Plano e À vista

**C45** - Coluna extra sem nome e sem valores some fora da edição (AC 45)
Proof: run web | ✓ [C45] coluna extra vazia some fora da edição

**C46** - À vista aceita valor em reais maior que zero até R$ 10.000.000,00 com até duas casas, e recusa o resto com "Informe um valor em reais maior que zero." (AC 46)
Proof: run server | PASSED [C46a] à vista aceita 4500 e 4500,50 e recusa 0, 10000000,01 e três casas
Proof: run web | ✓ [C46b] campo à vista converte 4.500,50 e recusa abc com a mensagem

**C47** - Parcelado fora de 2 a 60 parcelas ou com valor não positivo é recusado com "Parcelas de 2 a 60, com valor maior que zero."; vazio aparece como "—" (AC 47)
Proof: run server | PASSED [C47a] parcelado aceita 2 a 60 parcelas positivas e recusa o resto
Proof: run web | ✓ [C47b] parcelado inválido mostra a mensagem e vazio aparece como travessão

**C48** - À vista aparece "R$ <valor>" e parcelado "<n>x <valor>", com centavos só quando diferentes de zero (AC 48)
Proof: run web | ✓ [C48] formata R$ 4.500, 6x 1.166,67 e 3x 1.500

**C49** - O desconto máximo é um valor único em reais e aparece "Desconto máximo na call: R$ <valor>" (AC 49)
Proof: run web | ✓ [C49] desconto informado aparece como Desconto máximo na call R$ valor

**C50** - Sem desconto e com plano, aparece "Desconto máximo na call:" com o selo "a definir" (AC 50)
Proof: run web | ✓ [C50] sem desconto mostra o selo a definir

**C51** - Desconto negativo ou maior ou igual ao menor à vista é recusado com a mensagem do menor à vista (AC 51)
Proof: run server | PASSED [C51a] desconto 4499,99 aceito, 4500 recusado e 0 aceito com menor à vista 4500
Proof: run web | ✓ [C51b] desconto inválido mostra a mensagem com o menor à vista

**C52** - Sem plano, a linha do desconto não aparece (AC 52)
Proof: run web | ✓ [C52] sem plano a linha do desconto não aparece

**C53** - Sem call registrada, a coluna "Nas calls" não aparece (AC 53)
Proof: run web | ✓ [C53] sem call registrada a coluna Nas calls não aparece

### S3 - O gestor publica uma versão

**C54** - Publicar na revisão atual, com o mínimo, linhas completas, desconto válido e sem marca, cria a versão N+1 a partir de 1 e o topo mostra "Playbook: <nome> · versão <N+1>" (AC 54)
Proof: run server | PASSED [C54a] publicar cria a versão N+1 começando em 1
Proof: run web | ✓ [C54b] após publicar o topo mostra a versão nova

**C55** - Publicar com revisão antiga responde `409` `revision_conflict` e não cria versão (AC 55)
Proof: run server | PASSED [C55a] publicar com revisão antiga responde 409 e não cria versão
Proof: run web | ✓ [C55b] publicar com revision_conflict mostra O playbook foi alterado em outra aba

**C56** - Sem etapa, informação ou objeção responde `422` `publish_minimum` com a mensagem; tabela de preços vazia publica (AC 56)
Proof: run server | PASSED [C56a] sem etapa, informação ou objeção responde 422 publish_minimum e tabela vazia publica
Proof: run web | ✓ [C56b] publish_minimum mostra a mensagem do mínimo

**C57** - Linha incompleta responde `422` `incomplete_rows` com os `id`, destaca as linhas e mostra "Complete ou remova as linhas destacadas." (AC 57)
Proof: run server | PASSED [C57a] linha incompleta responde 422 incomplete_rows com os ids
Proof: run web | ✓ [C57b] incomplete_rows destaca as linhas e mostra Complete ou remova

**C58** - Desconto fora da regra responde `422` `discount_invalid` com a mensagem do desconto (AC 58)
Proof: run server | PASSED [C58] desconto fora da regra responde 422 discount_invalid

**C59** - Plano ou desconto marcado responde `422` `pending_review` e mostra "Confira os valores marcados "Importado · conferir" antes de publicar." (AC 59)
Proof: run server | PASSED [C59a] valor marcado a conferir responde 422 pending_review
Proof: run web | ✓ [C59b] pending_review mostra Confira os valores marcados

**C60** - Campo salvando ou com falha desabilita "Publicar" com "Há alterações não salvas. Salve ou desfaça antes de publicar." (AC 60)
Proof: run web | ✓ [C60] alteração pendente desabilita Publicar com Há alterações não salvas

**C61** - Rascunho diferente da versão mostra "Alterações não publicadas"; igual, "Publicar" desabilitado e a rota responde `409` `nothing_to_publish` (AC 61)
Proof: run server | PASSED [C61a] rascunho igual à versão responde 409 nothing_to_publish
Proof: run web | ✓ [C61b] Alterações não publicadas aparece só com diferença e Publicar desabilita sem ela

**C62** - Sem versão, o topo mostra "Playbook: <nome> · rascunho" (AC 62)
Proof: run web | ✓ [C62] sem versão o topo mostra rascunho

**C63** - `UPDATE` e `DELETE` em `playbook_versions` com o papel `app` falham por permissão (AC 63)
Proof: run server | PASSED [C63] UPDATE e DELETE em versões falham para o papel app

**C64** - A versão nova mantém o `id` dos itens que já existiam (AC 64)
Proof: run server | PASSED [C64] versão nova mantém os ids dos itens

### S4 - O gestor importa o playbook de um documento

**C65** - "Importar documento" oferece arquivo .docx, .pdf, .txt ou .md, texto colado e link do Google Docs (AC 65)
Proof: run web | ✓ [C65] Importar documento oferece arquivo, texto colado e link do Google Docs

**C66** - Enviar a origem responde `202`, cria a tarefa `queued` e mostra "Importando…" com o rascunho só para leitura e "Importar documento" e "Publicar" desabilitados (AC 66)
Proof: run server | PASSED [C66a] enviar a origem responde 202 e cria a tarefa queued
Proof: run web | ✓ [C66b] envio mostra Importando com rascunho só leitura e botões desabilitados

**C67** - Com tarefa `queued` ou `running`, quem abre a tela vê "Importando…" (AC 67)
Proof: run server | PASSED [C67a] GET do playbook devolve a tarefa ativa
Proof: run web | ✓ [C67b] tela aberta com tarefa ativa mostra Importando

**C68** - A importação acrescenta ao fim de cada bloco sem mudar o que existia e mostra "Importado: <a> etapas, <b> informações, <c> objeções, <d> planos." (AC 68)
Proof: run server | PASSED [C68a] importação acrescenta ao fim sem mudar o existente e conta os itens
Proof: run web | ✓ [C68b] importação concluída mostra Importado com as contagens

**C69** - Plano ou desconto importado chega marcado "Importado · conferir" com o trecho de origem; etapas, informações e objeções sem marca (AC 69)
Proof: run server | PASSED [C69a] plano e desconto importados chegam marcados com o trecho e o resto sem marca
Proof: run web | ✓ [C69b] valor marcado mostra Importado · conferir e o trecho

**C70** - "Conferido" ou editar o valor tira a marca do item (AC 70)
Proof: run web | ✓ [C70] Conferido ou edição tira a marca do item

**C71** - Valor em dinheiro que não aparece no texto é descartado e o campo fica vazio (AC 71)
Proof: run server | PASSED [C71] valor ausente do texto é descartado e presente é importado

**C72** - Desconto e nome da coluna vazios no rascunho recebem o valor do documento (AC 72)
Proof: run server | PASSED [C72] desconto e nome da coluna vazios recebem o valor do documento

**C73** - Desconto ou nome da coluna diferentes mantêm o atual e o resultado avisa a divergência (AC 73)
Proof: run server | PASSED [C73] desconto e coluna diferentes ficam com o atual e geram aviso
Proof: run web | ✓ [C73b] avisos da importação aparecem na mensagem do resultado

**C74** - Extensão não aceita responde `415` `unsupported_format` com "Formato não aceito. Envie .docx, .pdf, .txt ou .md." (AC 74)
Proof: run server | PASSED [C74] extensão não aceita responde 415 sem criar tarefa
Proof: run web | ✓ [C74b] mensagens da importação por código de erro

**C75** - Arquivo acima de 10 MB responde `413` `file_too_large` com "O arquivo passa de 10 MB." (AC 75)
Proof: run server | PASSED [C75] arquivo acima de 10 MB responde 413 sem criar tarefa
Proof: run web | ✓ [C74b] mensagens da importação por código de erro

**C76** - Com importação ativa, outro pedido responde `409` `import_running` (AC 76)
Proof: run server | PASSED [C76] segunda importação com tarefa ativa responde 409

**C77** - Texto extraído acima de 100.000 caracteres encerra em `failed` sem chamar o modelo (AC 77); ao modelo vai só o corpo do documento em XHTML, sem os metadados, e documento sem texto no corpo termina em `nothing_found` sem chamar o modelo
Proof: run server | PASSED [C77] texto acima de 100 mil caracteres falha sem chamar o modelo
Proof: run web | ✓ [C74b] mensagens da importação por código de erro

**C78** - PDF sem texto selecionável encerra em `failed` sem chamar o modelo (AC 78)
Proof: run server | PASSED [C78] PDF sem texto falha sem chamar o modelo
Proof: run web | ✓ [C74b] mensagens da importação por código de erro

**C79** - Link que não é Google Docs ou documento não público encerra em `failed` sem mudar o rascunho (AC 79)
Proof: run server | PASSED [C79] link que não é Google Docs ou não público falha sem mudar o rascunho
Proof: run web | ✓ [C74b] mensagens da importação por código de erro

**C80** - Documento sem nenhum item encerra em `failed` sem mudar o rascunho (AC 80)
Proof: run server | PASSED [C80] documento sem itens falha sem mudar o rascunho
Proof: run web | ✓ [C74b] mensagens da importação por código de erro

**C81** - Falha, demora acima de 120 s ou JSON fora do schema repete até 3 tentativas e então encerra em `failed` sem mudar o rascunho (AC 81)
Proof: run server | PASSED [C81] três falhas do modelo encerram a tarefa em failed sem mudar o rascunho
Proof: run server | PASSED [C81b] resposta do modelo depois do tempo limite conta como falha e repete até 3 vezes
Proof: run web | ✓ [C74b] mensagens da importação por código de erro

**C82** - Bloco acima do limite importa até o limite, na ordem, e avisa "<n> itens ficaram de fora pelo limite do bloco." (AC 82)
Proof: run server | PASSED [C82] bloco acima do limite importa até o limite e avisa os itens de fora

**C83** - Linhas importadas, revisão nova e estado final gravam na mesma transação; sem marcar a tarefa, nenhuma linha entra (AC 83)
Proof: run server | PASSED [C83] conclusão que não marca a tarefa não grava linhas

**C84** - Execução com concessão vencida não altera rascunho nem tarefa depois que outra assumiu (AC 84)
Proof: run server | PASSED [C84] execução com concessão vencida não conclui depois de outra assumir

**C85** - Tarefa `running` de um servidor reiniciado é reprocessada quando a concessão vence, sem duplicar linhas (AC 85)
Proof: run server | PASSED [C85] concessão vencida é reprocessada sem duplicar linhas

**C86** - `claim_next_import_job()` entrega tarefas de dois negócios e o papel `app` só vê as do seu negócio (AC 86)
Proof: run server | PASSED [C86] claim entrega tarefas de dois negócios e o papel app só vê as suas

**C87** - Ao terminar em `done` ou `failed`, o arquivo é apagado depois do commit e o texto extraído não é gravado (AC 87); um .docx que passa pelo processo de extração também conclui sem que nenhum arquivo da pasta de importação ou das pastas de extração contenha o texto, nem durante a extração (o resultado volta pela saída padrão do filho, AD-063), e nenhuma pasta de extração sobra
Proof: run server | PASSED [C87] arquivo da tarefa é apagado depois do commit e o texto não é gravado

**C88** - A varredura apaga arquivos de tarefas encerradas e órfãos com mais de 1 hora (AC 88)
Proof: run server | PASSED [C88] varredura apaga arquivos encerrados e órfãos com mais de 1 hora

**C89** - Toda chamada de importação ao OpenRouter leva `provider.zdr = true` (AC 89)
Proof: run server | PASSED [C89] chamada ao OpenRouter leva provider.zdr true

**C90** - Nenhum módulo do servidor usa o interno de outro; só o `api/` é público (Landing 1, AD-055)
Proof: run server | PASSED [C90] módulos do servidor só usam o api de outros módulos

### Revisão de código (Codex)

**C92** - "Importar documento" fica desabilitado enquanto há campo salvando ou com falha, para a importação não descartar o que não foi salvo (AC 40, AC 66)
Proof: run web | ✓ [C92] Importar documento fica desabilitado com alteração não salva ou com falha

**C93** - Falha ao gravar uma operação de linha (acrescentar, remover, reordenar) mostra "Não foi possível salvar. Tente de novo." com "Tentar de novo" e "Desfazer" (AC 36, AC 40)
Proof: run web | ✓ [C93] falha ao remover uma linha mostra a mensagem com Tentar de novo e Desfazer

**C94** - Voltar pelo histórico a outro negócio recria a tela, e as gravações vão para o negócio do endereço (AC 19, AD-045)
Proof: run web | ✓ [C94] voltar pelo histórico a outro negócio recria a tela e grava no negócio do endereço

**C95** - E-mail válido de exatamente 320 caracteres entra e a sessão é gravada com ele; com 321, `422` `invalid_email` (AC 5, AC 26)
Proof: run server | PASSED [C95] e-mail válido de até 320 caracteres entra e grava a sessão

**C96** - Rascunho nulo ou com item nulo responde `422` com `invalid_document`, e plano com `id` nulo responde `422` com os erros de campo, sem `500`; nada é gravado (AC 37, Surface do PUT)
Proof: run server | PASSED [C96] rascunho nulo ou com item nulo responde 422 invalid_document sem gravar

**C97** - Erros do próprio Spring (JSON malformado, tipo de conteúdo, método, parâmetro ausente) upload sem a parte `file` (`missing_parameter`), `Accept` sem formato disponível (`406` `not_acceptable`) e endereço inexistente (`404` `not_found`) saem em `application/problem+json` com `code`, `type` e `detail` em português (AD-057)
Proof: run server | PASSED [C97] erros do Spring saem em RFC 9457 com code e type

**C98** - Servidor de e-mail fora do ar faz o pedido de link responder `503` `mail_unavailable` em RFC 9457 (AC 2, AD-057)
Proof: run server | PASSED [C98] servidor de e-mail fora do ar responde 503 mail_unavailable em RFC 9457

**C99** - `deploy/` tem Docker Compose válido com os serviços `postgres`, `server` e `caddy`, e o Caddy rodando com o Caddyfile real entrega ao servidor, com o mesmo método, endereço e corpo, e um upload de 9 MB inteiro, `/api/*`, `/ott/*`, `/login/ott`, `/login/identity-link`, `/logout`, `/oauth2/*` e `/login/oauth2/*`, em todas as rotas do Surface com o método de cada uma, e em qualquer caminho sob esses prefixos com GET, POST, PUT, PATCH e DELETE, e o `index.html` do app nas rotas do app (`/`, `/negocios`, `/n/b1/playbook`, `/entrar`, `/entrar/link`, `/entrar/vincular`) (Landing 1, AD-004, AD-033)
Proof: `sh deploy/verify.sh`

**C100** - Remover a linha que tem erro local apaga o erro e libera "Publicar" e "Importar documento" depois da gravação (AC 36, AC 46, AC 60)
Proof: run web | ✓ [C100] remover a linha com erro local libera Publicar e Importar documento

**C101** - Requisição que recebe `401` `unauthenticated` durante a edição leva a pessoa a `/entrar`, sem mostrar falha de gravação (AC 25, AD-039)
Proof: run web | ✓ [C101] sessão expirada durante a edição leva a /entrar na requisição seguinte

**C102** - Valor de parcela acima de R$ 10.000.000,00 é aceito na tela, como no servidor; o teto vale só para o preço à vista (AC 47)
Proof: run web | ✓ [C102] parcela acima de R$ 10 milhões é aceita, sem o teto do preço à vista

**C103** - Erro recusado antes do MVC, por HTTP real, sai em `application/problem+json` com `code`, `type`, `detail` e `instance` igual ao endereço enviado: firewall do Spring Security (`/api//me`, `/api/me;extra`) `400` `request_rejected`; `TRACE` `405` `method_not_allowed`; endereço que o Tomcat recusa (`%2F`, `%00`, `%zz`) `400` `invalid_request`; byte não ASCII cru no caminho `400` `invalid_request` com `instance` `/`, porque o Tomcat recusa a linha do pedido antes de guardar o caminho (AD-057)
Proof: run server | PASSED [C103] erro recusado antes do MVC sai em RFC 9457 com code e type

**C104** - O script de papéis cria `migrator`, `app` e `job_claimer` com senhas que têm apóstrofo, aspas e ponto e vírgula, e `migrator` e `app` entram pela rede (`scram-sha-256`, de outro contêiner) com a própria senha e são recusados com uma senha errada, e `job_claimer` não tem login; a prova não deixa volume do Postgres para trás (Landing 1, AD-037)
Proof: `sh deploy/verify-roles.sh`

**C105** - Importação que começa com uma linha em edição fecha a linha, e nenhum campo da tela fica editável (AC 42, AC 66, AD-036)
Proof: run web | ✓ [C105] importação iniciada em outra aba fecha a linha aberta e nenhum campo fica editável

**C106** - Valor em dinheiro da resposta do modelo chega com todos os dígitos (`90071992547409.91`, `4500.10`), sem passar por ponto flutuante (AD-025, AC 71)
Proof: run server | PASSED [C106] valor em dinheiro da resposta do modelo chega com todos os dígitos

**C107** - Sem plano, o desconto e a marca dele ficam guardados: a importação preenche o desconto vazio com a marca "Importado · conferir" mesmo sem plano (AC 72), a publicação sem plano não responde `pending_review` por causa dele e grava a versão sem o desconto e sem a marca, com o rascunho igual à versão para "alterações não publicadas" e "nada a publicar" (AD-062), e com um plano a marca volta a bloquear até "Conferido" (AC 52, AC 59, AD-061)
Proof: run server | PASSED [C107] desconto sem plano fica guardado e só bloqueia a publicação quando há plano

**C108** - Enquanto a publicação está em andamento, nenhuma linha abre para edição e nenhum campo da tela fica editável; depois dela a edição volta (AC 35, AC 54)
Proof: run web | ✓ [C108] durante a publicação nenhuma linha abre para edição e nada digitado se perde

**C109** - "Desfazer" fica desabilitado enquanto outra gravação está em andamento, e a tela termina igual ao rascunho gravado (AC 40, AC 54)
Proof: run web | ✓ [C109] Desfazer fica indisponível enquanto outra gravação está em andamento

**C110** - Uma consulta da importação que falha (`503`) não encerra o acompanhamento: a tela consulta de novo e sai de "Importando…" quando a tarefa termina; ao sair da tela com uma consulta em andamento, nenhuma consulta nova é feita (AC 67, AC 68)
Proof: run web | ✓ [C110] consulta da importação que falha uma vez continua até a tarefa terminar

**C111** - Voltar um campo ao valor gravado apaga o erro do servidor daquele rascunho e libera "Publicar" e "Importar documento" (AC 37, AC 60)
Proof: run web | ✓ [C111] voltar o campo ao valor gravado apaga o erro do servidor e libera Publicar

**C24c** - "Sair" aparece na tela de acesso não liberado, na de criação de negócio e na lista com os negócios exibidos, e leva a `/entrar` (AC 24, AD-059)
Proof: run web | ✓ [C24c] Sair aparece no acesso não liberado, na criação de negócio e na lista e leva a /entrar

**C112** - Remover o último plano apaga o erro local do desconto, cuja linha some (AC 52), e libera "Publicar" e "Importar documento" (AC 46, AC 60)
Proof: run web | ✓ [C112] remover o último plano apaga o erro do desconto e libera Publicar e Importar documento

**C113** - Importação aceita com `202` deixa a tela em "Importando…" e o rascunho só leitura na hora ("Publicar", "+ Etapa" e "Importar documento" desabilitados, nenhuma linha abre), e o acompanhamento continua mesmo se a primeira consulta falhar (`503`) (AC 42, AC 66, AC 67, AC 68)
Proof: run web | ✓ [C113] importação aceita com 202 fica em Importando e é acompanhada mesmo se a primeira consulta falhar

**C114** - Número de parcelas fracionário (`60.9`) responde `400` `malformed_request` sem gravar, e `6.0` é gravado como `6` (AC 47, AD-023)
Proof: run server | PASSED [C114] número de parcelas fracionário é recusado sem gravar e 6.0 vale 6

**C115** - O e-mail exige STARTTLS e confere o certificado: com um servidor SMTP que não anuncia STARTTLS, o envio falha antes de mandar `AUTH`, `MAIL FROM` ou `DATA` (AD-039, Landing 1)
Proof: run server | PASSED [C115] SMTP sem STARTTLS recusa o envio antes de mandar senha ou mensagem

**C116** - Exportação do Google Docs acima de 10 MB não é baixada inteira (de uma de 200 MB o servidor entrega menos de 64 MB antes de o cliente parar) e a tarefa falha com `text_too_long`, sem chamar o modelo (AC 77, AC 79)
Proof: run server | PASSED [C116] exportação do Google Docs acima de 10 MB falha com text_too_long sem chamar o modelo

**C117** - Uma tabela temporária `import_jobs` criada pelo papel `app` não substitui a real em `claim_next_import_job` nem em `import_job_statuses`: as funções não devolvem a linha temporária nem a alteram (AD-037, AD-049)
Proof: run server | PASSED [C117] tabela temporária import_jobs do papel app não substitui a real nas funções privilegiadas

**C118** - O GET do playbook lê rascunho, revisão e tarefa de importação no mesmo snapshot: com a importação concluída entre a leitura do rascunho e a da tarefa, a resposta traz a tarefa e o rascunho de antes, e o GET seguinte traz os dois de depois (AC 68, AD-038)
Proof: run server | PASSED [C118] GET do playbook lê rascunho e tarefa no mesmo snapshot mesmo com a importação concluindo no meio

**C119** - Uma gravação de outro campo que termina enquanto a pessoa digita no parcelado não apaga a digitação (AC 35, AD-016)
Proof: run web | ✓ [C119] gravação de outro campo que termina enquanto a pessoa digita no parcelado não apaga a digitação

**C120** - `409` `import_running` ao gravar ou ao publicar deixa a tela em "Importando…" com o rascunho só leitura, e o acompanhamento continua mesmo se a consulta seguinte falhar (`503`) (AC 42, AC 67, AD-036)
Proof: run web | ✓ [C120] 409 import_running ao gravar ou publicar deixa a tela em Importando e acompanha mesmo com a consulta falhando

**C121** - A extração de .docx e .pdf roda num processo separado (AD-063): um .docx de menos de 10 MB que expande para 600 MB esgota só esse processo, a tarefa termina em `import_failed` sem chamar o modelo, o arquivo é apagado, o servidor não registra erro próprio e a importação seguinte conclui; títulos de 2 MB e 19 MB terminam sem corpo em `nothing_found`, sem chamar o modelo; um .docx legítimo com imagem ou objeto OLE de 25 MB importa normalmente (AC 77, AD-026)
Proof: run server | PASSED [C121] documento malicioso esgota só o processo de extração: a tarefa falha sem chamar o modelo e o servidor segue

**C122** - Um `Error` ao ler o documento (como esgotamento de memória) encerra a tarefa como `failed` `import_failed`, apaga o arquivo e registra o erro no log, em vez de deixá-la voltar à fila (AD-038)
Proof: run server | PASSED [C122] Error ao ler o documento encerra a tarefa como import_failed, apaga o arquivo e fica no log

**C123** - Na tela de entrada, `422` `invalid_email` mostra "Informe um e-mail válido." e falha de rede mostra "Não foi possível enviar o link. Tente de novo." (AC 2)
Proof: run web | ✓ [C123] e-mail recusado pelo servidor e falha de rede mostram mensagem na tela

**C124** - Arquivo cujo conteúdo não é do tipo da extensão (um zip enviado como `.pdf`) falha com `import_failed` sem chamar o modelo, e um `.docx` embutido em outro não é aberto: o texto dele não chega ao modelo (AC 74, AD-026)
Proof: run server | PASSED [C124] conteúdo de outro tipo que a extensão falha e documento embutido não é aberto

**C125** - Um PDF de menos de 1 MB cujos metadados XMP descompactam para 300 MB esgota só o processo de extração (o log registra o fim do filho, e o servidor não registra erro próprio) e a tarefa termina em `import_failed`, sem chamar o modelo (AC 77, AD-063)
Proof: run server | PASSED [C125] PDF com metadados que descompactam para centenas de MB falha com import_failed sem chamar o modelo

**C126** - O processo de extração que passa do tempo limite é encerrado, a tarefa termina em `import_failed` e o encerramento fica no log (AD-063)
Proof: run server | PASSED [C126] extração que passa do tempo limite é encerrada e a tarefa termina em import_failed

**C127** - Uma extração interrompida no meio (como no desligamento do servidor) encerra o processo filho em até 500 ms, em vez de deixá-lo rodando sem tempo limite (AD-063)
Proof: run server | PASSED [C127] extração interrompida encerra o processo filho

**C128** - Valor em dinheiro com representação enorme (`1E+2147483647` na parcela, `1E-2147483647` no à vista, `1E+100000000` no desconto sem plano) responde `422` com erro do campo, sem `500` e sem gravar; um valor de parcela grande de verdade (`999999999999.99`) continua aceito (AD-025, AC 47)
Proof: run server | PASSED [C128] valor em dinheiro com expoente enorme responde 422 com erro de campo, sem 500 nem gravar

## Coverage

| Set (size) | Member -> proof | Unproven |
| --- | --- | --- |
| `GET /oauth2/authorization/google` statuses (1) | 302 C8 | - |
| `POST /ott/generate` statuses (5) | 200 C2 · 403 sem CSRF C2 · 422 C95 · 429 C3 · 503 C98 | - |
| `GET /api/login-links/{token}` statuses (2) | 200 C4 · 404 C6 | - |
| `POST /login/ott` statuses (3) | 302 C5 · 401 C6 · 403 sem CSRF C5 | - |
| `GET /api/identity-links/{token}` statuses (2) | 200 C10 · 404 C11 | - |
| `POST /login/identity-link` statuses (3) | 302 C10 · 401 C11 · 403 sem CSRF C10 | - |
| `POST /logout` statuses (3) | 204 C24 · 401 C24 · 403 sem CSRF C24 | - |
| `GET /api/me` statuses (2) | 200 C16 · 401 C24 | - |
| `POST /api/businesses` statuses (5) | 201 C12 · 401 C12 · 403 C13 · 409 C14 · 422 C12 | - |
| `GET /api/businesses/{businessId}/playbook` statuses (3) | 200 C67 · 401 C19 · 404 C19 | - |
| `PUT /api/businesses/{businessId}/playbook/draft` statuses (7) | 200 C30 · 401 C19 · 403 C22 · 404 C19 · 409 revision_conflict C41 · 409 import_running C42 · 422 C37 | - |
| `POST /api/businesses/{businessId}/playbook/versions` statuses (11) | 201 C54 · 401 C19 · 403 C22 · 404 C19 · 409 revision_conflict C55 · 409 nothing_to_publish C61 · 409 import_running C42 · 422 publish_minimum C56 · 422 incomplete_rows C57 · 422 discount_invalid C58 · 422 pending_review C59 | - |
| `POST /api/businesses/{businessId}/playbook/imports` statuses (8) | 202 C66 · 401 C19 · 403 C22 · 404 C19 · 409 C76 · 413 C75 · 415 C74 · 422 C66 | - |
| source controls: tela 6 (2) | "+ Etapa" C30 · "+ Objeção" C33 | - |
| controls added by Differences (8) | "+ Informação" C32 · "+ Plano" C34 · edição no lugar C35 · "Remover" C36 · setas C31 · "Importar documento" C65 · "Publicar" C54 · "Conferido" C70 · "Trocar de negócio" C18 | - |
| link expiry, AC 6 (3 examples) | 14 min C6 · reuso C6 · 16 min C6 | - |
| Google authority, AC 9 (4 examples) | gmail C8 · hd C8 · sem hd C9 · depois da confirmação C10 | - |
| tab independence, AC 19 (2 examples) | aba 1 grava em A C19 · businessId B com playbookId A C19 | - |
| RLS, AC 20 (4 examples) | SELECT A C20 · UPDATE B C20 · sem variável C20 · claim de dois negócios C86 | - |
| cash value, AC 46 (6 examples) | 4500 C46 · 4.500,50 C46 · 0 C46 · abc C46 · 10.000.000,01 C46 · três casas C46 | - |
| format, AC 48 (3 examples) | R$ 4.500 C48 · 6x 1.166,67 C48 · 3x 1.500 C48 | - |
| discount, AC 51 (3 examples) | 4.499,99 C51 · 4.500 C51 · 0 C51 | - |
| publish rules (5 codes) | publish_minimum C56 · incomplete_rows C57 · discount_invalid C58 · pending_review C59 · revision_conflict C55 | - |
| import failure codes (8) | unsupported_format C74 · file_too_large C75 · text_too_long C77 · pdf_without_text C78 · google_docs_unavailable C79 · nothing_found C80 · model_failed C81 · import_running C76 | - |
| money in text, AC 71 (2 examples) | 7000 presente C71 · 6600 ausente C71 | - |
| scalar conflicts, AC 72-73 (4 cases) | desconto vazio C72 · coluna vazia C72 · desconto diferente C73 · coluna diferente C73 | - |
| startup config: roles and session (2 assemblies) | app entry point C26 · test harness C20 | - |

## Swept

- validation: C2, C12, C37, C38, C46, C47, C51, C74, C75, C79
- failure modes: C40, C81, C83
- idempotency: C6, C10, C85
- authorization: C13, C19, C20, C22, C63, C86
- concurrency: C10, C15, C41, C43, C55, C84
- data lifecycle: C25, C87, C88
- dependency failure: C79, C81
- state transitions: C54, C61, C66, C85
- observability: n/a - o plano não pede log, métrica nem rastreamento nesta parte

## Handoff

- **Settled mid-build:** "Vertical slice + Modulith (Recomendado)"; "Por funcionalidade (Recomendado)"; "Meio-termo (Recomendado)"; "Sim, Makefile (Recomendado)" - registrados em AD-053 a AD-056; "nao esquece de aplicar o RFC 9457: Problem Details for HTTP APIs" - AD-057; "prefira arquivos yml do que properties" - AD-058; textos do app web: "Sim, confirmo (Recomendado)"; menu: "Aviso ao clicar (Recomendado)" e "Sim, no rodapé do menu (Recomendado)" - AD-059; validação de telas: "Como a playwright-skill" - AD-060; revisão externa pelo Codex em ciclo, rodada por `codex exec --dangerously-bypass-approvals-and-sandbox` numa cópia `git worktree`, até o veredito `SEM DEFEITOS`, e só então o push, com confirmação (user: "continue até zerar os defeitos"; "Sem sandbox, na cópia"); desconto sem plano fica guardado e a publicação ignora a marca dele enquanto não houver plano (AD-061; user: "Mantém o desconto (Recomendado)"); versão publicada sem plano sai sem o desconto e a marca (AD-062; user: "Versão sem o desconto (Recomendado)"); extração de texto num processo isolado com memória e tempo limitados (AD-063; user: "Processo separado (Recomendado)")
- **Dispatched:** Verifier at `33a3580` - returned; screens round 1 at `33a3580` - returned; screens round 2 at `fa433df` - returned; Verifier for C92-C99 at `f348be9` - returned; screens round 2 again at `f348be9` and at `150517e` - returned; Verifier for the Codex round 2 checks at `6793cee` - returned; screens round 2 again at `6793cee` and at `4d4c2ec` - returned; Verifier for the Codex round 3 checks at `d2dbe74` - returned; screens round 2 again at `d2dbe74` - returned; Verifier for C99 and C103 at `7b96a57` - returned; screens round 2 again at `7b96a57` - returned; Verifier for C99 and C103 at `e299335`; screens round 2 again at `e299335` - returned; screens round 2 again at `6225db3` - returned; Verifier for the Codex round 4 checks at `5318011` - returned; screens round 2 again at `5318011` - returned; Verifier for C97, C103, C107, C110, C112, C24c after `6e72cc7`; screens round 2 again after `6e72cc7`; Verifier for the Codex round 5 checks at `0abd136` - returned; screens round 2 again at `0abd136` - returned; Verifier for C113 and C116 after `7d121d1`; screens round 2 again after `7d121d1`; Verifier for the Codex round 6 checks at `1b209b1` - returned; screens round 2 again at `1b209b1` - returned; Verifier for C104 and C120 at `037ed29` - returned; screens round 2 again at `037ed29` - returned; Verifier for C121-C123 at `70e138c` - returned; screens round 2 again at `70e138c` - returned; Verifier for C121, C122, C124 at `3e5a2ef` - returned; screens round 2 again at `3e5a2ef` - returned; Verifier for C121 and C124 at `69ee54c` - returned; screens round 2 again at `69ee54c` - returned; Verifier for C77, C121, C125 at `9b416fb` - returned; screens round 2 again at `9b416fb` - returned; Verifier for C121, C125, C126 at `ec10cda` - returned; screens round 2 again at `ec10cda` - returned; Verifier for C127 at `a852ce8` - returned; screens round 2 again at `a852ce8` - returned
- **Boundary:** C1-C91 closed at `a155717`; C92-C99 (revisão do Codex) closed at `e1aca23`; C100-C102 (segunda revisão do Codex) closed at `43e63a9`, C99 at `d1df50d`; gate `validate_verification.py` 0 errors
- **Abandoned:** suíte Playwright versionada com `toHaveScreenshot` - AD-060 escolheu a caminhada avulsa
- **Walked:** `make e2e-server` (Postgres de teste com os papéis de produção, e-mails gravados em arquivo) e o app web em `vite`, no Chrome com janela 1440 x 1300. Sem sessão, `/n/qualquer/playbook` levou a `/entrar` com "Entrar com Google" e o campo de e-mail; "Enviar link de acesso" mostrou "Enviamos um link de acesso para gestor@exemplo.test."; o link abriu "Entrar como gestor@exemplo.test?" e "Entrar" criou a sessão; a pessoa liberada viu o formulário de criação, criou o negócio e caiu em `/n/<id>/playbook` vazio; "Escrever o playbook" e "+ Etapa" com digitação e Tab gravaram a etapa (revisão 2); com o playbook de exemplo gravado pela API, clicar em "Garantias" abriu a edição no lugar e a saída do campo gravou "Garantias e suporte"; "Publicar" levou o topo a "Playbook: Venda consultiva · versão 1" e "Publicar" ficou desabilitado. A imagem de C29 foi salva sobre o build de produção (`vite preview`), com o mouse fora das linhas, em `captures/tela-6-playbook.running.png`. O gestor chega a cada estado pelo link de acesso, pela criação do negócio e pelo menu Playbook.
