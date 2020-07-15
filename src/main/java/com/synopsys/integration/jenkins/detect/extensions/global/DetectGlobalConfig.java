/**
 * blackduck-detect
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
import java.util.Locale;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang.StringUtils;
import org.apache.http.impl.EnglishReasonPhraseCatalog;
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
import com.synopsys.integration.jenkins.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.SynopsysCredentialsHelper;
import com.synopsys.integration.jenkins.annotations.HelpMarkdown;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.log.PrintStreamIntLogger;
import com.synopsys.integration.rest.client.ConnectionResult;
import com.synopsys.integration.rest.credentials.Credentials;
import com.synopsys.integration.rest.proxy.ProxyInfo;

import hudson.Extension;
import hudson.Functions;
import hudson.Util;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.IOUtils;
import hudson.util.ListBoxModel;
import hudson.util.Messages;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.util.xml.XMLUtils;

@Extension
public class DetectGlobalConfig extends GlobalConfiguration implements Serializable {
    private static final long serialVersionUID = -7629542889827231313L;

    @HelpMarkdown("Provide the URL that lets you access your Black Duck server.")
    private String blackDuckUrl;

    @HelpMarkdown("Choose the Username and Password from the list to authenticate to the Black Duck server.  \r\n" +
                      "Alternatively, choose the Api Token from the list to authenticate to the Black Duck server.  \r\n" +
                      "If the credentials you are looking for are not in the list then you can add them with the Add button.")
    private String blackDuckCredentialsId;

    @HelpMarkdown("If selected, Detect will automatically trust certificates when communicating with your Black Duck server.")
    private boolean trustBlackDuckCertificates;

    private int blackDuckTimeout = 120;

    @DataBoundConstructor
    public DetectGlobalConfig() {
        load();
    }

    public String getBlackDuckUrl() {
        return blackDuckUrl;
    }

    @DataBoundSetter
    public void setBlackDuckUrl(String blackDuckUrl) {
        this.blackDuckUrl = blackDuckUrl;
        save();
    }

    public int getBlackDuckTimeout() {
        return blackDuckTimeout;
    }

    @DataBoundSetter
    public void setBlackDuckTimeout(int blackDuckTimeout) {
        this.blackDuckTimeout = blackDuckTimeout;
        save();
    }

    public String getBlackDuckCredentialsId() {
        return blackDuckCredentialsId;
    }

    @DataBoundSetter
    public void setBlackDuckCredentialsId(String blackDuckCredentialsId) {
        this.blackDuckCredentialsId = blackDuckCredentialsId;
        save();
    }

    public boolean getTrustBlackDuckCertificates() {
        return trustBlackDuckCertificates;
    }

    @DataBoundSetter
    public void setTrustBlackDuckCertificates(boolean trustBlackDuckCertificates) {
        this.trustBlackDuckCertificates = trustBlackDuckCertificates;
        save();
    }

    public BlackDuckServerConfig getBlackDuckServerConfig(JenkinsProxyHelper jenkinsProxyHelper, SynopsysCredentialsHelper synopsysCredentialsHelper) throws IllegalArgumentException {
        return getBlackDuckServerConfigBuilder(jenkinsProxyHelper, synopsysCredentialsHelper).build();
    }

    public BlackDuckServerConfigBuilder getBlackDuckServerConfigBuilder(JenkinsProxyHelper jenkinsProxyHelper, SynopsysCredentialsHelper synopsysCredentialsHelper) throws IllegalArgumentException {
        return createBlackDuckServerConfigBuilder(jenkinsProxyHelper, synopsysCredentialsHelper, blackDuckUrl, blackDuckCredentialsId, blackDuckTimeout, trustBlackDuckCertificates);
    }

    public ListBoxModel doFillBlackDuckCredentialsIdItems() {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return new StandardListBoxModel().includeEmptyValue();
        }
        jenkins.checkPermission(Jenkins.ADMINISTER);
        return new StandardListBoxModel()
                   .includeEmptyValue()
                   .includeMatchingAs(ACL.SYSTEM, jenkins, BaseStandardCredentials.class, Collections.emptyList(), SynopsysCredentialsHelper.API_TOKEN_OR_USERNAME_PASSWORD_CREDENTIALS);
    }

    @POST
    public FormValidation doTestBlackDuckConnection(@QueryParameter("blackDuckUrl") String blackDuckUrl, @QueryParameter("blackDuckCredentialsId") String blackDuckCredentialsId, @QueryParameter("blackDuckTimeout") String blackDuckTimeout,
        @QueryParameter("trustBlackDuckCertificates") boolean trustBlackDuckCertificates) {
        Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins == null) {
            return FormValidation.warning(
                "Connection validation could not be completed: Validation couldn't retrieve the instance of Jenkins from the JVM. This may happen if Jenkins is still starting up or if this code is running on a different JVM than your Jenkins server.");
        }
        jenkins.checkPermission(Jenkins.ADMINISTER);

        SynopsysCredentialsHelper synopsysCredentialsHelper = new SynopsysCredentialsHelper(jenkins);
        JenkinsProxyHelper jenkinsProxyHelper = JenkinsProxyHelper.fromJenkins(jenkins);

        try {
            BlackDuckServerConfig blackDuckServerConfig = createBlackDuckServerConfigBuilder(jenkinsProxyHelper, synopsysCredentialsHelper, blackDuckUrl, blackDuckCredentialsId, Integer.parseInt(blackDuckTimeout),
                trustBlackDuckCertificates).build();
            ConnectionResult connectionResult = blackDuckServerConfig.createBlackDuckHttpClient(new PrintStreamIntLogger(System.out, LogLevel.DEBUG)).attemptConnection();
            if (connectionResult.isFailure()) {
                int statusCode = connectionResult.getHttpStatusCode();
                String statusPhrase = EnglishReasonPhraseCatalog.INSTANCE.getReason(statusCode, Locale.ENGLISH);

                // This is how Jenkins constructs an error with an exception stack trace, we're using it here because often a status code and phrase are not enough, but also (especially with proxies) the failure message can be too much.
                String moreDetailsHtml = connectionResult.getFailureMessage()
                                             .map(Util::escape)
                                             .map(msg -> String.format("<a href='#' class='showDetails'>%s</a><pre style='display:none'>%s</pre>", Messages.FormValidation_Error_Details(), msg))
                                             .orElse(StringUtils.EMPTY);

                return FormValidation.errorWithMarkup(String.format("ERROR: Connection attempt returned %s %s %s", statusCode, statusPhrase, moreDetailsHtml));
            }
        } catch (IllegalArgumentException e) {
            return FormValidation.error(e.getMessage());
        }

        return FormValidation.ok("Connection successful.");
    }

    // EX: http://localhost:8080/descriptorByName/com.synopsys.integration.jenkins.detect.extensions.global.DetectGlobalConfig/config.xml
    @WebMethod(name = "config.xml")
    public void doConfigDotXml(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException, ParserConfigurationException {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
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

    private void updateByXml(Source source) throws IOException, ParserConfigurationException {
        Document doc;
        try (StringWriter out = new StringWriter()) {
            // this allows us to use UTF-8 for storing data,
            // plus it checks any well-formedness issue in the submitted
            // data
            XMLUtils.safeTransform(source, new StreamResult(out));

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(out.toString()));

            doc = builder.parse(is);
        } catch (TransformerException | SAXException e) {
            throw new IOException("Failed to persist configuration.xml", e);
        }

        String blackDuckUrl = getNodeValue(doc, "blackDuckUrl").orElse(StringUtils.EMPTY);
        String blackDuckCredentialsId = getNodeValue(doc, "blackDuckCredentialsId").orElse(StringUtils.EMPTY);
        int blackDuckTimeout = getNodeIntegerValue(doc, "blackDuckTimeout").orElse(120);
        boolean trustBlackDuckCertificates = getNodeBooleanValue(doc, "trustBlackDuckCertificates").orElse(false);

        setBlackDuckUrl(blackDuckUrl);
        setBlackDuckCredentialsId(blackDuckCredentialsId);
        setBlackDuckTimeout(blackDuckTimeout);
        setTrustBlackDuckCertificates(trustBlackDuckCertificates);
        save();
    }

    private Optional<String> getNodeValue(Document doc, String tagName) {
        return Optional.ofNullable(doc.getElementsByTagName(tagName).item(0))
                   .map(Node::getFirstChild)
                   .map(Node::getNodeValue)
                   .map(String::trim);
    }

    private Optional<Integer> getNodeIntegerValue(Document doc, String tagName) {
        try {
            return getNodeValue(doc, tagName).map(Integer::valueOf);
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    private Optional<Boolean> getNodeBooleanValue(Document doc, String tagName) {
        return getNodeValue(doc, tagName).map(Boolean::valueOf);
    }

    private BlackDuckServerConfigBuilder createBlackDuckServerConfigBuilder(JenkinsProxyHelper jenkinsProxyHelper, SynopsysCredentialsHelper synopsysCredentialsHelper, String blackDuckUrl, String credentialsId, int timeout,
        boolean alwaysTrust) {
        ProxyInfo proxyInfo = jenkinsProxyHelper.getProxyInfo(blackDuckUrl);

        BlackDuckServerConfigBuilder builder = BlackDuckServerConfig.newBuilder()
                                                   .setUrl(blackDuckUrl)
                                                   .setTimeoutInSeconds(timeout)
                                                   .setTrustCert(alwaysTrust)
                                                   .setProxyInfo(proxyInfo);

        Credentials usernamePasswordCredentials = synopsysCredentialsHelper.getIntegrationCredentialsById(credentialsId);
        if (!usernamePasswordCredentials.isBlank()) {
            builder.setCredentials(usernamePasswordCredentials);
        } else {
            synopsysCredentialsHelper.getApiTokenByCredentialsId(credentialsId).ifPresent(builder::setApiToken);
        }

        return builder;
    }

}
