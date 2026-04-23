# ADR 0004 — Release Automation with Release Please

## Context

- The project ships a combined Oikontrol Bitwig extension and needs a repeatable release process that updates version references, produces release notes, and uploads the built `.bwextension` artifact.
- Release history before version 2.9.0 contains commits that should not drive future automated release notes or version calculation.
- `release-please` classifies changes from commit messages, so inconsistent commit messages would make release notes and version bumps less reliable.
- `main` is protected: direct updates are blocked, and all changes must land through branches and pull requests.

## Decision

- Use `release-please` as the release authority for the repository, configured by `release-please-config.json` and `.release-please-manifest.json`.
- Set the release bootstrap point to commit `54b21ea0b0489747eedaa2542bff9b2749a77f39` so release-please ignores earlier history when preparing future releases.
- Use Conventional Commits for commits that should influence releases, for example `feat: ...`, `fix: ...`, and `docs: ...`.
- Keep `main` as the protected integration branch. Human and agent changes should be prepared on branches and merged by pull request after review/checks.
- Keep the release workflow in `.github/workflows/release-please.yml`: on `main`, release-please opens or updates release PRs, and when a release is created the workflow builds the combined extension and uploads `modules/oikontrol/build/libs/Oikontrol.bwextension` to the GitHub release.

## Status

Accepted.

## Consequences

- Release notes and version bumps are derived from Conventional Commit history after the bootstrap SHA.
- Contributors and coding agents need to keep commit messages release-please friendly; repository agent instructions should continue to call this out.
- Protected `main` prevents accidental direct pushes and gives CI/review a consistent gate before changes can affect release automation.
- Every update requires branch and PR overhead, including small documentation or configuration changes, so local work should be grouped sensibly before opening PRs.
- Changes to versioned files must stay listed in `release-please-config.json` when release-please should update them automatically.
- If the release process changes materially, update this ADR or add a follow-up ADR rather than leaving only workflow/config changes to explain the decision.
