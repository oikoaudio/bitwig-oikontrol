# Contributing

## Conventional Commits

Use Conventional Commits for PR titles and squash-merge commit messages:

```text
type(scope): short imperative summary
```

Common types:

- `feat`: user-visible functionality, controller mappings, modes, or preferences
- `fix`: bug fixes and behavior regressions
- `docs`: documentation-only changes
- `test`: test-only changes
- `refactor`: behavior-preserving code changes
- `ci`: GitHub Actions and release automation
- `chore`: version bumps and repository housekeeping

Common scopes:

- `akai-fire`
- `launchcontrol`
- `common`
- `docs`

Examples:

```text
feat(akai-fire): add perform scene launch page
fix(akai-fire): preserve scene launch pending feedback
docs(akai-fire): document perform recording gestures
ci: add akai fire github actions
chore(release): bump akai fire to 2.9.0
```

Version impact:

- `feat:` indicates a minor release.
- `fix:` indicates a patch release.
- `type!:` or a `BREAKING CHANGE:` footer indicates a major release.
- `docs:`, `test:`, `refactor:`, `ci:`, and `chore:` normally do not create a release by themselves.

## Release Artifacts

Oikontrol release artifacts are built by GitHub Actions:

- PRs and pushes to `main` run Akai Fire compile/tests, Launch Control tests, and package the combined extension.
- Tags matching `oikontrol-v*` run `just oikontrol-build`, create or update the matching GitHub release, and attach `Oikontrol.bwextension`.
- Published GitHub releases also run `just oikontrol-build` and attach `Oikontrol.bwextension`.
- Manual workflow runs build and upload `Oikontrol.bwextension` as a workflow artifact.
