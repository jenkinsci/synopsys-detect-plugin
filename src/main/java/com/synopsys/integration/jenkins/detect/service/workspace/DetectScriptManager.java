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
package com.synopsys.integration.jenkins.detect.service.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.rest.client.IntHttpClient;
import com.synopsys.integration.rest.credentials.Credentials;
import com.synopsys.integration.rest.credentials.CredentialsBuilder;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.rest.proxy.ProxyInfoBuilder;
import com.synopsys.integration.rest.request.Request;
import com.synopsys.integration.rest.response.Response;

public class DetectScriptManager extends DetectExecutionManager {
    public static final String DETECT_INSTALL_DIRECTORY = "Detect_Installation";
    public static final String LATEST_SHELL_SCRIPT_URL = "https://detect.synopsys.com/detect.sh";
    public static final String LATEST_POWERSHELL_SCRIPT_URL = "https://detect.synopsys.com/detect.ps1";
    private static final long serialVersionUID = -8054698330656055909L;

    private final JenkinsIntLogger logger;
    private final String scriptUrl;
    private final String proxyHost;
    private final int proxyPort;
    private final String proxyUsername;
    private final String proxyPassword;
    private final String proxyNtlmDomain;
    private final String proxyNtlmWorkstation;
    private final String toolsDirectory;

    public DetectScriptManager(JenkinsIntLogger logger, String scriptUrl, ProxyInfo proxyInfo, String toolsDirectory) {
        this.logger = logger;
        this.scriptUrl = scriptUrl;
        // ProxyInfo itself isn't serializable, so we unpack it into serializable pieces and rebuild it later when we download the script. -- rotte JUL 2020
        this.proxyHost = proxyInfo.getHost().orElse(null);
        this.proxyPort = proxyInfo.getPort();
        this.proxyUsername = proxyInfo.getUsername().orElse(null);
        this.proxyPassword = proxyInfo.getPassword().orElse(null);
        this.proxyNtlmDomain = proxyInfo.getNtlmDomain().orElse(null);
        this.proxyNtlmWorkstation = proxyInfo.getNtlmWorkstation().orElse(null);
        this.toolsDirectory = toolsDirectory;
    }

    @Override
    public DetectSetupResponse setUpForExecution() throws IntegrationException {
        DetectSetupResponse.ExecutionStrategy executionStrategy;
        if (LATEST_POWERSHELL_SCRIPT_URL.equals(scriptUrl)) {
            executionStrategy = DetectSetupResponse.ExecutionStrategy.POWERSHELL_SCRIPT;
        } else if (LATEST_SHELL_SCRIPT_URL.equals(scriptUrl)) {
            executionStrategy = DetectSetupResponse.ExecutionStrategy.SHELL_SCRIPT;
        } else {
            throw new DetectJenkinsException("Invalid script url: " + scriptUrl + " valid script urls are " + LATEST_POWERSHELL_SCRIPT_URL + " and " + LATEST_SHELL_SCRIPT_URL);
        }

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

        return new DetectSetupResponse(executionStrategy, scriptRemotePath);
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
