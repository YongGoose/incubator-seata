<!--
    Licensed to the Apache Software Foundation (ASF) under one or more
    contributor license agreements.  See the NOTICE file distributed with
    this work for additional information regarding copyright ownership.
    The ASF licenses this file to You under the Apache License, Version 2.0
    (the "License"); you may not use this file except in compliance with
    the License.  You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
-->

# AGENTS instructions

Guidance for AI coding assistants working on Apache Seata. It records the
project rules that cannot be read from the code. `CLAUDE.md` imports this file.

## Repository

- `2.x` is the development branch. Pull requests target `2.x`.
- Java sources live under `org.apache.seata`. The `compatible/` module keeps the
  legacy `io.seata.*` API for users of 2.0 and earlier; it contains copies of many
  public classes, so a bug or a change may need to be applied in both places.

## Build and test

- Use `./mvnw`. The code targets Java 8, but some modules are only part of the
  build on newer JDKs (profiles in the root `pom.xml`):
  - JDK 17+: `server`, `test-suite/test-new-version`
  - JDK 21+: `threadpool-loom`
  - JDK 25+: `namingserver`, `console`

  A build on an older JDK skips these modules without an error.
- Test one module: `./mvnw -pl <module-dir> -am test -Dtest=<TestClass> -Dsurefire.failIfNoSpecifiedTests=false`
- Tests that need Redis or Nacos are skipped unless `-DredisCaseEnabled=true` or
  `-DnacosCaseEnabled=true` is set.
- `make help` lists the other build targets.

## Code style

- On JDK 17+, Spotless runs `apply` during `process-sources`, so every build
  reformats Java sources (palantir-java-format, unused imports removed, import
  order: other packages, then `javax`/`java`, then static). Run
  `make spotless-apply` before committing and expect formatting-only diffs.
- Checkstyle (`style/checkstyle.xml`) runs in CI on changed files only;
  `make checkstyle-diff` runs the same check locally.
- Every new file needs the ASF license header, including Markdown (as an HTML
  comment), YAML, properties and SQL files. Copy it from a neighbouring file.

## Tests

- JUnit 5 and Mockito. Name a test `XxxTest` and put it in the same module as the
  code under test.
- RPC behaviour is tested against the mock transaction coordinator in
  `mock-server/` (see `test-suite/test-new-version/.../mockserver/MockServerTest.java`).

## Pull requests

- Title and commit message: `<type>: <description>`, where type is one of
  `feature`, `bugfix`, `optimize`, `refactor`, `test`, `doc`, `security`.
  Do not use Conventional Commits scopes such as `fix(core):`.
- Register every PR in **both** `changes/en-us/2.x.md` and `changes/zh-cn/2.x.md`,
  under the section that matches the type, as
  `- [[#NNNN](https://github.com/apache/incubator-seata/pull/NNNN)] description`.
  Add the author to the contributor list at the bottom of both files if missing.
  Never edit the changes files of released versions (`2.5.0.md`, `2.6.0.md`, ...).
- Fill in `.github/PULL_REQUEST_TEMPLATE.md`. Link the issue with `fixes #NNNN`.

## Runtime wiring

Static "find usages" misses most of Seata's wiring. Look for these instead:

- **SPI.** `EnhancedServiceLoader` (`common/.../common/loader/`) loads the classes
  listed in `META-INF/services/<interface>`. An implementation is selected by its
  `@LoadLevel(name = ...)` (a store mode such as `db`, a database type such as
  `mysql`), or by the highest `order`. To find implementations of an interface,
  look for `src/main/resources/META-INF/services/<fully.qualified.Interface>`.
- **Branch type.** Resource managers, RM handlers and TC cores are kept in maps keyed
  by `BranchType` (`DefaultResourceManager`, `DefaultRMHandler`, `DefaultCore`).
  When two implementations declare the same branch type, the one loaded last wins
  silently; the `compatible/` module registers `io.seata.*` copies that take part
  in this.
- **RPC.** Each remoting endpoint maps `MessageType` codes to processors with
  `registerProcessor(...)`: `NettyRemotingServer` (TC), `RmNettyRemotingClient` and
  `TmNettyRemotingClient` (clients), and `MockNettyRemotingServer` (test TC).
  A message type without a registration is dropped with
  `This message type [N] has no processor.`

## Task playbooks

Read the matching playbook before starting one of these tasks. They are in
`.agents/skills/` (Claude Code also finds them in `.claude/skills/`).

| Task | Playbook |
|---|---|
| Add or change an RPC message type | `.agents/skills/add-rpc-message-type/SKILL.md` |
| Add AT-mode support for a database | `.agents/skills/add-database-dialect/SKILL.md` |
| Prepare a change for a pull request | `.agents/skills/prepare-pull-request/SKILL.md` |

## Boundaries

- Ask first before: changing the RPC wire format of an existing message, renaming
  or removing a public API in `compatible/`, adding a new third-party dependency
  (it also needs `distribution/LICENSE*` and `NOTICE` updates).
- Never: commit secrets; edit released `changes/*.md` files; remove a test to make
  the build pass.
