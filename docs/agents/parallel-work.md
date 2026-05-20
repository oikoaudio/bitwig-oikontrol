# Parallel work

The intended model is:

```text
A single agent works on one approved issue at a time.
Multiple agents may work in parallel on different approved issues.
```

Parallel work is allowed only when issues are:

- approved for implementation
- not blocked by unresolved dependencies
- assigned to separate work areas
- scoped to avoid overlapping file/module ownership
- labelled as safe for parallel work

For this JJ repo, a separate work area means a separate JJ workspace/change, with a dedicated bookmark when publishing a PR.

Do not start parallel agents unless the user explicitly asks to run parallel implementation.

## Dependency handling

- Publish or identify dependency issues before dependent slices.
- Do not implement a blocked issue while its blocker is unresolved.
- If an issue discovers a new shared dependency, stop and report/block rather than silently expanding scope.
- Do not parallelize foundation work unless explicitly approved.

Foundation work includes:

- schemas
- shared types
- public APIs
- naming
- architecture
- global config
- core domain boundaries
- controller-wide behaviour contracts

## File and module ownership

- Use one separate work area per issue.
- Give each agent clear file/module ownership.
- Avoid overlapping edits to shared helpers, generated docs, build files, and controller-wide contracts.
- No broad drive-by refactors during parallel implementation.
- Do not rename, move, or reformat unrelated code.
- Treat shared contracts as frozen during parallel issue implementation.
- If a shared contract is wrong, stop and report/block rather than silently changing it.

## Integration pass

After parallel slices complete, run a single integration pass.

The integration pass should:

- review changed files for accidental ownership overlap
- rerun relevant tests across all affected modules
- verify controller invariants in `docs/agents/controller-invariants.md`
- reconcile linked issues with the PRD parent issue
- update durable docs only for behaviour that actually landed

Integration work should be labelled `integration` when represented as an issue.
