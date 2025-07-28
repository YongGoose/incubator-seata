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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.async.methods.SimpleRequestProducer;
import org.apache.hc.client5.http.async.methods.SimpleResponseConsumer;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Http5ClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http5ClientUtil.class.getName());

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final CloseableHttpAsyncClient HTTP2_CLIENT;

    static {
        HTTP2_CLIENT = HttpAsyncClients.custom()
                .setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_2)
                .setDefaultRequestConfig(org.apache.hc.client5.http.config.RequestConfig.custom()
                        .setConnectTimeout(1_000, TimeUnit.MILLISECONDS)
                        .setResponseTimeout(1_000, TimeUnit.MILLISECONDS)
                        .setConnectionRequestTimeout(1_000, TimeUnit.MILLISECONDS)
                        .build())
                .build();
        HTTP2_CLIENT.start();
    }

    public static CompletableFuture<SimpleHttpResponse> doPostHttp(
            String url, Map<String, String> params, Map<String, String> headers) {
        try {
            final SimpleHttpRequest request = new SimpleHttpRequest("POST", url);
            String contentType = "";
            if (headers != null) {
                headers.forEach(request::setHeader);
                contentType = headers.get("Content-Type");
            }

            if (StringUtils.isNotBlank(contentType)) {
                if (ContentType.APPLICATION_FORM_URLENCODED.getMimeType().equals(contentType)) {
                    List<NameValuePair> nameValuePairs = new ArrayList<>();
                    if (params != null) {
                        params.forEach((k, v) -> nameValuePairs.add(new BasicNameValuePair(k, v)));
                    }
                    String requestBody = URLEncodedUtils.format(nameValuePairs, StandardCharsets.UTF_8);
                    request.setBody(requestBody, org.apache.hc.core5.http.ContentType.APPLICATION_FORM_URLENCODED);
                } else if (ContentType.APPLICATION_JSON.getMimeType().equals(contentType)) {
                    String requestBody = OBJECT_MAPPER.writeValueAsString(params);
                    request.setBody(requestBody, org.apache.hc.core5.http.ContentType.APPLICATION_JSON);
                }
            }

            final CompletableFuture<SimpleHttpResponse> future = new CompletableFuture<>();
            HTTP2_CLIENT.execute(
                    SimpleRequestProducer.create(request),
                    SimpleResponseConsumer.create(),
                    new FutureCallback<SimpleHttpResponse>() {
                        @Override
                        public void completed(SimpleHttpResponse result) {
                            future.complete(result);
                        }

                        @Override
                        public void failed(Exception e) {
                            future.completeExceptionally(e);
                        }

                        @Override
                        public void cancelled() {
                            future.cancel(true);
                        }
                    });

            return future;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

    // post request for http2
    public static CompletableFuture<SimpleHttpResponse> doPostHttp(String url, String body, Map<String, String> headers)
            throws IOException {
        try {
            String contentType = "";
            SimpleHttpRequest request = new SimpleHttpRequest("POST", url);
            if (headers != null) {
                headers.forEach(request::setHeader);
                contentType = headers.get("Content-Type");
            }

            if (StringUtils.isNotBlank(contentType)) {
                if (ContentType.APPLICATION_JSON.getMimeType().equals(contentType)) {
                    request.setBody(body, org.apache.hc.core5.http.ContentType.APPLICATION_JSON);
                }
            }

            CompletableFuture<SimpleHttpResponse> future = new CompletableFuture<>();
            HTTP2_CLIENT.execute(
                    SimpleRequestProducer.create(request),
                    SimpleResponseConsumer.create(),
                    new FutureCallback<SimpleHttpResponse>() {
                        @Override
                        public void completed(SimpleHttpResponse result) {
                            future.complete(result);
                        }

                        @Override
                        public void failed(Exception e) {
                            future.completeExceptionally(e);
                        }

                        @Override
                        public void cancelled() {
                            future.cancel(true);
                        }
                    });
            return future;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        return null;
    }

    public static CompletableFuture<SimpleHttpResponse> doGetHttp(String url, Map<String, String> headers, int timeout)
            throws IOException {
        try (CloseableHttpAsyncClient http2Client = HttpAsyncClients.custom()
                .setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_2)
                .setDefaultRequestConfig(org.apache.hc.client5.http.config.RequestConfig.custom()
                        .setConnectTimeout(timeout, TimeUnit.MILLISECONDS)
                        .setResponseTimeout(timeout, TimeUnit.MILLISECONDS)
                        .setConnectionRequestTimeout(timeout, TimeUnit.MILLISECONDS)
                        .build())
                .build()) {
            http2Client.start();

            SimpleHttpRequest request = new SimpleHttpRequest("GET", url);
            if (headers != null) {
                headers.forEach(request::setHeader);
            }
            CompletableFuture<SimpleHttpResponse> future = new CompletableFuture<>();
            http2Client.execute(request, new FutureCallback<SimpleHttpResponse>() {
                @Override
                public void completed(SimpleHttpResponse result) {
                    future.complete(result);
                }

                @Override
                public void failed(Exception e) {
                    future.completeExceptionally(e);
                }

                @Override
                public void cancelled() {
                    future.cancel(true);
                }
            });
            return future;
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }
}
