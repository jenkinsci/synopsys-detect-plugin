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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.synopsys.integration.IntegrationEscapeUtils;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.rest.HttpUrl;
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
    public static final String SHELL_SCRIPT_FILENAME = "detect.sh";
    public static final String LATEST_POWERSHELL_SCRIPT_URL = "https://detect.synopsys.com/detect.ps1";
    public static final String POWERSHELL_SCRIPT_FILENAME = "detect.ps1";

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
    public MasterToSlaveCallable<String, IntegrationException> getSetupCallable() throws IntegrationException {
        HttpUrl httpUrl;
        String scriptFileName;
        if (operatingSystemType == OperatingSystemType.WINDOWS) {
            httpUrl = new HttpUrl(LATEST_POWERSHELL_SCRIPT_URL);
            scriptFileName = POWERSHELL_SCRIPT_FILENAME;
        } else {
            httpUrl = new HttpUrl(LATEST_SHELL_SCRIPT_URL);
            scriptFileName = SHELL_SCRIPT_FILENAME;
        }

        // ProxyInfo itself isn't serializable, so we unpack it into serializable pieces and rebuild it later when we download the script. -- rotte JUL 2020
        ProxyInfo proxyInfo;
        try {
            proxyInfo = jenkinsProxyHelper.getProxyInfo(httpUrl.string());
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
        return new SetupCallableImpl(logger, toolsDirectory, httpUrl, scriptFileName, proxyHost, proxyPort, proxyUsername, proxyPassword, proxyNtlmDomain, proxyNtlmWorkstation);
    }

    public static class SetupCallableImpl extends MasterToSlaveCallable<String, IntegrationException> {
        private static final long serialVersionUID = -4954105356640324485L;
        private final JenkinsIntLogger logger;
        private final String toolsDirectory;
        private final HttpUrl httpUrl;
        private final String proxyHost;
        private final int proxyPort;
        private final String proxyUsername;
        private final String proxyPassword;
        private final String proxyNtlmDomain;
        private final String proxyNtlmWorkstation;
        private final String scriptFileName;

        public SetupCallableImpl(JenkinsIntLogger logger, String toolsDirectory, HttpUrl httpUrl, String scriptFileName, String proxyHost, int proxyPort, String proxyUsername, String proxyPassword,
            String proxyNtlmDomain, String proxyNtlmWorkstation) {
            this.logger = logger;
            this.toolsDirectory = toolsDirectory;
            this.httpUrl = httpUrl;
            this.scriptFileName = scriptFileName;
            this.proxyHost = proxyHost;
            this.proxyPort = proxyPort;
            this.proxyUsername = proxyUsername;
            this.proxyPassword = proxyPassword;
            this.proxyNtlmDomain = proxyNtlmDomain;
            this.proxyNtlmWorkstation = proxyNtlmWorkstation;
        }

        @Override
        public String call() throws IntegrationException {
            String scriptRemotePath;

            try {
                Path installationDirectory = Paths.get(toolsDirectory, DETECT_INSTALL_DIRECTORY);
                Files.createDirectories(installationDirectory);
                Path detectScriptPath = installationDirectory.resolve(scriptFileName);

                // .toFile().exists() is significantly more performant than Files.notExist, so we use that here. --rotte JUL 2020
                if (detectScriptPath.toFile().exists()) {
                    logger.info("Running already installed Detect script " + detectScriptPath);
                } else {
                    logger.info(String.format("Downloading Detect script from %s to %s", httpUrl.string(), detectScriptPath));

                    IntHttpClient intHttpClient = new IntHttpClient(logger, 120, true, rebuildProxyInfo());
                    Request request = new Request.Builder().url(httpUrl).build();

                    try (Response response = intHttpClient.execute(request)) {
                        response.throwExceptionForError();
                        Files.copy(response.getContent(), detectScriptPath);
                    }
                }

                scriptRemotePath = detectScriptPath.toRealPath().toString();
            } catch (Exception e) {
                throw new DetectJenkinsException("[ERROR] The Detect script was not downloaded successfully: " + e.getMessage(), e);
            }

            return scriptRemotePath;
        }

        private ProxyInfo rebuildProxyInfo() {
            CredentialsBuilder credentialsBuilder = Credentials.newBuilder();
            credentialsBuilder.setUsernameAndPassword(proxyUsername, proxyPassword);
            Credentials proxyCredentials = credentialsBuilder.build();

            ProxyInfoBuilder proxyInfoBuilder = ProxyInfo.newBuilder();
            proxyInfoBuilder.setHost(proxyHost);
            proxyInfoBuilder.setPort(proxyPort);
            proxyInfoBuilder.setCredentials(proxyCredentials);
            proxyInfoBuilder.setNtlmDomain(proxyNtlmDomain);
            proxyInfoBuilder.setNtlmWorkstation(proxyNtlmWorkstation);

            return proxyInfoBuilder.build();
        }
    }

}
