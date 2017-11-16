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
import java.net.URL;
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
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.rest.UnauthenticatedRestConnection;
import com.blackducksoftware.integration.log.IntLogger;
import com.blackducksoftware.integration.log.LogLevel;
import com.blackducksoftware.integration.log.PrintStreamIntLogger;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DetectVersionRequestService {
    private final IntLogger logger;
    private final String proxyHost;
    private final int proxyPort;
    private final String noProxyHost;
    private final String proxyUsername;
    private final String proxyPassword;

    public DetectVersionRequestService(final IntLogger logger, final String proxyHost, final int proxyPort, final String noProxyHost, final String proxyUsername, final String proxyPassword) {
        this.logger = logger;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.noProxyHost = noProxyHost;
        this.proxyUsername = proxyUsername;
        this.proxyPassword = proxyPassword;
    }

    public List<DetectVersionModel> getDetectVersionModels() throws IOException, IntegrationException, ParserConfigurationException, SAXException {
        final List<DetectVersionModel> detectVersions = new ArrayList<>();
        final URL detectMavenMetadata = new URL("https://test-repo.blackducksoftware.com/artifactory/bds-integrations-release/com/blackducksoftware/integration/hub-detect/maven-metadata.xml");
        final RestConnection restConnection = new UnauthenticatedRestConnection(new PrintStreamIntLogger(System.out, LogLevel.DEBUG), detectMavenMetadata, 30);
        setProxyInformation(restConnection);
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
            final String latestVersion = getLatestReleasedDetectVersion();
            final URL detectLatestVersionUrl = new URL("https://test-repo.blackducksoftware.com/artifactory/bds-integrations-release/com/blackducksoftware/integration/hub-detect/" + latestVersion + "/hub-detect-" + latestVersion + ".jar");
            final DetectVersionModel latestVersionModel = new DetectVersionModel(detectLatestVersionUrl, "Latest Release");
            if (detectLatestVersionUrl != null) {
                detectVersions.add(latestVersionModel);
            }
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

    private URL getDetectVersionFileURL(final String versionName) throws MalformedURLException {
        final String versionUrl = "https://test-repo.blackducksoftware.com/artifactory/bds-integrations-release/com/blackducksoftware/integration/hub-detect/" + versionName + "/hub-detect-" + versionName + ".jar";
        final URL detectVersionFileURL = new URL(versionUrl);
        return detectVersionFileURL;
    }

    public File downloadFile(final String url, final File file) throws IntegrationException, IOException {
        final URL contentsURL = new URL(url);
        final RestConnection restConnection = new UnauthenticatedRestConnection(new PrintStreamIntLogger(System.out, LogLevel.DEBUG), contentsURL, 30);
        setProxyInformation(restConnection);
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
        final URL latestDetectVersionUrl = new URL("https://test-repo.blackducksoftware.com/artifactory/api/search/latestVersion?g=com.blackducksoftware.integration&a=hub-detect&repos=bds-integrations-release");
        final RestConnection restConnection = new UnauthenticatedRestConnection(new PrintStreamIntLogger(System.out, LogLevel.DEBUG), latestDetectVersionUrl, 30);
        setProxyInformation(restConnection);
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

    private void setProxyInformation(final RestConnection restConnection) {
        if (JenkinsProxyHelper.shouldUseProxy(restConnection.hubBaseUrl, noProxyHost)) {
            if (proxyHost != null) {
                restConnection.proxyHost = proxyHost;
            }
            restConnection.proxyPort = proxyPort;
            if (proxyUsername != null) {
                restConnection.proxyUsername = proxyUsername;
            }
            if (proxyPassword != null) {
                restConnection.proxyPassword = proxyPassword;
            }
        }
    }
}
