/**
 * Black Duck Detect Plugin
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
package com.blackducksoftware.integration.detect.jenkins;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import com.blackducksoftware.integration.util.proxy.ProxyUtil;
import com.google.common.collect.Lists;

public class JenkinsProxyHelper {

    public static boolean shouldUseProxy(final String urlString, final String noProxyHosts) {
        if (StringUtils.isBlank(urlString)) {
            return false;
        }
        try {
            final URL url = new URL(urlString);
            return shouldUseProxy(url, noProxyHosts);
        } catch (final MalformedURLException e) {
            // Ignore these errors
        }
        return false;
    }

    public static boolean shouldUseProxy(final URL url, final String noProxyHosts) {
        if (url == null) {
            return false;
        }
        if (StringUtils.isBlank(noProxyHosts)) {
            return true;
        }
        final List<Pattern> noProxyHostPatterns = getNoProxyHostPatterns(noProxyHosts);
        return !ProxyUtil.shouldIgnoreHost(url.getHost(), noProxyHostPatterns);
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

}
