/*
 * blackduck-detect
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.jenkins.detect.service;

import com.blackduck.integration.jenkins.detect.DetectFreestyleCommands;
import com.blackduck.integration.jenkins.detect.DetectPipelineCommands;
import com.blackduck.integration.jenkins.detect.DetectRunner;
import com.blackduck.integration.jenkins.detect.service.strategy.DetectStrategyService;
import com.blackduck.integration.jenkins.extensions.JenkinsIntLogger;
import com.blackduck.integration.jenkins.service.JenkinsBuildService;
import com.blackduck.integration.jenkins.service.JenkinsConfigService;
import com.blackduck.integration.jenkins.service.JenkinsFreestyleServicesFactory;
import com.blackduck.integration.jenkins.service.JenkinsRemotingService;
import com.blackduck.integration.jenkins.wrapper.JenkinsWrapper;
import com.blackduck.integration.util.IntEnvironmentVariables;
import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.WorkspaceList;

import java.io.IOException;

public class DetectCommandsFactory {
    public static final String NULL_WORKSPACE = "Detect cannot be executed when the workspace is null";
    private final JenkinsWrapper jenkinsWrapper;
    private final TaskListener listener;
    private final EnvVars envVars;
    private final FilePath workspace;
    private final JenkinsIntLogger jenkinsIntLogger;

    private DetectCommandsFactory(JenkinsWrapper jenkinsWrapper, TaskListener listener, EnvVars envVars, FilePath workspace) throws AbortException {
        this.jenkinsWrapper = jenkinsWrapper;
        this.listener = listener;
        this.envVars = envVars;

        if (null == workspace) {
            throw new AbortException(NULL_WORKSPACE);
        }
        this.workspace = workspace;
        this.jenkinsIntLogger = setLogger();
    }

    public static DetectFreestyleCommands fromPostBuild(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        DetectCommandsFactory detectCommandsFactory = new DetectCommandsFactory(
            JenkinsWrapper.initializeFromJenkinsJVM(),
            listener,
            build.getEnvironment(listener),
            build.getWorkspace()
        );

        JenkinsFreestyleServicesFactory jenkinsFreestyleServicesFactory = new JenkinsFreestyleServicesFactory(
            detectCommandsFactory.getLogger(),
            build,
            detectCommandsFactory.envVars,
            launcher,
            listener,
            build.getBuiltOn(),
            detectCommandsFactory.workspace
        );

        JenkinsBuildService jenkinsBuildService = jenkinsFreestyleServicesFactory.createJenkinsBuildService();
        JenkinsConfigService jenkinsConfigService = jenkinsFreestyleServicesFactory.createJenkinsConfigService();
        JenkinsRemotingService jenkinsRemotingService = jenkinsFreestyleServicesFactory.createJenkinsRemotingService();

        return new DetectFreestyleCommands(jenkinsBuildService, detectCommandsFactory.createDetectRunner(jenkinsConfigService, jenkinsRemotingService));
    }

    public static DetectPipelineCommands fromPipeline(TaskListener listener, EnvVars envVars, Launcher launcher, Node node, FilePath workspace) throws AbortException {
        DetectCommandsFactory detectCommandsFactory = new DetectCommandsFactory(JenkinsWrapper.initializeFromJenkinsJVM(), listener, envVars, workspace);

        JenkinsFreestyleServicesFactory jenkinsFreestyleServicesFactory = new JenkinsFreestyleServicesFactory(
            detectCommandsFactory.getLogger(),
            null,
            envVars,
            launcher,
            listener,
            node,
            workspace
        );
        JenkinsConfigService jenkinsConfigService = jenkinsFreestyleServicesFactory.createJenkinsConfigService();
        JenkinsRemotingService jenkinsRemotingService = jenkinsFreestyleServicesFactory.createJenkinsRemotingService();

        return new DetectPipelineCommands(detectCommandsFactory.createDetectRunner(jenkinsConfigService, jenkinsRemotingService), detectCommandsFactory.getLogger());
    }

    private DetectRunner createDetectRunner(JenkinsConfigService jenkinsConfigService, JenkinsRemotingService jenkinsRemotingService) {
        return new DetectRunner(
            createDetectEnvironmentService(jenkinsConfigService),
            jenkinsRemotingService,
            createDetectStrategyService(jenkinsConfigService),
            createDetectArgumentService(),
            getLogger()
        );
    }

    private DetectArgumentService createDetectArgumentService() {
        return new DetectArgumentService(getLogger(), jenkinsWrapper.getVersionHelper());
    }

    private DetectEnvironmentService createDetectEnvironmentService(JenkinsConfigService jenkinsConfigService) {
        return new DetectEnvironmentService(
            getLogger(),
            jenkinsWrapper.getProxyHelper(),
            jenkinsWrapper.getVersionHelper(),
            jenkinsWrapper.getCredentialsHelper(),
            jenkinsConfigService,
            envVars
        );
    }

    private DetectStrategyService createDetectStrategyService(JenkinsConfigService jenkinsConfigService) {
        FilePath workspaceTempDir = WorkspaceList.tempDir(this.workspace);

        try {
            return new DetectStrategyService(getLogger(), jenkinsWrapper.getProxyHelper(), workspaceTempDir.getRemote(), jenkinsConfigService);
        } catch (NullPointerException e) {
            throw new RuntimeException("Work Space Directory returned null.", e);
        }
    }

    private JenkinsIntLogger setLogger() {
        JenkinsIntLogger jenkinsIntLogger = JenkinsIntLogger.logToListener(listener);
        IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.empty();
        intEnvironmentVariables.putAll(envVars);
        jenkinsIntLogger.setLogLevel(intEnvironmentVariables);
        return jenkinsIntLogger;
    }

    private JenkinsIntLogger getLogger() {
        return jenkinsIntLogger;
    }
}
