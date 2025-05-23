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
package org.apache.seata.discovery.registry.nacos;

import org.apache.seata.discovery.registry.RegistryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.util.List;


/**
 * The type Nacos registry serivce impl test
 */
public class NacosRegistryServiceImplTest {

    @BeforeAll
    public static void init() {
        System.setProperty("seata.registry.type", "nacos");
        System.setProperty("seata.registry.nacos.server-addr", "10.21.32.10:8848");
        System.setProperty("seata.registry.nacos.username", "nacos");
        System.setProperty("seata.registry.nacos.password", "nacos");
        System.setProperty("seata.registry.nacos.cluster", "testCluster");
    }

    @Test
    public void testGetInstance() {
        RegistryService instance = NacosRegistryServiceImpl.getInstance();
        Assertions.assertInstanceOf(NacosRegistryServiceImpl.class, instance);
        Assertions.assertEquals(instance, NacosRegistryServiceImpl.getInstance());
    }

    @Test
    public void testRegister() throws Exception {
        RegistryService instance = NacosRegistryServiceImpl.getInstance();
        instance.register(new InetSocketAddress("127.0.0.1", 8080));
        List<InetSocketAddress> testCluster = instance.lookup("testCluster");
        Assertions.assertEquals(testCluster.get(0), new InetSocketAddress("127.0.0.1", 8080));
    }

}
