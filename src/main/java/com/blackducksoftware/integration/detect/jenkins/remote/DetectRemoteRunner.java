/**
 * blackduck-detect
 * <p>
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.detect.jenkins.remote;

import com.blackducksoftware.integration.detect.jenkins.JenkinsDetectLogger;
import com.blackducksoftware.integration.detect.jenkins.PluginHelper;
import com.blackducksoftware.integration.detect.jenkins.tools.DetectDownloadManager;
import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.proxy.ProxyInfo;
import com.blackducksoftware.integration.hub.service.model.StreamRedirectThread;
import com.blackducksoftware.integration.util.CIEnvironmentVariables;
import hudson.EnvVars;
import hudson.remoting.Callable;
import jenkins.model.Jenkins;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public class DetectRemoteRunner implements Callable<DetectResponse, IntegrationException> {
    private final JenkinsDetectLogger logger;
    private final String javaHome;

    private final String hubUrl;
    private final String hubUsername;
    private final String hubPassword;
    private final String hubApiToken;
    private final int hubTimeout;
    private final boolean trustSSLCertificates;
    private final String detectDownloadUrl;
    private final String toolsDirectory;
    private final List<String> detectProperties;

    private final EnvVars envVars;

    private String proxyHost;
    private int proxyPort;
    private String proxyUsername;
    private String proxyPassword;
    private String proxyNtlmDomain;

    public DetectRemoteRunner(final JenkinsDetectLogger logger, final String javaHome, final String hubUrl, final String hubUsername, final String hubPassword, final String hubApiToken, final int hubTimeout,
            final boolean trustSSLCertificates, final String detectDownloadUrl, final String toolsDirectory, final List<String> detectProperties, final EnvVars envVars) {
        this.logger = logger;
        this.javaHome = javaHome;
        this.hubUrl = hubUrl;
        this.hubUsername = hubUsername;
        this.hubPassword = hubPassword;
        this.hubApiToken = hubApiToken;
        this.hubTimeout = hubTimeout;
        this.trustSSLCertificates = trustSSLCertificates;
        this.detectDownloadUrl = detectDownloadUrl;
        this.toolsDirectory = toolsDirectory;
        this.detectProperties = detectProperties;
        this.envVars = envVars;
    }

    @Override
    public DetectResponse call() throws IntegrationException {
        try {
            final CIEnvironmentVariables cIEnvironmentVariables = new CIEnvironmentVariables();
            cIEnvironmentVariables.putAll(envVars);

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
            logger.info("Running with JAVA : " + javaExecutablePath);
            logger.info("Detect configured : " + detectDownloadUrl);
            final DetectDownloadManager detectDownloadManager = new DetectDownloadManager(logger, toolsDirectory, trustSSLCertificates, hubTimeout, proxyHost, proxyPort, proxyUsername, proxyPassword, proxyNtlmDomain);
            final File hubDetectJar = detectDownloadManager.handleDownload(detectDownloadUrl);

            logger.info("Running Detect : " + hubDetectJar.getName());

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
            logger.info("Running Detect command : " + StringUtils.join(commands, " "));

            // Phone Home Properties that we do not want logged:
            commands.add("--detect.phone.home.passthrough.jenkins.version=" + Jenkins.getVersion().toString());
            commands.add("--detect.phone.home.passthrough.jenkins.plugin.version=" + PluginHelper.getPluginVersion());

            final ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.directory(new File(cIEnvironmentVariables.getValue("WORKSPACE")));
            processBuilder.environment().putAll(cIEnvironmentVariables.getVariables());
            setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_URL", hubUrl);
            setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_TIMEOUT", String.valueOf(hubTimeout));
            setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_USERNAME", hubUsername);
            setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_PASSWORD", hubPassword);
            setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_API_TOKEN", hubApiToken);

            setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_AUTO_IMPORT_CERT", String.valueOf(trustSSLCertificates));
            setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_TRUST_CERT", String.valueOf(trustSSLCertificates));

            if (StringUtils.isNotBlank(proxyHost)) {
                setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_PROXY_HOST", proxyHost);
            }
            setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_PROXY_PORT", String.valueOf(proxyPort));
            if (StringUtils.isNotBlank(proxyUsername)) {
                setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_PROXY_USERNAME", proxyUsername);
            }
            if (StringUtils.isNotBlank(proxyPassword)) {
                setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_PROXY_PASSWORD", proxyPassword);
            }
            if (StringUtils.isNotBlank(proxyNtlmDomain)) {
                setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_PROXY_NTLM_DOMAIN", proxyNtlmDomain);
            }

            final Process process = processBuilder.start();
            final StreamRedirectThread redirectStdOutThread = new StreamRedirectThread(process.getInputStream(), logger.getJenkinsListener().getLogger());
            redirectStdOutThread.start();
            int exitCode = -1;
            try {
                exitCode = process.waitFor();
                redirectStdOutThread.join(0);
                IOUtils.copy(process.getErrorStream(), logger.getJenkinsListener().getLogger());
            } catch (final InterruptedException e) {
                logger.error("Detect thread was interrupted.", e);
                process.destroy();
                redirectStdOutThread.interrupt();
                Thread.currentThread().interrupt();
            }
            return new DetectResponse(exitCode);
        } catch (final Exception e) {
            return new DetectResponse(e);
        }
    }

    private void setProcessEnvironmentVariableString(final ProcessBuilder processBuilder, final String environmentVariableName, final String value) {
        if (StringUtils.isNotBlank(value)) {
            processBuilder.environment().put(environmentVariableName, value);
        }
    }

    @Override
    public void checkRoles(final RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(DetectRemoteRunner.class));
    }

    public void setProxyInfo(final ProxyInfo proxyInfo) {
        this.proxyHost = proxyInfo.getHost();
        this.proxyPort = proxyInfo.getPort();
        this.proxyUsername = proxyInfo.getUsername();
        try {
            this.proxyPassword = proxyInfo.getDecryptedPassword();
        } catch (IllegalArgumentException | EncryptionException e) {
            logger.error(e.getMessage(), e);
        }
        this.proxyNtlmDomain = proxyInfo.getNtlmDomain();
    }

}
