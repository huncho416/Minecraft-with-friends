# Repository conventions

## No comments in source files

This repository keeps source files **strictly code, no comments**. Applies to every language: Java, Rust, TypeScript, JavaScript, Kotlin, Python. Per the project owner, well-named identifiers carry intent; commit messages and PR descriptions carry rationale.

Rules:
- Do not add `//`, `/* */`, `///`, `/** */`, `#`, or any other comment to source files.
- Strip existing comments from any source file you touch for another reason.
- Do not write Javadoc, Rustdoc, or TSDoc blocks.
- Vendored upstream code (e.g. `mythic-cord/infrarust/`) is exempt — leave third-party code untouched.
- YAML, TOML, JSON, Markdown, and shell-script files MAY keep comments where they configure operator-facing behavior or document a build step. Operators read those files; engineers don't.

When refactoring or generating code, write it without comments from the start. Do not write comments planning to "remove them later."

## Where to put the "why"

- Implementation decisions → commit message body.
- Architectural decisions → PLAN.md.
- User-facing behavior → README or operator-facing YAML files.
- Hidden invariants → the test that pins them.

## Other conventions

- Phase status lives in PLAN.md. Update it when a row's state changes.
- `mvn -B -ntp test` is the canonical test command for Java (uses JDK 21 + Maven 3.9.9).
- `cargo build --workspace --features with-infrarust` is the full Rust build once the Infrarust subtree is vendored; without the feature flag, the proxy builds in standalone mode.
