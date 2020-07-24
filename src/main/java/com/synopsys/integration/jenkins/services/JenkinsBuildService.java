package com.synopsys.integration.jenkins.services;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;

import hudson.model.AbstractBuild;
import hudson.model.Executor;
import hudson.model.Result;

public class JenkinsBuildService {
    private final JenkinsIntLogger logger;
    private final AbstractBuild<?, ?> build;

    public JenkinsBuildService(JenkinsIntLogger logger, AbstractBuild<?, ?> build) {
        this.logger = logger;
        this.build = build;
    }

    public void markBuildFailed(String message) {
        logger.error(message);
        build.setResult(Result.FAILURE);
    }

    public void markBuildFailed(Exception e) {
        logger.error(e.getMessage(), e);
        build.setResult(Result.FAILURE);
    }

    public void markBuildFailed(IntegrationException e) {
        logger.error(e.getMessage());
        logger.debug(e.getMessage(), e);
        build.setResult(Result.FAILURE);
    }

    public void markBuildUnstable(Exception e) {
        logger.error(e.getMessage(), e);
        build.setResult(Result.UNSTABLE);
    }

    public void markBuildAborted() {
        build.setResult(Result.ABORTED);
    }

    public void markBuildInterrupted() {
        Executor executor = build.getExecutor();
        if (executor == null) {
            markBuildAborted();
        } else {
            build.setResult(executor.abortResult());
        }
    }
}
