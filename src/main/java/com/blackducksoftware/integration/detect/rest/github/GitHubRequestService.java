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
package com.blackducksoftware.integration.detect.rest.github;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;

import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.rest.UnauthenticatedRestConnection;
import com.blackducksoftware.integration.log.LogLevel;
import com.blackducksoftware.integration.log.PrintStreamIntLogger;
import com.google.gson.Gson;

import hudson.ProxyConfiguration;
import jenkins.model.Jenkins;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GitHubRequestService {
    private final Gson gson = new Gson();

    public List<GitHubFileModel> getContents(final String username, final String repository, final String regex) throws IOException, IntegrationException {
        final String contentsUrl = "https://api.github.com/repos/" + username + "/" + repository + "/contents/?ref=gh-pages";
        final URL contentsURL = new URL(contentsUrl);
        final RestConnection restConnection = new UnauthenticatedRestConnection(new PrintStreamIntLogger(System.out, LogLevel.DEBUG), contentsURL, 30);
        setProxyInformation(restConnection);
        final HttpUrl contentHttpUrl = restConnection.createHttpUrl();
        final Request request = restConnection.createGetRequest(contentHttpUrl);
        final Response response = restConnection.handleExecuteClientCall(request);

        final GitHubFileModel[] gitHubFileModels = gson.fromJson(response.body().string(), GitHubFileModel[].class);
        final List<GitHubFileModel> gitHubFileModelList = new ArrayList<>();
        for (final GitHubFileModel gitHubFileModel : gitHubFileModels) {
            if (gitHubFileModel.name.matches(regex)) {
                gitHubFileModelList.add(gitHubFileModel);
            }
        }
        return gitHubFileModelList;
    }

    public File downloadFile(final String url, final File file) throws IntegrationException, IOException {
        final URL contentsURL = new URL(url);
        final RestConnection restConnection = new UnauthenticatedRestConnection(new PrintStreamIntLogger(System.out, LogLevel.DEBUG), contentsURL, 30);
        setProxyInformation(restConnection);
        final HttpUrl contentHttpUrl = restConnection.createHttpUrl();
        final Request request = restConnection.createGetRequest(contentHttpUrl);
        final Response response = restConnection.handleExecuteClientCall(request);
        final ResponseBody responseBody = response.body();
        final InputStream inputStream = responseBody.byteStream();
        final FileOutputStream fileOutputStream = new FileOutputStream(file);
        IOUtils.copy(inputStream, fileOutputStream);
        return file;
    }

    private void setProxyInformation(final RestConnection restConnection) {
        ProxyConfiguration proxyConfig = null;
        final Jenkins jenkins = Jenkins.getInstance();
        if (jenkins != null) {
            proxyConfig = jenkins.proxy;
        }
        if (proxyConfig != null) {
            restConnection.proxyHost = proxyConfig.name;
            restConnection.proxyPort = proxyConfig.port;
            restConnection.proxyNoHosts = proxyConfig.noProxyHost;
            restConnection.proxyUsername = proxyConfig.getUserName();
            restConnection.proxyPassword = proxyConfig.getPassword();
        }
    }

}
