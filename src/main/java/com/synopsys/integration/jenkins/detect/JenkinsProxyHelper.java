/**
 * blackduck-detect
 *
 * Copyright (c) 2019 Synopsys, Inc.
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.rest.credentials.Credentials;
import com.synopsys.integration.rest.credentials.CredentialsBuilder;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.rest.proxy.ProxyInfoBuilder;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

public class JenkinsProxyHelper {
    public static ProxyInfo getProxyInfoFromJenkins(final String url) {
        final ProxyConfiguration proxyConfig = Optional.ofNullable(Jenkins.getInstanceOrNull()).map(jenkins -> jenkins.proxy).orElse(null);
        if (proxyConfig == null) {
            return ProxyInfo.NO_PROXY_INFO;
        }

        String username = null;
        String ntlmDomain = null;
        if (StringUtils.isNotBlank(proxyConfig.getUserName())) {
            final String[] possiblyDomainSlashUsername = proxyConfig.getUserName().split(Pattern.quote("\\"));
            if (possiblyDomainSlashUsername.length == 1 || possiblyDomainSlashUsername[0].length() == 0) {
                ntlmDomain = null;
                username = proxyConfig.getUserName();
            } else {
                ntlmDomain = possiblyDomainSlashUsername[0];
                username = possiblyDomainSlashUsername[1];
            }
        }

        return getProxyInfo(url, proxyConfig.name, proxyConfig.port, username, proxyConfig.getPassword(), proxyConfig.getNoProxyHostPatterns(), ntlmDomain, StringUtils.EMPTY);
    }

    public static ProxyInfo getProxyInfo(final String url, final String proxyHost, final int proxyPort, final String proxyUsername, final String proxyPassword, final List<Pattern> ignoredProxyHosts, final String ntlmDomain,
        final String ntlmWorkstation) {
        ProxyInfo proxyInfo = ProxyInfo.NO_PROXY_INFO;

        if (shouldUseProxy(url, ignoredProxyHosts)) {
            final ProxyInfoBuilder proxyInfoBuilder = ProxyInfo.newBuilder();

            final CredentialsBuilder credentialsBuilder = Credentials.newBuilder();
            credentialsBuilder.setUsernameAndPassword(proxyUsername, proxyPassword);

            proxyInfoBuilder.setHost(proxyHost);
            proxyInfoBuilder.setPort(proxyPort);
            proxyInfoBuilder.setCredentials(credentialsBuilder.build());
            proxyInfoBuilder.setNtlmDomain(StringUtils.trimToNull(ntlmDomain));
            proxyInfoBuilder.setNtlmWorkstation(StringUtils.trimToNull(ntlmWorkstation));

            proxyInfo = proxyInfoBuilder.build();

        }

        return proxyInfo;
    }

    private static boolean shouldUseProxy(final String url, final List<Pattern> noProxyHosts) {
        try {
            final URL actualURL = new URL(url);
            return !shouldIgnoreHost(actualURL.getHost(), noProxyHosts);
        } catch (final MalformedURLException e) {
            return false;
        }
    }

    private static boolean shouldIgnoreHost(final String hostToMatch, final List<Pattern> ignoredProxyHostPatterns) {
        if (StringUtils.isBlank(hostToMatch) || ignoredProxyHostPatterns == null || ignoredProxyHostPatterns.isEmpty()) {
            return false;
        }

        for (final Pattern ignoredProxyHostPattern : ignoredProxyHostPatterns) {
            final Matcher m = ignoredProxyHostPattern.matcher(hostToMatch);
            if (m.matches()) {
                return true;
            }
        }
        return false;
    }

}
