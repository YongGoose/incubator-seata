---
name: add-database-dialect
description: >
  Checklist for adding AT-mode (and server DB store) support for a new database
  type, or fixing behaviour that differs per database. Use when a change adds a
  JdbcConstants database type or a class under rm-datasource/.../<db>/ or
  sqlparser/seata-sqlparser-druid/.../<db>/.
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

# Add a database dialect

Every per-database class is an SPI implementation selected by
`@LoadLevel(name = JdbcConstants.<DB>)`. The database type string comes from the
JDBC URL through Druid, so a class that is not registered in `META-INF/services`
is silently never used.

## Start from the closest existing dialect

Pick the dialect whose SQL is closest (Oracle-like databases usually start from
`oracle`, MySQL-like from `mysql`) and list where it is wired:

```bash
git grep -liw kingbase -- ':!**/src/test/**' ':!changes/**' ':!**/*.md'
```

## Checklist

Database type
- [ ] `sqlparser/seata-sqlparser-core/.../util/JdbcConstants`: the type name.
- [ ] `core/.../core/constants/DBType`: the enum value (used by the server store).

SQL parsing (`sqlparser/seata-sqlparser-druid/.../druid/<db>/`)
- [ ] `<Db>OperateRecognizerHolder` plus Insert, Update, Delete and SelectForUpdate
      recognizers (a `Base<Db>Recognizer` for shared logic).
- [ ] Register the holder in `META-INF/services/org.apache.seata.sqlparser.druid.SQLOperateRecognizerHolder`.
- [ ] If Druid reports a type whose grammar differs from the dialect (for example
      OceanBase in Oracle mode), map it in `DruidDbTypeAdapter`.

AT mode (`rm-datasource/.../rm/datasource/`), each registered in
`rm-datasource/src/main/resources/META-INF/services/`
- [ ] `exec/<db>/<Db>InsertExecutor` → `...exec.InsertExecutor`
- [ ] `sql/handler/<db>/<Db>EscapeHandler` → `...sqlparser.EscapeHandler`
- [ ] `sql/struct/cache/<Db>TableMetaCache` → `...sqlparser.struct.TableMetaCache`
- [ ] `undo/<db>/<Db>UndoLogManager` → `...undo.UndoLogManager`
- [ ] `undo/<db>/<Db>UndoExecutorHolder` and the Insert, Update and Delete undo
      executors → `...undo.UndoExecutorHolder`
- [ ] Dialect branches outside SPI: check `DataSourceProxy` (resource id and user
      name for Oracle-like databases), `exec/SelectForUpdateExecutor` (savepoint
      release) and `util/XAUtils` (XA mode) for `JdbcConstants.ORACLE` checks that
      should include the new type.

Server DB store (only if the TC can use this database with `store.mode=db`)
- [ ] `core/.../store/db/sql/lock/<Db>LockStoreSql` and `.../log/<Db>LogStoreSqls`,
      registered in `core/src/main/resources/META-INF/services/`.
- [ ] `LockStoreSqlFactory`, `DistributedLockSqlFactory` and
      `common/.../util/PageUtil` if they special-case database types.
- [ ] `script/server/db/<db>.sql`.

Client scripts
- [ ] `script/client/at/db/<db>.sql` (undo_log), and `script/client/tcc/db/` and
      `script/client/saga/db/` when those modes are supported.

Dependencies
- [ ] A JDBC driver added for tests needs `dependencies/pom.xml` and, if it is
      shipped, `distribution/LICENSE*`.

Tests
- [ ] Recognizer tests in `sqlparser/seata-sqlparser-druid/src/test/.../<db>/`.
- [ ] Undo executor, escape handler and table meta cache tests in
      `rm-datasource/src/test/` next to the existing dialects.

Finish with the `prepare-pull-request` playbook.
