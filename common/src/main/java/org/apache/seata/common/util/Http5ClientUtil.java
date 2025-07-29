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
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.nio.ssl.TlsStrategy;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.http2.ssl.H2ClientTlsStrategy;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.apache.seata.common.executor.HttpCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Http5ClientUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(Http5ClientUtil.class.getName());

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final CloseableHttpAsyncClient HTTP_CLIENT;

    static {
        HTTP_CLIENT = createOptimalHttpClient();
        HTTP_CLIENT.start();
    }

    private static CloseableHttpAsyncClient createOptimalHttpClient() {
        HttpAsyncClientBuilder clientBuilder = HttpAsyncClients.custom()
                .setDefaultRequestConfig(RequestConfig.custom()
                        .setConnectTimeout(10_000, TimeUnit.MILLISECONDS)
                        .setResponseTimeout(10_000, TimeUnit.MILLISECONDS)
                        .setConnectionRequestTimeout(10_000, TimeUnit.MILLISECONDS)
                        .build());

        String jdkVersion = System.getProperty("java.version");
        int majorVersion = getMajorJavaVersion(jdkVersion);

        if (majorVersion >= 9) {
            LOGGER.info("JDK 9+ detected. Enabling HTTP/2 with native ALPN support.");
            return clientBuilder.setVersionPolicy(HttpVersionPolicy.NEGOTIATE).build();
        }

        // For Java 8 and below
        return configureJdk8(clientBuilder);
    }

    private static CloseableHttpAsyncClient configureJdk8(HttpAsyncClientBuilder clientBuilder) {
        if (isConscryptAvailable()) {
            LOGGER.info("Conscrypt library detected. Configuring HTTP/2 support for JDK 8.");
            return setUpConscryptClient(clientBuilder);
        }

        LOGGER.warn("Running on JDK 8 without Conscrypt. HTTP/2 is not supported. Falling back to HTTP/1.1.");
        return clientBuilder.setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_1).build();
    }

    private static CloseableHttpAsyncClient setUpConscryptClient(HttpAsyncClientBuilder clientBuilder) {
        try {
            Class<?> conscryptClass = Class.forName("org.conscrypt.Conscrypt");
            Object provider = conscryptClass.getMethod("newProvider").invoke(null);
            Security.insertProviderAt((java.security.Provider) provider, 1);

            SSLContext sslContext =
                    SSLContextBuilder.create().setProvider("Conscrypt").build();
            TlsStrategy tlsStrategy = new H2ClientTlsStrategy(sslContext);

            PoolingAsyncClientConnectionManager connectionManager = PoolingAsyncClientConnectionManagerBuilder.create()
                    .setTlsStrategy(tlsStrategy)
                    .build();

            return clientBuilder
                    .setConnectionManager(connectionManager)
                    .setVersionPolicy(HttpVersionPolicy.NEGOTIATE)
                    .build();
        } catch (Exception e) {
            LOGGER.error("Failed to configure HTTP/2 with Conscrypt on JDK 8. Falling back to HTTP/1.1.", e);
            return clientBuilder
                    .setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_1)
                    .build();
        }
    }

    private static boolean isConscryptAvailable() {
        try {
            Class.forName("org.conscrypt.Conscrypt");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    private static int getMajorJavaVersion(String version) {
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, 3));
        }
        int dotIndex = version.indexOf('.');
        return (dotIndex != -1) ? Integer.parseInt(version.substring(0, dotIndex)) : Integer.parseInt(version);
    }

    public static void doPostHttp(
            String url,
            Map<String, String> params,
            Map<String, String> headers,
            HttpCallback<SimpleHttpResponse> callback) {
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
            HTTP_CLIENT.execute(
                    SimpleRequestProducer.create(request),
                    SimpleResponseConsumer.create(),
                    new FutureCallback<SimpleHttpResponse>() {
                        @Override
                        public void completed(SimpleHttpResponse result) {
                            callback.onSuccess(result);
                        }

                        @Override
                        public void failed(Exception e) {
                            callback.onFailure(e);
                        }

                        @Override
                        public void cancelled() {
                            callback.onCancelled();
                        }
                    });
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    // post request for http2
    public static void doPostHttp(
            String url, String body, Map<String, String> headers, HttpCallback<SimpleHttpResponse> callback)
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
            HTTP_CLIENT.execute(
                    SimpleRequestProducer.create(request),
                    SimpleResponseConsumer.create(),
                    new FutureCallback<SimpleHttpResponse>() {
                        @Override
                        public void completed(SimpleHttpResponse result) {
                            callback.onSuccess(result);
                        }

                        @Override
                        public void failed(Exception e) {
                            callback.onFailure(e);
                        }

                        @Override
                        public void cancelled() {
                            callback.onCancelled();
                        }
                    });
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    public static void doGetHttp(
            String url, Map<String, String> headers, int timeout, HttpCallback<SimpleHttpResponse> callback)
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
                    callback.onSuccess(result);
                }

                @Override
                public void failed(Exception e) {
                    callback.onFailure(e);
                }

                @Override
                public void cancelled() {
                    callback.onCancelled();
                }
            });
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }
}
