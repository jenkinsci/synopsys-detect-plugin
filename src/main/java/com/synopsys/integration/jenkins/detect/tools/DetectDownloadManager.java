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
package com.synopsys.integration.jenkins.detect.steps;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.rest.client.IntHttpClient;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.rest.request.Request;
import com.synopsys.integration.rest.request.Response;

public class DetectDownloadManager {
    public static final String DETECT_INSTALL_DIRECTORY = "Detect_Installation";
    //TODO: Update these
    public static final String LATEST_SHELL_SCRIPT_URL = "https://raw.githubusercontent.com/blackducksoftware/hub-detect/gh-pages/hub-detect.sh";
    public static final String LATEST_POWERSHELL_SCRIPT_URL = "https://raw.githubusercontent.com/blackducksoftware/hub-detect/gh-pages/hub-detect.ps1";

    private final IntLogger logger;
    private final String toolsDirectory;
    private final Integer blackDuckTimeout;
    private final Boolean blackDuckTrustCertificates;
    private final ProxyInfo blackDuckProxyInfo;

    public DetectDownloadManager(final IntLogger logger, final String toolsDirectory, final Integer blackDuckTimeout, final Boolean blackDuckTrustCertificates, final ProxyInfo blackDuckProxyInfo) {
        this.logger = logger;
        this.toolsDirectory = toolsDirectory;
        this.blackDuckTimeout = blackDuckTimeout;
        this.blackDuckTrustCertificates = blackDuckTrustCertificates;
        this.blackDuckProxyInfo = blackDuckProxyInfo;
    }

    public File downloadScript(final String scriptDownloadUrl) throws IntegrationException, IOException {
        final String scriptFileName = scriptDownloadUrl.substring(scriptDownloadUrl.lastIndexOf("/") + 1).trim();
        final File scriptDownloadDirectory = prepareScriptDownloadDirectory();
        final File localScriptFile = new File(scriptDownloadDirectory, scriptFileName);

        if (shouldDownloadScript(scriptDownloadUrl, localScriptFile)) {
            logger.info("Downloading Detect from: " + scriptDownloadUrl + " to: " + localScriptFile.getAbsolutePath());
            downloadScriptTo(scriptDownloadUrl, localScriptFile);
        } else {
            logger.debug("Detect is already installed at: " + localScriptFile.getAbsolutePath());
        }

        return localScriptFile;
    }

    private boolean shouldDownloadScript(final String scriptDownloadUrl, final File localScriptFile) {
        return !localScriptFile.exists() && StringUtils.isNotBlank(scriptDownloadUrl);
    }

    private File prepareScriptDownloadDirectory() throws IntegrationException {
        final File installationDirectory = new File(toolsDirectory, DETECT_INSTALL_DIRECTORY);

        try {
            installationDirectory.mkdirs();
        } catch (final Exception e) {
            throw new IntegrationException("Could not create the Detect installation directory: " + installationDirectory.getAbsolutePath(), e);
        }

        return installationDirectory;
    }

    private void downloadScriptTo(final String url, final File file) throws IntegrationException, IOException {
        final IntHttpClient intHttpClient = new IntHttpClient(logger, blackDuckTimeout, blackDuckTrustCertificates, blackDuckProxyInfo);

        final Request request = new Request.Builder().uri(url).build();
        try (final Response response = intHttpClient.execute(request); final FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            IOUtils.copy(response.getContent(), fileOutputStream);
        }
    }

}
