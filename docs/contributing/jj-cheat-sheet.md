# JJ cheat sheet

## Daily start

```sh
jj git fetch
jj status
jj log
```

Start new work from protected `main`:

```sh
jj new main
```

## Work on a change

Edit files normally.

```sh
jj status
jj diff
```

Describe the current change:

```sh
jj describe -m "fix: clarify Akai Fire encoder guide"
```

There is no separate commit step. The working copy is already a JJ change; `describe` gives it the commit message.

Start another stacked chunk:

```sh
jj new
jj describe -m "feat: add next thing"
```

## Publish a PR

Create a GitHub-visible bookmark at the change you want to publish:

```sh
jj bookmark create docs/clarify-guide -r @
jj git push --remote origin --bookmark docs/clarify-guide
gh pr create --repo oikoaudio/bitwig-oikontrol --base main --head docs/clarify-guide
```

Update the same PR after more edits:

```sh
jj status
jj diff
jj git push --remote origin --bookmark docs/clarify-guide
```

If the bookmark needs moving to the current change:

```sh
jj bookmark move docs/clarify-guide --to @
jj git push --remote origin --bookmark docs/clarify-guide
```

When the working copy is an empty JJ change on top of the actual feature tip, create the PR bookmark on the parent commit:

```sh
jj bookmark create docs/clarify-guide -r @-
```

## After PR merge

```sh
jj git fetch --remote origin
jj bookmark move main --to main@origin
jj new main
```

Optionally clean the local PR bookmark:

```sh
jj bookmark delete docs/clarify-guide
```

## Mistake recovery

Show JJ operation history:

```sh
jj op log
```

Undo the last JJ operation:

```sh
jj undo
```

Throw away the current unwanted change:

```sh
jj abandon @
```

Restore a file from the parent/current base:

```sh
jj restore path/to/file
```

## Useful review commands

```sh
jj diff
jj show @
jj log -r 'main..@'
jj bookmark list
```

With Hunk:

```sh
hunk diff
hunk show @
hunk diff --watch
```

## Hunk setup

Set Vim as JJ's editor:

```sh
jj config set --user ui.editor vim
```

Set Hunk as the default `jj diff` viewer:

```sh
jj config set --user ui.diff-formatter hunk
jj config set --user merge-tools.hunk.program hunk
jj config set --user merge-tools.hunk.diff-args '["diff", "$left", "$right"]'
```

Then verify:

```sh
jj config get ui.editor
jj config get ui.diff-formatter
jj diff
```

Useful alternative: add a native Hunk alias, since your installed `hunk diff` understands JJ revsets:

```sh
jj config set --user aliases.hunk '["util", "exec", "--", "hunk", "diff"]'
```

Then you can run:

```sh
jj hunk
jj hunk @
jj hunk 'trunk()..@'
```

For one-off use without changing defaults:

```sh
JJ_EDITOR=vim jj describe
jj diff --tool hunk
```

## Repo rules

`main` is protected. Do not push directly to it.

Use Conventional Commits because release-please reads them:

```sh
fix: patch release
feat: minor release
docs: usually no release
chore: usually no release
```

For user-facing docs here, edit `docs/user-guide.md`; bundled `Documentation/index.html` is generated. Run:

```sh
GRADLE_USER_HOME=/tmp/gradle-home ./gradlew --no-daemon generateBundledDocumentation
```

Then review generated files before publishing.
