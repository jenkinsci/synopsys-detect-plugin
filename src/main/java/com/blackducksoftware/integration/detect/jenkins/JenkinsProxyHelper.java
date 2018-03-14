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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.hub.proxy.ProxyInfo;
import com.blackducksoftware.integration.hub.proxy.ProxyInfoBuilder;
import com.blackducksoftware.integration.util.proxy.ProxyUtil;
import com.google.common.collect.Lists;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;

public class JenkinsProxyHelper {

    public static ProxyInfo getProxyInfo() {
        ProxyInfo proxyInfo = ProxyInfo.NO_PROXY_INFO;
        final ProxyInfoBuilder proxyInfoBuilder = new ProxyInfoBuilder();
        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            final ProxyConfiguration proxyConfig = jenkins.proxy;
            if (proxyConfig != null) {
                applyJenkinsProxy(proxyConfig, proxyInfoBuilder);
                proxyInfo = proxyInfoBuilder.build();
            }
        }
        return proxyInfo;
    }

    public static boolean shouldUseProxy(final ProxyInfo proxyInfo, final String url) {
        if (StringUtils.isBlank(url) || null == proxyInfo || ProxyInfo.NO_PROXY_INFO == proxyInfo) {
            return false;
        }
        try {
            final URL actualURL = new URL(url);
            String noProxyHosts = null;
            final Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null) {
                final ProxyConfiguration proxyConfig = jenkins.proxy;
                if (proxyConfig != null) {
                    noProxyHosts = proxyConfig.noProxyHost;
                }
            }
            if (StringUtils.isBlank(noProxyHosts)) {
                return true;
            }
            final List<Pattern> noProxyHostPatterns = getNoProxyHostPatterns(noProxyHosts);
            return !ProxyUtil.shouldIgnoreHost(actualURL.getHost(), noProxyHostPatterns);
        } catch (final MalformedURLException e) {
            return false;
        }
    }

    private static List<Pattern> getNoProxyHostPatterns(final String noProxyHosts) {
        final List<Pattern> noProxyHostPatterns = Lists.newArrayList();
        for (final String currentNoProxyHost : noProxyHosts.split("[ \t\n,|]+")) {
            if (currentNoProxyHost.length() == 0) {
                continue;
            }
            noProxyHostPatterns.add(Pattern.compile(currentNoProxyHost.replace(".", "\\.").replace("*", ".*")));
        }
        return noProxyHostPatterns;
    }

    private static void applyJenkinsProxy(final ProxyConfiguration proxyConfig, final ProxyInfoBuilder proxyInfoBuilder) {
        if (StringUtils.isNotBlank(proxyConfig.name) && proxyConfig.port >= 0) {
            proxyInfoBuilder.setHost(proxyConfig.name);
            proxyInfoBuilder.setPort(proxyConfig.port);
            applyJenkinsProxyCredentials(proxyConfig, proxyInfoBuilder);
        }
    }

    private static void applyJenkinsProxyCredentials(final ProxyConfiguration proxyConfig, final ProxyInfoBuilder proxyInfoBuilder) {
        if (StringUtils.isNotBlank(proxyConfig.getUserName()) && StringUtils.isNotBlank(proxyConfig.getPassword())) {
            if (proxyConfig.getUserName().indexOf('\\') >= 0) {
                final String domain = proxyConfig.getUserName().substring(0, proxyConfig.getUserName().indexOf('\\'));
                final String user = proxyConfig.getUserName().substring(proxyConfig.getUserName().indexOf('\\') + 1);
                proxyInfoBuilder.setNtlmDomain(domain);
                proxyInfoBuilder.setUsername(user);
                proxyInfoBuilder.setPassword(proxyConfig.getPassword());
            } else {
                proxyInfoBuilder.setUsername(proxyConfig.getUserName());
                proxyInfoBuilder.setPassword(proxyConfig.getPassword());
            }
        }
    }

}
