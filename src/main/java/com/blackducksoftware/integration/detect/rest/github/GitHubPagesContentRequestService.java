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
