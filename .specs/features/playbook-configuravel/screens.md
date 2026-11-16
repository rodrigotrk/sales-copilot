# Playbook configurável screens

**Verifier**: independent sub-agent (author != verifier)
**Round**: 2
**HEAD**: 1f82212

| Screen | Compared with | Verdict | Finding |
| --- | --- | --- | --- |
| `captures/tela-6-playbook.running.png` | `captures/tela-6-playbook.png` | matches | no row differs |

### `captures/tela-6-playbook.running.png`

Both images are 1440 x 1300 and the shared labels (title, block headings, table text) read at the same size, so the picture is not resized.

| Element | Result | Picture draws |
| --- | --- | --- |
| render: picture size and text size against the capture | same | |
| page arrangement: fixed left menu, top bar across the content, content area on a light grey background | same | |
| menu: avatar "S" in a purple circle at the top | same | |
| menu items: icon over label, in the order the Differences give (Nova call, Ao vivo, Calls, Coach, Playbook) | same | Note: Nova call uses the calendar icon the capture uses for Agenda |
| menu: horizontal divider before Coach | same | divider between Calls and Coach, which follows from Pipeline leaving the menu |
| menu: active item (Playbook) on a light purple rounded tile, purple icon and label | same | |
| menu: Pipeline badge "1" | same | gone with Pipeline, per the Differences |
| menu: no control at the bottom | same | "Sair" at the bottom of the menu, per the Differences row "Menu sem controle no rodapé" (AD-059) |
| top bar: left-aligned text, bottom border | same | "Playbook: Venda consultiva · versão 1", replacing the selo, timer, method text and prototype note per the Differences |
| title "Playbook", bold, top left of content | same | |
| subtitle under the title, grey | same | "Cada negócio cadastra o próprio jeito de vender, sem programar." per the Differences |
| selo "Novo: …" under the subtitle | same | removed per the Differences |
| "Importar documento" and "Publicar" (Differences addition) | same | both on the right of the title row; "Importar documento" outlined, "Publicar" filled purple in its disabled shade with no "Alterações não publicadas", which fits a draft equal to version 1 |
| row 1 arrangement: "Etapas da call" card left (narrower), "O que precisa ser descoberto na call" card right (wider), equal height | same | |
| cards: white, rounded corners, thin grey border | same | |
| block headings: small monospace uppercase grey letter-spaced text | same | |
| etapas: numbered list, number in a light purple circle, name to the right, divider line under each row | same | arrows and "Remover" are not drawn; they belong to the hover or edit state, not judged here |
| etapas: outlined "+ Etapa" button under the list, left | same | |
| descoberto: two-column table, headers "Informação" and "Pergunta sugerida" in monospace uppercase, divider lines | same | Note: the question column starts about 25 px further left |
| descoberto: suggested question in double quotes | same | |
| descoberto: "+ Informação" outlined button under the table (Differences addition) | same | |
| row 2: "Objeções e sugestões de fala" card spanning the full width | same | |
| objeções: outlined "+ Objeção" at the top right of the card | same | |
| objeções: headers "Objeção", "Como o lead costuma dizer", "Sugestão de fala" | same | |
| objeções: objection name bold, lead phrase grey in quotes, suggestion dark in quotes, divider lines | same | |
| objeções: "Nas calls" column with count chips | same | hidden, per the Differences and criterion 53 |
| row 3 arrangement: "Tabela de preços" card left, same width as the etapas card | same | |
| row 3: "Testar o método" card on the right | same | removed per the Differences; the right half of the row is empty |
| preços: headers "Plano", extra column ("Encontros"), "À vista", "Parcelado" in monospace uppercase | same | extra column carries the gestor's name, per the Differences |
| preços: à vista as "R$ 4.500" and parcelado as "3x 1.500" in monospace, divider lines | same | Note: columns sit slightly closer together |
| preços: "+ Plano" outlined button under the table (Differences addition) | same | |
| preços: "Desconto máximo na call:" grey with the yellow "a definir" chip in monospace uppercase | same | |
| empty space under the last row | same | |
