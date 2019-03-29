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
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.synopsys.integration.blackduck.service.model.StreamRedirectThread;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.JenkinsDetectLogger;

import hudson.EnvVars;
import hudson.remoting.Callable;

public abstract class DetectRemoteRunner implements Callable<DetectResponse, IntegrationException> {
    private static final long serialVersionUID = -4754831395795794586L;
    private static final String SYNOPSYS_LOG_LEVEL_PARAMETER = "logging.level.com.synopsys.integration";
    protected final JenkinsDetectLogger logger;
    protected final List<String> detectProperties;
    protected final EnvVars envVars;
    protected final String workspacePath;
    private final String jenkinsVersion;
    private final String pluginVersion;

    public DetectRemoteRunner(final JenkinsDetectLogger logger, final List<String> detectProperties, final EnvVars envVars, final String workspacePath, final String jenkinsVersion, final String pluginVersion) {
        this.logger = logger;
        this.detectProperties = detectProperties;
        this.envVars = envVars;
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

    private ProcessBuilder createDetectProcessBuilder(final List<String> invocationParameters) throws Exception {
        final List<String> commandLineParameters = new ArrayList<>(invocationParameters);

        boolean setLoggingLevel = false;
        if (detectProperties != null && !detectProperties.isEmpty()) {
            for (final String property : detectProperties) {
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
        processBuilder.environment().putAll(envVars);
        return processBuilder;
    }

    private String formatAsCommandLineParameter(final String detectProperty, final String value) {
        return String.format("--%s=%s", detectProperty, value);
    }

    @Override
    public void checkRoles(final RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(this.getClass()));
    }
}
