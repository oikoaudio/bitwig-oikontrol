---
name: to-issues
description: Split an approved PRD into vertical-slice GitHub Issue-ready bodies with dependencies, parallel-safety, acceptance criteria, likely touched files, files to avoid, test strategy, integration risks, and expected labels/state.
---

# to-issues

## Purpose

Split an approved PRD into vertical-slice GitHub Issue-ready bodies.

Each issue should include dependencies, parallel-safety, acceptance criteria, likely touched files/modules, files/modules to avoid, test strategy, integration risks, and labels/state.

Distinguish foundation, slice, integration, ready-for-agent, ready-for-human, blocked, and parallel-safe work.

Stop after drafting unless explicitly instructed to publish or implement.

## When To Use

Use this skill when a PRD has been approved by the human and needs to become an implementation queue.

Do not use it for unapproved PRDs or vague ideas.

## Inputs To Inspect

- Approved PRD
- Parent GitHub Issue, if one exists
- `docs/agents/issue-tracker.md`
- `docs/agents/triage-labels.md`
- `docs/agents/parallel-work.md`
- `GLOSSARY.md`
- Relevant ADRs and reference docs
- Current code structure to estimate likely touched modules

## Process

1. Confirm that the PRD is approved for issue drafting.
2. Identify foundation work that blocks slices.
3. Split user-visible behavior into vertical slices.
4. Add an integration issue when multiple slices may conflict.
5. Mark dependencies explicitly.
6. Mark `parallel-safe` only when work is dependency-unblocked and unlikely to collide.
7. Prefer issues that one agent can complete and verify independently.
8. Draft issue bodies under `.scratch/issues/` unless another destination is requested.

## Output Format

Each issue body should include:

- Parent PRD
- Goal
- User-visible behavior
- Non-goals
- Dependencies
- Parallelization notes
- Acceptance criteria
- Likely files/modules touched
- Files/modules to avoid
- Test strategy
- Integration risks
- Expected labels

## Stop Conditions

Stop after issue drafts are produced.

Do not publish GitHub Issues, implement drafted issues, modify code, or start parallel agents unless explicitly instructed.
