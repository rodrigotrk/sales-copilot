# Fluxo do harness — Fase 0: preparar o projeto

A Fase 0 deixa o `sales-copilot` pronto para o fluxo de especificação. Ela termina com o projeto versionado no GitHub, as skills do fluxo instaladas e um `CLAUDE.md` que fixa a ordem, os caminhos e as regras. Nenhum documento de produto muda nesta fase.

O fluxo completo:

```
grilling → tlc-prd → tlc-plan → tlc-implement → spec-driven-eval
   └──────────── deslop audita todo .md escrito ────────────┘
```

| Skill | Origem | Papel |
|---|---|---|
| `grilling` | `mattpocock/skills` (MIT) | Entrevista em rodadas para fechar perguntas em aberto |
| `tlc-prd` | `tech-leads-club/agent-skills` (CC-BY-4.0) | PRD só de produto, sem tecnologia |
| `tlc-plan` | idem | Plano com critérios EARS e aprovação humana registrada |
| `tlc-implement` | idem | Checks com provas, build e Verifier independente |
| `spec-driven-eval` | idem | Nota de quanto a implementação cumpre o PRD |
| `deslop` + `writing-standard` | `communication-standard`, já no projeto | Padrão de escrita e auditoria de cortes |

## Estado atual do harness

- `.claude/settings.json` liga quatro hooks:
  - `SessionStart` e `SubagentStart` injetam o `writing-standard` em toda sessão e subagente;
  - `PostToolUse` anota todo `.md` escrito;
  - `Stop` bloqueia o encerramento uma vez e manda um subagente aplicar o `deslop` nesses arquivos (`.claude/hooks/doc_review.py`).
- As skills `deslop` e `writing-standard` ficam em `.claude/plugins/communication-standard/skills/`. Elas não aparecem como `/skill`: os hooks as leem pelo caminho, e isso é intencional.
- O `deslop` local é a tradução para PT-BR da versão 0.1.0. A versão 0.2.0, em `~/projetos/anoria/workspace/ANORIA-MARKETPLACE/plugins/anoria-communication/skills/deslop`, acrescenta padrões novos ao `references/prose.md`, como "Decisão enterrada" e metadiscurso sobre como o texto foi feito, e ajusta o `writing-standard/SKILL.md` e o `references/code.md`.
- O projeto não é um repositório git.
- `pesquisas/videos/` ocupa 4,1 GB. Cada pasta de `Diagnosticos/` guarda a gravação `.mov`, a legenda `.srt`, a ata `.md` e o `CRM.txt` de um lead, com nome e dados da call.
- `pesquisas/repos/playbook-sales-copilot` é um clone de terceiros com o próprio `.git`.

## Passos

### 1. Versionar e publicar

1. Criar o `.gitignore`:
   ```gitignore
   .DS_Store
   __pycache__/
   *.mov
   *.mp4
   pesquisas/repos/
   pesquisas/videos/
   ```
   O GitHub recusa arquivos acima de 100 MB, e os vídeos passam disso. O clone de terceiros fica fora para não virar um repositório aninhado.
2. `pesquisas/videos/` fica inteiro fora do GitHub, inclusive atas, legendas e `CRM.txt`, porque identificam leads reais. As análises em `pesquisas/*.md` são versionadas.
3. Configurar a identidade pessoal para `~/Estudos/`: este projeto usa a conta pessoal `rodrigotrk`, e a `rodrigo-anoria`, exclusiva do projeto Anoria, nunca toca este repositório. O `~/.gitconfig` já separa as identidades por pasta com `includeIf`, mas `~/Estudos/` não tem regra. Criar `~/.gitconfig-pessoal` com `user.email = rodrigo.trk@gmail.com` e `core.sshCommand` apontando para a chave SSH da conta `rodrigotrk` (hoje `~/.ssh/id_i4business`), e acrescentar ao `~/.gitconfig`:
   ```gitconfig
   [includeIf "gitdir:~/Estudos/"]
   	path = ~/.gitconfig-pessoal
   ```
