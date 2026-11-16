# playbook-configuravel proofs

**HEAD**: 822ab7b
**Tree**: clean
**Shell**: /bin/sh
**Result**: 177 pass, 0 fail

| Check | Proof | Result |
| --- | --- | --- |
| C1 | `run web / ✓ [C01] sem sessão qualquer tela vai para /entrar com Google e campo de e-mail` | pass |
| C2 | `run server / PASSED [C02a] pedir o link envia o e-mail e responde igual para e-mail desconhecido` | pass |
| C2 | `run web / ✓ [C02b] e-mail sem arroba mostra Informe um e-mail válido e não chama a API` | pass |
| C2 | `run web / ✓ [C02c] pedido aceito mostra Enviamos um link de acesso para o e-mail` | pass |
| C3 | `run server / PASSED [C03a] segundo pedido em menos de 60 s responde 429 e não envia` | pass |
| C3 | `run web / ✓ [C03b] resposta 429 mostra Aguarde 1 minuto para pedir outro link` | pass |
| C4 | `run server / PASSED [C04a] ler o link duas vezes não consome o token` | pass |
| C4 | `run web / ✓ [C04b] página do link mostra Entrar como o e-mail e o botão Entrar` | pass |
| C5 | `run server / PASSED [C05] POST /login/ott consome o token e cria a sessão` | pass |
| C5 | `run web / ✓ [C05b] Entrar envia o token por POST /login/ott` | pass |
| C6 | `run server / PASSED [C06a] token aos 14 min entra, reuso e 16 min são recusados` | pass |
| C6 | `run web / ✓ [C06b] link recusado mostra Este link já foi usado ou expirou` | pass |
| C7 | `run server / PASSED [C07a] Google sem e-mail verificado não cria sessão` | pass |
| C7 | `run web / ✓ [C07b] erro google_unverified mostra Seu e-mail do Google não está verificado` | pass |
| C8 | `run server / PASSED [C08] Google com gmail ou hd entra e liga a identidade pelo e-mail` | pass |
| C9 | `run server / PASSED [C09a] Google fora do Gmail sem hd grava desafio, envia link e não cria sessão` | pass |
| C9 | `run web / ✓ [C09b] confirmação pendente mostra Confirme seu e-mail` | pass |
| C10 | `run server / PASSED [C10a] consumir o desafio grava google_subject e cria a sessão` | pass |
| C10 | `run server / PASSED [C10b] dois consumos simultâneos do desafio têm um só vencedor` | pass |
| C11 | `run server / PASSED [C11] link comum não liga identidade pendente e desafio vencido é recusado` | pass |
| C12 | `run server / PASSED [C12a] criar negócio liberado cria gestor e playbook e valida 1 a 80 caracteres` | pass |
| C12 | `run web / ✓ [C12b] formulário de criação abre /n/id/playbook` | pass |
| C13 | `run server / PASSED [C13a] e-mail fora da lista responde 403 signup_not_allowed` | pass |
| C13 | `run web / ✓ [C13b] sem liberação mostra Seu acesso ainda não foi liberado` | pass |
| C14 | `run server / PASSED [C14] membro que cria negócio recebe 409 already_member` | pass |
| C15 | `run server / PASSED [C15] duas criações simultâneas criam um negócio e um 409` | pass |
| C16 | `run server / PASSED [C16a] /api/me devolve o último negócio usado` | pass |
| C16 | `run web / ✓ [C16b] entrada abre o último negócio válido ou o único` | pass |
| C17 | `run web / ✓ [C17] mais de um negócio sem último válido mostra a lista e abre o escolhido` | pass |
| C18 | `run web / ✓ [C18] Trocar de negócio aparece só com mais de um negócio e abre a lista` | pass |
| C19 | `run server / PASSED [C19] businessId sem associação ou playbookId de outro negócio responde 404` | pass |
| C20 | `run server / PASSED [C20] RLS do papel app devolve só o negócio da variável` | pass |
| C21 | `run server / PASSED [C21] chave composta recusa business_id divergente` | pass |
| C22 | `run server / PASSED [C22] closer recebe 403 nas escritas` | pass |
| C23 | `run web / ✓ [C23] menu com cinco itens na ordem e Disponível em breve` | pass |
| C24 | `run server / PASSED [C24a] Sair invalida a sessão e o cookie antigo recebe 401` | pass |
| C24 | `run web / ✓ [C24b] Sair abre /entrar` | pass |
| C25 | `run server / PASSED [C25] sessão expira com 7 dias sem uso ou 30 dias desde o login` | pass |
| C26 | `run server / PASSED [C26] sessão gravada no Postgres vale para outra instância do repositório` | pass |
| C91 | `run web / ✓ [C91] sem cookie XSRF-TOKEN busca o token antes do primeiro POST e o envia` | pass |
| C27 | `run web / ✓ [C27] rascunho vazio mostra título, subtítulo e os dois jeitos de começar` | pass |
| C28 | `run web / ✓ [C28] Escrever o playbook mostra os quatro blocos vazios com os botões` | pass |
| C29 | `browser - abrir /n/<businessId>/playbook com um playbook de exemplo, janela 1440 x 1300, salvar captures/tela-` | pass |
| C30 | `run web / ✓ [C30] + Etapa acrescenta ao fim numerada e salva ao sair do campo` | pass |
| C31 | `run web / ✓ [C31] setas trocam a etapa, renumeram, salvam e desabilitam nas pontas` | pass |
| C32 | `run web / ✓ [C32] + Informação acrescenta a linha ao fim do bloco` | pass |
| C33 | `run web / ✓ [C33] + Objeção acrescenta a linha com os três campos` | pass |
| C34 | `run web / ✓ [C34] + Plano acrescenta a linha ao fim da tabela` | pass |
| C35 | `run web / ✓ [C35] clicar numa linha edita no lugar e salva cada campo ao sair` | pass |
| C36 | `run web / ✓ [C36] Remover tira a linha sem confirmação e renumera as etapas` | pass |
| C37 | `run server / PASSED [C37a] texto acima de 120 ou 500 caracteres responde 422 com o campo` | pass |
| C37 | `run web / ✓ [C37b] erro de limite mostra Máximo de N caracteres e mantém o valor salvo` | pass |
| C38 | `run server / PASSED [C38a] rascunho acima do limite de itens responde 422` | pass |
| C38 | `run web / ✓ [C38b] bloco no limite desabilita o botão e mostra Limite de N itens` | pass |
| C39 | `run web / ✓ [C39] textos aparecem entre aspas e são guardados sem aspas` | pass |
| C40 | `run web / ✓ [C40] falha ao salvar mantém o valor e oferece Tentar de novo e Desfazer` | pass |
| C41 | `run server / PASSED [C41a] gravação com revisão antiga responde 409 revision_conflict` | pass |
| C41 | `run web / ✓ [C41b] revision_conflict mostra O playbook foi alterado em outra aba` | pass |
| C42 | `run server / PASSED [C42a] gravação com importação ativa responde 409 import_running` | pass |
| C42 | `run web / ✓ [C42b] importação ativa deixa o rascunho só para leitura` | pass |
| C43 | `run server / PASSED [C43] gravação e importação simultâneas nunca gravam depois da tarefa` | pass |
| C44 | `run web / ✓ [C44] nome da coluna extra vira o título entre Plano e À vista` | pass |
| C45 | `run web / ✓ [C45] coluna extra vazia some fora da edição` | pass |
| C46 | `run server / PASSED [C46a] à vista aceita 4500 e 4500,50 e recusa 0, 10000000,01 e três casas` | pass |
| C46 | `run web / ✓ [C46b] campo à vista converte 4.500,50 e recusa abc com a mensagem` | pass |
| C47 | `run server / PASSED [C47a] parcelado aceita 2 a 60 parcelas positivas e recusa o resto` | pass |
| C47 | `run web / ✓ [C47b] parcelado inválido mostra a mensagem e vazio aparece como travessão` | pass |
| C48 | `run web / ✓ [C48] formata R$ 4.500, 6x 1.166,67 e 3x 1.500` | pass |
| C49 | `run web / ✓ [C49] desconto informado aparece como Desconto máximo na call R$ valor` | pass |
| C50 | `run web / ✓ [C50] sem desconto mostra o selo a definir` | pass |
| C51 | `run server / PASSED [C51a] desconto 4499,99 aceito, 4500 recusado e 0 aceito com menor à vista 4500` | pass |
| C51 | `run web / ✓ [C51b] desconto inválido mostra a mensagem com o menor à vista` | pass |
| C52 | `run web / ✓ [C52] sem plano a linha do desconto não aparece` | pass |
| C53 | `run web / ✓ [C53] sem call registrada a coluna Nas calls não aparece` | pass |
| C54 | `run server / PASSED [C54a] publicar cria a versão N+1 começando em 1` | pass |
| C54 | `run web / ✓ [C54b] após publicar o topo mostra a versão nova` | pass |
| C55 | `run server / PASSED [C55a] publicar com revisão antiga responde 409 e não cria versão` | pass |
| C55 | `run web / ✓ [C55b] publicar com revision_conflict mostra O playbook foi alterado em outra aba` | pass |
| C56 | `run server / PASSED [C56a] sem etapa, informação ou objeção responde 422 publish_minimum e tabela vazia public` | pass |
| C56 | `run web / ✓ [C56b] publish_minimum mostra a mensagem do mínimo` | pass |
| C57 | `run server / PASSED [C57a] linha incompleta responde 422 incomplete_rows com os ids` | pass |
| C57 | `run web / ✓ [C57b] incomplete_rows destaca as linhas e mostra Complete ou remova` | pass |
| C58 | `run server / PASSED [C58] desconto fora da regra responde 422 discount_invalid` | pass |
| C59 | `run server / PASSED [C59a] valor marcado a conferir responde 422 pending_review` | pass |
| C59 | `run web / ✓ [C59b] pending_review mostra Confira os valores marcados` | pass |
| C60 | `run web / ✓ [C60] alteração pendente desabilita Publicar com Há alterações não salvas` | pass |
| C61 | `run server / PASSED [C61a] rascunho igual à versão responde 409 nothing_to_publish` | pass |
| C61 | `run web / ✓ [C61b] Alterações não publicadas aparece só com diferença e Publicar desabilita sem ela` | pass |
| C62 | `run web / ✓ [C62] sem versão o topo mostra rascunho` | pass |
| C63 | `run server / PASSED [C63] UPDATE e DELETE em versões falham para o papel app` | pass |
| C64 | `run server / PASSED [C64] versão nova mantém os ids dos itens` | pass |
| C65 | `run web / ✓ [C65] Importar documento oferece arquivo, texto colado e link do Google Docs` | pass |
| C66 | `run server / PASSED [C66a] enviar a origem responde 202 e cria a tarefa queued` | pass |
| C66 | `run web / ✓ [C66b] envio mostra Importando com rascunho só leitura e botões desabilitados` | pass |
| C67 | `run server / PASSED [C67a] GET do playbook devolve a tarefa ativa` | pass |
| C67 | `run web / ✓ [C67b] tela aberta com tarefa ativa mostra Importando` | pass |
| C68 | `run server / PASSED [C68a] importação acrescenta ao fim sem mudar o existente e conta os itens` | pass |
| C68 | `run web / ✓ [C68b] importação concluída mostra Importado com as contagens` | pass |
| C69 | `run server / PASSED [C69a] plano e desconto importados chegam marcados com o trecho e o resto sem marca` | pass |
| C69 | `run web / ✓ [C69b] valor marcado mostra Importado · conferir e o trecho` | pass |
| C70 | `run web / ✓ [C70] Conferido ou edição tira a marca do item` | pass |
| C70 | `run web / ✓ [C70b] editar o à vista ou o parcelado de um plano marcado tira a marca dele` | pass |
| C71 | `run server / PASSED [C71] valor ausente do texto é descartado e presente é importado` | pass |
| C72 | `run server / PASSED [C72] desconto e nome da coluna vazios recebem o valor do documento` | pass |
| C73 | `run server / PASSED [C73] desconto e coluna diferentes ficam com o atual e geram aviso` | pass |
| C73 | `run web / ✓ [C73b] avisos da importação aparecem na mensagem do resultado` | pass |
| C74 | `run server / PASSED [C74] extensão não aceita responde 415 sem criar tarefa` | pass |
| C74 | `run web / ✓ [C74b] mensagens da importação por código de erro` | pass |
| C75 | `run server / PASSED [C75] arquivo acima de 10 MB responde 413 sem criar tarefa` | pass |
| C75 | `run web / ✓ [C74b] mensagens da importação por código de erro` | pass |
| C76 | `run server / PASSED [C76] segunda importação com tarefa ativa responde 409` | pass |
| C77 | `run server / PASSED [C77] texto acima de 100 mil caracteres falha sem chamar o modelo` | pass |
| C77 | `run web / ✓ [C74b] mensagens da importação por código de erro` | pass |
| C78 | `run server / PASSED [C78] PDF sem texto falha sem chamar o modelo` | pass |
| C78 | `run web / ✓ [C74b] mensagens da importação por código de erro` | pass |
| C79 | `run server / PASSED [C79] link que não é Google Docs ou não público falha sem mudar o rascunho` | pass |
| C79 | `run web / ✓ [C74b] mensagens da importação por código de erro` | pass |
| C80 | `run server / PASSED [C80] documento sem itens falha sem mudar o rascunho` | pass |
| C80 | `run web / ✓ [C74b] mensagens da importação por código de erro` | pass |
| C81 | `run server / PASSED [C81] três falhas do modelo encerram a tarefa em failed sem mudar o rascunho` | pass |
| C81 | `run server / PASSED [C81b] resposta do modelo depois do tempo limite conta como falha e repete até 3 vezes` | pass |
| C81 | `run web / ✓ [C74b] mensagens da importação por código de erro` | pass |
| C82 | `run server / PASSED [C82] bloco acima do limite importa até o limite e avisa os itens de fora` | pass |
| C83 | `run server / PASSED [C83] conclusão que não marca a tarefa não grava linhas` | pass |
| C84 | `run server / PASSED [C84] execução com concessão vencida não conclui depois de outra assumir` | pass |
| C85 | `run server / PASSED [C85] concessão vencida é reprocessada sem duplicar linhas` | pass |
| C86 | `run server / PASSED [C86] claim entrega tarefas de dois negócios e o papel app só vê as suas` | pass |
| C87 | `run server / PASSED [C87] arquivo da tarefa é apagado depois do commit e o texto não é gravado` | pass |
| C88 | `run server / PASSED [C88] varredura apaga arquivos encerrados e órfãos com mais de 1 hora` | pass |
| C89 | `run server / PASSED [C89] chamada ao OpenRouter leva provider.zdr true` | pass |
| C90 | `run server / PASSED [C90] módulos do servidor só usam o api de outros módulos` | pass |
| C92 | `run web / ✓ [C92] Importar documento fica desabilitado com alteração não salva ou com falha` | pass |
| C93 | `run web / ✓ [C93] falha ao remover uma linha mostra a mensagem com Tentar de novo e Desfazer` | pass |
| C94 | `run web / ✓ [C94] voltar pelo histórico a outro negócio recria a tela e grava no negócio do endereço` | pass |
| C95 | `run server / PASSED [C95] e-mail válido de até 320 caracteres entra e grava a sessão` | pass |
| C96 | `run server / PASSED [C96] rascunho nulo ou com item nulo responde 422 invalid_document sem gravar` | pass |
| C97 | `run server / PASSED [C97] erros do Spring saem em RFC 9457 com code e type` | pass |
| C98 | `run server / PASSED [C98] servidor de e-mail fora do ar responde 503 mail_unavailable em RFC 9457` | pass |
| C99 | `sh deploy/verify.sh` | pass |
| C100 | `run web / ✓ [C100] remover a linha com erro local libera Publicar e Importar documento` | pass |
| C101 | `run web / ✓ [C101] sessão expirada durante a edição leva a /entrar na requisição seguinte` | pass |
| C102 | `run web / ✓ [C102] parcela acima de R$ 10 milhões é aceita, sem o teto do preço à vista` | pass |
| C103 | `run server / PASSED [C103] erro recusado antes do MVC sai em RFC 9457 com code e type` | pass |
| C104 | `sh deploy/verify-roles.sh` | pass |
| C105 | `run web / ✓ [C105] importação iniciada em outra aba fecha a linha aberta e nenhum campo fica editável` | pass |
| C106 | `run server / PASSED [C106] valor em dinheiro da resposta do modelo chega com todos os dígitos` | pass |
| C107 | `run server / PASSED [C107] desconto sem plano fica guardado e só bloqueia a publicação quando há plano` | pass |
| C108 | `run web / ✓ [C108] durante a publicação nenhuma linha abre para edição e nada digitado se perde` | pass |
| C108 | `run web / ✓ [C108b] depois de 409 nothing_to_publish o rascunho só volta a aceitar edição quando o reload term` | pass |
| C109 | `run web / ✓ [C109] Desfazer fica indisponível enquanto outra gravação está em andamento` | pass |
| C110 | `run web / ✓ [C110] consulta da importação que falha uma vez continua até a tarefa terminar` | pass |
| C111 | `run web / ✓ [C111] voltar o campo ao valor gravado apaga o erro do servidor e libera Publicar` | pass |
| C111 | `run web / ✓ [C24c] Sair aparece no acesso não liberado, na criação de negócio e na lista e leva a /entrar` | pass |
| C112 | `run web / ✓ [C112] remover o último plano apaga o erro do desconto e libera Publicar e Importar documento` | pass |
| C113 | `run web / ✓ [C113] importação aceita com 202 fica em Importando e é acompanhada mesmo se a primeira consulta f` | pass |
| C114 | `run server / PASSED [C114] número de parcelas fracionário é recusado sem gravar e 6.0 vale 6` | pass |
| C115 | `run server / PASSED [C115] SMTP sem STARTTLS recusa o envio antes de mandar senha ou mensagem` | pass |
| C116 | `run server / PASSED [C116] exportação do Google Docs acima de 10 MB falha com text_too_long sem chamar o model` | pass |
| C117 | `run server / PASSED [C117] tabela temporária import_jobs do papel app não substitui a real nas funções privile` | pass |
| C118 | `run server / PASSED [C118] GET do playbook lê rascunho e tarefa no mesmo snapshot mesmo com a importação concl` | pass |
| C119 | `run web / ✓ [C119] gravação de outro campo que termina enquanto a pessoa digita no parcelado não apaga a digit` | pass |
| C120 | `run web / ✓ [C120] 409 import_running ao gravar ou publicar deixa a tela em Importando e acompanha mesmo com a` | pass |
| C121 | `run server / PASSED [C121] documento malicioso esgota só o processo de extração: a tarefa falha sem chamar o m` | pass |
| C122 | `run server / PASSED [C122] Error ao ler o documento encerra a tarefa como import_failed, apaga o arquivo e fic` | pass |
| C123 | `run web / ✓ [C123] e-mail recusado pelo servidor e falha de rede mostram mensagem na tela` | pass |
| C124 | `run server / PASSED [C124] conteúdo de outro tipo que a extensão falha e documento embutido não é aberto` | pass |
| C125 | `run server / PASSED [C125] PDF com metadados que descompactam para centenas de MB falha com import_failed sem ` | pass |
| C126 | `run server / PASSED [C126] extração que passa do tempo limite é encerrada e a tarefa termina em import_failed` | pass |
| C127 | `run server / PASSED [C127] extração interrompida encerra o processo filho` | pass |
| C128 | `run server / PASSED [C128] valor em dinheiro com expoente enorme responde 422 com erro de campo, sem 500 nem g` | pass |
| C129 | `run server / PASSED [C129] número em dinheiro maior que numeric(12,2) não entra no rascunho, mesmo presente no` | pass |
| C130 | `run server / PASSED [C130] resultado do filho com status desconhecido ou texto cortado vira import_failed` | pass |
| C131 | `run server / PASSED [C131] valor negativo do documento não autoriza o positivo, e hífen de lista, separador ou` | pass |
| C133 | `run server / PASSED [C133] número em notação científica não autoriza nenhum pedaço dele` | pass |
| C132 | `run web / ✓ [C132] com o envio da importação pendente o diálogo não fecha, e o rascunho não fica editável ante` | pass |
| Build | `make build` | pass |
| Build | `make lint` | pass |

