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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class GitHubPagesContentRequestService {
    public List<GitHubFileModel> getContents(final String username, final String repository, final String regex) {
        try {
            final OkHttpClient client = new OkHttpClient();
            final Gson gson = new Gson();
            final String contentsUrl = "https://api.github.com/repos/"+username+"/"+repository+"/contents/?ref=gh-pages";
            final Request request = new Request.Builder().url(contentsUrl).build();
            final Response response = client.newCall(request).execute();
            final GitHubFileModel[] gitHubFileModels = gson.fromJson(response.body().string(), GitHubFileModel[].class);
            final List<GitHubFileModel> gitHubFileModelList = new ArrayList<>();
            for (final GitHubFileModel gitHubFileModel : gitHubFileModels) {
                if(gitHubFileModel.name.matches(regex)) {
                    gitHubFileModelList.add(gitHubFileModel);
                }
            }
            return gitHubFileModelList;
        } catch (final Exception e) {
            return null;
        }
    }

}