4. `git init -b main`, primeiro commit e `git remote add origin git@github.com:rodrigotrk/sales-copilot.git`.
5. Criar o repositório `rodrigotrk/sales-copilot` como privado, pela web ou pelo `gh` com a conta `rodrigotrk`, e fazer o push pelo SSH. O `gh` desta máquina está autenticado só como `rodrigo-anoria`, então não serve para este passo.

A `tlc-implement` exige git: ela faz um commit por check e o `check_proofs.py` confere as provas no `HEAD`.

### 2. Instalar as skills do Tech Leads Club

```bash
cd /Users/rodrigo/Estudos/vendas/sales-copilot
npx @tech-leads-club/agent-skills install -s tlc-prd tlc-plan tlc-implement spec-driven-eval -a claude-code
```

Elas vão para `.claude/skills/` e aparecem como `/skill`. As skills não são editadas, para continuar recebendo atualizações. As adaptações ficam no `CLAUDE.md` (passo 5). O `python3` 3.15 da máquina atende aos scripts de validação. Rodar o `selftest.py` da `tlc-plan` e da `tlc-implement` para confirmar.

### 3. Copiar o `grilling`

Copiar `skills/productivity/grilling/SKILL.md` de `mattpocock/skills` para `.claude/skills/grilling/SKILL.md`, com a licença MIT ao lado. Não há instalador para uma skill só desse repositório.

### 4. Ajustar o `communication-standard`

1. Acrescentar `.specs` ao `EXCLUDED_PARTS` do `doc_review.py`. A `tlc-plan` e a `tlc-implement` escrevem `plan.md`, `checks.md` e `proofs.md` em `.specs/`, e os scripts de validação leem linhas literais desses arquivos, como `**Approved**: ...` e `Proof: run ...`. Um corte do `deslop` ali quebra a validação.
2. Portar para a tradução PT-BR as diferenças da 0.2.0 no `deslop` e no `writing-standard`. A versão local passa a ser 0.2.0.

O `SubagentStart` também injeta o padrão no Verifier e no subagente de telas da `tlc-implement`. Isso é aceitável: o padrão governa a escrita, não o julgamento.

### 5. Escrever o `CLAUDE.md`

Ele fixa, em poucas linhas:

- **A ordem do fluxo**, com a entrada e a saída de cada skill.
- **Os caminhos.** As skills definem os seus: o PRD em `.prd/<slug>.md` e os planos, checks e provas em `.specs/`. A pasta `especificacao/` guarda o que as skills recebem como fonte: protótipos, diagramas, notas técnicas e o documento de telas e regras de negócio (`especificacao/design-sales-copilot.md`). O PRD atual, `especificacao/prd-sales-copilot.md`, vai para `.prd/` quando a `tlc-prd` o reescrever, na Fase 2.
- **As capturas.** Os PNGs e o HTML de `especificacao/prototipos/` são fonte obrigatória das telas: um desvio precisa ser pedido e registrado.
- **O idioma.** Os documentos são em PT-BR. Os títulos de seção da `tlc-prd` ficam em inglês, como a skill exige.
- **A fase atual.** O projeto está em levantamento: código só depois que o PRD estiver fechado e a stack decidida.

### 6. Verificar

Abrir uma sessão nova no projeto e confirmar:

- [ ] `/grilling`, `/tlc-prd`, `/tlc-plan`, `/tlc-implement` e `/spec-driven-eval` aparecem na lista de skills.
- [ ] O `writing-standard` chega no início da sessão, pelo hook.
- [ ] Escrever um `.md` de teste fora de `.specs/` dispara o `deslop` no Stop, e um dentro de `.specs/` não dispara.
- [ ] Os `selftest.py` passam.
- [ ] `git status` limpo e o repositório privado no GitHub com o mesmo conteúdo, sem vídeos.

## Fora desta fase

- Fechar as perguntas em aberto do PRD, com o `grilling` (Fase 1).
- Reescrever o PRD e separar telas e regras (Fase 2).
- Escolher a stack e o comando de testes, que são pré-requisitos da `tlc-plan` (Fase 3).