## Output

`Run web` - exit 0

```
 ✓ src/features/playbook/__tests__/playbook.test.ts > tela do playbook > [C62] sem versão o topo mostra rascunho 10ms
 ✓ src/features/playbook/__tests__/review.test.ts > correções da revisão de código > [C110] consulta da importação que falha uma vez continua até a tarefa terminar 10556ms
 ✓ src/features/playbook/__tests__/review.test.ts > correções da revisão de código > [C111] voltar o campo ao valor gravado apaga o erro do servidor e libera Publicar 329ms
 ✓ src/features/playbook/__tests__/review.test.ts > correções da revisão de código > [C24c] Sair aparece no acesso não liberado, na criação de negócio e na lista e leva a /entrar 27ms
 ✓ src/features/playbook/__tests__/review.test.ts > correções da revisão de código > [C112] remover o último plano apaga o erro do desconto e libera Publicar e Importar documento 72ms
 ✓ src/features/playbook/__tests__/review.test.ts > correções da revisão de código > [C113] importação aceita com 202 fica em Importando e é acompanhada mesmo se a primeira consulta falhar 2104ms
 ✓ src/features/playbook/__tests__/review.test.ts > correções da revisão de código > [C119] gravação de outro campo que termina enquanto a pessoa digita no parcelado não apaga a digitação 191ms
 ✓ src/features/playbook/__tests__/review.test.ts > correções da revisão de código > [C120] 409 import_running ao gravar ou publicar deixa a tela em Importando e acompanha mesmo com a consulta falhando 4143ms
 ✓ src/features/playbook/__tests__/review.test.ts > correções da revisão de código > [C132] com o envio da importação pendente o diálogo não fecha, e o rascunho não fica editável antes do 202 134ms
 ✓ src/features/playbook/__tests__/review.test.ts > correções da revisão de código > [C108b] depois de 409 nothing_to_publish o rascunho só volta a aceitar edição quando o reload termina 47ms

 Test Files  6 passed (6)
      Tests  79 passed (79)
   Start at  09:32:51
   Duration  19.93s (transform 2.08s, setup 0ms, import 4.88s, tests 21.90s, environment 2.29s)
```

`Run server` - exit 0

```
2026-11-16T09:33:54.891-03:00  WARN 49444 --- [server] [           main] a.s.importing.ExtractionProcess          : Extração de 26580ff9-7200-4797-8d35-395f66649b96.docx passou de PT0.001S e foi encerrada
PASSED [C126] extração que passa do tempo limite é encerrada e a tarefa termina em import_failed
PASSED [C127] extração interrompida encerra o processo filho
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.767 s -- in app.salescopilot.importing.ExtractionTimeoutTest
[INFO] 
[INFO] Results:
[INFO] 
[INFO] Tests run: 88, Failures: 0, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  46.039 s
[INFO] Finished at: 2026-11-16T09:33:57-03:00
[INFO] ------------------------------------------------------------------------
```

`sh deploy/verify.sh` - exit 0

```
deploy ok
```

`sh deploy/verify-roles.sh` - exit 0

```
roles ok
```
