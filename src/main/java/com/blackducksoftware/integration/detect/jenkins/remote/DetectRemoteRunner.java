/**
 * Black Duck Detect Plugin for Jenkins
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
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
package com.blackducksoftware.integration.detect.jenkins.remote;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.blackducksoftware.integration.detect.jenkins.JenkinsDetectLogger;
import com.blackducksoftware.integration.detect.jenkins.tools.DetectDownloadManager;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.StreamRedirectThread;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.util.CIEnvironmentVariables;

import hudson.remoting.Callable;

public class DetectRemoteRunner implements Callable<String, IntegrationException> {
    private final JenkinsDetectLogger logger;
    private final String javaHome;

    private final String hubUrl;
    private final String hubUsername;
    private final String hubPassword;
    private final int hubTimeout;
    private final boolean importSSLCerts;
    private final String detectDownloadUrl;
    private final String toolsDirectory;
    private final List<String> detectProperties;

    private final CIEnvironmentVariables cIEnvironmentVariables;

    private String proxyHost;
    private int proxyPort;
    private String proxyUsername;
    private String proxyPassword;

    public DetectRemoteRunner(final JenkinsDetectLogger logger, final String javaHome, final String hubUrl, final String hubUsername, final String hubPassword, final int hubTimeout, final boolean importSSLCerts,
            final String detectDownloadUrl, final String toolsDirectory, final List<String> detectProperties, final CIEnvironmentVariables cIEnvironmentVariables) {
        this.logger = logger;
        this.javaHome = javaHome;
        this.hubUrl = hubUrl;
        this.hubUsername = hubUsername;
        this.hubPassword = hubPassword;
        this.hubTimeout = hubTimeout;
        this.importSSLCerts = importSSLCerts;
        this.detectDownloadUrl = detectDownloadUrl;
        this.toolsDirectory = toolsDirectory;
        this.detectProperties = detectProperties;
        this.cIEnvironmentVariables = cIEnvironmentVariables;
    }

    @Override
    public String call() throws IntegrationException {
        try {
            String javaExecutablePath = "java";
            if (javaHome != null) {
                javaExecutablePath = javaHome + "bin" + File.separator;
                if (SystemUtils.IS_OS_WINDOWS) {
                    javaExecutablePath = javaExecutablePath + "java.exe";
                } else {
                    javaExecutablePath = javaExecutablePath + "java";
                }
            }
            logger.info("Running with JAVA : " + javaExecutablePath);

            final DetectDownloadManager detectDownloadManager = new DetectDownloadManager(logger, toolsDirectory);
            final File hubDetectJar = detectDownloadManager.handleDownload(detectDownloadUrl);

            logger.info("Running Detect Version : " + detectDownloadManager.getDetectFileName(detectDownloadUrl));

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

            final ProcessBuilder processBuilder = new ProcessBuilder(commands);
            processBuilder.directory(new File(cIEnvironmentVariables.getValue("WORKSPACE")));
            processBuilder.environment().putAll(cIEnvironmentVariables.getVariables());
            setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_URL", hubUrl);
            setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_TIMEOUT", String.valueOf(hubTimeout));
            setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_USERNAME", hubUsername);
            setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_PASSWORD", hubPassword);

            setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_AUTO_IMPORT_CERT", String.valueOf(importSSLCerts));

            if (proxyHost != null) {
                setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_PROXY_HOST", proxyHost);
                setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_PROXY_PORT", String.valueOf(proxyPort));
                setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_PROXY_USERNAME", proxyUsername);
                setProcessEnvironmentVariableString(processBuilder, "BLACKDUCK_HUB_PROXY_PASSWORD", proxyPassword);
            }

            final Process process = processBuilder.start();

            final StreamRedirectThread redirectStdOutThread = new StreamRedirectThread(process.getInputStream(), logger.getJenkinsListener().getLogger());
            redirectStdOutThread.start();

            final StreamRedirectThread redirectErrOutThread = new StreamRedirectThread(process.getErrorStream(), logger.getJenkinsListener().getLogger());
            redirectErrOutThread.start();

            final int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new HubIntegrationException("Hub Detect failed with exit code : " + exitCode);
            }
        } catch (final Exception e) {
            throw new IntegrationException(e);
        }
        return null;
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

    public void setProxyHost(final String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(final int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public void setProxyUsername(final String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public void setProxyPassword(final String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

}
