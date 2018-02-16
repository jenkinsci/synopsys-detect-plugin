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
package com.blackducksoftware.integration.detect.jenkins.post;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.WebMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.blackducksoftware.integration.detect.DetectVersionModel;
import com.blackducksoftware.integration.detect.DetectVersionRequestService;
import com.blackducksoftware.integration.detect.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.detect.jenkins.JenkinsProxyHelper;
import com.blackducksoftware.integration.detect.jenkins.Messages;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.validator.HubServerConfigValidator;
import com.blackducksoftware.integration.log.LogLevel;
import com.blackducksoftware.integration.log.PrintStreamIntLogger;
import com.blackducksoftware.integration.validator.ValidationResults;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;

import hudson.Extension;
import hudson.ProxyConfiguration;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.IOUtils;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;

@Extension()
@SuppressWarnings("serial")
public class DetectPostBuildStepDescriptor extends BuildStepDescriptor<Publisher> implements Serializable {
    private String hubUrl;
    private String hubCredentialsId;
    private int hubTimeout = 120;
    private boolean trustSSLCertificates;
    private String detectArtifactUrl;
    private String detectDownloadUrl;
    private final String couldNotGetVersionsMessage = "Could not reach Black Duck public Artifactory";

    public DetectPostBuildStepDescriptor() {
        super(DetectPostBuildStep.class);
        load();
        HubServerInfoSingleton.getInstance().setHubUrl(hubUrl);
        HubServerInfoSingleton.getInstance().setHubCredentialsId(hubCredentialsId);
        HubServerInfoSingleton.getInstance().setHubTimeout(hubTimeout);
        HubServerInfoSingleton.getInstance().setTrustSSLCertificates(trustSSLCertificates);
        HubServerInfoSingleton.getInstance().setDetectArtifactUrl(detectArtifactUrl);
        HubServerInfoSingleton.getInstance().setDetectDownloadUrl(detectDownloadUrl);
    }

    public String getHubUrl() {
        return hubUrl;
    }

    public void setHubUrl(final String hubUrl) {
        this.hubUrl = hubUrl;
    }

    public String getHubCredentialsId() {
        return hubCredentialsId;
    }

    public void setHubCredentialsId(final String hubCredentialsId) {
        this.hubCredentialsId = hubCredentialsId;
    }

    public int getHubTimeout() {
        return hubTimeout;
    }

    public void setHubTimeout(final int hubTimeout) {
        this.hubTimeout = hubTimeout;
    }

    public boolean isTrustSSLCertificates() {
        return trustSSLCertificates;
    }

    public void setTrustSSLCertificates(final boolean trustSSLCertificates) {
        this.trustSSLCertificates = trustSSLCertificates;
    }

    public String getDetectArtifactUrl() {
        return detectArtifactUrl;
    }

    public void setDetectArtifactUrl(final String detectArtifactURL) {
        this.detectArtifactUrl = detectArtifactURL;
    }

    public String getDetectDownloadUrl() {
        return detectDownloadUrl;
    }

    public void setDetectDownloadUrl(final String detectDownloadUrl) {
        this.detectDownloadUrl = detectDownloadUrl;
    }

