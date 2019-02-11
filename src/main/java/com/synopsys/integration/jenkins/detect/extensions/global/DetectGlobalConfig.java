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
package com.synopsys.integration.jenkins.detect.extensions.global;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.verb.POST;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.blackduck.configuration.ConnectionResult;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.DetectVersionModel;
import com.synopsys.integration.jenkins.detect.DetectVersionRequestService;
import com.synopsys.integration.jenkins.detect.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.detect.tools.DetectDownloadManager;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.log.PrintStreamIntLogger;
import com.synopsys.integration.rest.proxy.ProxyInfo;

import hudson.Extension;
import hudson.Functions;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.IOUtils;
import hudson.util.ListBoxModel;
import hudson.util.Secret;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.util.xml.XMLUtils;

@Extension
public class DetectGlobalConfig extends GlobalConfiguration implements Serializable {
    private static final long serialVersionUID = -7629542889827231313L;
    private static final String COULD_NOT_REACH_BLACK_DUCK_PUBLIC_ARTIFACTORY = "Could not reach Black Duck public Artifactory";
    private static final Class<StringCredentialsImpl> API_TOKEN_CREDENTIALS_CLASS = StringCredentialsImpl.class;
    private static final Class<UsernamePasswordCredentialsImpl> USERNAME_PASSWORD_CREDENTIALS_CLASS = UsernamePasswordCredentialsImpl.class;
    private final Logger logger = Logger.getLogger(DetectGlobalConfig.class.getName());

    private String detectArtifactUrl;
    private String detectDownloadUrl;
    private String url;
    private String credentialsId;
    private int timeout;
    private boolean trustCertificates;

    @DataBoundConstructor
    public DetectGlobalConfig() {
    }

    public String getUrl() {
        return url;
    }

    @DataBoundSetter
    public void setUrl(final String url) {
        this.url = url;
        save();
    }

    public int getTimeout() {
        return timeout;
    }

    @DataBoundSetter
    public void setTimeout(final int timeout) {
        this.timeout = timeout;
        save();
    }

    public boolean getTrustCertificates() {
        return trustCertificates;
    }

