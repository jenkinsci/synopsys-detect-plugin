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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.types.Commandline;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.JenkinsDetectLogger;
import com.synopsys.integration.jenkins.detect.PluginHelper;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.detect.steps.remote.DetectRemoteJarRunner;
import com.synopsys.integration.jenkins.detect.steps.remote.DetectRemoteRunner;
import com.synopsys.integration.jenkins.detect.steps.remote.DetectRemoteScriptRunner;
import com.synopsys.integration.jenkins.detect.steps.remote.DetectResponse;
import com.synopsys.integration.jenkins.detect.tools.DummyToolInstallation;
import com.synopsys.integration.jenkins.detect.tools.DummyToolInstaller;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfigBuilder;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Util;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;

public class ExecuteDetectStep {
    private final Node node;
    private final TaskListener listener;
    private final EnvVars envVars;
    private final FilePath workspace;
    private final Run run;
    private final String javaHome;

    public ExecuteDetectStep(final Node node, final TaskListener listener, final FilePath workspace, final EnvVars envVars, final Run run, final String javaHome) {
        this.node = node;
        this.listener = listener;
        this.envVars = envVars;
        this.workspace = workspace;
        this.run = run;
        this.javaHome = javaHome;
    }

    public void executeDetect(final String detectProperties) {
        final JenkinsDetectLogger logger = new JenkinsDetectLogger(listener);
        final IntEnvironmentVariables variables = new IntEnvironmentVariables();
        variables.putAll(envVars);
        logger.setLogLevel(variables);

        try {
            final String pluginVersion = PluginHelper.getPluginVersion();
            logger.info("Running Detect jenkins plugin version: " + pluginVersion);

            populateAllBlackDuckEnvironmentVariables(envVars::putIfNotNull);
            populateAllPolarisEnvironmentVariables(envVars::putIfNotNull);

            final DetectRemoteRunner detectRunner = createAppropriateDetectRemoteRunner(logger, detectProperties);

            final DetectResponse response = node.getChannel().call(detectRunner);

            if (response.getExitCode() > 0) {
                logger.error("Detect failed with exit code: " + response.getExitCode());
                run.setResult(Result.FAILURE);
            } else if (null != response.getException()) {
                throw new DetectJenkinsException("Detect encountered an exception", response.getException());
            }
        } catch (final Exception e) {
            setBuildStatusFromException(logger, e, run::setResult);
        }
    }

    private void populateAllBlackDuckEnvironmentVariables(final BiConsumer<String, String> environmentPutter) {
        final BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = PluginHelper.getDetectGlobalConfig().getBlackDuckServerConfigBuilder();

        Arrays.stream(BlackDuckServerConfigBuilder.Property.values())
            .forEach(property -> environmentPutter.accept(property.getBlackDuckEnvironmentVariableKey(), blackDuckServerConfigBuilder.get(property)));
    }

    // TODO: Replace these puts with a cleaner impl (see populateAllBlackDuckEnvironmentVariables) when Polaris Common supports it
    private void populateAllPolarisEnvironmentVariables(final BiConsumer<String, String> environmentPutter) {
        final PolarisServerConfigBuilder polarisServerConfigBuilder = PluginHelper.getDetectGlobalConfig().getPolarisServerConfigBuilder();

        environmentPutter.accept("POLARIS_TIMEOUT_IN_SECONDS", String.valueOf(polarisServerConfigBuilder.getTimeoutSeconds()));
        environmentPutter.accept("POLARIS_TRUST_CERT", String.valueOf(polarisServerConfigBuilder.isTrustCert()));
        environmentPutter.accept("POLARIS_PROXY_HOST", polarisServerConfigBuilder.getProxyHost());
        environmentPutter.accept("POLARIS_PROXY_USERNAME", polarisServerConfigBuilder.getProxyUsername());
        environmentPutter.accept("POLARIS_PROXY_PASSWORD", polarisServerConfigBuilder.getProxyPassword());
        environmentPutter.accept("POLARIS_PROXY_NTLM_DOMAIN", polarisServerConfigBuilder.getProxyNtlmDomain());
        environmentPutter.accept("POLARIS_PROXY_NTLM_WORKSTATION", polarisServerConfigBuilder.getProxyNtlmWorkstation());
        if (polarisServerConfigBuilder.getProxyPort() != -1) {
            environmentPutter.accept("POLARIS_PROXY_PORT", String.valueOf(polarisServerConfigBuilder.getProxyPort()));
        }

        try {
            polarisServerConfigBuilder.build().populateEnvironmentVariables(environmentPutter);
        } catch (final Exception ignored) {
            // If this doesn't work, Detect will throw an exception later on.
        }

    }

    private DetectRemoteRunner createAppropriateDetectRemoteRunner(final JenkinsDetectLogger logger, final String detectProperties) throws DetectJenkinsException {
        final DetectRemoteRunner detectRunner;
        final String pathToDetectJar = envVars.get("DETECT_JAR");

        final String jenkinsVersion = PluginHelper.getJenkinsVersion();
        final String pluginVersion = PluginHelper.getPluginVersion();

        if (StringUtils.isNotBlank(pathToDetectJar)) {
            detectRunner = new DetectRemoteJarRunner(logger, envVars, workspace.getRemote(), jenkinsVersion, pluginVersion, javaHome, pathToDetectJar, getCorrectedParameters(detectProperties));
        } else {
            final DummyToolInstaller dummyInstaller = new DummyToolInstaller();
            final String toolsDirectory = dummyInstaller.getToolDir(new DummyToolInstallation(), node).getRemote();
            detectRunner = new DetectRemoteScriptRunner(logger, toolsDirectory, workspace.getRemote(), envVars, jenkinsVersion, pluginVersion, getCorrectedParameters(detectProperties));
        }

        return detectRunner;
    }

    private List<String> getCorrectedParameters(final String commandLineParameters) throws DetectJenkinsException {
        final String[] separatedParameters = Commandline.translateCommandline(commandLineParameters);
        final List<String> correctedParameters = new ArrayList<>();
        for (final String parameter : separatedParameters) {
            correctedParameters.add(handleVariableReplacement(envVars, parameter));
        }
        return correctedParameters;
    }

    private String handleVariableReplacement(final Map<String, String> variables, final String value) throws DetectJenkinsException {
        if (value != null) {
            final String newValue = Util.replaceMacro(value, variables);
            if (StringUtils.isNotBlank(newValue) && newValue.contains("$")) {
                throw new DetectJenkinsException("Variable was not properly replaced. Value: " + value + ", Result: " + newValue + ". Make sure the variable has been properly defined.");
            }
            return newValue;
        }
        return null;
    }

    private void setBuildStatusFromException(final JenkinsDetectLogger logger, final Exception exception, final Consumer<Result> resultConsumer) {
        if (exception instanceof InterruptedException) {
            logger.error("Detect thread was interrupted", exception);
            resultConsumer.accept(Result.ABORTED);
            Thread.currentThread().interrupt();
        } else if (exception instanceof IntegrationException) {
            logger.error(exception.getMessage());
            logger.debug(exception.getMessage(), exception);
            resultConsumer.accept(Result.UNSTABLE);
        } else {
            logger.error(exception.getMessage(), exception);
            resultConsumer.accept(Result.UNSTABLE);
        }
    }

}