    @Override
    public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
        return true;
    }

    @Override
    public String getDisplayName() {
        return Messages.DetectPostBuildStep_getDisplayName();
    }

    public ListBoxModel doFillDetectDownloadUrlItems() {
        final ListBoxModel boxModel = new ListBoxModel();
        try {
            final DetectVersionRequestService detectVersionRequestService = getDetectVersionRequestService(getProxyConfiguration());
            final List<DetectVersionModel> detectVersionModels = detectVersionRequestService.getDetectVersionModels();
            for (final DetectVersionModel detectVersionModel : detectVersionModels) {
                boxModel.add(detectVersionModel.getVersionName(), detectVersionModel.getVersionURL());
            }
        } catch (final IntegrationException e) {
            System.err.println(couldNotGetVersionsMessage);
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.err.println(sw.toString());
        } catch (final Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.err.println(sw.toString());
        }
        boxModel.add("Default", "");
        return boxModel;
    }

    public FormValidation doCheckDetectDownloadUrl(@QueryParameter("detectDownloadUrl") final String detectDownloadUrl) {
        try {
            final DetectVersionRequestService detectVersionRequestService = getDetectVersionRequestService(getProxyConfiguration());
            detectVersionRequestService.getDetectVersionModels();
        } catch (final IntegrationException e) {
            return FormValidation.error(couldNotGetVersionsMessage);
        } catch (final IOException e) {
            return FormValidation.error(e.toString());
        } catch (final Exception e) {
            return FormValidation.error(e.toString());
        }
        return FormValidation.ok();
    }

    private DetectVersionRequestService getDetectVersionRequestService(final ProxyConfiguration proxyConfiguration) {
        String proxyHost = null;
        int proxyPort = 0;
        String noProxyHost = null;
        String proxyUsername = null;
        String proxyPassword = null;

        if (proxyConfiguration != null) {
            proxyHost = proxyConfiguration.name;
            proxyPort = proxyConfiguration.port;
            noProxyHost = proxyConfiguration.noProxyHost;
            proxyUsername = proxyConfiguration.getUserName();
            proxyPassword = proxyConfiguration.getPassword();
        }
        return new DetectVersionRequestService(new PrintStreamIntLogger(System.out, LogLevel.DEBUG), isTrustSSLCertificates(), getHubTimeout(), proxyHost, proxyPort, noProxyHost, proxyUsername, proxyPassword);
    }

    private ProxyConfiguration getProxyConfiguration() {
        ProxyConfiguration proxyConfig = null;
        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            proxyConfig = jenkins.proxy;
        }
        return proxyConfig;
    }

    public FormValidation doCheckHubTimeout(@QueryParameter("hubTimeout") final String hubTimeout) {
        if (StringUtils.isBlank(hubTimeout)) {
            return FormValidation.error(Messages.DetectPostBuildStep_getPleaseSetTimeout());
        }
        final HubServerConfigValidator validator = new HubServerConfigValidator();
        validator.setTimeout(hubTimeout);

        final ValidationResults results = new ValidationResults();
        validator.validateTimeout(results);

        if (!results.isSuccess()) {
            return FormValidation.error(results.getAllResultString());
        }
        return FormValidation.ok();
    }

    /**
     * Performs on-the-fly validation of the form field 'serverUrl'.
     *
     */
    public FormValidation doCheckHubUrl(@QueryParameter("hubUrl") final String hubUrl) {
        if (StringUtils.isBlank(hubUrl)) {
            return FormValidation.ok();
        }
        ProxyConfiguration proxyConfig = null;
        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            proxyConfig = jenkins.proxy;
        }
        final HubServerConfigValidator validator = new HubServerConfigValidator();
        validator.setHubUrl(hubUrl);
        validator.setAlwaysTrustServerCertificate(isTrustSSLCertificates());
        if (proxyConfig != null) {
            if (JenkinsProxyHelper.shouldUseProxy(hubUrl, proxyConfig.noProxyHost)) {
                validator.setProxyHost(proxyConfig.name);
                validator.setProxyPort(proxyConfig.port);
                validator.setProxyUsername(proxyConfig.getUserName());
                validator.setProxyPassword(proxyConfig.getPassword());
                // Must call assertProxyValid to complete setup of proxyInfo on this object
                validator.assertProxyValid();
            }
        }
        final ValidationResults results = new ValidationResults();
        validator.validateHubUrl(results);

        if (!results.isSuccess()) {
            return FormValidation.error(results.getAllResultString());
        }
        return FormValidation.ok();
    }

    public ListBoxModel doFillHubCredentialsIdItems() {
        ListBoxModel boxModel = null;
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        boolean changed = false;
        try {
            if (DetectPostBuildStepDescriptor.class.getClassLoader() != originalClassLoader) {
                changed = true;
                Thread.currentThread().setContextClassLoader(DetectPostBuildStepDescriptor.class.getClassLoader());
            }
            // Code copied from https://github.com/jenkinsci/git-plugin/blob/f6d42c4e7edb102d3330af5ca66a7f5809d1a48e/src/main/java/hudson/plugins/git/UserRemoteConfig.java
            final CredentialsMatcher credentialsMatcher = CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class));
            // Dont want to limit the search to a particular project for the drop down menu
            final AbstractProject<?, ?> project = null;
            boxModel = new StandardListBoxModel().withEmptySelection().withMatching(credentialsMatcher, CredentialsProvider.lookupCredentials(StandardCredentials.class, project, ACL.SYSTEM, Collections.<DomainRequirement> emptyList()));
        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
        return boxModel;
    }

    public FormValidation doTestConnection(@QueryParameter("hubUrl") final String hubUrl, @QueryParameter("hubCredentialsId") final String hubCredentialsId, @QueryParameter("hubTimeout") final String hubTimeout,
            @QueryParameter("trustSSLCertificates") final boolean trustSSLCertificates) {
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        boolean changed = false;
        try {
            if (DetectPostBuildStepDescriptor.class.getClassLoader() != originalClassLoader) {
                changed = true;
                Thread.currentThread().setContextClassLoader(DetectPostBuildStepDescriptor.class.getClassLoader());
            }
            if (StringUtils.isBlank(hubUrl)) {
                return FormValidation.error(Messages.DetectPostBuildStep_getPleaseSetServerUrl());
            }
            if (StringUtils.isBlank(hubCredentialsId)) {
                return FormValidation.error(Messages.DetectPostBuildStep_getPleaseSetHubCredentials());
            }

            String credentialUserName = null;
            String credentialPassword = null;

            UsernamePasswordCredentialsImpl credential = null;
            final AbstractProject<?, ?> project = null;
            final List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM, Collections.<DomainRequirement> emptyList());
            final IdMatcher matcher = new IdMatcher(hubCredentialsId);
            for (final StandardCredentials c : credentials) {
                if (matcher.matches(c) && c instanceof UsernamePasswordCredentialsImpl) {
                    credential = (UsernamePasswordCredentialsImpl) c;
                }
            }
            if (credential == null) {
                return FormValidation.error(Messages.DetectPostBuildStep_getPleaseSetHubCredentials());
            }
            credentialUserName = credential.getUsername();
            credentialPassword = credential.getPassword().getPlainText();

            final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
            hubServerConfigBuilder.setHubUrl(hubUrl);
            hubServerConfigBuilder.setUsername(credentialUserName);
            hubServerConfigBuilder.setPassword(credentialPassword);
            hubServerConfigBuilder.setTimeout(hubTimeout);
            hubServerConfigBuilder.setAlwaysTrustServerCertificate(trustSSLCertificates);

            final Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null) {
                final ProxyConfiguration proxyConfig = jenkins.proxy;
                if (proxyConfig != null) {
                    if (JenkinsProxyHelper.shouldUseProxy(hubUrl, proxyConfig.noProxyHost)) {
                        if (StringUtils.isNotBlank(proxyConfig.name) && proxyConfig.port >= 0) {
                            hubServerConfigBuilder.setProxyHost(proxyConfig.name);
                            hubServerConfigBuilder.setProxyPort(proxyConfig.port);
                            if (StringUtils.isNotBlank(jenkins.proxy.getUserName()) && StringUtils.isNotBlank(jenkins.proxy.getPassword())) {
                                hubServerConfigBuilder.setProxyUsername(jenkins.proxy.getUserName());
                                hubServerConfigBuilder.setProxyPassword(jenkins.proxy.getPassword());
                            }
                        }
                    }
                }
            }
            final HubServerConfig hubServerConfig = hubServerConfigBuilder.build();

            final RestConnection connection = hubServerConfig.createCredentialsRestConnection(new PrintStreamIntLogger(System.out, LogLevel.DEBUG));
            connection.connect();
            return FormValidation.ok(Messages.DetectPostBuildStep_getCredentialsValidFor_0_(hubUrl));

        } catch (final IllegalStateException e) {
            return FormValidation.error(e.getMessage());
        } catch (final HubIntegrationException e) {
            String message;
            if (e.getCause() != null) {
                message = e.getCause().toString();
                if (message.contains("(407)")) {
                    return FormValidation.error(e, message);
                }
            }
            return FormValidation.error(e, e.getMessage());
        } catch (final Exception e) {
            String message = null;
            if (e.getCause() != null && e.getCause().getCause() != null) {
                message = e.getCause().getCause().toString();
            } else if (e.getCause() != null) {
                message = e.getCause().toString();
            } else {
                message = e.toString();
            }
            if (message.toLowerCase().contains("service unavailable")) {
                message = Messages.DetectPostBuildStep_getCanNotReachThisServer_0_(hubUrl);
            } else if (message.toLowerCase().contains("precondition failed")) {
                message = message + ", Check your configuration.";
            }
            return FormValidation.error(e, message);
        } finally {
            if (changed) {
                Thread.currentThread().setContextClassLoader(originalClassLoader);
            }
        }
    }

    @Override
    public boolean configure(final StaplerRequest req, final JSONObject formData) throws Descriptor.FormException {
        // To persist global configuration information,
        // set that to properties and call save().

        hubUrl = formData.getString("hubUrl");
        hubCredentialsId = formData.getString("hubCredentialsId");
        hubTimeout = NumberUtils.toInt(formData.getString("hubTimeout"), 120);
        trustSSLCertificates = formData.getBoolean("trustSSLCertificates");
        detectArtifactUrl = formData.getString("detectArtifactURL");
        detectDownloadUrl = formData.getString("detectDownloadUrl");
        save();
        HubServerInfoSingleton.getInstance().setHubUrl(hubUrl);
        HubServerInfoSingleton.getInstance().setHubCredentialsId(hubCredentialsId);
        HubServerInfoSingleton.getInstance().setHubTimeout(hubTimeout);
        HubServerInfoSingleton.getInstance().setTrustSSLCertificates(trustSSLCertificates);
        HubServerInfoSingleton.getInstance().setDetectArtifactUrl(detectArtifactUrl);
        HubServerInfoSingleton.getInstance().setDetectDownloadUrl(detectDownloadUrl);

        return super.configure(req, formData);
    }

    @WebMethod(name = "config.xml")
    public void doConfigDotXml(final StaplerRequest req, final StaplerResponse rsp) throws IOException, TransformerException, hudson.model.Descriptor.FormException, ParserConfigurationException, SAXException {
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        boolean changed = false;
        try {
            if (DetectPostBuildStepDescriptor.class.getClassLoader() != originalClassLoader) {
                changed = true;
                Thread.currentThread().setContextClassLoader(DetectPostBuildStepDescriptor.class.getClassLoader());
            }
            if (req.getMethod().equals("GET")) {
                // read
                rsp.setContentType("application/xml");
                IOUtils.copy(getConfigFile().getFile(), rsp.getOutputStream());
                return;
            }
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

    public void updateByXml(final Source source) throws IOException, TransformerException, ParserConfigurationException, SAXException {
        final TransformerFactory tFactory = TransformerFactory.newInstance();
        final Transformer transformer = tFactory.newTransformer();
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");

        final ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();

        final StreamResult result = new StreamResult(byteOutput);
        transformer.transform(source, result);

        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final InputSource is = new InputSource(new StringReader(byteOutput.toString("UTF-8")));
        final Document doc = builder.parse(is);

        final String hubUrl = getNodeValue(doc, "hubUrl", null);
        final String hubCredentialsId = getNodeValue(doc, "hubCredentialsId", null);
        final String hubTimeout = getNodeValue(doc, "hubTimeout", "120");
        final String trustSSLCertificatesString = getNodeValue(doc, "trustSSLCertificates", "false");
        final String detectArtifactURL = getNodeValue(doc, "detectArtifactURL", null);
        final String detectDownloadUrl = getNodeValue(doc, "detectDownloadUrl", null);

        int serverTimeout = 120;
        final boolean trustSSLCertificates = Boolean.valueOf(trustSSLCertificatesString);
        try {
            serverTimeout = Integer.valueOf(hubTimeout);
        } catch (final NumberFormatException e) {
            System.err.println("Could not convert the provided timeout : " + hubTimeout + ", to an int value.");
            e.printStackTrace(System.err);
        }
        setHubUrl(hubUrl);
        setHubCredentialsId(hubCredentialsId);
        setHubTimeout(serverTimeout);
        setTrustSSLCertificates(trustSSLCertificates);

        HubServerInfoSingleton.getInstance().setHubUrl(hubUrl);
        HubServerInfoSingleton.getInstance().setHubCredentialsId(hubCredentialsId);
        HubServerInfoSingleton.getInstance().setHubTimeout(serverTimeout);
        HubServerInfoSingleton.getInstance().setTrustSSLCertificates(trustSSLCertificates);
        HubServerInfoSingleton.getInstance().setDetectArtifactUrl(detectArtifactURL);
        HubServerInfoSingleton.getInstance().setDetectDownloadUrl(detectDownloadUrl);
        save();
    }

    private String getNodeValue(final Document doc, final String tagName, final String defaultValue) {
        String nodeAsString = defaultValue != null ? defaultValue : "";
        final Node actualNode = doc.getElementsByTagName(tagName).item(0);
        if (actualNode != null && actualNode.getChildNodes() != null && actualNode.getChildNodes().item(0) != null) {
            nodeAsString = actualNode.getChildNodes().item(0).getNodeValue();
            if (nodeAsString != null) {
                nodeAsString = nodeAsString.trim();
            }
        }
        return nodeAsString;
    }

}
