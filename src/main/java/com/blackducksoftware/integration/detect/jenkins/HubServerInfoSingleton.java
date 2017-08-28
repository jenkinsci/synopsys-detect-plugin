/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
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
    private boolean importSSLCerts;

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

    public boolean isImportSSLCerts() {
        return importSSLCerts;
    }

    public void setImportSSLCerts(final boolean importSSLCerts) {
        this.importSSLCerts = importSSLCerts;
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
        // Only need to look up the credential when you first run a build or if the credential that the user wants to
        // use has changed.
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
