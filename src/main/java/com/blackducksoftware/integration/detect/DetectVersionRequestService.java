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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.blackducksoftware.integration.detect.jenkins.JenkinsProxyHelper;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.rest.UnauthenticatedRestConnection;
import com.blackducksoftware.integration.log.LogLevel;
import com.blackducksoftware.integration.log.PrintStreamIntLogger;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class DetectVersionRequestService {

    public List<DetectVersionModel> getDetectVersionModels() throws IOException, IntegrationException, ParserConfigurationException, SAXException {
        final List<DetectVersionModel> detectVersions = new ArrayList<>();
        final URL detectMavenMetadata = new URL("http://repo2.maven.org/maven2/com/blackducksoftware/integration/hub-detect/maven-metadata.xml");
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
            final NodeList versionNodes = document.getElementsByTagName("version");
            for (int i = 0; i < versionNodes.getLength(); i++) {
                final DetectVersionModel versionModel = getDetectVersionModelFromNode(versionNodes.item(i));
                detectVersions.add(versionModel);
            }
            final DetectVersionModel latestVersionModel = new DetectVersionModel(new URL("http://repository.sonatype.org/service/local/artifact/maven/redirect?r=central-proxy&g=com.blackducksoftware.integration&a=hub-detect&v=LATEST"),
                    "Latest Release");
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

    private URL getDetectVersionFileURL(final String versionName) throws MalformedURLException {
        final String versionUrl = "http://repo2.maven.org/maven2/com/blackducksoftware/integration/hub-detect/" + versionName + "/hub-detect-" + versionName + ".jar";
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

    private void setProxyInformation(final RestConnection restConnection) {
        ProxyConfiguration proxyConfig = null;
        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            proxyConfig = jenkins.proxy;
        }
        if (proxyConfig != null) {
            if (JenkinsProxyHelper.shouldUseProxy(restConnection.hubBaseUrl, proxyConfig.noProxyHost)) {
                restConnection.proxyHost = proxyConfig.name;
                restConnection.proxyPort = proxyConfig.port;
                restConnection.proxyUsername = proxyConfig.getUserName();
                restConnection.proxyPassword = proxyConfig.getPassword();
            }
        }
    }
}
