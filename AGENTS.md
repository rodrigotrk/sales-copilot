# Sales Copilot

Copiloto para o closer de venda consultiva, de qualquer nicho: ajuda durante a call, treina com lead simulado e fecha a call com ata, próximo passo e resultado da venda. Cada negócio configura o próprio playbook sem programar, ou o produto monta um rascunho a partir das gravações. As 12 gravações da mentoria EPC/MEPC servem só como material de treino e de ajuste do playbook, nunca como requisito. As decisões de produto estão em `especificacao/decisoes-levantamento.md`.

O projeto está em levantamento. Código só depois que o PRD estiver fechado e a stack decidida.

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
- `especificacao/` guarda as fontes: protótipos, diagramas, notas técnicas e o documento de telas e regras de negócio (`especificacao/design-sales-copilot.md`), que acompanha o PRD em `.prd/sales-copilot.md`.
- `pesquisas/` guarda as análises das referências. `pesquisas/videos/` (gravações, atas e CRM dos leads) e `pesquisas/repos/` ficam fora do git.
- `planejamento/` guarda os planos de trabalho.

## Regras

- Os PNGs e o HTML de `especificacao/prototipos/` são fonte obrigatória das telas. Um desvio precisa ser pedido ao usuário e registrado na tabela de desvios da tela, em `especificacao/design-sales-copilot.md`.
- Documentos em PT-BR. Os títulos de seção da `tlc-prd` ficam em inglês, como a skill exige. Identificadores não são traduzidos.
- O padrão de escrita (`.claude/plugins/communication-standard/skills/writing-standard/SKILL.md`) vale para chat, documentos e código. Os hooks o injetam em toda sessão e subagente. Se não estiver no contexto, leia o arquivo.
- Git com a conta pessoal `rodrigotrk`. Push e mudanças fora do repositório pedem confirmação.
