#!/usr/bin/env python3
"""Obriga todo documento escrito no projeto a passar pelo deslop antes de a sessão encerrar.

`record` (PostToolUse em Write/Edit/MultiEdit) anota os .md alterados numa lista por sessão.
`stop` (Stop) bloqueia o encerramento uma vez enquanto houver documentos anotados e manda
auditá-los; no Stop seguinte (stop_hook_active) a lista é limpa, o que garante uma única
passada de revisão em vez de um laço, já que as próprias edições do deslop voltam a anotar.
"""
import json
import sys
import tempfile
from pathlib import Path

PROJECT = Path(__file__).resolve().parents[2]
STATE_DIR = Path(tempfile.gettempdir()) / "claude-doc-review"
# Transcrições são literais, repos clonados são de terceiros e .specs/ é lido pelos scripts da TLC: revisar estragaria o conteúdo.
EXCLUDED_PARTS = {".claude", ".specs", "repos", "node_modules"}
EXCLUDED_NAME_FRAGMENTS = ("transcricao",)


def is_doc(path: Path) -> bool:
    try:
        rel = path.resolve().relative_to(PROJECT)
    except ValueError:
        return False
    if path.suffix.lower() not in {".md", ".mdx"}:
        return False
    if EXCLUDED_PARTS.intersection(rel.parts):
        return False
    return not any(f in path.name.lower() for f in EXCLUDED_NAME_FRAGMENTS)


def state_file(session_id: str) -> Path:
    return STATE_DIR / f"{session_id}.json"


def load(session_id: str) -> list[str]:
    try:
        return json.loads(state_file(session_id).read_text())
    except (OSError, ValueError):
        return []


def save(session_id: str, docs: list[str]) -> None:
    STATE_DIR.mkdir(parents=True, exist_ok=True)
    state_file(session_id).write_text(json.dumps(docs))


def record(event: dict) -> None:
    file_path = (event.get("tool_input") or {}).get("file_path")
    if not file_path or not is_doc(Path(file_path)):
        return
    docs = load(event["session_id"])
    rel = str(Path(file_path).resolve().relative_to(PROJECT))
    if rel not in docs:
        save(event["session_id"], docs + [rel])


def stop(event: dict) -> None:
    session_id = event["session_id"]
    if event.get("stop_hook_active"):
        state_file(session_id).unlink(missing_ok=True)
        return
    docs = [d for d in load(session_id) if (PROJECT / d).exists()]
    if not docs:
        return
    listing = "\n".join(f"- {d}" for d in docs)
    reason = (
        "Revisão de escrita obrigatória neste projeto. Estes documentos foram escritos ou alterados "
        f"nesta sessão:\n{listing}\n\n"
        "Antes de encerrar, dispare um subagente (Agent) para auditar todos esses arquivos com o deslop, "
        "para que a revisão não fique ancorada em quem escreveu. Passe a ele: "
        "'Leia .claude/plugins/communication-standard/skills/deslop/SKILL.md e siga-o, com references/prose.md, em cada um destes arquivos: "
        f"{', '.join(docs)}. O usuário já pediu a aplicação: aplique diretamente todos os cortes que respeitam "
        "o piso (nenhum fato, número, ressalva ou dúvida genuína perdido, nada acrescentado). Devolva por "
        "arquivo: quantos cortes aplicou, os padrões principais, o que ficou em Mantido e as Perguntas.' "
        "Depois relate ao usuário, numa linha por arquivo, o que mudou, e repasse as Perguntas."
    )
    print(json.dumps({"decision": "block", "reason": reason}))


def main() -> int:
    event = json.load(sys.stdin)
    {"record": record, "stop": stop}[sys.argv[1]](event)
    return 0


if __name__ == "__main__":
    sys.exit(main())
