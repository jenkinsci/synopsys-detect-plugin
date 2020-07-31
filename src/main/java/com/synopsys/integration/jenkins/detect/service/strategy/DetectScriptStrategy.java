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
package com.synopsys.integration.jenkins.detect.service.strategy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.IntegrationEscapeUtils;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.rest.client.IntHttpClient;
import com.synopsys.integration.rest.credentials.Credentials;
import com.synopsys.integration.rest.credentials.CredentialsBuilder;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.rest.proxy.ProxyInfoBuilder;
import com.synopsys.integration.rest.request.Request;
import com.synopsys.integration.rest.response.Response;
import com.synopsys.integration.util.OperatingSystemType;

import jenkins.security.MasterToSlaveCallable;

public class DetectScriptStrategy extends DetectExecutionStrategy {
    public static final String DETECT_INSTALL_DIRECTORY = "Detect_Installation";
    public static final String LATEST_SHELL_SCRIPT_URL = "https://detect.synopsys.com/detect.sh";
    public static final String LATEST_POWERSHELL_SCRIPT_URL = "https://detect.synopsys.com/detect.ps1";

    private final JenkinsIntLogger logger;
    private final OperatingSystemType operatingSystemType;
    private final JenkinsProxyHelper jenkinsProxyHelper;
    private final String toolsDirectory;

    public DetectScriptStrategy(JenkinsIntLogger logger, JenkinsProxyHelper jenkinsProxyHelper, OperatingSystemType operatingSystemType, String toolsDirectory) {
        this.logger = logger;
        this.jenkinsProxyHelper = jenkinsProxyHelper;
        this.operatingSystemType = operatingSystemType;
        this.toolsDirectory = toolsDirectory;
    }

    @Override
    public Function<String, String> getArgumentEscaper() {
        if (operatingSystemType == OperatingSystemType.WINDOWS) {
            return IntegrationEscapeUtils::escapePowerShell;
        }
        return IntegrationEscapeUtils::escapeXSI;
    }

    @Override
    public List<String> getInitialArguments(String remoteExecutablePath) {
        if (operatingSystemType == OperatingSystemType.WINDOWS) {
            return new ArrayList<>(Arrays.asList("powershell", String.format("\"Import-Module '%s'; detect\"", remoteExecutablePath)));
        }
        return new ArrayList<>(Arrays.asList("bash", remoteExecutablePath));
    }

    @Override
    public MasterToSlaveCallable<String, IntegrationException> getSetupCallable() {
        String scriptUrl;
        if (operatingSystemType == OperatingSystemType.WINDOWS) {
            scriptUrl = LATEST_POWERSHELL_SCRIPT_URL;
        } else {
            scriptUrl = LATEST_SHELL_SCRIPT_URL;
        }

        // ProxyInfo itself isn't serializable, so we unpack it into serializable pieces and rebuild it later when we download the script. -- rotte JUL 2020
        ProxyInfo proxyInfo;
        try {
            proxyInfo = jenkinsProxyHelper.getProxyInfo(scriptUrl);
        } catch (IllegalArgumentException e) {
            logger.warn("Synopsys Detect for Jenkins could not resolve proxy info from Jenkins because: " + e.getMessage());
            logger.warn("Continuing without proxy...");
            logger.trace("Stack trace:", e);
            proxyInfo = ProxyInfo.NO_PROXY_INFO;
        }

        String proxyHost = proxyInfo.getHost().orElse(null);
        int proxyPort = proxyInfo.getPort();
        String proxyUsername = proxyInfo.getUsername().orElse(null);
        String proxyPassword = proxyInfo.getPassword().orElse(null);
        String proxyNtlmDomain = proxyInfo.getNtlmDomain().orElse(null);
        String proxyNtlmWorkstation = proxyInfo.getNtlmWorkstation().orElse(null);
        return new SetupCallableImpl(logger, toolsDirectory, scriptUrl, proxyHost, proxyPort, proxyUsername, proxyPassword, proxyNtlmDomain, proxyNtlmWorkstation);
    }

