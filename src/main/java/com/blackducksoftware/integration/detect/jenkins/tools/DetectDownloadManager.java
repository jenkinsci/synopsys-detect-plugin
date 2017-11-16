/**
 * Black Duck Detect Plugin
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
package com.blackducksoftware.integration.detect.jenkins.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.detect.DetectVersionRequestService;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.log.IntLogger;

public class DetectDownloadManager {
    public static final String DEFAULT_DETECT_JAR = "hub-detect-1.2.0.jar";
    public static final String DETECT_INSTALL_DIRECTORY = "Detect_Installation";
    private final IntLogger logger;
    private final String toolsDirectory;

    private final Boolean trustSSLCertificates;
    private final int connectionTimeout;

    private final String proxyHost;
    private final int proxyPort;
    private final String noProxyHost;
    private final String proxyUsername;
    private final String proxyPassword;

    public DetectDownloadManager(final IntLogger logger, final String toolsDirectory, final Boolean trustSSLCertificates, final int connectionTimeout, final String proxyHost, final int proxyPort, final String noProxyHost,
            final String proxyUsername, final String proxyPassword) {
        this.logger = logger;
        this.toolsDirectory = toolsDirectory;
        this.trustSSLCertificates = trustSSLCertificates;
        this.connectionTimeout = connectionTimeout;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.noProxyHost = noProxyHost;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
    }

    public File handleDownload(final String fileUrl) throws IntegrationException, IOException {
        final File detectFile = getDetectFile(fileUrl);
        if (shouldInstallDetect(detectFile, fileUrl)) {
            logger.info("Downloading Hub Detect from : " + fileUrl + " to : " + detectFile.getAbsolutePath());
            final DetectVersionRequestService detectVersionRequestService = new DetectVersionRequestService(logger, trustSSLCertificates, connectionTimeout, proxyHost, proxyPort, noProxyHost, proxyUsername, proxyPassword);
            detectVersionRequestService.downloadFile(fileUrl, detectFile);
        } else if (shouldInstallDefaultDetect(detectFile)) {
            logger.info("Moving the default Hub Detect jar to : " + detectFile.getAbsolutePath());
            final InputStream inputStream = getClass().getResourceAsStream("/" + DEFAULT_DETECT_JAR);
            final FileOutputStream fileOutputStream = new FileOutputStream(detectFile);
            IOUtils.copy(inputStream, fileOutputStream);
        } else {
            logger.debug("Hub Detect is already installed at : " + detectFile.getAbsolutePath());
        }
        return detectFile;
    }

    public String getDetectFileName(final String fileUrl) {
        if (StringUtils.isBlank(fileUrl)) {
            return DEFAULT_DETECT_JAR;
        }
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }

    private File getDetectFile(final String fileUrl) throws IntegrationException {
        final File installationDirectory = getInstallationDirectory();
        final File detectFile = new File(installationDirectory, getDetectFileName(fileUrl));
        return detectFile;
    }

    public File getInstallationDirectory() throws IntegrationException {
        File installationDirectory = new File(toolsDirectory);
        installationDirectory = new File(installationDirectory, DETECT_INSTALL_DIRECTORY);
        try {
            installationDirectory.mkdirs();
        } catch (final Exception e) {
            throw new IntegrationException("Could not create the Detect installation directory: " + installationDirectory.getAbsolutePath(), e);
        }
        return installationDirectory;
    }

    public boolean shouldInstallDetect(final File detectFile, final String fileUrl) {
        return !detectFile.exists() && StringUtils.isNotBlank(fileUrl);
    }

    public boolean shouldInstallDefaultDetect(final File detectFile) {
        return !detectFile.exists();
    }

}
