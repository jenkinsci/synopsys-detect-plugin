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
package com.synopsys.integration.jenkins.detect.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.types.Commandline;

import com.synopsys.integration.jenkins.detect.DetectJenkinsEnvironmentVariable;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.wrapper.JenkinsVersionHelper;
import com.synopsys.integration.phonehome.request.PhoneHomeRequestBody;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.Util;

public class DetectArgumentService {
    private static final String LOGGING_LEVEL_KEY = "logging.level.com.synopsys.integration";
    private final JenkinsIntLogger logger;
    private final JenkinsVersionHelper jenkinsVersionHelper;

    public DetectArgumentService(JenkinsIntLogger logger, JenkinsVersionHelper jenkinsVersionHelper) {
        this.logger = logger;
        this.jenkinsVersionHelper = jenkinsVersionHelper;
    }

    public List<String> parseDetectArgumentString(IntEnvironmentVariables intEnvironmentVariables, Function<String, String> strategyEscaper, List<String> invocationParameters, String detectArgumentString) {
        boolean shouldEscape = Boolean.parseBoolean(intEnvironmentVariables.getValue(DetectJenkinsEnvironmentVariable.SHOULD_ESCAPE.stringValue(), "true"));
        Function<String, String> argumentEscaper;
        if (shouldEscape) {
            argumentEscaper = strategyEscaper;
        } else {
            argumentEscaper = Function.identity();
        }

        List<String> detectArguments = new ArrayList<>(invocationParameters);

        if (StringUtils.isNotBlank(detectArgumentString)) {
            Arrays.stream(Commandline.translateCommandline(detectArgumentString))
                .map(argumentBlobString -> argumentBlobString.split("\\r?\\n"))
                .flatMap(Arrays::stream)
                .filter(StringUtils::isNotBlank)
                .map(argument -> this.handleVariableReplacement(intEnvironmentVariables, argument))
                .map(argumentEscaper)
                .forEachOrdered(detectArguments::add);
        }

        if (detectArguments.stream().noneMatch(argument -> argument.contains(LOGGING_LEVEL_KEY))) {
            detectArguments.add(formatAsPropertyAndEscapedValue(intEnvironmentVariables, strategyEscaper, LOGGING_LEVEL_KEY, logger.getLogLevel().toString()));
        }

        logger.info("Running Detect command: " + StringUtils.join(detectArguments, " "));

        // Phone Home arguments that we do not want logged:
        String jenkinsVersion = jenkinsVersionHelper.getJenkinsVersion().orElse(PhoneHomeRequestBody.UNKNOWN_FIELD_VALUE);
        detectArguments.add(formatAsPropertyAndEscapedValue(intEnvironmentVariables, strategyEscaper, "detect.phone.home.passthrough.jenkins.version", jenkinsVersion));
        String pluginVersion = jenkinsVersionHelper.getPluginVersion("blackduck-detect").orElse(PhoneHomeRequestBody.UNKNOWN_FIELD_VALUE);
        detectArguments.add(formatAsPropertyAndEscapedValue(intEnvironmentVariables, strategyEscaper, "detect.phone.home.passthrough.jenkins.plugin.version", pluginVersion));

        return detectArguments;
    }

    private String handleVariableReplacement(IntEnvironmentVariables intEnvironmentVariables, String value) {
        if (value != null) {
            String newValue = Util.replaceMacro(value, intEnvironmentVariables.getVariables());
            if (StringUtils.isNotBlank(newValue) && newValue.contains("$")) {
                logger.warn("Variable may not have been properly replaced. Argument: " + value + ", resolved argument: " + newValue + ". Make sure the variable has been properly defined.");
            }
            return newValue;
        }
        return null;
    }

    private String formatAsPropertyAndEscapedValue(IntEnvironmentVariables intEnvironmentVariables, Function<String, String> argumentEscaper, String detectProperty, String value) {
        String escapedValue = Arrays.stream(Commandline.translateCommandline(value))
                                  .filter(StringUtils::isNotBlank)
                                  .map(commandPiece -> this.handleVariableReplacement(intEnvironmentVariables, commandPiece))
                                  .map(argumentEscaper)
                                  .collect(Collectors.joining());

        return String.format("--%s=%s", detectProperty, escapedValue);
    }

}
