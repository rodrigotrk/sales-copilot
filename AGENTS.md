# Sales Copilot

Copiloto de vendas consultivas: ajuda o closer durante a call e automatiza o que vem depois. Cada negócio entra como um playbook configurável. A mentoria EPC/MEPC é o primeiro playbook e o piloto.

O projeto está em levantamento. Código só depois que o PRD estiver fechado e a stack decidida.

## Fluxo de especificação

Siga as etapas em ordem. Cada uma recebe o arquivo da anterior e não depende da conversa que o gerou.

| Etapa | Skill | Recebe | Produz |
|---|---|---|---|
| 1. Fechar perguntas em aberto | `grilling` | PRD e atas das calls | Decisões registradas no PRD |
| 2. PRD de produto | `tlc-prd` | Problema e decisões | `.prd/<slug>.md`, sem tecnologia |
| 3. Plano | `tlc-plan` | PRD e fontes de `especificacao/` | `.specs/features/<feature>/plan.md`, aprovado por escrito |
| 4. Construção | `tlc-implement` | Plano aprovado | Código, `checks.md` e `proofs.md` |
| 5. Avaliação | `spec-driven-eval` | PRD e implementação | Nota por critério |

Todo `.md` escrito fora de `.specs/` passa pelo `deslop` antes de a sessão encerrar. O hook de `Stop` cobra isso.

## Caminhos

- `.prd/` e `.specs/` seguem o formato das skills TLC. Os scripts de validação leem linhas literais de `.specs/`, então esses arquivos não passam pelo `deslop`.
- `especificacao/` guarda as fontes: protótipos, diagramas, notas técnicas e o documento de telas e regras de negócio. O PRD atual, `especificacao/prd-sales-copilot.md`, vai para `.prd/` quando a `tlc-prd` o reescrever.
- `pesquisas/` guarda as análises das referências. `pesquisas/videos/` (gravações, atas e CRM dos leads) e `pesquisas/repos/` ficam fora do git.
- `planejamento/` guarda os planos de trabalho do projeto.

## Regras

- Os PNGs e o HTML de `especificacao/prototipos/` são fonte obrigatória das telas. Um desvio precisa ser pedido ao usuário e registrado.
- Documentos em PT-BR. Os títulos de seção da `tlc-prd` ficam em inglês, como a skill exige. Identificadores não são traduzidos.
- O padrão de escrita (`.claude/plugins/communication-standard/skills/writing-standard/SKILL.md`) vale para chat, documentos e código. Os hooks o injetam em toda sessão e subagente. Se não estiver no contexto, leia o arquivo.
- Git com a conta pessoal `rodrigotrk`. Push e mudanças fora do repositório pedem confirmação.
