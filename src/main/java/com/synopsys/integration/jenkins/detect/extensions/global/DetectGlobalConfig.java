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
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiFunction;
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

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.jenkins.detect.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.detect.SynopsysCredentialsHelper;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.log.PrintStreamIntLogger;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfig;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfigBuilder;
import com.synopsys.integration.rest.client.AuthenticatingIntHttpClient;
import com.synopsys.integration.rest.client.ConnectionResult;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.util.Buildable;
import com.synopsys.integration.util.IntegrationBuilder;

import hudson.Extension;
import hudson.Functions;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.IOUtils;
import hudson.util.ListBoxModel;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.util.xml.XMLUtils;

@Extension
public class DetectGlobalConfig extends GlobalConfiguration implements Serializable {
    private static final long serialVersionUID = -7629542889827231313L;
    private final Logger logger = Logger.getLogger(DetectGlobalConfig.class.getName());

    private String blackDuckUrl;
    private String blackDuckCredentialsId;
    private int blackDuckTimeout = 120;
    private boolean trustBlackDuckCertificates;
    private String polarisUrl;
    private String polarisCredentialsId;
    private int polarisTimeout = 120;
    private boolean trustPolarisCertificates;

    @DataBoundConstructor
    public DetectGlobalConfig() {
    }

    public String getBlackDuckUrl() {
        return blackDuckUrl;
    }

    @DataBoundSetter
    public void setBlackDuckUrl(final String blackDuckUrl) {
        this.blackDuckUrl = blackDuckUrl;
        save();
    }

    public int getBlackDuckTimeout() {
        return blackDuckTimeout;
    }

    @DataBoundSetter
    public void setBlackDuckTimeout(final int blackDuckTimeout) {
        this.blackDuckTimeout = blackDuckTimeout;
        save();
    }

    public boolean getTrustBlackDuckCertificates() {
        return trustBlackDuckCertificates;
    }

    @DataBoundSetter
    public void setTrustBlackDuckCertificates(final boolean trustBlackDuckCertificates) {
        this.trustBlackDuckCertificates = trustBlackDuckCertificates;
        save();
    }

    public String getBlackDuckCredentialsId() {
        return blackDuckCredentialsId;
    }

    @DataBoundSetter
    public void setBlackDuckCredentialsId(final String blackDuckCredentialsId) {
        this.blackDuckCredentialsId = blackDuckCredentialsId;
        save();
    }

    public String getPolarisUrl() {
        return polarisUrl;
    }

    @DataBoundSetter
    public void setPolarisUrl(final String polarisUrl) {
        this.polarisUrl = polarisUrl;
        save();
    }

    public String getPolarisCredentialsId() {
        return polarisCredentialsId;
    }

    @DataBoundSetter
    public void setPolarisCredentialsId(final String polarisCredentialsId) {
        this.polarisCredentialsId = polarisCredentialsId;
        save();
    }

    public boolean getTrustPolarisCertificates() {
        return trustPolarisCertificates;
    }

    @DataBoundSetter
    public void setTrustPolarisCertificates(final boolean trustPolarisCertificates) {
        this.trustPolarisCertificates = trustPolarisCertificates;
        save();
    }

    public int getPolarisTimeout() {
        return polarisTimeout;
    }

    @DataBoundSetter
    public void setPolarisTimeout(final int polarisTimeout) {
        this.polarisTimeout = polarisTimeout;
        save();
    }

    public Optional<String> getPolarisApiToken(@QueryParameter("polarisCredentialsId") final String polarisCredentialsId) {
        return SynopsysCredentialsHelper.getApiTokenFromCredentials(polarisCredentialsId);
    }

    public BlackDuckServerConfig getBlackDuckServerConfig() throws IllegalArgumentException {
        return getBlackDuckServerConfigBuilder().build();
    }

    public BlackDuckServerConfigBuilder getBlackDuckServerConfigBuilder() throws IllegalArgumentException {
        return createBlackDuckServerConfigBuilder(blackDuckUrl, blackDuckCredentialsId, trustBlackDuckCertificates, blackDuckTimeout);
    }

    public PolarisServerConfig getPolarisServerConfig() throws IllegalArgumentException {
        return getPolarisServerConfigBuilder().build();
    }

