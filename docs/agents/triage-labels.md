# Triage labels

This repo uses Pocock-style triage labels where practical.

## Mutually exclusive state labels

Use at most one state label on an issue at a time:

- `needs-triage`: maintainer needs to evaluate the issue.
- `needs-info`: more information is needed before the issue can move forward.
- `ready-for-agent`: fully specified and ready for agent implementation.
- `ready-for-human`: needs human judgment or manual action.
- `wontfix`: intentionally not planned.

Prefer `ready-for-agent` over inventing `agent-ready`.

## Coordination labels

These labels may be combined with state labels:

- `prd`: approved or draft PRD parent issue.
- `slice`: vertical implementation issue linked to a PRD parent.
- `foundation`: shared contract, architecture, naming, API, schema, or core boundary work.
- `parallel-safe`: approved for parallel implementation when dependencies are clear.
- `blocked`: blocked by unresolved dependencies or missing decisions.
- `integration`: integration pass or cross-slice verification work.

Agents must not create, rename, or apply labels unless explicitly instructed by the user.
