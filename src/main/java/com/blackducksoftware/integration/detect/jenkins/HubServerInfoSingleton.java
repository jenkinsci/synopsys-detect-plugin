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
package com.blackducksoftware.integration.detect.jenkins;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;

import hudson.model.AbstractProject;
import hudson.security.ACL;

public class HubServerInfoSingleton {
    private final static HubServerInfoSingleton HUB_SERVER_INFO_SINGLETON;

    static // static constructor
    {
        // instantiate the singleton at class loading time.
        HUB_SERVER_INFO_SINGLETON = new HubServerInfoSingleton();
    }

    private String hubUrl;
    private String hubCredentialsId;
    private int hubTimeout;
    private boolean trustSSLCertificates;
    private String detectDownloadUrl;

    private HubServerInfoSingleton() {
    }

    public static HubServerInfoSingleton getInstance() {
        return HUB_SERVER_INFO_SINGLETON;
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

    public String getDetectDownloadUrl() {
        return detectDownloadUrl;
    }

    public void setDetectDownloadUrl(final String detectDownloadUrl) {
        this.detectDownloadUrl = detectDownloadUrl;
    }

    public String getHubUsername() {
        final UsernamePasswordCredentialsImpl creds = getCredential();
        if (creds == null) {
            return null;
        } else {
            return creds.getUsername();
        }
    }

    public String getHubPassword() {
        final UsernamePasswordCredentialsImpl creds = getCredential();
        if (creds == null) {
            return null;
        } else {
            return creds.getPassword().getPlainText();
        }
    }

    public UsernamePasswordCredentialsImpl getCredential() {
        UsernamePasswordCredentialsImpl credential = null;
        if (StringUtils.isNotBlank(hubCredentialsId)) {
            final AbstractProject<?, ?> project = null;
            final List<StandardUsernamePasswordCredentials> credentials = CredentialsProvider.lookupCredentials(StandardUsernamePasswordCredentials.class, project, ACL.SYSTEM, Collections.<DomainRequirement> emptyList());
            final IdMatcher matcher = new IdMatcher(hubCredentialsId);
            for (final StandardCredentials c : credentials) {
                if (matcher.matches(c) && c instanceof UsernamePasswordCredentialsImpl) {
                    credential = (UsernamePasswordCredentialsImpl) c;
                }
            }
        }
        return credential;
    }

}
