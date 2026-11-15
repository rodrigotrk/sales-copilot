# Playbook configurável verification

**Verdict**: FAIL
**Proofs**: `proofs.md` at 33a3580
**Verifier**: independent sub-agent (author != verifier)

## Checks

| Check | Test | Holds |
| --- | --- | --- |
| C1 | `auth.test.ts` `[C01] sem sessão qualquer tela vai para /entrar com Google e campo de e-mail` | yes |
| C2 | `LoginLinkTest` `[C02a] pedir o link envia o e-mail e responde igual para e-mail desconhecido`; `auth.test.ts` `[C02b]`, `[C02c]` | yes |
| C3 | `LoginLinkTest` `[C03a] segundo pedido em menos de 60 s responde 429 e não envia`; `auth.test.ts` `[C03b]` | yes - 59 s recusa, 61 s aceita |
| C4 | `LoginLinkTest` `[C04a] ler o link duas vezes não consome o token`; `auth.test.ts` `[C04b]` | yes |
| C5 | `LoginLinkTest` `[C05] POST /login/ott consome o token e cria a sessão`; `auth.test.ts` `[C05b]` | yes |
| C6 | `LoginLinkTest` `[C06a] token aos 14 min entra, reuso e 16 min são recusados`; `auth.test.ts` `[C06b]` | yes |
| C7 | `GoogleSignInTest` `[C07a] Google sem e-mail verificado não cria sessão`; `auth.test.ts` `[C07b]` | yes |
| C8 | `GoogleSignInTest` `[C08] Google com gmail ou hd entra e liga a identidade pelo e-mail` | yes |
| C9 | `GoogleSignInTest` `[C09a] Google fora do Gmail sem hd grava desafio, envia link e não cria sessão`; `auth.test.ts` `[C09b]` | yes - "não consultar `allowed_signups`" não é observado por nenhum teste |
| C10 | `GoogleSignInTest` `[C10a] consumir o desafio grava google_subject e cria a sessão`, `[C10b] dois consumos simultâneos do desafio têm um só vencedor` | yes |
| C11 | `GoogleSignInTest` `[C11] link comum não liga identidade pendente e desafio vencido é recusado` | yes |
| C12 | `BusinessTest` `[C12a] criar negócio liberado cria gestor e playbook e valida 1 a 80 caracteres`; `business.test.ts` `[C12b]` | yes |
| C13 | `BusinessTest` `[C13a] e-mail fora da lista responde 403 signup_not_allowed`; `business.test.ts` `[C13b]` | yes |
| C14 | `BusinessTest` `[C14] membro que cria negócio recebe 409 already_member` | yes |
| C15 | `BusinessTest` `[C15] duas criações simultâneas criam um negócio e um 409` | yes |
| C16 | `BusinessTest` `[C16a] /api/me devolve o último negócio usado`; `business.test.ts` `[C16b]` | yes |
| C17 | `business.test.ts` `[C17] mais de um negócio sem último válido mostra a lista e abre o escolhido` | yes |
| C18 | `business.test.ts` `[C18] Trocar de negócio aparece só com mais de um negócio e abre a lista` | yes |
| C19 | `BusinessTest` `[C19] businessId sem associação ou playbookId de outro negócio responde 404` | yes |
| C20 | `BusinessTest` `[C20] RLS do papel app devolve só o negócio da variável` | yes |
| C21 | `BusinessTest` `[C21] chave composta recusa business_id divergente` | yes |
| C22 | `BusinessTest` `[C22] closer recebe 403 nas escritas` | yes |
| C23 | `playbook.test.ts` `[C23] menu com cinco itens na ordem e Disponível em breve` | yes |
| C24 | `LoginLinkTest` `[C24a] Sair invalida a sessão e o cookie antigo recebe 401`; `auth.test.ts` `[C24b]` | yes - a `6793cee`, `[C24a]` afirma `204` no logout, `401` em RFC 9457 no cookie antigo e, sem sessão, `401` com `type` e `code` `unauthenticated` |
| C25 | `LoginLinkTest` `[C25] sessão expira com 7 dias sem uso ou 30 dias desde o login` | yes |
| C26 | `LoginLinkTest` `[C26] sessão gravada no Postgres vale para outra instância do repositório` | yes |
| C27 | `playbook.test.ts` `[C27] rascunho vazio mostra título, subtítulo e os dois jeitos de começar` | yes |
| C28 | `playbook.test.ts` `[C28] Escrever o playbook mostra os quatro blocos vazios com os botões` | yes |
| C29 | browser proof `captures/tela-6-playbook.running.png` | not judged here - `screens.md` |
| C30 | `playbook.test.ts` `[C30] + Etapa acrescenta ao fim numerada e salva ao sair do campo` | yes |
| C31 | `playbook.test.ts` `[C31] setas trocam a etapa, renumeram, salvam e desabilitam nas pontas` | yes |
| C32 | `playbook.test.ts` `[C32] + Informação acrescenta a linha ao fim do bloco` | yes |
| C33 | `playbook.test.ts` `[C33] + Objeção acrescenta a linha com os três campos` | yes |
| C34 | `playbook.test.ts` `[C34] + Plano acrescenta a linha ao fim da tabela` | yes |
| C35 | `playbook.test.ts` `[C35] clicar numa linha edita no lugar e salva cada campo ao sair` | yes |
| C36 | `playbook.test.ts` `[C36] Remover tira a linha sem confirmação e renumera as etapas` | yes |
| C37 | `PlaybookTest` `[C37a] texto acima de 120 ou 500 caracteres responde 422 com o campo`; `playbook.test.ts` `[C37b]` | yes |
| C38 | `PlaybookTest` `[C38a] rascunho acima do limite de itens responde 422`; `playbook.test.ts` `[C38b]` | no - só o limite de 30 etapas; 50 informações, 50 objeções e 20 planos não são afirmados no servidor nem na tela |
| C39 | `playbook.test.ts` `[C39] textos aparecem entre aspas e são guardados sem aspas` | yes |
| C40 | `playbook.test.ts` `[C40] falha ao salvar mantém o valor e oferece Tentar de novo e Desfazer` | no - só erro de rede; a resposta `5xx` do critério não é exercitada |
| C41 | `PlaybookTest` `[C41a] gravação com revisão antiga responde 409 revision_conflict`; `playbook.test.ts` `[C41b]` | yes |
| C42 | `PlaybookTest` `[C42a] gravação com importação ativa responde 409 import_running`; `playbook.test.ts` `[C42b]` | yes |
| C43 | `ImportTest` `[C43] gravação e importação simultâneas nunca gravam depois da tarefa` | yes |
| C44 | `playbook.test.ts` `[C44] nome da coluna extra vira o título entre Plano e À vista` | yes |
| C45 | `playbook.test.ts` `[C45] coluna extra vazia some fora da edição` | yes |
| C46 | `DraftRulesTest` `[C46a] à vista aceita 4500 e 4500,50 e recusa 0, 10000000,01 e três casas`; `playbook.test.ts` `[C46b]` | yes |
| C47 | `DraftRulesTest` `[C47a] parcelado aceita 2 a 60 parcelas positivas e recusa o resto`; `playbook.test.ts` `[C47b]` | yes |
| C48 | `playbook.test.ts` `[C48] formata R$ 4.500, 6x 1.166,67 e 3x 1.500` | yes |
| C49 | `playbook.test.ts` `[C49] desconto informado aparece como Desconto máximo na call R$ valor` | yes |
| C50 | `playbook.test.ts` `[C50] sem desconto mostra o selo a definir` | yes |
| C51 | `DraftRulesTest` `[C51a] desconto 4499,99 aceito, 4500 recusado e 0 aceito com menor à vista 4500`; `playbook.test.ts` `[C51b]` | yes |
| C52 | `playbook.test.ts` `[C52] sem plano a linha do desconto não aparece` | yes |
| C53 | `playbook.test.ts` `[C53] sem call registrada a coluna Nas calls não aparece` | yes |
| C54 | `PlaybookTest` `[C54a] publicar cria a versão N+1 começando em 1`; `playbook.test.ts` `[C54b]` | yes |
| C55 | `PlaybookTest` `[C55a] publicar com revisão antiga responde 409 e não cria versão`; `playbook.test.ts` `[C55b]` | yes |
| C56 | `PlaybookTest` `[C56a] sem etapa, informação ou objeção responde 422 publish_minimum e tabela vazia publica`; `playbook.test.ts` `[C56b]` | no - prova o rascunho vazio e a falta de objeção; falta só de etapa ou só de informação não é testada |
| C57 | `PlaybookTest` `[C57a] linha incompleta responde 422 incomplete_rows com os ids`; `playbook.test.ts` `[C57b]` | no - prova etapa sem nome e plano sem à vista; informação sem pergunta, objeção sem sugestão e objeção sem "Como o lead costuma dizer" (que precisa publicar) não são testadas |
| C58 | `PlaybookTest` `[C58] desconto fora da regra responde 422 discount_invalid` | yes |
| C59 | `PlaybookTest` `[C59a] valor marcado a conferir responde 422 pending_review`; `playbook.test.ts` `[C59b]` | no - não afirma os `id` dos planos marcados que o critério exige na resposta |
| C60 | `playbook.test.ts` `[C60] alteração pendente desabilita Publicar com Há alterações não salvas` | yes |
| C61 | `PlaybookTest` `[C61a] rascunho igual à versão responde 409 nothing_to_publish`; `playbook.test.ts` `[C61b]` | yes - mas a tela erra depois de uma gravação que volta ao conteúdo publicado (achado) |
| C62 | `playbook.test.ts` `[C62] sem versão o topo mostra rascunho` | yes |
| C63 | `PlaybookTest` `[C63] UPDATE e DELETE em versões falham para o papel app` | yes |
| C64 | `PlaybookTest` `[C64] versão nova mantém os ids dos itens` | yes |
| C65 | `importing.test.ts` `[C65] Importar documento oferece arquivo, texto colado e link do Google Docs` | yes |
| C66 | `ImportTest` `[C66a] enviar a origem responde 202 e cria a tarefa queued`; `importing.test.ts` `[C66b]` | yes |
| C67 | `ImportTest` `[C67a] GET do playbook devolve a tarefa ativa`; `importing.test.ts` `[C67b]` | yes |
| C68 | `ImportTest` `[C68a] importação acrescenta ao fim sem mudar o existente e conta os itens`; `importing.test.ts` `[C68b]` | yes |
| C69 | `ImportTest` `[C69a] plano e desconto importados chegam marcados com o trecho e o resto sem marca`; `importing.test.ts` `[C69b]` | yes |
| C70 | `importing.test.ts` `[C70] Conferido ou edição tira a marca do item` | yes |
| C71 | `ImportMergeTest` `[C71] valor ausente do texto é descartado e presente é importado` | no - passa nos dois exemplos do plano, mas a regra aceita 7 quando o texto diz "R$ 7.000" (achado) |
| C72 | `ImportMergeTest` `[C72] desconto e nome da coluna vazios recebem o valor do documento` | yes |
| C73 | `ImportMergeTest` `[C73] desconto e coluna diferentes ficam com o atual e geram aviso`; `importing.test.ts` `[C73b]` | yes |
| C74 | `ImportTest` `[C74] extensão não aceita responde 415 sem criar tarefa`; `importing.test.ts` `[C74b]` | yes |
| C75 | `ImportTest` `[C75] arquivo acima de 10 MB responde 413 sem criar tarefa`; `importing.test.ts` `[C74b]` | yes - 10 MB + 1 byte recusa, 10 MB aceita |
| C76 | `ImportTest` `[C76] segunda importação com tarefa ativa responde 409` | yes |
| C77 | `ImportTest` `[C77] texto acima de 100 mil caracteres falha sem chamar o modelo`; `importing.test.ts` `[C74b]` | yes - texto colado com 100.000 e 100.001, .docx com 100.001 e 97.000 e .docx com título de 100.001 caracteres; exemplo próprio: .docx com título e descrição de 200.000 cada e corpo de 99.000 passa, corpo de 100.001 dá `text_too_long`. No .docx o limite conta também as quebras de linha do texto extraído |
| C78 | `ImportTest` `[C78] PDF sem texto falha sem chamar o modelo`; `importing.test.ts` `[C74b]` | yes |
| C79 | `ImportTest` `[C79] link que não é Google Docs ou não público falha sem mudar o rascunho`; `importing.test.ts` `[C74b]` | yes |
| C80 | `ImportTest` `[C80] documento sem itens falha sem mudar o rascunho`; `importing.test.ts` `[C74b]` | yes |
| C81 | `ImportTest` `[C81] três falhas do modelo encerram a tarefa em failed sem mudar o rascunho`; `importing.test.ts` `[C74b]` | yes - cada uma das três respostas fora do schema (nome numérico, parcelas `2.9`, campo obrigatório ausente) precisa ser recusada, senão a tarefa termina em `done`; exemplo próprio contra `parse`: `cash` em texto, `leadSays` numérico, propriedade extra em `priceTable`, `approvedResponse` nulo, raiz em lista e etapa em texto, todos recusados. A demora de 120 s é provada pelo valor de `spring.ai.openai.timeout`, propriedade que existe no Spring AI 2.0.1, não por uma chamada lenta |
| C82 | `ImportTest` `[C82] bloco acima do limite importa até o limite e avisa os itens de fora` | yes |
| C83 | `ImportTest` `[C83] conclusão que não marca a tarefa não grava linhas` | yes |
| C84 | `ImportTest` `[C84] execução com concessão vencida não conclui depois de outra assumir` | yes |
| C85 | `ImportTest` `[C85] concessão vencida é reprocessada sem duplicar linhas` | yes |
| C86 | `ImportTest` `[C86] claim entrega tarefas de dois negócios e o papel app só vê as suas` | yes |
| C87 | `ImportTest` `[C87] arquivo da tarefa é apagado depois do commit e o texto não é gravado` | yes - só o caminho `done`; o apagamento em `failed` não é afirmado |
| C88 | `ImportTest` `[C88] varredura apaga arquivos encerrados e órfãos com mais de 1 hora` | yes |
| C89 | `ImportTest` `[C89] chamada ao OpenRouter leva provider.zdr true` | yes |
| C90 | `ModulesTest` `[C90] módulos do servidor só usam o api de outros módulos` | yes |
| C91 | `http.test.ts` `[C91] sem cookie XSRF-TOKEN busca o token antes do primeiro POST e o envia` | yes |
| C92 | `review.test.ts` `[C92] Importar documento fica desabilitado com alteração não salva ou com falha` | no - só o estado de falha; com a gravação em andamento ("salvando") o botão não é afirmado, e tirar `saving` de `unsaved` passaria |
| C93 | `review.test.ts` `[C93] falha ao remover uma linha mostra a mensagem com Tentar de novo e Desfazer` | no - só "Remover"; falha ao acrescentar e ao reordenar não é exercitada |
| C94 | `review.test.ts` `[C94] voltar pelo histórico a outro negócio recria a tela e grava no negócio do endereço` | yes |
| C95 | `LoginLinkTest` `[C95] e-mail válido de até 320 caracteres entra e grava a sessão` | yes - 320 caracteres exatos entram e ficam em `spring_session.principal_name`; 321 responde `422` `invalid_email` |
| C96 | `PlaybookTest` `[C96] rascunho nulo ou com item nulo responde 422 invalid_document sem gravar` | yes - acrescenta plano com `id` nulo, que responde exatamente `invalid_id`, `invalid_cash` e `invalid_installments`, com a revisão em zero; item nulo só em `stages`, e `DraftRules.validate` trata os quatro blocos na mesma condição |
| C97 | `ProblemsTest` `[C97] erros do Spring saem em RFC 9457 com code e type` | yes - `type` e o tipo de conteúdo só afirmados no JSON malformado; os outros três afirmam `code` |
| C98 | `LoginLinkTest` `[C98] servidor de e-mail fora do ar responde 503 mail_unavailable em RFC 9457` | yes |
| C99 | `sh deploy/verify.sh` | no - confere os serviços e lê a configuração gerada por `caddy adapt`, mas aceita encaminhamento parcial: com `handle @server { handle /api/health { reverse_proxy server:8080 } respond 404 }` o script imprime `deploy ok`, e as sete rotas respondem 404 (achado). Tirar `/logout` do matcher falha, como esperado |
| C100 | `review.test.ts` `[C100] remover a linha com erro local libera Publicar e Importar documento` | yes - tirar o filtro de `localErrors` em `removeItem` faz o teste falhar; exemplo próprio: erro na parcela da segunda linha, removida, libera os dois botões e deixa só `p1` no rascunho |
| C101 | `review.test.ts` `[C101] sessão expirada durante a edição leva a /entrar na requisição seguinte` | yes - sem a chamada a `onUnauthenticated` o teste falha; afirma a rota `/entrar`, a sessão local vazia e a ausência da mensagem de falha |
| C102 | `review.test.ts` `[C102] parcela acima de R$ 10 milhões é aceita, sem o teto do preço à vista` | yes - com `parseMoney(v)` sem `capped: false` o teste falha; exemplo próprio contra `parseMoney`: `10.000.000,01` recusado com teto e aceito sem, `99999999999999999999,99` sem teto sai exato, `007` vira `7.00`, `0,00` só com `allowZero`. O teto do preço à vista não é afirmado aqui |

