# Source formatting

The repository uses Spotless for mechanical source formatting and `.editorconfig` for matching editor defaults.
`.gitattributes` makes LF the repository-wide text-file convention while preserving CRLF for Windows batch files.

## Defaults

- Use UTF-8, LF line endings, a final newline, and no trailing whitespace.
- Use four spaces for Java, Groovy, and Gradle files.
- Use two spaces for Markdown, JSON, and YAML files.
- Preserve tabs where the file format requires them, such as Makefiles.

Java is formatted with Google Java Format's four-space AOSP style. Gradle and Groovy build scripts receive
deterministic whitespace, indentation, line-ending, and final-newline cleanup.

## Checks

Apply formatting with:

```sh
just format
```

Check formatting without changing files with:

```sh
just format-check
```

The normal `just build` workflow also runs Spotless checks through Gradle's `check` lifecycle. Before handing off
any change, also run `git diff --check` to catch whitespace errors in file types outside the formatter targets.
