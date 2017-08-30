/**
 * Black Duck Detect Plugin for Jenkins
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
package com.blackducksoftware.integration.detect.jenkins.remote;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.blackducksoftware.integration.hub.exception.HubIntegrationException;

import hudson.remoting.Callable;

public class DetectRemoteRunner implements Callable<String, HubIntegrationException> {
    private final String hubUrl;
    private final String hubUsername;
    private final String hubPassword;
    private final int hubTimeout;
    private final boolean importSSLCerts;
    private final String detectDownloadUrl;

    private String proxyHost;
    private int proxyPort;
    private String proxyNoHost;
    private String proxyUsername;
    private String proxyPassword;

    public DetectRemoteRunner(final String hubUrl, final String hubUsername, final String hubPassword, final int hubTimeout, final boolean importSSLCerts, final String detectDownloadUrl) {
        this.hubUrl = hubUrl;
        this.hubUsername = hubUsername;
        this.hubPassword = hubPassword;
        this.hubTimeout = hubTimeout;
        this.importSSLCerts = importSSLCerts;
        this.detectDownloadUrl = detectDownloadUrl;
    }

    @Override
    public String call() throws HubIntegrationException {
        // TODO Run Hub detect
        return null;
    }

    @Override
    public void checkRoles(final RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(DetectRemoteRunner.class));
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(final String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(final int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyNoHost() {
        return proxyNoHost;
    }

    public void setProxyNoHost(final String proxyNoHost) {
        this.proxyNoHost = proxyNoHost;
    }

    public String getProxyUsername() {
        return proxyUsername;
    }

    public void setProxyUsername(final String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public String getProxyPassword() {
        return proxyPassword;
    }

    public void setProxyPassword(final String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

}