    public static class SetupCallableImpl extends MasterToSlaveCallable<String, IntegrationException> {
        private static final long serialVersionUID = -4954105356640324485L;
        private final JenkinsIntLogger logger;
        private final String toolsDirectory;
        private final String scriptUrl;
        private final String proxyHost;
        private final int proxyPort;
        private final String proxyUsername;
        private final String proxyPassword;
        private final String proxyNtlmDomain;
        private final String proxyNtlmWorkstation;

        public SetupCallableImpl(JenkinsIntLogger logger, String toolsDirectory, String scriptUrl, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword,
            String proxyNtlmDomain, String proxyNtlmWorkstation) {
            this.logger = logger;
            this.toolsDirectory = toolsDirectory;
            this.scriptUrl = scriptUrl;
            this.proxyHost = proxyHost;
            this.proxyPort = proxyPort;
            this.proxyUsername = proxyUsername;
            this.proxyPassword = proxyPassword;
            this.proxyNtlmDomain = proxyNtlmDomain;
            this.proxyNtlmWorkstation = proxyNtlmWorkstation;
        }

        @Override
        public String call() throws IntegrationException {
            String scriptFileName = scriptUrl.substring(scriptUrl.lastIndexOf("/") + 1).trim();
            Path scriptDownloadDirectory = prepareScriptDownloadDirectory();
            Path detectScriptPath = scriptDownloadDirectory.resolve(scriptFileName);

            // .toFile().exists() is significantly more performant than Files.notExist, so we use that here. --rotte JUL 2020
            if (!detectScriptPath.toFile().exists()) {
                logger.info("Downloading Detect script from " + scriptUrl + " to " + detectScriptPath);
                downloadScriptTo(scriptUrl, detectScriptPath);
            } else {
                logger.info("Running already installed Detect script " + detectScriptPath);
            }

            String scriptRemotePath;
            try {
                scriptRemotePath = detectScriptPath.toRealPath().toString();
                if (StringUtils.isBlank(scriptRemotePath)) {
                    throw new DetectJenkinsException("[ERROR] The Detect script was not downloaded successfully.");
                }
            } catch (IOException e) {
                throw new DetectJenkinsException("[ERROR] The Detect script was not downloaded successfully: " + e.getMessage(), e);
            }

            return scriptRemotePath;
        }

        private Path prepareScriptDownloadDirectory() throws IntegrationException {
            Path installationDirectory = Paths.get(toolsDirectory, DETECT_INSTALL_DIRECTORY);

            try {
                Files.createDirectories(installationDirectory);
            } catch (Exception e) {
                throw new IntegrationException("Could not create the Detect installation directory: " + installationDirectory, e);
            }

            return installationDirectory;
        }

        private void downloadScriptTo(String url, Path path) throws IntegrationException {
            // Because we're rebuilding the ProxyInfo here, we shouldn't need to worry about IllegalArgumentExceptions. If we change that implementation, they should be handled nicely here. -- rotte JUL 2020
            CredentialsBuilder credentialsBuilder = Credentials.newBuilder();
            credentialsBuilder.setUsernameAndPassword(proxyUsername, proxyPassword);
            Credentials proxyCredentials = credentialsBuilder.build();

            ProxyInfoBuilder proxyInfoBuilder = ProxyInfo.newBuilder();
            proxyInfoBuilder.setHost(proxyHost);
            proxyInfoBuilder.setPort(proxyPort);
            proxyInfoBuilder.setCredentials(proxyCredentials);
            proxyInfoBuilder.setNtlmDomain(proxyNtlmDomain);
            proxyInfoBuilder.setNtlmWorkstation(proxyNtlmWorkstation);

            ProxyInfo proxyInfo = proxyInfoBuilder.build();

            IntHttpClient intHttpClient = new IntHttpClient(logger, 120, true, proxyInfo);

            Request request = new Request.Builder().uri(url).build();
            try (Response response = intHttpClient.execute(request)) {
                response.throwExceptionForError();
                Files.copy(response.getContent(), path);
            } catch (IOException e) {
                throw new DetectJenkinsException("Synopsys Detect for Jenkins could not download the script successfully: " + e.getMessage(), e);
            }
        }
    }

}