    @DataBoundSetter
    public void setTrustCertificates(final boolean trustCertificates) {
        this.trustCertificates = trustCertificates;
        save();
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    @DataBoundSetter
    public void setCredentialsId(final String credentialsId) {
        this.credentialsId = credentialsId;
        save();
    }

    public String getDetectArtifactUrl() {
        return detectArtifactUrl;
    }

    @DataBoundSetter
    public void setDetectArtifactUrl(final String detectArtifactUrl) {
        this.detectArtifactUrl = detectArtifactUrl;
        save();
    }

    public String getDetectDownloadUrl() {
        return detectDownloadUrl;
    }

    @DataBoundSetter
    public void setDetectDownloadUrl(final String detectDownloadUrl) {
        this.detectDownloadUrl = detectDownloadUrl;
        save();
    }

    public BlackDuckServerConfig getBlackDuckServerConfig() throws IllegalArgumentException {
        return constructBlackDuckServerConfig(url, credentialsId, trustCertificates, timeout);
    }

    public Optional<URL> getBlackDuckUrl() {
        URL blackDuckUrl = null;
        if (url != null) {
            try {
                blackDuckUrl = new URL(url);
            } catch (final MalformedURLException ignored) {
                // Handled by form validation in the global configuration
            }
        }

        return Optional.ofNullable(blackDuckUrl);
    }

    public ListBoxModel doFillDetectDownloadUrlItems() {
        final ListBoxModel boxModel = new ListBoxModel();
        try {
            final DetectVersionRequestService detectVersionRequestService = getDetectVersionRequestService();
            final List<DetectVersionModel> detectVersionModels = detectVersionRequestService.getDetectVersionModels();
            for (final DetectVersionModel detectVersionModel : detectVersionModels) {
                boxModel.add(detectVersionModel.getVersionName(), detectVersionModel.getVersionURL());
            }
        } catch (final IntegrationException e) {
            logger.log(Level.SEVERE, COULD_NOT_REACH_BLACK_DUCK_PUBLIC_ARTIFACTORY, e);
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        boxModel.add(String.format("Default (%s)", DetectDownloadManager.DEFAULT_DETECT_VERSION), "");
        boxModel.add("Latest Air Gap Zip", DetectVersionRequestService.AIR_GAP_ZIP);
        return boxModel;
    }

    public ListBoxModel doFillCredentialsIdItems() {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        return new StandardListBoxModel()
                   .includeEmptyValue()
                   .includeMatchingAs(ACL.SYSTEM, Jenkins.getInstance(), BaseStandardCredentials.class, Collections.emptyList(),
                       CredentialsMatchers.either(CredentialsMatchers.instanceOf(API_TOKEN_CREDENTIALS_CLASS), CredentialsMatchers.instanceOf(USERNAME_PASSWORD_CREDENTIALS_CLASS)));
    }

    @POST
    public FormValidation doCheckDetectDownloadUrl(@QueryParameter("detectDownloadUrl") final String detectDownloadUrl) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        if (StringUtils.isBlank(detectDownloadUrl)) {
            return FormValidation.ok();
        }
        try {
            final boolean foundMatch = getDetectVersionRequestService().getDetectVersionModels().stream()
                                           .map(DetectVersionModel::getVersionURL)
                                           .anyMatch(detectDownloadUrl::equals);

            if (!foundMatch) {
                return FormValidation.error(detectDownloadUrl + ", does not appear to be a valid URL for Detect.");
            }
        } catch (final IntegrationException e) {
            return FormValidation.error(COULD_NOT_REACH_BLACK_DUCK_PUBLIC_ARTIFACTORY);
        } catch (final Exception e) {
            return FormValidation.error(e.toString());
        }
        return FormValidation.ok();
    }

    @POST
    public FormValidation doTestConnection(@QueryParameter("url") final String url, @QueryParameter("credentialsId") final String credentialsId, @QueryParameter("timeout") final String timeout,
        @QueryParameter("trustCertificates") final boolean trustCertificates) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        try {
            final BlackDuckServerConfig blackDuckServerConfig = constructBlackDuckServerConfig(url, credentialsId, trustCertificates, Integer.valueOf(timeout));
            final ConnectionResult connectionResult = blackDuckServerConfig.attemptConnection(new PrintStreamIntLogger(System.out, LogLevel.DEBUG));
            final String credentialsValid = String.format("Credentials valid for: %s", url);
            return connectionResult.getErrorMessage()
                       .map(FormValidation::error)
                       .orElse(FormValidation.ok(credentialsValid));
        } catch (final IllegalArgumentException e) {
            return FormValidation.error(e.getMessage());
        }
    }

