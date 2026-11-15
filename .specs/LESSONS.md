# LESSONS - auto-maintained by scripts/lessons.py

> Machine-owned. Do NOT hand-edit. Changes are overwritten on the next `lessons.py` write.
> Canonical state lives in `.specs/lessons.json`. Edit lessons only via the script.
> promote_threshold=2 distinct features · window_days=45 · quarantine_threshold=2

## Confirmed (load these at Plan/Checks)

Corroborated across multiple features. Safe to apply as guidance.

_none_

## Candidates (under observation - do NOT load as guidance yet)

Seen once or not yet corroborated. Tracked, not trusted.

### L-001 - Give each money token in a document a single reading and test thousands-separated values against small model answers.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `importing` · harmful: 0
- features: playbook-configuravel
- evidence: verification.md finding 1 (C71) (importing)
- last seen: 2026-11-15T18:39:04Z

### L-002 - Validate model output against the requested schema and count a schema violation as a failed attempt.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `importing` · harmful: 0
- features: playbook-configuravel
- evidence: verification.md finding 2 (C81) (importing)
- last seen: 2026-11-15T18:39:04Z

### L-003 - Apply document size limits to the extracted text, not to the markup sent to the model.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `importing` · harmful: 0
- features: playbook-configuravel
- evidence: verification.md finding 3 (C77) (importing)
- last seen: 2026-11-15T18:39:04Z

### L-004 - Take derived state like unpublished changes from the server after each save and test an edit that returns to the published content.
- signal: `weak_test` · recurrence: 1 feature(s) · scope: `web` · harmful: 0
- features: playbook-configuravel
- evidence: verification.md finding 4 (C61) (web)
- last seen: 2026-11-15T18:39:04Z

### L-005 - Prove every limit, required field and status a rule names, not one representative case.
- signal: `weak_test` · recurrence: 1 feature(s) · harmful: 0
- features: playbook-configuravel
- evidence: verification.md finding 5 (C38, C40, C56, C57, C59)
- last seen: 2026-11-15T18:39:04Z

### L-006 - List every control the plan adds to a captured screen in Differences from the source before building it.
- signal: `spec_deviation` · recurrence: 1 feature(s) · scope: `screens` · harmful: 0
- features: playbook-configuravel
- evidence: verification.md finding 6 (C24) (screens)
- last seen: 2026-11-15T18:39:04Z

## Quarantined (failed when applied - ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_
