# zoom-out

## Purpose

Pause implementation and reassess the feature or architecture from a broader perspective.

Identify whether the current approach is too narrow, overfit, overengineered, or misaligned with the PRD. Recommend whether to continue, simplify, split, stop, or return to grilling, PRD drafting, or issue planning.

Do not modify code unless explicitly asked.

## When To Use

Use this skill when implementation feels stuck, scope is expanding, abstractions are becoming questionable, or the current path may no longer match the PRD.

## Inputs To Inspect

- Current PRD and issue
- Current diff and implementation notes
- `GLOSSARY.md`
- Relevant ADRs
- Test failures or integration notes
- Handoffs from prior work

## Process

1. Restate the intended outcome and current approach.
2. Compare the approach to PRD goals and non-goals.
3. Identify overfitting, unnecessary abstraction, hidden coupling, and naming drift.
4. Separate implementation problems from product or architecture questions.
5. Recommend one of: continue, simplify, split, stop, return to grilling, return to PRD, or return to issues.
6. Name any decision that needs human approval.

## Output Format

Provide:

- Current approach summary
- Alignment with PRD
- Risks or mismatches
- Options considered
- Recommendation
- Decisions needed

## Stop Conditions

Stop after giving the reassessment.

Do not modify code, rewrite plans, create issues, or start parallel agents unless explicitly instructed.
