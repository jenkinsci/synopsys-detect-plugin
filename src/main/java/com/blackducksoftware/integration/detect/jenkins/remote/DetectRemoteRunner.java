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

import com.blackducksoftware.integration.detect.jenkins.tools.DetectDownloadManager;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.log.IntLogger;

import hudson.remoting.Callable;

public class DetectRemoteRunner implements Callable<String, IntegrationException> {
    private final IntLogger logger;
    private final String hubUrl;
    private final String hubUsername;
    private final String hubPassword;
    private final int hubTimeout;
    private final boolean importSSLCerts;
    private final String detectDownloadUrl;
    private final String toolsDirectory;

    private String proxyHost;
    private int proxyPort;
    private String proxyNoHost;
    private String proxyUsername;
    private String proxyPassword;

    public DetectRemoteRunner(final IntLogger logger, final String hubUrl, final String hubUsername, final String hubPassword, final int hubTimeout, final boolean importSSLCerts, final String detectDownloadUrl,
            final String toolsDirectory) {
        this.logger = logger;
        this.hubUrl = hubUrl;
        this.hubUsername = hubUsername;
        this.hubPassword = hubPassword;
        this.hubTimeout = hubTimeout;
        this.importSSLCerts = importSSLCerts;
        this.detectDownloadUrl = detectDownloadUrl;
        this.toolsDirectory = toolsDirectory;
    }

    @Override
    public String call() throws IntegrationException {
        // TODO Run Hub detect
        try {
            if (detectDownloadUrl != null) {
                final DetectDownloadManager detectDownloadManager = new DetectDownloadManager(logger, toolsDirectory);
                detectDownloadManager.handleDownload(detectDownloadUrl);
            }

        } catch (final Exception e) {
            throw new IntegrationException(e);
        }
        return null;
    }

    @Override
    public void checkRoles(final RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(DetectRemoteRunner.class));
    }

    public void setProxyHost(final String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(final int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public void setProxyNoHost(final String proxyNoHost) {
        this.proxyNoHost = proxyNoHost;
    }

    public void setProxyUsername(final String proxyUsername) {
        this.proxyUsername = proxyUsername;
    }

    public void setProxyPassword(final String proxyPassword) {
        this.proxyPassword = proxyPassword;
    }

}
