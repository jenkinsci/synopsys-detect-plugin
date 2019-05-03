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
package com.synopsys.integration.jenkins.detect.steps.remote;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.types.Commandline;
import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.synopsys.integration.blackduck.service.model.StreamRedirectThread;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.JenkinsDetectLogger;

import hudson.Util;
import hudson.remoting.Callable;

public abstract class DetectRemoteRunner implements Callable<DetectResponse, IntegrationException> {
    private static final long serialVersionUID = -4754831395795794586L;
    private static final String SYNOPSYS_LOG_LEVEL_PARAMETER = "logging.level.com.synopsys.integration";
    protected final JenkinsDetectLogger logger;
    protected final String detectProperties;
    protected final HashMap<String, String> environmentVariables;
    protected final String workspacePath;
    private final String jenkinsVersion;
    private final String pluginVersion;

    public DetectRemoteRunner(final JenkinsDetectLogger logger, final String detectProperties, final HashMap<String, String> environmentVariables, final String workspacePath, final String jenkinsVersion, final String pluginVersion) {
        this.logger = logger;
        this.detectProperties = detectProperties;
        this.environmentVariables = environmentVariables;
        this.workspacePath = workspacePath;
        this.jenkinsVersion = jenkinsVersion;
        this.pluginVersion = pluginVersion;
    }

    @Override
    public DetectResponse call() throws IntegrationException {
        try {
            setUp();
            final List<String> invocationParameters = getInvocationParameters();
            final ProcessBuilder processBuilder = createDetectProcessBuilder(invocationParameters);

            final Process process = processBuilder.start();
            final StreamRedirectThread redirectStdOutThread = new StreamRedirectThread(process.getInputStream(), logger.getJenkinsListener().getLogger());
            redirectStdOutThread.start();
            final int exitCode;
            try {
                exitCode = process.waitFor();
                redirectStdOutThread.join(0);
                IOUtils.copy(process.getErrorStream(), logger.getJenkinsListener().getLogger());
            } catch (final InterruptedException e) {
                logger.error("Detect thread was interrupted.", e);
                process.destroy();
                redirectStdOutThread.interrupt();
                return new DetectResponse(e);
            }
            return new DetectResponse(exitCode);
        } catch (final Exception e) {
            return new DetectResponse(e);
        }
    }

    protected abstract void setUp() throws Exception;

    protected abstract List<String> getInvocationParameters() throws Exception;

    protected abstract Function<String, String> getEscapingFunction();

    private ProcessBuilder createDetectProcessBuilder(final List<String> invocationParameters) throws Exception {
        final List<String> commandLineParameters = new ArrayList<>(invocationParameters);
        final List<String> detectArguments = parseDetectArguments(logger, detectProperties, getEscapingFunction());

        boolean setLoggingLevel = false;
        if (detectArguments != null && !detectArguments.isEmpty()) {
            for (final String property : detectArguments) {
                if (property.toLowerCase().contains(SYNOPSYS_LOG_LEVEL_PARAMETER)) {
                    setLoggingLevel = true;
                }
                commandLineParameters.add(property);
            }
        }
        if (!setLoggingLevel) {
            commandLineParameters.add(formatAsCommandLineParameter(SYNOPSYS_LOG_LEVEL_PARAMETER, logger.getLogLevel().toString()));
        }

        logger.info("Running Detect command: " + StringUtils.join(commandLineParameters, " "));

        // Phone Home Properties that we do not want logged:
        commandLineParameters.add(formatAsCommandLineParameter("detect.phone.home.passthrough.jenkins.version", jenkinsVersion));
        commandLineParameters.add(formatAsCommandLineParameter("detect.phone.home.passthrough.jenkins.plugin.version", pluginVersion));

        final ProcessBuilder processBuilder = new ProcessBuilder(commandLineParameters).directory(new File(workspacePath));
        processBuilder.environment().putAll(environmentVariables);
        return processBuilder;
    }

    private List<String> parseDetectArguments(final JenkinsDetectLogger logger, final String detectArgumentsBlob, final Function<String, String> escapeStringForExecution) {
        return Arrays.stream(Commandline.translateCommandline(detectArgumentsBlob))
                   .map(argumentBlobString -> argumentBlobString.split("\\r?\\n"))
                   .flatMap(Arrays::stream)
                   .filter(StringUtils::isNotBlank)
                   .map(argument -> handleVariableReplacement(logger, argument))
                   .map(escapeStringForExecution)
                   .collect(Collectors.toList());
    }

    private String handleVariableReplacement(final JenkinsDetectLogger logger, final String value) {
        if (value != null) {
            final String newValue = Util.replaceMacro(value, environmentVariables);
            if (StringUtils.isNotBlank(newValue) && newValue.contains("$")) {
                logger.warn("Variable may not have been properly replaced. Argument: " + value + ", resolved argument: " + newValue + ". Make sure the variable has been properly defined.");
            }
            return newValue;
        }
        return null;
    }

    private String formatAsCommandLineParameter(final String detectProperty, final String value) {
        return String.format("--%s=%s", detectProperty, value);
    }

    @Override
    public void checkRoles(final RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(this.getClass()));
    }
}
