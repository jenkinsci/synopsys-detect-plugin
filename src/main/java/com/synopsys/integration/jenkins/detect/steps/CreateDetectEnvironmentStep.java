/**
 * blackduck-detect
 *
 * Copyright (c) 2019 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.jenkins.detect.steps;

import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.jenkins.detect.JenkinsDetectLogger;
import com.synopsys.integration.jenkins.detect.PluginHelper;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfigBuilder;
import com.synopsys.integration.util.IntEnvironmentVariables;

public class CreateDetectEnvironmentStep {
    private final JenkinsDetectLogger logger;

    public CreateDetectEnvironmentStep(final JenkinsDetectLogger logger) {
        this.logger = logger;
    }

    public IntEnvironmentVariables setDetectEnvironment(final Map<String, String> environmentVariables) {
        final IntEnvironmentVariables intEnvironmentVariables = new IntEnvironmentVariables();
        intEnvironmentVariables.putAll(environmentVariables);
        logger.setLogLevel(intEnvironmentVariables);

        populateAllBlackDuckEnvironmentVariables(intEnvironmentVariables::put);
        populateAllPolarisEnvironmentVariables(intEnvironmentVariables::put);

        final String pluginVersion = PluginHelper.getPluginVersion();
        logger.info("Running Detect jenkins plugin version: " + pluginVersion);

        return intEnvironmentVariables;
    }

    private void populateAllBlackDuckEnvironmentVariables(final BiConsumer<String, String> environmentPutter) {
        final BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = PluginHelper.getDetectGlobalConfig().getBlackDuckServerConfigBuilder();

        blackDuckServerConfigBuilder.getProperties()
            .forEach((builderPropertyKey, propertyValue) -> acceptIfNotNull(environmentPutter, builderPropertyKey.getKey(), propertyValue));
    }

    private void populateAllPolarisEnvironmentVariables(final BiConsumer<String, String> environmentPutter) {
        final PolarisServerConfigBuilder polarisServerConfigBuilder = PluginHelper.getDetectGlobalConfig().getPolarisServerConfigBuilder();

        polarisServerConfigBuilder.getProperties()
            .forEach((builderPropertyKey, propertyValue) -> acceptIfNotNull(environmentPutter, builderPropertyKey.getKey(), propertyValue));

        try {
            polarisServerConfigBuilder.build().populateEnvironmentVariables(environmentPutter);
        } catch (final Exception ignored) {
            // If this doesn't work, Detect will throw an exception later on.
        }
    }

    private void acceptIfNotNull(final BiConsumer<String, String> environmentPutter, final String key, final String value) {
        if (StringUtils.isNoneBlank(key, value)) {
            environmentPutter.accept(key, value);
        }
    }

}
