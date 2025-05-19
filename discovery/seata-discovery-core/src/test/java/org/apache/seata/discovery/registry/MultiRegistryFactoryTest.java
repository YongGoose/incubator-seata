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
package org.apache.seata.discovery.registry;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.NotSupportYetException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Multi registry factory test.
 */
public class MultiRegistryFactoryTest {

    private static final String REGISTRY_TYPE_KEY =
            ConfigurationKeys.FILE_ROOT_REGISTRY + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR + ConfigurationKeys.FILE_ROOT_TYPE;

    @AfterEach
    public void tearDown() {
        System.clearProperty(REGISTRY_TYPE_KEY);
    }

    /**
     * Test getInstances with default config.
     */
    @Test
    public void testGetInstancesWithDefaultConfig() {
        // Set "registry.type = file" as default config
        System.setProperty(REGISTRY_TYPE_KEY, RegistryType.File.name());

        List<RegistryService> instances = MultiRegistryFactory.getInstances();
        Assertions.assertNotNull(instances);

        for (RegistryService service : instances) {
            Assertions.assertNotNull(service);
        }
    }

    /**
     * Test buildRegistryServices with multiple tests of the same registry type.
     */
    @Test
    public void testGetInstancesWithMultiRegistryTypes() throws Throwable {
        // Set up multiple registration center configurations
        System.setProperty(REGISTRY_TYPE_KEY, RegistryType.File.name() + Constants.REGISTRY_TYPE_SPLIT_CHAR + RegistryType.File.name());

        List<RegistryService> instances = invokeBuildRegistryServices();
        Assertions.assertNotNull(instances);
        Assertions.assertEquals(1, instances.size());
    }

    /**
     * Test buildRegistryServices with blank registry type.
     */
    @Test
    public void testGetInstancesWithBlankRegistryType() throws Throwable {
        System.setProperty(REGISTRY_TYPE_KEY, "");

        List<RegistryService> instances = invokeBuildRegistryServices();
        Assertions.assertNotNull(instances);
    }

    /**
     * Test buildRegistryServices with invalid registry type.
     */
    @Test
    public void testGetInstancesWithInvalidRegistryType() {
        System.setProperty(REGISTRY_TYPE_KEY, "InvalidRegistryType");

        Assertions.assertThrows(NotSupportYetException.class, MultiRegistryFactoryTest::invokeBuildRegistryServices);
    }

    /**
     * Use reflection to call the buildRegistryServices method
     */
    private static List<RegistryService> invokeBuildRegistryServices() throws Throwable {
        Method buildMethod = MultiRegistryFactory.class.getDeclaredMethod("buildRegistryServices");
        buildMethod.setAccessible(true);

        try {
            return (List<RegistryService>) buildMethod.invoke(null);
        } catch (InvocationTargetException e) {
            throw e.getTargetException();
        }
    }
}
