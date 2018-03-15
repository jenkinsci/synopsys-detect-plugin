/**
 * Black Duck Detect Plugin
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
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
import com.blackducksoftware.integration.hub.proxy.ProxyInfo;
import com.blackducksoftware.integration.log.IntLogger;

public class DetectDownloadManager {
    public static final String DEFAULT_DETECT_JAR = "hub-detect-2.4.1.jar";
    public static final String DETECT_INSTALL_DIRECTORY = "Detect_Installation";
    public static final String DETECT_AIR_GAP_DIRECTORY = "Air_Gap";

    private final IntLogger logger;
    private final String toolsDirectory;

    private final DetectVersionRequestService detectVersionRequestService;

    public DetectDownloadManager(final IntLogger logger, final String toolsDirectory, final Boolean trustSSLCertificates, final int connectionTimeout, final ProxyInfo proxyInfo) {
        this.logger = logger;
        this.toolsDirectory = toolsDirectory;

        this.detectVersionRequestService = new DetectVersionRequestService(logger, trustSSLCertificates, connectionTimeout, proxyInfo);
    }

    public File handleDownload(final String fileUrl) throws IntegrationException, IOException {
        File detectFile = null;
        String downloadUrl = fileUrl;
        if (DetectVersionRequestService.LATEST_RELELASE.equals(fileUrl) || DetectVersionRequestService.AIR_GAP_ZIP.equals(fileUrl)) {
            final String versionName = detectVersionRequestService.getLatestReleasedDetectVersion();
            downloadUrl = detectVersionRequestService.getDetectVersionFileURL(versionName);
        }

        detectFile = getDetectJarFile(downloadUrl);

        if (shouldInstallDetect(detectFile, downloadUrl)) {
            if (downloadUrl.endsWith(".zip")) {
                final File airGapZip = new File(getAirGapDirectory(), trimToFileName(downloadUrl));
                logger.info("Downloading Hub Detect air gap zip from : " + downloadUrl + " to : " + airGapZip.getAbsolutePath());
                detectVersionRequestService.downloadFile(downloadUrl, airGapZip);
                logger.info("Unzipping air gap files...");
                ArchiveUtils.unzip(airGapZip);
            } else {
                logger.info("Downloading Hub Detect from : " + downloadUrl + " to : " + detectFile.getAbsolutePath());
                detectVersionRequestService.downloadFile(downloadUrl, detectFile);
            }
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

    private File getDetectJarFile(final String fileUrl) throws IntegrationException {
        File installationDirectory = getInstallationDirectory();
        if (StringUtils.isNotBlank(fileUrl) && fileUrl.endsWith(DetectVersionRequestService.AIR_GAP_ZIP_SUFFIX)) {
            installationDirectory = getAirGapDirectory();
            installationDirectory = new File(installationDirectory, getUnzippedDirectoryName(fileUrl));
        }
        final String detectFileName = getDetectFileName(fileUrl);
        return new File(installationDirectory, detectFileName);
    }

    // TODO make this method private in 2.0.0 so that this class can be refactored
    public String getDetectFileName(final String fileUrl) {
        if (StringUtils.isNotBlank(fileUrl)) {
            String fileName = trimToFileName(fileUrl);
            if (fileName.endsWith(DetectVersionRequestService.AIR_GAP_ZIP_SUFFIX)) {
                fileName = fileName.replace(DetectVersionRequestService.AIR_GAP_ZIP_SUFFIX, ".jar");
            }
            return fileName;
        }
        return DEFAULT_DETECT_JAR;
    }

    private String trimToFileName(final String fileUrl) {
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1).trim();
    }

    // TODO make this method private in 2.0.0 so that this class can be refactored
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

    private File getAirGapDirectory() throws IntegrationException {
        final File airGapDirectory = new File(getInstallationDirectory(), DETECT_AIR_GAP_DIRECTORY);
        try {
            airGapDirectory.mkdirs();
        } catch (final Exception e) {
            throw new IntegrationException("Could not create the Detect air gap directory: " + airGapDirectory.getAbsolutePath(), e);
        }
        return airGapDirectory;
    }

    private String getUnzippedDirectoryName(final String fileUrl) {
        if (StringUtils.isNotBlank(fileUrl)) {
            return ArchiveUtils.getUnzippedDirectoryName(fileUrl); // fileUrl.substring(fileUrl.lastIndexOf('/') + 1, fileUrl.lastIndexOf('.'));
        }
        return ArchiveUtils.getUnzippedDirectoryName(DEFAULT_DETECT_JAR); // DEFAULT_DETECT_JAR.substring(0, DEFAULT_DETECT_JAR.lastIndexOf('.'));
    }

    // TODO make this method private in 2.0.0 so that this class can be refactored
    public boolean shouldInstallDetect(final File detectFile, final String fileUrl) {
        return !detectFile.exists() && StringUtils.isNotBlank(fileUrl);
    }

    // TODO make this method private in 2.0.0 so that this class can be refactored
    public boolean shouldInstallDefaultDetect(final File detectFile) {
        return !detectFile.exists();
    }

}
