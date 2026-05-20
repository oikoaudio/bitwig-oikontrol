# to-prd

## Purpose

Turn an aligned idea into an implementation-facing PRD. Draft to `.scratch/prd/` by default.

The PRD is authoritative for feature intent, scope, non-goals, and acceptance criteria. It is not proof of current code behavior.

Optionally produce a GitHub PRD parent issue body when explicitly requested.

Stop after producing the PRD unless the human explicitly asks to continue to issue drafting.

## When To Use

Use this skill after grilling, discovery, or human discussion has produced enough alignment to define implementation intent.

Do not use it when the idea still needs fundamental clarification.

## Inputs To Inspect

- Aligned idea notes or grilling output
- `GLOSSARY.md`
- `docs/agents/domain.md`
- `docs/agents/workflows.md`
- Relevant ADRs in `docs/adr/`
- Relevant references in `docs/reference/`
- Current code only when needed to avoid impossible or misleading requirements

## Process

1. Confirm the requested PRD scope.
2. Identify the target user, problem, outcome, and success criteria.
3. Define in-scope behavior and non-goals.
4. Capture assumptions and dependencies.
5. Write acceptance criteria in externally observable terms.
6. Note vocabulary or ADR updates that should be handled separately.
7. Save the draft under `.scratch/prd/` unless another destination is requested.
8. If requested, produce a GitHub parent issue body using the PRD.

## Output Format

A PRD should include:

- Title
- Status
- Context
- Goals
- Non-goals
- User-visible behavior
- Scope
- Acceptance criteria
- Dependencies and assumptions
- Risks
- Open questions
- Follow-up docs, glossary entries, or ADRs

## Stop Conditions

Stop after drafting or updating the PRD.

Do not generate issues, publish GitHub Issues, implement code, or start parallel agents unless explicitly instructed.
