/**
 * blackduck-detect
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
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
import com.blackducksoftware.integration.hub.configuration.HubServerConfig;
import com.blackducksoftware.integration.hub.configuration.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.configuration.HubServerConfigValidator;
import com.blackducksoftware.integration.hub.exception.HubIntegrationException;
import com.blackducksoftware.integration.hub.proxy.ProxyInfo;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.log.LogLevel;
import com.blackducksoftware.integration.log.PrintStreamIntLogger;
import com.blackducksoftware.integration.validator.ValidationResults;
import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;

import hudson.Extension;
import hudson.Functions;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import hudson.util.IOUtils;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.util.xml.XMLUtils;
import net.sf.json.JSONObject;

@Extension()
@SuppressWarnings("serial")
public class DetectPostBuildStepDescriptor extends BuildStepDescriptor<Publisher> implements Serializable {
    private final String couldNotGetVersionsMessage = "Could not reach Black Duck public Artifactory";
    private String hubUrl;
    private String hubCredentialsId;
    private int hubTimeout = 120;
    private boolean trustSSLCertificates;
    private String detectArtifactUrl;
    private String detectDownloadUrl;

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

    public void setDetectArtifactUrl(final String detectArtifactUrl) {
        this.detectArtifactUrl = detectArtifactUrl;
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
            final DetectVersionRequestService detectVersionRequestService = getDetectVersionRequestService();
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
        boxModel.add("Latest Air Gap Zip", DetectVersionRequestService.AIR_GAP_ZIP.toString());
        return boxModel;
    }

    public FormValidation doCheckDetectDownloadUrl(@QueryParameter("detectDownloadUrl") final String detectDownloadUrl) {
        try {
            final DetectVersionRequestService detectVersionRequestService = getDetectVersionRequestService();
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

    private DetectVersionRequestService getDetectVersionRequestService() {
        return new DetectVersionRequestService(new PrintStreamIntLogger(System.out, LogLevel.DEBUG), isTrustSSLCertificates(), getHubTimeout());
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
     */
    public FormValidation doCheckHubUrl(@QueryParameter("hubUrl") final String hubUrl, @QueryParameter("trustSSLCertificates") final boolean trustSSLCertificates) {
        if (StringUtils.isBlank(hubUrl)) {
            return FormValidation.ok();
        }

        final HubServerConfigValidator validator = new HubServerConfigValidator();
        validator.setHubUrl(hubUrl);
        validator.setAlwaysTrustServerCertificate(trustSSLCertificates);
        final JenkinsProxyHelper jenkinsProxyHelper = getJenkinsProxyHelper();
        final ProxyInfo proxyInfo = jenkinsProxyHelper.getProxyInfoFromJenkins(hubUrl);
        if (ProxyInfo.NO_PROXY_INFO != proxyInfo) {
            validator.setProxyHost(proxyInfo.getHost());
            validator.setProxyPort(proxyInfo.getPort());
            validator.setProxyUsername(proxyInfo.getUsername());
            validator.setProxyPassword(proxyInfo.getEncryptedPassword());
            validator.setProxyPasswordLength(proxyInfo.getActualPasswordLength());
            validator.setProxyNtlmDomain(proxyInfo.getNtlmDomain());
            validator.setProxyNtlmWorkstation(proxyInfo.getNtlmWorkstation());

            // Must call assertProxyValid to complete setup of proxyInfo on this object
            validator.assertProxyValid();
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
            final CredentialsMatcher credentialsMatcher = CredentialsMatchers.anyOf(CredentialsMatchers.instanceOf(StandardUsernamePasswordCredentials.class), CredentialsMatchers.instanceOf(StringCredentials.class));
            // Dont want to limit the search to a particular project for the drop down menu
            final AbstractProject<?, ?> project = null;
            boxModel = new StandardListBoxModel().withEmptySelection().withMatching(credentialsMatcher, CredentialsProvider.lookupCredentials(BaseStandardCredentials.class, project, ACL.SYSTEM, Collections.<DomainRequirement>emptyList()));
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
            String hubApiToken = null;
            if (StringUtils.isNotBlank(hubCredentialsId)) {
                BaseStandardCredentials credential = null;
                if (StringUtils.isNotBlank(hubCredentialsId)) {
                    final AbstractProject<?, ?> project = null;
                    final List<BaseStandardCredentials> credentials = CredentialsProvider.lookupCredentials(BaseStandardCredentials.class, project, ACL.SYSTEM, Collections.<DomainRequirement>emptyList());
                    final IdMatcher matcher = new IdMatcher(hubCredentialsId);
                    for (final BaseStandardCredentials c : credentials) {
                        if (matcher.matches(c)) {
                            credential = c;
                        }
                    }
                }
                if (credential == null) {
                    return FormValidation.error(Messages.DetectPostBuildStep_getPleaseSetHubCredentials());
                } else if (credential instanceof UsernamePasswordCredentialsImpl) {
                    final UsernamePasswordCredentialsImpl creds = (UsernamePasswordCredentialsImpl) credential;
                    credentialUserName = creds.getUsername();
                    credentialPassword = creds.getPassword().getPlainText();
                } else if (credential instanceof StringCredentialsImpl) {
                    final StringCredentialsImpl creds = (StringCredentialsImpl) credential;
                    hubApiToken = creds.getSecret().getPlainText();
                }
            }

            final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
            hubServerConfigBuilder.setHubUrl(hubUrl);
            hubServerConfigBuilder.setUsername(credentialUserName);
            hubServerConfigBuilder.setPassword(credentialPassword);
            hubServerConfigBuilder.setApiToken(hubApiToken);
            hubServerConfigBuilder.setTimeout(hubTimeout);
            hubServerConfigBuilder.setAlwaysTrustServerCertificate(trustSSLCertificates);
            final JenkinsProxyHelper jenkinsProxyHelper = getJenkinsProxyHelper();
            final ProxyInfo proxyInfo = jenkinsProxyHelper.getProxyInfoFromJenkins(hubUrl);
            if (ProxyInfo.NO_PROXY_INFO != proxyInfo) {
                hubServerConfigBuilder.setProxyHost(proxyInfo.getHost());
                hubServerConfigBuilder.setProxyPort(proxyInfo.getPort());
                hubServerConfigBuilder.setProxyUsername(proxyInfo.getUsername());
                hubServerConfigBuilder.setProxyPassword(proxyInfo.getEncryptedPassword());
                hubServerConfigBuilder.setProxyPasswordLength(proxyInfo.getActualPasswordLength());
                hubServerConfigBuilder.setProxyNtlmDomain(proxyInfo.getNtlmDomain());
                hubServerConfigBuilder.setProxyNtlmWorkstation(proxyInfo.getNtlmWorkstation());
            }

            final HubServerConfig hubServerConfig = hubServerConfigBuilder.build();

            final RestConnection connection = hubServerConfig.createRestConnection(new PrintStreamIntLogger(System.out, LogLevel.DEBUG));
            connection.connect();
            return FormValidation.ok(Messages.DetectPostBuildStep_getCredentialsValidFor_0_(hubUrl));

        } catch (final IllegalStateException e) {
            return FormValidation.error(e.getMessage());
        } catch (final HubIntegrationException e) {
            final String message;
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
        detectArtifactUrl = formData.getString("detectArtifactUrl");
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

    // EX: http://localhost:8080/descriptorByName/com.blackducksoftware.integration.detect.jenkins.post.DetectPostBuildStep/config.xml
    @WebMethod(name = "config.xml")
    public void doConfigDotXml(final StaplerRequest req, final StaplerResponse rsp) throws IOException, ServletException, ParserConfigurationException {
        final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        boolean changed = false;
        try {
            if (DetectPostBuildStepDescriptor.class.getClassLoader() != originalClassLoader) {
                changed = true;
                Thread.currentThread().setContextClassLoader(DetectPostBuildStepDescriptor.class.getClassLoader());
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

        final String hubUrl = getNodeValue(doc, "hubUrl", null);
        final String hubCredentialsId = getNodeValue(doc, "hubCredentialsId", null);
        final String hubTimeout = getNodeValue(doc, "hubTimeout", "120");
        final String trustSSLCertificatesString = getNodeValue(doc, "trustSSLCertificates", "false");
        final String detectArtifactUrl = getNodeValue(doc, "detectArtifactUrl", null);
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
        HubServerInfoSingleton.getInstance().setDetectArtifactUrl(detectArtifactUrl);
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

    public JenkinsProxyHelper getJenkinsProxyHelper() {
        return new JenkinsProxyHelper();
    }

}
