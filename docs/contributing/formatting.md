# Source formatting

The repository uses `.editorconfig` to keep new and actively edited source consistent without reformatting
inherited controller code wholesale.

## Defaults

- Use UTF-8, LF line endings, a final newline, and no trailing whitespace.
- Use four spaces for Java, Groovy, and Gradle files.
- Use two spaces for Markdown, JSON, and YAML files.
- Preserve tabs where the file format requires them, such as Makefiles.

Apply these defaults to new files and to lines you change. Some inherited Java and Gradle files still contain
mixed CRLF/LF endings or older indentation. Do not normalize or reindent an entire file as part of an unrelated
change; make broad normalization a separate, explicitly reviewed change.

## Checks

Before handing off a change, run:

```sh
git diff --check
```

For a mechanical edit to a legacy mixed-ending file, Git may report the existing carriage return on each changed
line as trailing whitespace. Verify that case without hiding actual spaces using:

```sh
git -c core.whitespace=cr-at-eol diff --check
```

That exception is for review of existing CRLF lines, not permission to introduce new CRLF source. New files should
follow `.editorconfig`.
