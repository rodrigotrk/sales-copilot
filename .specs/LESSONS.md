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

### L-007 - Block any action that reloads the draft while a field is saving or failed, so unsaved input is never discarded.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `web` · harmful: 0
- features: playbook-configuravel
- evidence: Codex 1 (C92) (web)
- last seen: 2026-11-15T19:10:45Z

### L-008 - Key route views by the resource in the address so a reused component never keeps the previous tenant.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `web` · harmful: 0
- features: playbook-configuravel
- evidence: Codex 2 (C94) (web)
- last seen: 2026-11-15T19:10:45Z

### L-009 - Show save recovery for row operations too, not only for a focused field.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `web` · harmful: 0
- features: playbook-configuravel
- evidence: Codex 3 (C93) (web)
- last seen: 2026-11-15T19:10:45Z

### L-010 - Size every column that stores an identifier to the longest value the input validation accepts.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `database` · harmful: 0
- features: playbook-configuravel
- evidence: Codex 4 (C95) (database)
- last seen: 2026-11-15T19:10:45Z

### L-011 - Validate the raw model JSON for types and nulls before mapping it into records that normalize missing data.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `importing` · harmful: 0
- features: playbook-configuravel
- evidence: Codex 5 (C81) (importing)
- last seen: 2026-11-15T19:10:45Z

### L-012 - Give every Landing artifact a check whose proof reads the artifact, so a promised file cannot go missing.
- signal: `spec_precision_gap` · recurrence: 1 feature(s) · scope: `deploy` · harmful: 0
- features: playbook-configuravel
- evidence: Codex 6 (C99) (deploy)
- last seen: 2026-11-15T19:10:45Z

### L-013 - Reject null documents and null list items at the API boundary with a structured error.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `server` · harmful: 0
- features: playbook-configuravel
- evidence: Codex 7 (C96) (server)
- last seen: 2026-11-15T19:10:46Z

### L-014 - Route framework and infrastructure errors through the same problem-details mapping as business errors.
- signal: `ac_gap` · recurrence: 1 feature(s) · scope: `server` · harmful: 0
- features: playbook-configuravel
- evidence: Codex 8 (C97, C98) (server)
- last seen: 2026-11-15T19:10:46Z

### L-015 - Prove each state and operation a criterion lists, including in-flight states and every row operation.
- signal: `weak_test` · recurrence: 1 feature(s) · scope: `web` · harmful: 0
- features: playbook-configuravel
- evidence: verification.md C92-C99 finding (web)
- last seen: 2026-11-15T19:10:46Z

## Quarantined (failed when applied - ignore)

A confirmed lesson that recurred alongside failure. Kept for the maintainer to review.

_none_
