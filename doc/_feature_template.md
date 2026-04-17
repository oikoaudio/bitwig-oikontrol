# Feature Plan Template

Use this template for feature plans that are meant to guide implementation work.

Keep it concrete. Prefer explicit behavior, scope boundaries, file references, and testable acceptance criteria over long design narrative.

---

# [Feature Name]

## Summary

Briefly describe the feature and the intended user-facing outcome.

Include any important first-pass constraints, for example:

- what should change
- what should stay the same
- any notable exclusions for the first pass

## Goal

Describe the target workflow in concrete terms.

Example:

1. User enters `MODE_X`
2. User performs gesture `Y`
3. The controller responds with behavior `Z`

## Acceptance Criteria

List the conditions that must be true before implementation starts and before the feature is considered complete.

- behavior A works in situation B
- existing behavior C remains unchanged
- docs and feedback reflect the new behavior

## Current Implementation

Document the current code shape and relevant constraints.

Include:

- current class or subsystem responsible
- hard-coded assumptions that matter
- relevant entry points
- existing behavior that must be preserved

Add direct file references where useful.

## Scope Recommendation

State the recommended scope for the first implementation pass.

Include:

- in scope
- explicitly out of scope
- follow-up work that should be deferred

## UX Proposal

Describe the intended interaction model.

Suggested subsections:

### Entering and Toggling

### Layout or Control Mapping

### Gestures

### Feedback

Document:

- button combinations
- pad behavior
- encoder behavior
- OLED or popup wording if relevant

## Implementation Direction

Describe the preferred code approach without over-prescribing the final implementation.

Useful prompts:

- should this be a local refactor or a new abstraction?
- should the feature reuse an existing state model?
- where should branching logic live?
- what would make the change easier to test?

If helpful, break this into numbered steps.

## Technical Notes

Capture supporting details that matter for implementation but are not the main UX spec.

Examples:

- API limitations or uncertainty
- paging/indexing rules
- state ownership
- performance constraints
- data-model notes

## Suggested File Touches

List the files most likely to change.

If appropriate, separate:

- likely edits
- possible new helpers
- documentation files to update
- tests to add or update

## Testing Strategy

Split this into the kinds of verification that matter.

Suggested subsections:

### Unit or Logic Tests

### Manual Smoke Tests

### Regression Areas

Prefer concrete checks over generic statements.

## Risks

Call out the main ways the work could go wrong.

Examples:

- hidden hard-coded assumptions
- API uncertainty
- regressions in similar modes
- complexity creep from mixing unrelated concerns

## Open Questions

Capture unresolved decisions that are important enough to note but not blocking enough to stop drafting the plan.

If there are no important open questions, say so.

## Success Criteria

Summarize the end state in a short, scannable list.

- feature works
- key preserved behavior still works
- tests cover the core logic
- docs describe the change accurately

## Recommendation

State the recommended implementation order.

For larger work, split into passes, for example:

### Pass 1

- minimal viable user-facing feature

### Pass 2

- refinements
- follow-up ergonomics
- deferred edge cases
