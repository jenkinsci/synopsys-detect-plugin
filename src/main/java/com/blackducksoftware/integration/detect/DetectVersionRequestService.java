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
package com.blackducksoftware.integration.detect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.blackducksoftware.integration.detect.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.detect.jenkins.JenkinsProxyHelper;
import com.blackducksoftware.integration.exception.EncryptionException;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.proxy.ProxyInfo;
import com.blackducksoftware.integration.hub.request.Request;
import com.blackducksoftware.integration.hub.request.Response;
import com.blackducksoftware.integration.hub.rest.AbstractRestConnectionBuilder;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.rest.UnauthenticatedRestConnectionBuilder;
import com.blackducksoftware.integration.log.IntLogger;

public class DetectVersionRequestService {
    public static final String AIR_GAP_ZIP = "AIR_GAP_ZIP";
    public static final String AIR_GAP_ZIP_SUFFIX = "-air-gap.zip";
    public static final String LATEST_RELELASE = "LATEST_RELELASE";

    private final IntLogger logger;

    private final Boolean trustSSLCertificates;
    private final int connectionTimeout;

    private final ProxyInfo proxyInfo;

    public DetectVersionRequestService(final IntLogger logger, final Boolean trustSSLCertificates, final int connectionTimeout, final ProxyInfo proxyInfo) {
        this.logger = logger;
        this.trustSSLCertificates = trustSSLCertificates;
        this.connectionTimeout = connectionTimeout;
        this.proxyInfo = proxyInfo;
    }

    public List<DetectVersionModel> getDetectVersionModels() throws IOException, IntegrationException, ParserConfigurationException, SAXException {
        final List<DetectVersionModel> detectVersions = new ArrayList<>();
        final String detectMavenMetadataUrl = getArtifactoryBaseUrl() + "/artifactory/bds-integrations-release/com/blackducksoftware/integration/hub-detect/maven-metadata.xml";
        final RestConnection restConnection = createUnauthenticatedRestConnection(detectMavenMetadataUrl);

        final Request request = new Request.Builder().uri(detectMavenMetadataUrl).build();
        Response response = null;
        try {
            response = restConnection.executeRequest(request);
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document document = builder.parse(response.getContent());
            final Element versionsNode = (Element) document.getElementsByTagName("versions").item(0);
            final NodeList versionNodes = versionsNode.getElementsByTagName("version");
            for (int i = 0; i < versionNodes.getLength(); i++) {
                final DetectVersionModel versionModel = getDetectVersionModelFromNode(versionNodes.item(i));
                detectVersions.add(versionModel);
            }
            final DetectVersionModel latestVersionModel = new DetectVersionModel(LATEST_RELELASE, "Latest Release");
            detectVersions.add(latestVersionModel);
        } finally {
            if (response != null) {
                response.close();
            }
        }

        return detectVersions;
    }

    private DetectVersionModel getDetectVersionModelFromNode(final Node node) throws MalformedURLException {
        final String versionName = node.getTextContent();
        final String versionDisplayName = versionName;
        final DetectVersionModel versionModel = new DetectVersionModel(getDetectVersionFileURL(versionName), versionDisplayName);
        return versionModel;
    }

    public String getDetectVersionFileURL(final String versionName) throws MalformedURLException {
        final String detectVersionFileURL = getArtifactoryBaseUrl() + "/artifactory/bds-integrations-release/com/blackducksoftware/integration/hub-detect/" + versionName + "/hub-detect-" + versionName + getDetectFileExtension();
        return detectVersionFileURL;
    }

    public File downloadFile(final String url, final File file) throws IntegrationException, IOException {
        final RestConnection restConnection = createUnauthenticatedRestConnection(url);

        final Request request = new Request.Builder().uri(url).build();
        Response response = null;
        FileOutputStream fileOutputStream = null;
        try {
            response = restConnection.executeRequest(request);
            fileOutputStream = new FileOutputStream(file);
            IOUtils.copy(response.getContent(), fileOutputStream);
        } finally {
            if (response != null) {
                response.close();
            }
            if (fileOutputStream != null) {
                fileOutputStream.close();
            }
        }
        return file;
    }

    public String getLatestReleasedDetectVersion() throws IntegrationException, IOException {
        final String detectLatestVersionUrl = getArtifactoryBaseUrl() + "/artifactory/bds-integrations-release/com/blackducksoftware/integration/hub-detect/maven-metadata.xml";
        final RestConnection restConnection = createUnauthenticatedRestConnection(detectLatestVersionUrl);

        final Request request = new Request.Builder().uri(detectLatestVersionUrl).mimeType("text/plain").build();
        Response response = null;
        try {
            response = restConnection.executeRequest(request);
            return response.getContentString();
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private String getArtifactoryBaseUrl() {
        String baseUrl = "https://test-repo.blackducksoftware.com";
        final String overrideUrl = HubServerInfoSingleton.getInstance().getDetectArtifactUrl();
        if (StringUtils.isNotBlank(overrideUrl) && AIR_GAP_ZIP.equals(HubServerInfoSingleton.getInstance().getDetectDownloadUrl())) {
            baseUrl = overrideUrl.trim();
            baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 2) : baseUrl;
        }
        return baseUrl;
    }

    private String getDetectFileExtension() {
        if (AIR_GAP_ZIP.equals(HubServerInfoSingleton.getInstance().getDetectDownloadUrl())) {
            return AIR_GAP_ZIP_SUFFIX;
        }
        return ".jar";
    }

    private RestConnection createUnauthenticatedRestConnection(final String url) {
        final UnauthenticatedRestConnectionBuilder restConnectionBuilder = new UnauthenticatedRestConnectionBuilder();
        restConnectionBuilder.setAlwaysTrustServerCertificate(trustSSLCertificates);
        restConnectionBuilder.setBaseUrl(url);
        restConnectionBuilder.setLogger(logger);
        setProxyInformation(restConnectionBuilder, url);
        restConnectionBuilder.setTimeout(connectionTimeout);
        return restConnectionBuilder.build();
    }

    private void setProxyInformation(final AbstractRestConnectionBuilder restConnectionBuilder, final String url) {
        if (JenkinsProxyHelper.shouldUseProxy(proxyInfo, url)) {
            restConnectionBuilder.setProxyHost(proxyInfo.getHost());
            restConnectionBuilder.setProxyPort(proxyInfo.getPort());
            restConnectionBuilder.setProxyUsername(proxyInfo.getUsername());
            try {
                restConnectionBuilder.setProxyPassword(proxyInfo.getDecryptedPassword());
            } catch (IllegalArgumentException | EncryptionException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }
}
