/**
 * blackduck-detect
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.jenkins.detect.services;

import java.io.IOException;

import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.services.JenkinsBuildService;
import com.synopsys.integration.jenkins.services.JenkinsRemotingService;
import com.synopsys.integration.jenkins.wrapper.JenkinsWrapper;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.TaskListener;
import hudson.slaves.WorkspaceList;

public class DetectServicesFactory {
    private final JenkinsWrapper jenkinsWrapper;
    private final TaskListener listener;
    private final EnvVars envVars;
    private final Launcher launcher;
    private final FilePath workspace;
    private final AbstractBuild<?, ?> build;

    // TODO: Validate the pieces that we need to validate
    private DetectServicesFactory(JenkinsWrapper jenkinsWrapper, TaskListener listener, EnvVars envVars, Launcher launcher, FilePath workspace, AbstractBuild<?, ?> build) {
        this.jenkinsWrapper = jenkinsWrapper;
        this.listener = listener;
        this.envVars = envVars;
        this.launcher = launcher;
        this.workspace = workspace;
        this.build = build;
    }

    public static DetectServicesFactory fromPostBuild(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        return new DetectServicesFactory(JenkinsWrapper.initializeFromJenkinsJVM(), listener, build.getEnvironment(listener), launcher, build.getWorkspace(), build);
    }

    public static DetectServicesFactory fromPipeline(JenkinsWrapper jenkinsWrapper, TaskListener listener, EnvVars envVars, Launcher launcher, FilePath workspace) {
        return new DetectServicesFactory(jenkinsWrapper, listener, envVars, launcher, workspace, null);
    }

    public DetectArgumentService createDetectArgumentService() {
        return new DetectArgumentService(getLogger(), jenkinsWrapper.getVersionHelper());
    }

    public DetectEnvironmentService createDetectEnvironmentService() {
        return new DetectEnvironmentService(getLogger(), jenkinsWrapper.getProxyHelper(), jenkinsWrapper.getVersionHelper(), jenkinsWrapper.getCredentialsHelper(), envVars);
    }

    public DetectWorkspaceService createDetectWorkspaceService() {
        return new DetectWorkspaceService(getLogger(), jenkinsWrapper.getProxyHelper(), createJenkinsRemotingService(), WorkspaceList.tempDir(workspace).getRemote());
    }

    public JenkinsRemotingService createJenkinsRemotingService() {
        return new JenkinsRemotingService(launcher, workspace, listener);
    }

    public JenkinsBuildService createJenkinsBuildService() {
        return new JenkinsBuildService(getLogger(), build);
    }

    public JenkinsIntLogger getLogger() {
        return new JenkinsIntLogger(listener);
    }

}
