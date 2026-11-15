# Sales Copilot

Copiloto para o closer de venda consultiva, de qualquer nicho: ajuda durante a call, treina com lead simulado e fecha a call com ata, próximo passo e resultado da venda. Cada negócio configura o próprio playbook sem programar, ou o produto monta um rascunho a partir das gravações. As 12 gravações da mentoria EPC/MEPC servem só como material de treino e de ajuste do playbook, nunca como requisito. As decisões de produto estão em `especificacao/decisoes-levantamento.md`; as tomadas no planejamento (stack, arquitetura, telas e ajustes de produto), em `.specs/STATE.md`.

O PRD está fechado, a stack está decidida e o plano da parte 1, o playbook configurável, está aprovado em `.specs/features/playbook-configuravel/plan.md`. A próxima etapa é a construção da parte 1 com a `tlc-implement`.

## Fluxo de especificação

Siga as etapas em ordem. Cada uma recebe o arquivo da anterior e não depende da conversa que o gerou.

| Etapa | Skill | Recebe | Produz |
|---|---|---|---|
| 1. Rever desde a ideia | `grilling` | Atas, pesquisas e o PRD atual como hipótese | `especificacao/decisoes-levantamento.md`, com veredito datado |
| 2. PRD de produto | `tlc-prd` | Arquivo de decisões, como decisão já tomada | `.prd/<slug>.md`, sem tecnologia |
| 3. Plano | `tlc-plan` | PRD e fontes de `especificacao/` | `.specs/features/<feature>/plan.md`, aprovado por escrito |
| 4. Construção | `tlc-implement` | Plano aprovado | Código, `checks.md` e `proofs.md` |
| 5. Avaliação | `spec-driven-eval` | PRD e implementação | Nota por critério |

Todo `.md` escrito fora de `.specs/` passa pelo `deslop` antes de a sessão encerrar. O hook de `Stop` cobra isso.

## Caminhos

- `.prd/` e `.specs/` seguem o formato das skills TLC. Os scripts de validação leem linhas literais de `.specs/`, então esses arquivos não passam pelo `deslop`.
- `especificacao/` guarda as fontes: o diagrama de jornada e o documento de telas e regras de negócio (`especificacao/design-sales-copilot.md`), que acompanha o PRD em `.prd/sales-copilot.md`.
- `.specs/STATE.md` guarda as decisões `AD-NNN` que toda parte segue, e `.specs/features/<feature>/` guarda o plano de cada parte com as capturas das telas que ele usa.
- `pesquisas/` guarda as análises das referências. `pesquisas/videos/` (gravações, atas e CRM dos leads), `pesquisas/repos/` e `pesquisas/prototipos-originais/` ficam fora do git.
- `planejamento/` guarda os planos de trabalho.

## Regras

- O protótipo é fonte obrigatória das telas. Os PNGs e o HTML originais trazem nomes de leads e ficam fora do git, em `pesquisas/prototipos-originais/`. Cada plano copia para o próprio `captures/` a tela que usa, com os dados pessoais cobertos, e essa cópia passa a ser a fonte da tela; antes disso, a fonte é o original local. Um desvio precisa ser pedido ao usuário e registrado na tabela de desvios da tela, em `especificacao/design-sales-copilot.md`.
- Documentos em PT-BR. Os títulos de seção da `tlc-prd` ficam em inglês, como a skill exige. Identificadores não são traduzidos.
- O padrão de escrita (`.claude/plugins/communication-standard/skills/writing-standard/SKILL.md`) vale para chat, documentos e código. Os hooks o injetam em toda sessão e subagente. Se não estiver no contexto, leia o arquivo.
- Git com a conta pessoal `rodrigotrk`. Push e mudanças fora do repositório pedem confirmação.