    public PolarisServerConfigBuilder getPolarisServerConfigBuilder() throws IllegalArgumentException {
        return createPolarisServerConfigBuilder(polarisUrl, polarisCredentialsId, trustPolarisCertificates, polarisTimeout);
    }

    public ListBoxModel doFillBlackDuckCredentialsIdItems() {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        return new StandardListBoxModel()
                   .includeEmptyValue()
                   .includeMatchingAs(ACL.SYSTEM, Jenkins.getInstance(), BaseStandardCredentials.class, Collections.emptyList(), SynopsysCredentialsHelper.API_TOKEN_OR_USERNAME_PASSWORD_CREDENTIALS);
    }

    public ListBoxModel doFillPolarisCredentialsIdItems() {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        return new StandardListBoxModel()
                   .includeEmptyValue()
                   .includeMatchingAs(ACL.SYSTEM, Jenkins.getInstance(), BaseStandardCredentials.class, Collections.emptyList(), SynopsysCredentialsHelper.API_TOKEN_CREDENTIALS);
    }

    @POST
    public FormValidation doTestBlackDuckConnection(@QueryParameter("blackDuckUrl") final String blackDuckUrl, @QueryParameter("blackDuckCredentialsId") final String blackDuckCredentialsId,
        @QueryParameter("blackDuckTimeout") final String blackDuckTimeout, @QueryParameter("trustBlackDuckCertificates") final boolean trustBlackDuckCertificates) {
        final BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = createBlackDuckServerConfigBuilder(blackDuckUrl, blackDuckCredentialsId, trustBlackDuckCertificates, Integer.valueOf(blackDuckTimeout));
        return validateConnection(blackDuckServerConfigBuilder, BlackDuckServerConfig::createBlackDuckHttpClient);
    }

    @POST
    public FormValidation doTestPolarisConnection(@QueryParameter("polarisUrl") final String polarisUrl, @QueryParameter("polarisCredentialsId") final String polarisCredentialsId,
        @QueryParameter("polarisTimeout") final String polarisTimeout, @QueryParameter("trustPolarisCertificates") final boolean trustPolarisCertificates) {
        final PolarisServerConfigBuilder polarisServerConfigBuilder = createPolarisServerConfigBuilder(polarisUrl, polarisCredentialsId, trustPolarisCertificates, Integer.valueOf(polarisTimeout));
        return validateConnection(polarisServerConfigBuilder, PolarisServerConfig::createPolarisHttpClient);
    }

    // EX: http://localhost:8080/descriptorByName/com.synopsys.integration.jenkins.detect.extensions.global.DetectGlobalConfig/config.xml
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

    private <T extends Buildable> FormValidation validateConnection(final IntegrationBuilder<T> configBuilder, final BiFunction<T, PrintStreamIntLogger, AuthenticatingIntHttpClient> createHttpClientMethod) {
        Jenkins.getInstance().checkPermission(Jenkins.ADMINISTER);
        try {
            final T config = configBuilder.build();
            final ConnectionResult connectionResult = createHttpClientMethod.apply(config, new PrintStreamIntLogger(System.out, LogLevel.DEBUG)).attemptConnection();
            return connectionResult.getFailureMessage()
                       .map(FormValidation::error)
                       .orElse(FormValidation.ok("Connection successful"));
        } catch (final IllegalArgumentException e) {
            return FormValidation.error(e.getMessage());
        }
    }

    private void updateByXml(final Source source) throws IOException, ParserConfigurationException {
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

        final String blackDuckUrl = getNodeValue(doc, "blackDuckUrl").orElse(StringUtils.EMPTY);
        final String blackDuckCredentialsId = getNodeValue(doc, "blackDuckCredentialsId").orElse(StringUtils.EMPTY);
        final int blackDuckTimeout = getNodeIntegerValue(doc, "blackDuckTimeout").orElse(120);
        final boolean trustBlackDuckCertificates = getNodeBooleanValue(doc, "trustBlackDuckCertificates").orElse(false);
        final String polarisUrl = getNodeValue(doc, "polarisUrl").orElse(StringUtils.EMPTY);
        final String polarisCredentialsId = getNodeValue(doc, "polarisCredentialsId").orElse(StringUtils.EMPTY);
        final int polarisTimeout = getNodeIntegerValue(doc, "polarisTimeout").orElse(120);
        final boolean trustPolarisCertificates = getNodeBooleanValue(doc, "trustPolarisCertificates").orElse(false);

        setBlackDuckUrl(blackDuckUrl);
        setBlackDuckCredentialsId(blackDuckCredentialsId);
        setBlackDuckTimeout(blackDuckTimeout);
        setTrustBlackDuckCertificates(trustBlackDuckCertificates);
        setPolarisUrl(polarisUrl);
        setPolarisCredentialsId(polarisCredentialsId);
        setPolarisTimeout(polarisTimeout);
        setTrustPolarisCertificates(trustPolarisCertificates);
        save();
    }

