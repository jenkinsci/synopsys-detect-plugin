/**
 * blackduck-detect
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.blackduck.service.model.StreamRedirectThread;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.DetectVersionRequestService;
import com.synopsys.integration.jenkins.detect.JenkinsDetectLogger;
import com.synopsys.integration.jenkins.detect.PluginHelper;
import com.synopsys.integration.jenkins.detect.tools.DetectDownloadManager;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.rest.credentials.Credentials;
import com.synopsys.integration.rest.credentials.CredentialsBuilder;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.rest.proxy.ProxyInfoBuilder;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.EnvVars;
import hudson.remoting.Callable;
import jenkins.model.Jenkins;

public class DetectRemoteRunner implements Callable<DetectResponse, IntegrationException> {
    private static final long serialVersionUID = -3893076074803560801L;
    private final JenkinsDetectLogger logger;
    private final String javaHome;
    private final Integer blackDuckTimeout;
    private final Boolean blackDuckTrustCertificates;

    private final String detectDownloadUrl;
    private final String toolsDirectory;
    private final List<String> detectProperties;

    private final EnvVars envVars;
    private final String blackDuckPassword;
    private final String blackDuckUsername;
    private final String blackDuckUrl;
    private final String blackDuckApiToken;
    private final String blackDuckProxyHost;
    private final Integer blackDuckProxyPort;
    private final String blackDuckProxyUsername;
    private final String blackDuckProxyPassword;
    private final String blackDuckProxyNtlmDomain;
    private final String blackDuckProxyNtlmWorkstation;

    public DetectRemoteRunner(final JenkinsDetectLogger logger, final String javaHome, final BlackDuckServerConfig blackDuckServerConfig, final String detectDownloadUrl, final String toolsDirectory, final List<String> detectProperties,
        final EnvVars envVars) {
        this.logger = logger;
        this.javaHome = javaHome;
        this.blackDuckUrl = blackDuckServerConfig.getBlackDuckUrl().toString();
        this.blackDuckTimeout = blackDuckServerConfig.getTimeout();
        this.blackDuckUsername = blackDuckServerConfig.getCredentials().flatMap(Credentials::getUsername).orElse(StringUtils.EMPTY);
        this.blackDuckPassword = blackDuckServerConfig.getCredentials().flatMap(Credentials::getPassword).orElse(StringUtils.EMPTY);
        this.blackDuckApiToken = blackDuckServerConfig.getApiToken().orElse(StringUtils.EMPTY);
        this.blackDuckTrustCertificates = blackDuckServerConfig.isAlwaysTrustServerCertificate();
        this.blackDuckProxyHost = blackDuckServerConfig.getProxyInfo().getHost().orElse(StringUtils.EMPTY);
        this.blackDuckProxyPort = blackDuckServerConfig.getProxyInfo().getPort();
        this.blackDuckProxyUsername = blackDuckServerConfig.getProxyInfo().getUsername().orElse(StringUtils.EMPTY);
        this.blackDuckProxyPassword = blackDuckServerConfig.getProxyInfo().getPassword().orElse(StringUtils.EMPTY);
        this.blackDuckProxyNtlmDomain = blackDuckServerConfig.getProxyInfo().getNtlmDomain().orElse(StringUtils.EMPTY);
        this.blackDuckProxyNtlmWorkstation = blackDuckServerConfig.getProxyInfo().getNtlmWorkstation().orElse(StringUtils.EMPTY);
        this.detectDownloadUrl = detectDownloadUrl;
        this.toolsDirectory = toolsDirectory;
        this.detectProperties = detectProperties;
        this.envVars = envVars;
    }

    @Override
    public DetectResponse call() throws IntegrationException {
        try {
            final IntEnvironmentVariables intEnvironmentVariables = new IntEnvironmentVariables();
            intEnvironmentVariables.putAll(envVars);

            String javaExecutablePath = "java";
            if (javaHome != null) {
                File java = new File(javaHome);
                java = new File(java, "bin");
                if (SystemUtils.IS_OS_WINDOWS) {
                    java = new File(java, "java.exe");
                } else {
                    java = new File(java, "java");
                }
                javaExecutablePath = java.getCanonicalPath();
            }
            logger.info("Running with JAVA: " + javaExecutablePath);
            logger.info("Detect configured: " + detectDownloadUrl);

            debuggingLogs(intEnvironmentVariables);

            ProxyInfo blackDuckProxyInfo;
            try {
                final ProxyInfoBuilder proxyInfoBuilder = ProxyInfo.newBuilder();
                proxyInfoBuilder.setHost(blackDuckProxyHost);
                proxyInfoBuilder.setPort(blackDuckProxyPort);
                final CredentialsBuilder credentialsBuilder = Credentials.newBuilder();
                credentialsBuilder.setUsernameAndPassword(blackDuckProxyUsername, blackDuckProxyPassword);
                proxyInfoBuilder.setCredentials(credentialsBuilder.build());
                proxyInfoBuilder.setNtlmDomain(blackDuckProxyNtlmDomain);
                proxyInfoBuilder.setNtlmWorkstation(blackDuckProxyNtlmWorkstation);
                blackDuckProxyInfo = proxyInfoBuilder.build();
            } catch (final Exception e) {
                blackDuckProxyInfo = null;
            }

            final DetectVersionRequestService detectVersionRequestService = new DetectVersionRequestService(logger, blackDuckTimeout, blackDuckTrustCertificates, blackDuckProxyInfo);
            final DetectDownloadManager detectDownloadManager = new DetectDownloadManager(logger, toolsDirectory, detectVersionRequestService);
            final File hubDetectJar = detectDownloadManager.handleDownload(detectDownloadUrl);

            logger.info("Running Detect: " + hubDetectJar.getName());

            final List<String> commands = new ArrayList<>();
            commands.add(javaExecutablePath);
            commands.add("-jar");
            commands.add(hubDetectJar.getCanonicalPath());

            boolean setLoggingLevel = false;
            if (detectProperties != null && !detectProperties.isEmpty()) {
                for (final String property : detectProperties) {
                    if (property.toLowerCase().contains("logging.level.com.blackducksoftware.integration")) {
                        setLoggingLevel = true;
                    }
                    commands.add(property);
                }
            }
            if (!setLoggingLevel) {
                commands.add("--logging.level.com.blackducksoftware.integration=" + logger.getLogLevel().toString());
            }
            logger.info("Running Detect command: " + StringUtils.join(commands, " "));

            // Phone Home Properties that we do not want logged:
            commands.add("--detect.phone.home.passthrough.jenkins.version=" + Jenkins.getVersion().toString());
            commands.add("--detect.phone.home.passthrough.jenkins.plugin.version=" + PluginHelper.getPluginVersion());

            final ProcessBuilder processBuilder = new ProcessBuilder(commands);
            // Why aren't we passing in the workspace that we have available to us in ExecuteDetectStep?
            processBuilder.directory(new File(intEnvironmentVariables.getValue("WORKSPACE")));
            processBuilder.environment().putAll(intEnvironmentVariables.getVariables());

            setProcessEnvironmentVariableString(processBuilder, BlackDuckServerConfigBuilder.Property.URL, blackDuckUrl);
            setProcessEnvironmentVariableString(processBuilder, BlackDuckServerConfigBuilder.Property.TIMEOUT, String.valueOf(blackDuckTimeout));
            setProcessEnvironmentVariableString(processBuilder, BlackDuckServerConfigBuilder.Property.USERNAME, blackDuckUsername);
            setProcessEnvironmentVariableString(processBuilder, BlackDuckServerConfigBuilder.Property.PASSWORD, blackDuckPassword);
            setProcessEnvironmentVariableString(processBuilder, BlackDuckServerConfigBuilder.Property.API_TOKEN, blackDuckApiToken);
            setProcessEnvironmentVariableString(processBuilder, BlackDuckServerConfigBuilder.Property.TRUST_CERT, String.valueOf(blackDuckTrustCertificates));
            setProcessEnvironmentVariableString(processBuilder, BlackDuckServerConfigBuilder.Property.PROXY_HOST, blackDuckProxyHost);
            setProcessEnvironmentVariableString(processBuilder, BlackDuckServerConfigBuilder.Property.PROXY_PORT, String.valueOf(blackDuckProxyPort));
            setProcessEnvironmentVariableString(processBuilder, BlackDuckServerConfigBuilder.Property.PROXY_USERNAME, blackDuckProxyUsername);
            setProcessEnvironmentVariableString(processBuilder, BlackDuckServerConfigBuilder.Property.PROXY_PASSWORD, blackDuckProxyPassword);
            setProcessEnvironmentVariableString(processBuilder, BlackDuckServerConfigBuilder.Property.PROXY_NTLM_DOMAIN, blackDuckProxyNtlmDomain);
            setProcessEnvironmentVariableString(processBuilder, BlackDuckServerConfigBuilder.Property.PROXY_NTLM_WORKSTATION, blackDuckProxyNtlmWorkstation);

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

    private void setProcessEnvironmentVariableString(final ProcessBuilder processBuilder, final BlackDuckServerConfigBuilder.Property blackDuckServerProperty, final String value) {
        if (StringUtils.isNotBlank(value)) {
            processBuilder.environment().put(blackDuckServerProperty.getBlackDuckEnvironmentVariableKey(), value);
        }
    }

    @Override
    public void checkRoles(final RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(DetectRemoteRunner.class));
    }

    private void debuggingLogs(final IntEnvironmentVariables intEnvironmentVariables) {
        logger.debug("PATH: " + intEnvironmentVariables.getValue("PATH"));
        if (LogLevel.DEBUG == logger.getLogLevel()) {
            try {
                logger.info("Java version: ");
                final ProcessBuilder processBuilder = new ProcessBuilder(Arrays.asList("java", "-version"));
                processBuilder.environment().putAll(intEnvironmentVariables.getVariables());

                final Process process = processBuilder.start();

                process.waitFor();
                IOUtils.copy(process.getErrorStream(), logger.getJenkinsListener().getLogger());
                IOUtils.copy(process.getInputStream(), logger.getJenkinsListener().getLogger());
            } catch (final InterruptedException | IOException e) {
                logger.debug("Error printing the JAVA version: " + e.getMessage(), e);
            }
        }
    }

}
