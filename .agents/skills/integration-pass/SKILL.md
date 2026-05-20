# integration-pass

## Purpose

Reconcile work after multiple slices or parallel agents. Check for duplicated concepts, inconsistent naming, stale mocks, missing tests, conflicts with PRD acceptance criteria, and broken invariants.

Do not add new feature scope.

## When To Use

Use this skill after multiple issues, JJ workspaces/changes, or parallel agent outputs need to be reviewed together before final delivery.

## Inputs To Inspect

- Parent PRD
- Completed issue bodies and handoffs
- Changed files and diffs
- Test results
- `GLOSSARY.md`
- Relevant ADRs
- `docs/agents/parallel-work.md`

## Process

1. Re-read the PRD acceptance criteria.
2. Compare completed slices against the PRD and issue boundaries.
3. Identify duplicated or conflicting concepts.
4. Check naming consistency with `GLOSSARY.md`.
5. Find stale mocks, temporary scaffolding, and missing integration tests.
6. Resolve merge or behavior conflicts within existing scope.
7. Run appropriate focused and broad checks.
8. Document remaining risks and follow-up issues.

## Output Format

Provide:

- Integration summary
- Conflicts found and resolved
- Remaining gaps
- Checks run
- Checks not run
- Follow-up issues or ADR/glossary updates

## Stop Conditions

Stop when the integrated work satisfies the PRD as far as practical, or when unresolved conflicts require human decision.

Do not add new product scope or start new feature work unless explicitly instructed.
