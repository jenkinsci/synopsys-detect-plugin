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
package com.synopsys.integration.jenkins.detect;

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

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.rest.client.IntHttpClient;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.rest.request.Request;
import com.synopsys.integration.rest.request.Response;

public class DetectVersionRequestService {
    public static final String AIR_GAP_ZIP = "AIR_GAP_ZIP";
    public static final String AIR_GAP_ZIP_SUFFIX = "-air-gap.zip";
    public static final String LATEST_RELELASE = "LATEST_RELELASE";

    private final IntLogger logger;
    private final Integer blackDuckTimeout;
    private final Boolean blackDuckTrustCertificates;
    private final ProxyInfo blackDuckProxyInfo;

    public DetectVersionRequestService(final IntLogger logger, final Integer blackDuckTimeout, final Boolean blackDuckTrustCertificates, final ProxyInfo blackDuckProxyInfo) {
        this.logger = logger;
        this.blackDuckTimeout = blackDuckTimeout;
        this.blackDuckTrustCertificates = blackDuckTrustCertificates;
        this.blackDuckProxyInfo = blackDuckProxyInfo;
    }

    public List<DetectVersionModel> getDetectVersionModels() throws IOException, IntegrationException, ParserConfigurationException, SAXException {
        final List<DetectVersionModel> detectVersions = new ArrayList<>();
        final String detectMavenMetadataUrl = getArtifactoryBaseUrl() + "/artifactory/bds-integrations-release/com/blackducksoftware/integration/hub-detect/maven-metadata.xml";
        final IntHttpClient intHttpClient = createIntHttpClient();

        final Request request = new Request.Builder().uri(detectMavenMetadataUrl).build();
        Response response = null;
        try {
            response = intHttpClient.execute(request);
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
        return new DetectVersionModel(getDetectVersionFileURL(versionName), versionName);
    }

    public String getDetectVersionFileURL(final String versionName) throws MalformedURLException {
        return String.format("%s/artifactory/bds-integrations-release/com/blackducksoftware/integration/hub-detect/%s/hub-detect-%s%s", getArtifactoryBaseUrl(), versionName, versionName, getDetectFileExtension());
    }

    public File downloadFile(final String url, final File file) throws IntegrationException, IOException {
        final IntHttpClient intHttpClient = createIntHttpClient();

        final Request request = new Request.Builder().uri(url).build();
        Response response = null;
        FileOutputStream fileOutputStream = null;
        try {
            response = intHttpClient.execute(request);
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
        final String detectLatestVersionUrl = getArtifactoryBaseUrl() + "/artifactory/api/search/latestVersion?g=com.blackducksoftware.integration&a=hub-detect&repos=bds-integrations-release";
        final IntHttpClient intHttpCLient = createIntHttpClient();

        final Request request = new Request.Builder().uri(detectLatestVersionUrl).mimeType("text/plain").build();
        Response response = null;
        try {
            response = intHttpCLient.execute(request);
            return response.getContentString();
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    private String getArtifactoryBaseUrl() {
        String baseUrl = "https://test-repo.blackducksoftware.com";
        final String overrideUrl = PluginHelper.getDetectGlobalConfig().getDetectArtifactUrl();
        if (StringUtils.isNotBlank(overrideUrl) && AIR_GAP_ZIP.equals(PluginHelper.getDetectGlobalConfig().getDetectDownloadUrl())) {
            baseUrl = overrideUrl.trim();
            baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 2) : baseUrl;
        }
        return baseUrl;
    }

    private String getDetectFileExtension() {
        if (AIR_GAP_ZIP.equals(PluginHelper.getDetectGlobalConfig().getDetectDownloadUrl())) {
            return AIR_GAP_ZIP_SUFFIX;
        }
        return ".jar";
    }

    private IntHttpClient createIntHttpClient() {
        return new IntHttpClient(logger, blackDuckTimeout, blackDuckTrustCertificates, blackDuckProxyInfo);
    }

}
