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
import java.util.Map;
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
    private static final String DETECT_LOGLEVEL_ARGUMENT = "logging.level.com.synopsys.integration";
    private static final String DETECT_PHONEHOME_JENKINS_VERSION_ARGUMENT = "detect.phone.home.passthrough.jenkins.version";
    private static final String DETECT_PHONEHOME_PLUGIN_VERSION_ARGUMENT = "detect.phone.home.passthrough.jenkins.plugin.version";
    private final JenkinsIntLogger logger;
    private final JenkinsVersionHelper jenkinsVersionHelper;

    public DetectArgumentService(JenkinsIntLogger logger, JenkinsVersionHelper jenkinsVersionHelper) {
        this.logger = logger;
        this.jenkinsVersionHelper = jenkinsVersionHelper;
    }

    public List<String> getDetectArguments(IntEnvironmentVariables intEnvironmentVariables, Function<String, String> strategyEscaper, List<String> initialArguments, String detectArgumentString) {
        boolean shouldEscape = Boolean.parseBoolean(intEnvironmentVariables.getValue(DetectJenkinsEnvironmentVariable.SHOULD_ESCAPE.stringValue(), "true"));
        Function<String, String> argumentEscaper;
        if (shouldEscape) {
            argumentEscaper = strategyEscaper;
        } else {
            argumentEscaper = Function.identity();
        }

        List<String> userProvidedArguments = parseDetectArgumentString(intEnvironmentVariables.getVariables(), argumentEscaper, detectArgumentString);

        List<String> additionalArguments = new ArrayList<>();

        if (userProvidedArguments.stream().noneMatch(argument -> argument.contains(DETECT_LOGLEVEL_ARGUMENT))) {
            additionalArguments.add(asEscapedDetectArgument(argumentEscaper, DETECT_LOGLEVEL_ARGUMENT, logger.getLogLevel().toString()));
        }

        String jenkinsVersion = jenkinsVersionHelper.getJenkinsVersion().orElse(PhoneHomeRequestBody.UNKNOWN_FIELD_VALUE);
        String pluginVersion = jenkinsVersionHelper.getPluginVersion("blackduck-detect").orElse(PhoneHomeRequestBody.UNKNOWN_FIELD_VALUE);

        additionalArguments.add(asEscapedDetectArgument(argumentEscaper, DETECT_PHONEHOME_JENKINS_VERSION_ARGUMENT, jenkinsVersion));
        additionalArguments.add(asEscapedDetectArgument(argumentEscaper, DETECT_PHONEHOME_PLUGIN_VERSION_ARGUMENT, pluginVersion));

        List<String> detectArguments = new ArrayList<>();
        detectArguments.addAll(initialArguments);
        detectArguments.addAll(userProvidedArguments);

        logger.info("Running Detect command: " + StringUtils.join(detectArguments, " "));

        detectArguments.addAll(additionalArguments);
        return detectArguments;
    }

    private String asEscapedDetectArgument(Function<String, String> escaper, String key, String value) {
        String argumentString = String.format("--%s=%s", key, value);
        return escaper.apply(argumentString);
    }

    public List<String> parseDetectArgumentString(Map<String, String> environmentVariables, Function<String, String> argumentEscaper, String argumentString) {
        return Arrays.stream(Commandline.translateCommandline(argumentString))
                   .map(argumentBlobString -> argumentBlobString.split("\\r?\\n"))
                   .flatMap(Arrays::stream)
                   .map(argument -> Util.replaceMacro(argument, environmentVariables))
                   .filter(this::validateExpandedArguments)
                   .map(argumentEscaper)
                   .collect(Collectors.toList());
    }

    private boolean validateExpandedArguments(String argument) {
        if (StringUtils.isBlank(argument)) {
            return false;
        }

        if (argument.contains("$")) {
            logger.warn("A variable may not have been properly replaced in resolved argument: " + argument + ". Make sure the variable has been properly defined.");
        }

        return true;
    }

}