    // EX: http://localhost:8080/descriptorByName/com.blackducksoftware.integration.detect.jenkins.post.DetectPostBuildStep/config.xml
    @WebMethod(name = "config.xml")
    public void doConfigDotXml(final StaplerRequest req, final StaplerResponse rsp) throws IOException, ServletException, ParserConfigurationException {
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        boolean changed = false;
        try {
            if (this.getClass().getClassLoader() != originalClassLoader) {
                changed = true;
                Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
            }
            Functions.checkPermission(Jenkins.ADMINISTER);
            if (req.getMethod().equals("GET")) {
                // read
                rsp.setContentType("application/xml");
                IOUtils.copy(getConfigFile().getFile(), rsp.getOutputStream());
                return;
            }
            Functions.checkPermission(Jenkins.ADMINISTER);
            if (req.getMethod().equals("POST")) {
                // submission
                updateByXml(new StreamSource(req.getReader()));
                return;
            }
            // huh?
            rsp.sendError(javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST);
        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    public void updateByXml(final Source source) throws IOException, ParserConfigurationException {
        final Document doc;
        try (final StringWriter out = new StringWriter()) {
            // this allows us to use UTF-8 for storing data,
            // plus it checks any well-formedness issue in the submitted
            // data
            XMLUtils.safeTransform(source, new StreamResult(out));

            final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = factory.newDocumentBuilder();
            final InputSource is = new InputSource(new StringReader(out.toString()));

            doc = builder.parse(is);
        } catch (TransformerException | SAXException e) {
            throw new IOException("Failed to persist configuration.xml", e);
        }

        final String hubUrl = getNodeValue(doc, "url").orElse(StringUtils.EMPTY);
        final String hubCredentialsId = getNodeValue(doc, "credentialsId").orElse(StringUtils.EMPTY);
        final String hubTimeout = getNodeValue(doc, "timeout").orElse("120");
        final String trustSSLCertificatesString = getNodeValue(doc, "trustCertificates").orElse("false");
        final String detectArtifactUrl = getNodeValue(doc, "detectArtifactUrl").orElse(StringUtils.EMPTY);
        final String detectDownloadUrl = getNodeValue(doc, "detectDownloadUrl").orElse(StringUtils.EMPTY);

        int serverTimeout = 120;
        final boolean trustSSLCertificates = Boolean.valueOf(trustSSLCertificatesString);
        try {
            serverTimeout = Integer.valueOf(hubTimeout);
        } catch (final NumberFormatException e) {
            logger.log(Level.SEVERE, "Could not convert the provided timeout: " + hubTimeout + ", to an int value.", e);
        }

        setUrl(hubUrl);
        setCredentialsId(hubCredentialsId);
        setTimeout(serverTimeout);
        setTrustCertificates(trustSSLCertificates);
        setDetectArtifactUrl(detectArtifactUrl);
        setDetectDownloadUrl(detectDownloadUrl);
        save();
    }

    private Optional<String> getNodeValue(final Document doc, final String tagName) {
        return Optional.ofNullable(doc.getElementsByTagName(tagName).item(0))
                   .map(Node::getFirstChild)
                   .map(Node::getNodeValue)
                   .map(String::trim);
    }

    private DetectVersionRequestService getDetectVersionRequestService() throws IllegalArgumentException {
        return new DetectVersionRequestService(new PrintStreamIntLogger(System.out, LogLevel.DEBUG), getTimeout(), getTrustCertificates(), getBlackDuckServerConfig().getProxyInfo());
    }

    private BlackDuckServerConfig constructBlackDuckServerConfig(final String url, final String credentialsId, final boolean trustCertificates, final int timeout) throws IllegalArgumentException {
        final BlackDuckServerConfigBuilder builder = new BlackDuckServerConfigBuilder()
                                                         .setUrl(url)
                                                         .setTrustCert(trustCertificates)
                                                         .setTimeout(timeout);

        getBlackDuckUsername(credentialsId).ifPresent(builder::setUsername);
        getBlackDuckPassword(credentialsId).ifPresent(builder::setPassword);
        getBlackDuckApiToken(credentialsId).ifPresent(builder::setApiToken);

        final ProxyInfo proxyInfo = JenkinsProxyHelper.getProxyInfoFromJenkins(url);

        proxyInfo.getHost().ifPresent(builder::setProxyHost);

        if (proxyInfo.getPort() != 0) {
            builder.setProxyPort(proxyInfo.getPort());
        }

        proxyInfo.getUsername().ifPresent(builder::setProxyUsername);
        proxyInfo.getPassword().ifPresent(builder::setProxyPassword);
        proxyInfo.getNtlmDomain().ifPresent(builder::setProxyNtlmDomain);
        proxyInfo.getNtlmWorkstation().ifPresent(builder::setProxyNtlmWorkstation);

        return builder.build();
    }

    private Optional<String> getBlackDuckUsername(final String credentialsId) {
        return getCredentials(credentialsId)
                   .filter(USERNAME_PASSWORD_CREDENTIALS_CLASS::isInstance)
                   .map(USERNAME_PASSWORD_CREDENTIALS_CLASS::cast)
                   .map(UsernamePasswordCredentialsImpl::getUsername);
    }

    private Optional<String> getBlackDuckPassword(final String credentialsId) {
        return getCredentials(credentialsId)
                   .filter(USERNAME_PASSWORD_CREDENTIALS_CLASS::isInstance)
                   .map(USERNAME_PASSWORD_CREDENTIALS_CLASS::cast)
                   .map(UsernamePasswordCredentialsImpl::getPassword)
                   .map(Secret::getPlainText);
    }

    private Optional<String> getBlackDuckApiToken(final String credentialsId) {
        return getCredentials(credentialsId)
                   .filter(API_TOKEN_CREDENTIALS_CLASS::isInstance)
                   .map(API_TOKEN_CREDENTIALS_CLASS::cast)
                   .map(StringCredentialsImpl::getSecret)
                   .map(Secret::getPlainText);
    }

    private Optional<BaseStandardCredentials> getCredentials(final String credentialsId) {
        if (StringUtils.isBlank(credentialsId)) {
            return Optional.empty();
        }

        final IdMatcher idMatcher = new IdMatcher(credentialsId);

        return CredentialsProvider.lookupCredentials(BaseStandardCredentials.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.emptyList()).stream()
                   .filter(idMatcher::matches)
                   .findAny();
    }

}
