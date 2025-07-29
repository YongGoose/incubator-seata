/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.common.util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.seata.common.executor.HttpCallback;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class Http5ClientUtilTest {

    private static final String CLASS_NAME = "org.apache.seata.common.util.Http5ClientUtil";

    private final List<Logger> watchedLoggers = new ArrayList<>();
    private final ListAppender<ILoggingEvent> logWatcher = new ListAppender<>();

    @BeforeEach
    void setUp() {
        logWatcher.start();
        setUpLogger();
    }

    @AfterEach
    void tearDown() {
        logWatcher.stop();
        watchedLoggers.forEach(Logger::detachAndStopAllAppenders);
        watchedLoggers.clear();
    }

    @Test
    void testDoPostHttp_param_onSuccess() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<SimpleHttpResponse> callback = new HttpCallback<SimpleHttpResponse>() {
            @Override
            public void onSuccess(SimpleHttpResponse result) {
                assertNotNull(result);
                assertEquals("HTTP/2.0", result.getVersion().toString());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable e) {
                fail("Should not fail");
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        Map<String, String> params = new HashMap<>();
        params.put("key", "value");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Http5ClientUtil.doPostHttp("https://www.apache.org/", params, headers, callback);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testDoPostHttp_param_onFailure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<SimpleHttpResponse> callback = new HttpCallback<SimpleHttpResponse>() {
            @Override
            public void onSuccess(SimpleHttpResponse response) {
                fail("Should not succeed");
            }

            @Override
            public void onFailure(Throwable t) {
                assertNotNull(t);
                System.out.println(t.getMessage());
                latch.countDown();
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        Map<String, String> params = new HashMap<>();
        params.put("key", "value");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Http5ClientUtil.doPostHttp("http://localhost:9999/invalid", params, headers, callback);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testDoPostHttp_body_onSuccess() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<SimpleHttpResponse> callback = new HttpCallback<SimpleHttpResponse>() {
            @Override
            public void onSuccess(SimpleHttpResponse result) {
                assertNotNull(result);
                assertEquals("HTTP/2.0", result.getVersion().toString());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable e) {
                fail("Should not fail");
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Http5ClientUtil.doPostHttp("https://www.apache.org/", "{\"key\":\"value\"}", headers, callback);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testDoPostHttp_body_onFailure() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<SimpleHttpResponse> callback = new HttpCallback<SimpleHttpResponse>() {
            @Override
            public void onSuccess(SimpleHttpResponse response) {
                fail("Should not succeed");
            }

            @Override
            public void onFailure(Throwable t) {
                assertNotNull(t);
                System.out.println(t.getMessage());
                latch.countDown();
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Http5ClientUtil.doPostHttp("http://localhost:9999/invalid", "{\"key\":\"value\"}", headers, callback);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testDoPostHttp_param_onSuccess_forceHttp1() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<SimpleHttpResponse> callback = new HttpCallback<SimpleHttpResponse>() {
            @Override
            public void onSuccess(SimpleHttpResponse result) {
                assertNotNull(result);
                assertEquals("HTTP/1.1", result.getVersion().toString());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable e) {
                fail("Should not fail");
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        Map<String, String> params = new HashMap<>();
        params.put("key", "value");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Http5ClientUtil.doPostHttp("http://httpbin.org/post", params, headers, callback);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @Test
    void testDoPostHttp_body_onSuccess_forceHttp1() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<SimpleHttpResponse> callback = new HttpCallback<SimpleHttpResponse>() {
            @Override
            public void onSuccess(SimpleHttpResponse result) {
                assertNotNull(result);
                assertEquals("HTTP/1.1", result.getVersion().toString());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable e) {
                fail("Should not fail");
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Http5ClientUtil.doPostHttp("http://httpbin.org/post", "{\"key\":\"value\"}", headers, callback);
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @EnabledOnJre(JRE.JAVA_8)
    @Test
    void testDoPostHttp_param_onSuccess_java8() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<SimpleHttpResponse> callback = new HttpCallback<SimpleHttpResponse>() {
            @Override
            public void onSuccess(SimpleHttpResponse result) {
                assertNotNull(result);
                assertEquals("HTTP/2.0", result.getVersion().toString());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable e) {
                fail("Should not fail");
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        Map<String, String> params = new HashMap<>();
        params.put("key", "value");

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Http5ClientUtil.doPostHttp("https://www.apache.org/", params, headers, callback);
        assertTrue(getLogs(Level.INFO).stream()
                .anyMatch(log -> log.equals("Conscrypt library detected. Configuring HTTP/2 support for JDK 8.")));
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    @EnabledOnJre(JRE.JAVA_8)
    @Test
    void testDoPostHttp_body_onSuccess_java8() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);

        HttpCallback<SimpleHttpResponse> callback = new HttpCallback<SimpleHttpResponse>() {
            @Override
            public void onSuccess(SimpleHttpResponse result) {
                assertNotNull(result);
                assertEquals("HTTP/2.0", result.getVersion().toString());
                latch.countDown();
            }

            @Override
            public void onFailure(Throwable e) {
                fail("Should not fail");
            }

            @Override
            public void onCancelled() {
                fail("Should not be cancelled");
            }
        };

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        Http5ClientUtil.doPostHttp("https://www.apache.org/", "{\"key\":\"value\"}", headers, callback);
        assertTrue(getLogs(Level.INFO).stream()
                .anyMatch(log -> log.equals("Conscrypt library detected. Configuring HTTP/2 support for JDK 8.")));
        assertTrue(latch.await(10, TimeUnit.SECONDS));
    }

    private List<String> getLogs(Level level) {
        return logWatcher.list.stream()
                .filter(event -> event.getLoggerName().endsWith(CLASS_NAME)
                        && event.getLevel().equals(level))
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    private void setUpLogger() {
        Logger logger = ((Logger) LoggerFactory.getLogger(CLASS_NAME));
        logger.addAppender(logWatcher);
        watchedLoggers.add(logger);
    }
}
