/**
 * blackduck-detect
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.jenkins.detect.substeps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.types.Commandline;

import com.synopsys.integration.IntegrationEscapeUtils;
import com.synopsys.integration.jenkins.JenkinsVersionHelper;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.Util;

public class ParseDetectArguments {
    private static final String LOGGING_LEVEL_KEY = "logging.level.com.synopsys.integration";
    private final JenkinsIntLogger logger;
    private final IntEnvironmentVariables intEnvironmentVariables;
    private final DetectSetupResponse detectSetupResponse;
    private final String detectProperties;

    public ParseDetectArguments(final JenkinsIntLogger logger, final IntEnvironmentVariables intEnvironmentVariables, final DetectSetupResponse detectSetupResponse, final String detectProperties) {
        this.logger = logger;
        this.intEnvironmentVariables = intEnvironmentVariables;
        this.detectSetupResponse = detectSetupResponse;
        this.detectProperties = detectProperties;
    }

    public List<String> parseDetectArguments() {
        final DetectSetupResponse.ExecutionStrategy executionStrategy = detectSetupResponse.getExecutionStrategy();
        final Function<String, String> argumentEscaper = getArgumentEscaper(executionStrategy);

        final String detectRemotePath = detectSetupResponse.getDetectRemotePath();
        final String remoteJavaPath = detectSetupResponse.getRemoteJavaHome();
        final List<String> detectArguments = new ArrayList<>(getInvocationParameters(executionStrategy, detectRemotePath, remoteJavaPath));

        if (StringUtils.isNotBlank(detectProperties)) {
            Arrays.stream(Commandline.translateCommandline(detectProperties))
                .map(argumentBlobString -> argumentBlobString.split("\\r?\\n"))
                .flatMap(Arrays::stream)
                .filter(StringUtils::isNotBlank)
                .map(this::handleVariableReplacement)
                .map(argumentEscaper)
                .forEachOrdered(detectArguments::add);
        }

        if (detectArguments.stream().noneMatch(argument -> argument.contains(LOGGING_LEVEL_KEY))) {
            detectArguments.add(formatAsPropertyAndEscapedValue(argumentEscaper, LOGGING_LEVEL_KEY, logger.getLogLevel().toString()));
        }

        logger.info("Running Detect command: " + StringUtils.join(detectArguments, " "));

        // Phone Home arguments that we do not want logged:
        final String jenkinsVersion = JenkinsVersionHelper.getJenkinsVersion();
        detectArguments.add(formatAsPropertyAndEscapedValue(argumentEscaper, "detect.phone.home.passthrough.jenkins.version", jenkinsVersion));
        final String pluginVersion = JenkinsVersionHelper.getPluginVersion("blackduck-detect");
        detectArguments.add(formatAsPropertyAndEscapedValue(argumentEscaper, "detect.phone.home.passthrough.jenkins.plugin.version", pluginVersion));

        return detectArguments;
    }

    private Function<String, String> getArgumentEscaper(final DetectSetupResponse.ExecutionStrategy executionStrategy) {
        switch (executionStrategy) {
            case POWERSHELL_SCRIPT:
                return IntegrationEscapeUtils::escapePowerShell;
            case SHELL_SCRIPT:
                return IntegrationEscapeUtils::escapeXSI;
            default:
                return Function.identity();
        }
    }

    private List<String> getInvocationParameters(final DetectSetupResponse.ExecutionStrategy executionStrategy, final String remoteDetectPath, final String remoteJavaPath) {
        switch (executionStrategy) {
            case JAR:
                return Arrays.asList(remoteJavaPath, "-jar", remoteDetectPath);
            case POWERSHELL_SCRIPT:
                return Arrays.asList("powershell", String.format("\"Import-Module '%s'; detect\"", remoteDetectPath));
            case SHELL_SCRIPT:
                return Arrays.asList("bash", remoteDetectPath);
            default:
                return Collections.emptyList();
        }
    }

    private String handleVariableReplacement(final String value) {
        if (value != null) {
            final String newValue = Util.replaceMacro(value, intEnvironmentVariables.getVariables());
            if (StringUtils.isNotBlank(newValue) && newValue.contains("$")) {
                logger.warn("Variable may not have been properly replaced. Argument: " + value + ", resolved argument: " + newValue + ". Make sure the variable has been properly defined.");
            }
            return newValue;
        }
        return null;
    }

    private String formatAsPropertyAndEscapedValue(final Function<String, String> argumentEscaper, final String detectProperty, final String value) {
        final String escapedValue = Arrays.stream(Commandline.translateCommandline(value))
                                        .filter(StringUtils::isNotBlank)
                                        .map(this::handleVariableReplacement)
                                        .map(argumentEscaper)
                                        .collect(Collectors.joining());

        return String.format("--%s=%s", detectProperty, escapedValue);
    }

}
