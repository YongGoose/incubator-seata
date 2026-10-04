---
name: prepare-pull-request
description: >
  Steps to make a Seata change ready for a pull request: changes files, commit
  message, license headers, formatting and the PR template. Use before committing
  or opening a pull request against 2.x.
license: Apache-2.0
---
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

# Prepare a pull request

## 1. Register the change

Edit **both** `changes/en-us/2.x.md` and `changes/zh-cn/2.x.md`. Never edit the file
of a released version such as `2.6.0.md`.

- Add one line under the section that matches the change type
  (`feature`, `bugfix`, `optimize`, `security`, `test`, `doc`):

  ```markdown
  - [[#NNNN](https://github.com/apache/incubator-seata/pull/NNNN)] short description
  ```

  `NNNN` is the pull request number. If the PR does not exist yet, use the issue
  number and update it after opening the PR. Write the zh-cn line in Chinese.
- Add the author's GitHub ID to the contributor list at the end of both files if
  it is not there yet: `- [github-id](https://github.com/github-id)`.

## 2. Check the files

- [ ] Every new file has the ASF license header (Java, Markdown as an HTML comment,
      YAML, properties, SQL, proto).
- [ ] Tests are next to the code (`XxxTest`, JUnit 5) and cover the fix or feature.
- [ ] No unrelated reformatting. On JDK 17+, run `make spotless-apply` so the CI
      formatting check passes, then review the diff.
- [ ] `make checkstyle-diff` passes.

## 3. Commit

- Message: `<type>: <description>` in English, imperative, lower case after the
  colon. Types: `feature`, `bugfix`, `optimize`, `refactor`, `test`, `doc`,
  `security`. No Conventional Commits scope (not `fix(core):`).
- One logical change per commit; each commit should pass CI.
- If AI tooling helped, say so with a `Generated-by:` or `Co-authored-by:` trailer
  (ASF generative tooling guidance).

## 4. Open the PR

- Target `2.x`.
- Fill in `.github/PULL_REQUEST_TEMPLATE.md`: tick the two checkboxes, describe the
  change, write `fixes #NNNN` for the issue, explain how to verify it.