    private Optional<String> getNodeValue(final Document doc, final String tagName) {
        return Optional.ofNullable(doc.getElementsByTagName(tagName).item(0))
                   .map(Node::getFirstChild)
                   .map(Node::getNodeValue)
                   .map(String::trim);
    }

    private Optional<Boolean> getNodeBooleanValue(final Document doc, final String tagName) {
        return getNodeValue(doc, tagName).map(Boolean::valueOf);
    }

    private Optional<Integer> getNodeIntegerValue(final Document doc, final String tagName) {
        try {
            return getNodeValue(doc, tagName).map(Integer::valueOf);
        } catch (final NumberFormatException ignored) {
            logger.log(Level.WARNING, "Could not parse node " + tagName + ", provided value is not a valid integer. Using default value.");
            return Optional.empty();
        }
    }

    private BlackDuckServerConfigBuilder createBlackDuckServerConfigBuilder(final String blackDuckUrl, final String credentialsId, final boolean trustCertificates, final int timeout) {
        final BlackDuckServerConfigBuilder builder = BlackDuckServerConfig.newBuilder()
                                                         .setUrl(blackDuckUrl)
                                                         .setTrustCert(trustCertificates)
                                                         .setTimeout(timeout);

        SynopsysCredentialsHelper.getUsernameFromCredentials(credentialsId).ifPresent(builder::setUsername);
        SynopsysCredentialsHelper.getPasswordFromCredentials(credentialsId).ifPresent(builder::setPassword);
        SynopsysCredentialsHelper.getApiTokenFromCredentials(credentialsId).ifPresent(builder::setApiToken);

        final ProxyInfo proxyInfo = JenkinsProxyHelper.getProxyInfoFromJenkins(blackDuckUrl);

        //TODO: Just set the proxyInfo with the next version of Black Duck common

        proxyInfo.getHost().ifPresent(builder::setProxyHost);

        if (proxyInfo.getPort() != 0) {
            builder.setProxyPort(proxyInfo.getPort());
        }

        proxyInfo.getUsername().ifPresent(builder::setProxyUsername);
        proxyInfo.getPassword().ifPresent(builder::setProxyPassword);
        proxyInfo.getNtlmDomain().ifPresent(builder::setProxyNtlmDomain);
        proxyInfo.getNtlmWorkstation().ifPresent(builder::setProxyNtlmWorkstation);

        return builder;
    }

    private PolarisServerConfigBuilder createPolarisServerConfigBuilder(final String polarisUrl, final String credentialsId, final boolean trustCertificates, final int timeout) {
        final PolarisServerConfigBuilder builder = PolarisServerConfig.newBuilder();
        builder.setPolarisUrl(polarisUrl);
        builder.setTrustCert(trustCertificates);
        builder.setTimeoutSeconds(timeout);
        SynopsysCredentialsHelper.getApiTokenFromCredentials(credentialsId).ifPresent(builder::setAccessToken);

        final ProxyInfo proxyInfo = JenkinsProxyHelper.getProxyInfoFromJenkins(polarisUrl);

        //TODO: Just set the proxyInfo with the next version of Black Duck common

        proxyInfo.getHost().ifPresent(builder::setProxyHost);

        if (proxyInfo.getPort() != 0) {
            builder.setProxyPort(proxyInfo.getPort());
        }

        proxyInfo.getUsername().ifPresent(builder::setProxyUsername);
        proxyInfo.getPassword().ifPresent(builder::setProxyPassword);
        proxyInfo.getNtlmDomain().ifPresent(builder::setProxyNtlmDomain);
        proxyInfo.getNtlmWorkstation().ifPresent(builder::setProxyNtlmWorkstation);

        return builder;
    }

}