## Findings

| Finding | Evidence | Fix | Closed |
| --- | --- | --- | --- |
| A importação aceita preço que o documento não traz: com o texto "Plano de 6 meses por R$ 7.000.", o modelo devolvendo 7 importa R$ 7,00 e devolvendo 6 importa R$ 6,00 (AC 71) | exemplo próprio em `ImportMerge.merge` a 33a3580: 7 -> `7.00`, 6 -> `6.00`, 70 -> `null`; `MoneyInText` lê "7.000" também como 7.000 em inglês (= 7) e aceita qualquer número do texto, como os 6 meses | ler "7.000" só como milhar quando os grupos têm 3 dígitos, ou aceitar só números escritos como dinheiro; acrescentar a C71 o caso 7 contra "R$ 7.000" | `b1d15a8` - `MoneyInText` dá uma leitura só a cada número; C71 prova 7 e 70 recusados contra "R$ 7.000" |
| JSON do modelo fora do schema não é repetido nem recusado (AC 81): `{"etapas":[...]}` faz 1 chamada e termina em `nothing_found`; uma etapa com `"name": null` termina em `done` e grava etapa sem nome no rascunho | exemplo próprio contra o stub a 33a3580: `requests=1 error_code=nothing_found`; `requests=1 status=done stages=[{"name":null}]`; `PlaybookExtraction.extract` só faz `readValue` no record | validar o JSON devolvido contra o schema (campos obrigatórios, textos não nulos) e contar a recusa como tentativa falha; acrescentar a C81 um JSON válido fora do schema | `b1d15a8` - `PlaybookExtraction.parse` recusa chave desconhecida, bloco ausente e item sem campo obrigatório como tentativa falha; C81 prova 3 tentativas fora do schema e `model_failed`, C81b o tempo limite |
| O limite de 100 mil caracteres conta o XHTML que vai ao modelo, não o texto extraído (AC 77): um .docx com 97.890 caracteres de texto gera 161.634 e falharia com `text_too_long` | exemplo próprio com `DocumentText.fromFile` a 33a3580: `plain=97890 forModel=161634`; `ImportWorker.process` compara `text.forModel().length()` | comparar `text.plain().length()` com o limite; acrescentar a C77 um .docx abaixo do limite em texto e acima em XHTML | `b1d15a8` - `ImportWorker` compara `text.plain()`; C77 prova .docx de 97 mil caracteres processado e de 100.001 recusado |
| Depois de qualquer gravação aceita, a tela mostra "Alterações não publicadas" e habilita "Publicar", mesmo quando o rascunho voltou a ser igual à versão publicada (AC 61); o clique leva `409` `nothing_to_publish` | `editor.ts` `save()` faz `state.value!.hasUnpublishedChanges = true` sem comparar; nenhum teste cobre editar e desfazer a edição depois de publicar | obter `hasUnpublishedChanges` do servidor na resposta do PUT ou recarregar o estado; testar editar e voltar ao valor publicado | `b1d15a8` - PUT devolve `hasUnpublishedChanges` e `editor.ts` o usa; C61b prova a gravação que volta à versão publicada sem "Alterações não publicadas" |
| Asserções mais frouxas que o critério: C38 só prova o limite de 30 etapas; C40 só erro de rede, sem `5xx`; C56 não prova a falta só de etapa ou só de informação; C57 não prova informação sem pergunta, objeção sem sugestão nem que objeção sem "Como o lead costuma dizer" publica; C59 não afirma os `id` dos planos marcados | `PlaybookTest.java` `itemLimits`, `publishMinimum`, `incompleteRows`, `pendingReviewBlocksPublishing`; `playbook.test.ts` `[C38b]`, `[C40]` | acrescentar os casos que faltam a cada teste e afirmar `errors[*].itemId` em `pending_review` | `b1d15a8` - C38 (50, 50 e 20 no servidor e na tela), C40 (503), C56 (só sem etapa, só sem informação), C57 (pergunta e sugestão vazias, fala do lead opcional publica) e C59 (`errors[0].itemId`) |
| O botão "Sair" do menu (AC 24) não está na captura nem na tabela `## Differences from the source`, nem na lista de textos novos | `captures/tela-6-playbook.png` mostra o menu sem "Sair"; plano, `## Differences from the source` e a linha "Textos novos" de `## Assumptions` | pedir ao usuário e registrar a linha "Sair" em `## Differences from the source` com `Confirmed: y` | `b1d15a8` - pergunta ao Rodrigo: "Sim, no rodapé do menu (Recomendado)"; AD-059 e linha "Menu sem controle no rodapé" em Differences from the source |
| Revisão externa (Codex) 1: iniciar importação com alteração não salva descarta o texto digitado | `PlaybookView.vue` habilitava "Importar documento" com `unsaved`; `reload()` sobrescreve o rascunho | bloquear a importação com alteração pendente | `8f95557` - botão desabilitado com `unsaved`; C92 |
| Revisão externa (Codex) 2: voltar pelo histórico a outro negócio mantém o editor do negócio anterior | `App.vue` reaproveitava a tela; `createEditor(props.businessId)` roda uma vez | recriar a tela por endereço | `8f95557` - `RouterView` com `key` por caminho; C94 |
| Revisão externa (Codex) 3: falha ao remover linha não mostra recuperação | `save()` sem campo deixava `failedField` nulo ou apontando para outro campo | recuperação também para operações de linha | `8f95557` - recuperação no topo quando não há campo; C93 |
| Revisão externa (Codex) 4: e-mail acima de 100 caracteres derruba a gravação da sessão | `spring_session.principal_name varchar(100)` | ampliar a coluna e limitar o e-mail aceito | `8f95557` - migração V2 com 320 caracteres e `invalid_email` acima disso; C95 |
| Revisão externa (Codex) 5: JSON do modelo com blocos nulos vira lista vazia e termina em `nothing_found` | o record normalizava antes da validação | validar tipo e nulidade no JSON bruto | `8f95557` - `PlaybookExtraction.parse` exige arrays e objeto; C81 |
| Revisão externa (Codex) 6: faltam Docker Compose e Caddyfile prometidos no Landing 1 | `deploy/` só tinha o script dos papéis | entregar Compose, Caddyfile e imagens | `8f95557` - Compose, Caddyfile, Dockerfiles e `.env.example`, subidos localmente; C99 |
| Revisão externa (Codex) 7: rascunho nulo provoca `NullPointerException` | `DraftRules.validate` acessava campos sem conferir nulo | responder erro estruturado | `8f95557` - `422` `invalid_document` para rascunho ou item nulo; C96 |
| Revisão externa (Codex) 8: erros nativos do Spring saem sem `code` e `type` | `Problems` só tratava `ApiException` | uniformizar os erros do Spring | `8f95557` - `Problems` estende `ResponseEntityExceptionHandler`, e a falha de SMTP sai como `503` `mail_unavailable`; C97, C98 |
| Asserções mais frouxas que o critério em C92, C93 e C99: C92 não afirma o botão desabilitado durante a gravação; C93 só prova a falha de "Remover", sem acrescentar nem reordenar; C99 passa com qualquer Compose válido | `review.test.ts` `[C92]`, `[C93]`; prova de C99 é só `docker compose ... config -q`. O código a f348be9 cobre os casos: `unsaved` inclui `saving` e a recuperação vale para qualquer gravação sem campo | C92: segurar o PUT pendente e afirmar o botão desabilitado; C93: falha em "+ Etapa" e numa seta; C99: acrescentar à prova a lista de serviços (`config --services` igual a `postgres`, `server`, `caddy`) e `caddy validate` do Caddyfile |  `3c22a3f` - C92 com gravação em andamento, C93 com reordenar e acrescentar, C99 com `deploy/verify.sh` |
| Revisão externa (Codex, rodada 2) 1: resposta do modelo com tipo errado ou campo obrigatório ausente termina em `done` | `PlaybookExtraction.parse` só conferia os blocos, e o record convertia `123` em texto e `2.9` em `2` | validar o JSON bruto contra o schema completo | `43e63a9` - `matches` confere tipo, `required` e propriedades em todo nível; C81 com nome numérico, parcelas `2.9` e campo ausente |
| Revisão externa (Codex, rodada 2) 2: remover linha com erro local deixa Publicar e Importar bloqueados | `removeItem` não apagava os erros da linha | apagar os erros do item removido | `43e63a9` - `removeItem` filtra os erros locais e do servidor da linha; C100 |
| Revisão externa (Codex, rodada 2) 3: sessão expirada durante a edição vira falha de gravação | o cliente HTTP tratava `401` como erro comum | levar a `/entrar` | `43e63a9` - `whenUnauthenticated` registrado pelo roteador; C101 |
| Revisão externa (Codex, rodada 2) 4: título nos metadados conta no limite de texto | o limitador do Tika recebia o XHTML inteiro | contar só o corpo | `43e63a9` - limitador dentro do `BodyContentHandler`; C77 com título de 100.001 caracteres |
| Revisão externa (Codex, rodada 2) 5: plano com `id` nulo provoca `500` | `plan.id().toString()` depois do `invalid_id` | não desreferenciar o id | `43e63a9` - `key(id)` nulo-seguro; C96 |
| Revisão externa (Codex, rodada 2) 6: logout sem sessão responde `401` sem corpo | `logoutSuccessHandler` só definia o status | escrever o problema | `43e63a9` - `Problems.write` com `unauthenticated`; C24a |
| Revisão externa (Codex, rodada 2) 7: parcela acima de R$ 10 milhões recusada na tela | `parseMoney` aplicava o teto do preço à vista | teto só no preço à vista | `43e63a9` - opção `capped`; C102 |
| Revisão externa (Codex, rodada 2) 8: C95 passa com a coluna em 200 caracteres | o teste usava e-mail de cerca de 130 caracteres | testar 320 e 321 | `43e63a9` - C95 com 320 gravado em `principal_name` e 321 recusado; mutação para 200 falha |
| Revisão externa (Codex, rodada 2) 9: `verify.sh` aprova rota só citada em comentário | o script procurava texto no Caddyfile | conferir o encaminhamento na configuração do Caddy | `43e63a9` - `caddy adapt` e leitura das rotas com `reverse_proxy` para `server:8080`; a mutação do Codex falha; C99 |
| Revisão externa (Codex, rodada 2) 9 fechada só em parte: `verify.sh` aprova Caddyfile que encaminha ao servidor só parte de uma rota | `proxies_to_server` devolve verdadeiro se qualquer rota aninhada faz `reverse_proxy`, sem olhar o matcher dela; com `handle @server { handle /api/health { reverse_proxy server:8080 } respond 404 }` o script imprime `deploy ok` | contar o caminho só quando o `reverse_proxy` está na própria rota ou numa rota aninhada sem matcher de caminho mais estreito | |
