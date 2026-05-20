# grill-with-docs

## Purpose

Challenge and refine a feature idea before implementation. Identify unclear behavior, hidden assumptions, bad boundaries, missing vocabulary, and conflicts with project conventions.

Recommend durable vocabulary updates to `GLOSSARY.md` when new canonical terms appear. Recommend terse ADRs when durable architectural, product, or technical decisions are discovered.

Stop before producing a PRD unless the human explicitly asks to continue to PRD drafting.

## When To Use

Use this skill when a feature idea, product change, workflow, or architecture direction needs interrogation before it becomes implementation work.

Do not use it as a substitute for implementation, issue planning, or status reporting.

## Inputs To Inspect

- The user's feature idea or problem statement
- Any referenced docs, tickets, conversations, or diagrams
- `GLOSSARY.md`
- `docs/agents/domain.md`
- `docs/agents/workflows.md`
- Relevant terse ADRs in `docs/adr/`
- Relevant external references in `docs/reference/`
- Current code only when needed to understand feasibility or existing behavior

## Process

1. Restate the idea in concrete, testable terms.
2. Identify users, goals, non-goals, and expected behavior.
3. Probe ambiguous language, unstated constraints, edge cases, and failure modes.
4. Compare the idea against existing vocabulary and project conventions.
5. Call out naming conflicts or missing canonical terms.
6. Surface durable decisions that may need a terse ADR.
7. Separate confirmed alignment from open questions.
8. Recommend the next stage only when enough clarity exists.

## ADR Guidance

Recommend an ADR only when a decision is durable, hard to reverse, surprising without context, and the result of a real trade-off.

Keep ADRs terse. The goal is to record the decision and why, not write an architecture essay.

Do not recommend ADRs for PRDs, issue breakdowns, task plans, debugging notes, ordinary implementation details, or choices that are obvious from local code.

If a decision supersedes an earlier ADR, recommend a new ADR and marking/linking the old one as superseded rather than silently rewriting history.

## Output Format

Provide:

- Summary of the aligned idea
- Open questions
- Behavioral decisions made
- Non-goals and boundaries
- Vocabulary updates to consider for `GLOSSARY.md`
- Terse ADRs to consider, including why each one clears the ADR bar
- Recommended next step

## Stop Conditions

Stop when the idea is aligned enough for a human to decide the next stage, or when unresolved questions block responsible planning.

Do not create a PRD, issues, GitHub Issues, code, tests, JJ workspaces/bookmarks, or parallel agents unless explicitly instructed.
