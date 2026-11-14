#!/usr/bin/env python3
"""Injeta o padrão de escrita como contexto nos eventos SessionStart e SubagentStart.

Uma falha aqui nunca pode bloquear a sessão: se o padrão não puder ser lido, o script
ainda sai com 0 e injeta um aviso curto no lugar, para o usuário saber que o padrão
está configurado mas inativo.
"""
import json
import re
import sys
from pathlib import Path

STANDARD = Path(__file__).resolve().parent.parent / "skills" / "writing-standard" / "SKILL.md"
EVENTS = {"SessionStart", "SubagentStart"}
UNREADABLE = (
    "O padrão de escrita do projeto está configurado, mas não foi possível ler "
    ".claude/plugins/communication-standard/skills/writing-standard/SKILL.md. "
    "Avise o usuário uma vez que o padrão fica inativo até o arquivo ser restaurado."
)
# O front matter só serve para descobrir a skill; a sessão que recebe o padrão inteiro não precisa dele.
FRONT_MATTER = re.compile(r"\A---\n.*?\n---\n+", re.DOTALL)


def main() -> int:
    event = sys.argv[1] if len(sys.argv) > 1 else ""
    if event not in EVENTS:
        return 0
    try:
        context = FRONT_MATTER.sub("", STANDARD.read_text(encoding="utf-8"))
    except (OSError, UnicodeError):
        context = UNREADABLE
    envelope = {"hookSpecificOutput": {"hookEventName": event, "additionalContext": context}}
    print(json.dumps(envelope))
    return 0


if __name__ == "__main__":
    sys.exit(main())
