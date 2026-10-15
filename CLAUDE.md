# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

RStudio's fork of [GWT](https://github.com/gwtproject/gwt), used to build the GWT SDK that the
RStudio IDE compiles against. Branching, release naming, building the SDK for RStudio, and
publishing it are documented in
<https://github.com/rstudio/rstudio/blob/main/src/gwt/tools/build-gwt-README.md> — read that
rather than guessing at the process.

The key point for day-to-day work: `main` tracks upstream `gwtproject/gwt` (synced by merge).
RStudio-specific patches (mostly accessibility and widget behavior in `user/`, e.g. `MenuItem`,
`MenuBar`, `SplitLayoutPanel`, `TabLayoutPanel`) live on `rstudio/<release>` branches such as
`rstudio/montauk-daisy`, not on `main`.

## Prerequisites

- JDK 17 (newer JDKs may fail when building the SDK for RStudio; CI also tests 21 and 25).
- `ant`.
- The [gwtproject/tools](https://github.com/gwtproject/tools) repo, expected at `../tools`
  or wherever `GWT_TOOLS` points. The build fails immediately without it.
- For tests: `export TZ=America/Los_Angeles ANT_OPTS=-Dfile.encoding=UTF-8`.

## Commands

```sh
ant clean dist-dev -Dgwt.version=x.y.z   # build SDK; jars in build/lib, SDK in build/staging
ant compile.tests apicheck checkstyle    # pre-commit check: compile tests, API compat, style
```

CI (`.github/workflows/quick-check.yml`) runs `ant clean compile.tests dist doc checkstyle
apicheck` and no tests. `ant test` runs everything (hours) — always narrow it.

Tests are run per module, either `cd <module> && ant <task>` or `ant <module> -Dtarget=<task>`.
Select classes with comma-separated include patterns:

```sh
# Single compiler/dev test
ant dev -Dtarget=test -Dgwt.junit.testcase.dev.core.includes="**/JsniRefTest.class"

# Pure-JVM tests in user (no GWTTestCase)
(cd user && ant test.nongwt -Dgwt.nongwt.testcase.includes="**/I18NJreSuite.class")

# GWTTestCase tests in user, compiled to JS and run in HtmlUnit
(cd user && ant test.web.htmlunit -Dgwt.junit.testcase.web.includes="**/MenuBarTest.class")
```

The README has the full table of test tasks and `-Dtest.<x>.disable=true` skip properties.

## Layout and architecture

- `dev/core` — the compiler and dev tooling (`gwt-dev.jar`), in `com.google.gwt.dev`. Entry
  points are `Compiler`, `DevMode`, and the `Precompile`/`CompilePerms`/`Link` pipeline. Java
  source is parsed with JDT (`jdt/`, `javac/`), lowered to the Java AST in `jjs/ast`,
  optimized, translated to the JS AST in `js/`, then permutations are linked into output.
- `dev/codeserver` — Super Dev Mode server.
- `user` — the client library (`gwt-user.jar`): widgets (`user/client/ui`), DOM, RPC, UiBinder,
  i18n, resources, etc. Code under `src/` that runs in the browser is translatable Java;
  `user/super/` holds super-source (JRE emulation in `emul/`, and browser-side replacements
  for JVM-side classes). Modules are declared by `*.gwt.xml` files beside the source.
- `user/test` — tests, grouped into `*Suite` classes (e.g. `UiSuite`). `*JreSuite` classes run
  on the JVM; the rest are `GWTTestCase`s that compile to JS.
- `servlet`, `requestfactory`, `tools` (incl. `api-checker`), `samples`, `doc` — supporting
  artifacts. Build output goes under `build/`.

## Conventions

- Style is enforced by checkstyle using `eclipse/settings/code-style/gwt-checkstyle.xml`
  (tests: `gwt-checkstyle-tests.xml`); import order is in `gwt.importorder`.
- `apicheck` compares the public `user`/`dev` API against the previous release
  (`tools/api-checker/config/gwt212_213userapi.conf`). Removing or changing public API fails
  it; additive changes (new constructors, events, handlers) are fine.
