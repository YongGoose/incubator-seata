---
name: add-rpc-message-type
description: >
  Checklist for adding or changing a Seata RPC message type (a new request/response
  between TM, RM and TC). Use when a change touches MessageType, a class under
  core/.../core/protocol/, a message codec, or a remoting processor.
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

# Add an RPC message type

A message type is wired in several modules and none of them reference each other
directly. A missing step compiles fine and fails only at runtime, usually as
`This message type [N] has no processor.` (for example #8274, where the mock
server was not updated when `TYPE_UNREG_RM` was added).

## Start from a sibling

Pick the existing request/response pair closest to the new one (for example
`RegisterRMRequest`/`RegisterRMResponse` for a client-to-server registration) and
list every place it appears in production code:

```bash
git grep -lw RegisterRMRequest -- ':!**/src/test/**' ':!changes/**'
```

Mirror each hit for the new message. The checklist below is the expected result.

## Checklist

Protocol (`core/src/main/java/org/apache/seata/core/protocol/`)
- [ ] `MessageType`: add the request code and its `*_RESULT` code (result = request + 1).
- [ ] Request and response classes; `getTypeCode()` returns the new constants.
- [ ] `Version`: if older servers or clients cannot handle the message, add a
      version constant and check the peer version before sending (as
      `UnregisterRMRequest` does for servers below 2.6.0).

Serialization
- [ ] Seata codec: `serializer/seata-serializer-seata/.../protocol/<Message>Codec.java`
      for both classes, and both `switch` blocks in `MessageCodecFactory`
      (codec by type code, and message instance by type code).
- [ ] Protobuf: `<message>.proto` files, the enum in `messageType.proto` (same codes as
      `MessageType`), `<Message>Convertor` classes, and the three maps in
      `ProtobufConvertManager`.
- [ ] `core/.../serializer/SerializerSecurityRegistry`: add both classes to the
      allow list, or hessian, kryo and fory reject them at runtime.

Processors (who receives the message)
- [ ] TC side: register a processor in `NettyRemotingServer#registerProcessor`
      (processors live in `core/.../rpc/processor/server/`).
- [ ] Client side: register the `*_RESULT` type with `ClientOnResponseProcessor` in
      `RmNettyRemotingClient` and/or `TmNettyRemotingClient`, or a request processor
      if the TC sends the request (`core/.../rpc/processor/client/`).
- [ ] Mock TC: register the same types in
      `mock-server/.../MockNettyRemotingServer#registerProcessor`, reusing the real
      processor or a mock one under `mock-server/.../processor/`.

Tests
- [ ] Codec round trip: `serializer/seata-serializer-seata/src/test/.../protocol/<Message>SerializerTest.java`.
- [ ] Protobuf convertor: `serializer/seata-serializer-protobuf/src/test/.../convertor/<Message>ConvertorTest.java`.
- [ ] Processor unit test next to the processor (for example `UnregRmProcessorTest`).
- [ ] End to end against the mock TC:
      `test-suite/test-new-version/.../mockserver/MockServerTest.java` (JDK 17+).

Finish with the `prepare-pull-request` playbook.
