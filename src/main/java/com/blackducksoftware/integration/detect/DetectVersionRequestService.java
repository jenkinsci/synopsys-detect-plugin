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
package com.blackducksoftware.integration.detect;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.blackducksoftware.integration.detect.jenkins.JenkinsProxyHelper;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.rest.AbstractRestConnectionBuilder;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.rest.UnauthenticatedRestConnectionBuilder;
import com.blackducksoftware.integration.log.IntLogger;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DetectVersionRequestService {
    public static final String LATEST_RELELASE = "LATEST_RELELASE";
    private final IntLogger logger;

    private final Boolean trustSSLCertificates;
    private final int connectionTimeout;

    private final String proxyHost;
    private final int proxyPort;
    private final String noProxyHost;
    private final String proxyUsername;
    private final String proxyPassword;

    public DetectVersionRequestService(final IntLogger logger, final Boolean trustSSLCertificates, final int connectionTimeout, final String proxyHost, final int proxyPort, final String noProxyHost, final String proxyUsername,
            final String proxyPassword) {
        this.logger = logger;
        this.trustSSLCertificates = trustSSLCertificates;
        this.connectionTimeout = connectionTimeout;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.noProxyHost = noProxyHost;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
    }

    public List<DetectVersionModel> getDetectVersionModels() throws IOException, IntegrationException, ParserConfigurationException, SAXException {
        final List<DetectVersionModel> detectVersions = new ArrayList<>();
        final UnauthenticatedRestConnectionBuilder restConnectionBuilder = new UnauthenticatedRestConnectionBuilder();
        restConnectionBuilder.setAlwaysTrustServerCertificate(trustSSLCertificates);
        restConnectionBuilder.setBaseUrl("https://test-repo.blackducksoftware.com/artifactory/bds-integrations-release/com/blackducksoftware/integration/hub-detect/maven-metadata.xml");
        restConnectionBuilder.setLogger(logger);
        setProxyInformation(restConnectionBuilder);
        restConnectionBuilder.setTimeout(connectionTimeout);
        final RestConnection restConnection = restConnectionBuilder.build();
        final HttpUrl detectMavenMetadataHttpUrl = restConnection.createHttpUrl();
        final Request request = restConnection.createGetRequest(detectMavenMetadataHttpUrl);
        Response response = null;
        try {
            response = restConnection.handleExecuteClientCall(request);
            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final Document document = builder.parse(response.body().byteStream());
            final Element versionsNode = (Element) document.getElementsByTagName("versions").item(0);
            final NodeList versionNodes = versionsNode.getElementsByTagName("version");
            for (int i = 0; i < versionNodes.getLength(); i++) {
                final DetectVersionModel versionModel = getDetectVersionModelFromNode(versionNodes.item(i));
                detectVersions.add(versionModel);
            }
            final DetectVersionModel latestVersionModel = new DetectVersionModel(LATEST_RELELASE, "Latest Release");
            detectVersions.add(latestVersionModel);
        } finally {
            IOUtils.closeQuietly(response);
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
        final String detectVersionFileURL = "https://test-repo.blackducksoftware.com/artifactory/bds-integrations-release/com/blackducksoftware/integration/hub-detect/" + versionName + "/hub-detect-" + versionName + ".jar";
        return detectVersionFileURL;
    }

    public File downloadFile(final String url, final File file) throws IntegrationException, IOException {
        final UnauthenticatedRestConnectionBuilder restConnectionBuilder = new UnauthenticatedRestConnectionBuilder();
        restConnectionBuilder.setAlwaysTrustServerCertificate(trustSSLCertificates);
        restConnectionBuilder.setBaseUrl(url);
        restConnectionBuilder.setLogger(logger);
        setProxyInformation(restConnectionBuilder);
        restConnectionBuilder.setTimeout(connectionTimeout);

        final RestConnection restConnection = restConnectionBuilder.build();
        final HttpUrl contentHttpUrl = restConnection.createHttpUrl();
        final Request request = restConnection.createGetRequest(contentHttpUrl);
        Response response = null;
        FileOutputStream fileOutputStream = null;
        try {
            response = restConnection.handleExecuteClientCall(request);
            final ResponseBody responseBody = response.body();
            final InputStream inputStream = responseBody.byteStream();
            fileOutputStream = new FileOutputStream(file);
            IOUtils.copy(inputStream, fileOutputStream);
        } finally {
            IOUtils.closeQuietly(response);
            IOUtils.closeQuietly(fileOutputStream);
        }
        return file;
    }

    public String getLatestReleasedDetectVersion() throws IntegrationException, IOException {
        final UnauthenticatedRestConnectionBuilder restConnectionBuilder = new UnauthenticatedRestConnectionBuilder();
        restConnectionBuilder.setAlwaysTrustServerCertificate(trustSSLCertificates);
        restConnectionBuilder.setBaseUrl("https://test-repo.blackducksoftware.com/artifactory/api/search/latestVersion?g=com.blackducksoftware.integration&a=hub-detect&repos=bds-integrations-release");
        restConnectionBuilder.setLogger(logger);
        setProxyInformation(restConnectionBuilder);
        restConnectionBuilder.setTimeout(connectionTimeout);

        final RestConnection restConnection = restConnectionBuilder.build();
        final HttpUrl contentHttpUrl = restConnection.createHttpUrl();
        final Request request = restConnection.createGetRequest(contentHttpUrl, "text/plain");
        Response response = null;
        try {
            response = restConnection.handleExecuteClientCall(request);
            final ResponseBody responseBody = response.body();
            final String version = responseBody.string();
            return version;
        } finally {
            IOUtils.closeQuietly(response);
        }
    }

    private void setProxyInformation(final AbstractRestConnectionBuilder restConnectionBuilder) {
        if (JenkinsProxyHelper.shouldUseProxy(restConnectionBuilder.getBaseConnectionUrl(), noProxyHost)) {
            restConnectionBuilder.setProxyHost(proxyHost);
            restConnectionBuilder.setProxyPort(proxyPort);
            restConnectionBuilder.setProxyUsername(proxyUsername);
            restConnectionBuilder.setProxyPassword(proxyPassword);
        }
    }
}
