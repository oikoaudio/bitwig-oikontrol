---
name: writing-commit-messages
description: Writes Git commit messages. Activates when the user asks to write a commit message, draft a commit message for a PR/merge request message, or similar.
---

Write Git messages in Conventional Commits style so release-please can classify changes.

For ordinary commit messages:

- Use `type(scope): summary` when a clear scope exists, otherwise `type: summary`.
- Prefer these types: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`, `build`, `ci`, `perf`, `style`, `revert`.
- Keep the summary imperative, concrete, and under roughly 72 characters.
- Add a body only when it clarifies motivation, tradeoffs, or notable behavioral details.
- For breaking changes, add `!` after the type or scope and include a `BREAKING CHANGE:` footer.

For PR or merge request messages that will become the merge commit:

- Make the title a valid Conventional Commit header that release-please can parse.
- Put details in the body, not in the title.
- Use the body to summarize important implementation details, user-visible behavior, tests, migrations, or follow-up notes.
- If multiple changes are included, choose the release-relevant primary type for the title and explain secondary changes in the body.
- Include `BREAKING CHANGE:` footers when applicable.

Do not invent issue numbers, scopes, breaking changes, or test results. If the diff or user context is insufficient, either omit that detail or state the assumption clearly.
