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
package com.synopsys.integration.jenkins.detect.service;

import java.io.IOException;
import java.util.Optional;

import com.synopsys.integration.function.ThrowingSupplier;
import com.synopsys.integration.jenkins.detect.DetectFreestyleCommands;
import com.synopsys.integration.jenkins.detect.DetectPipelineCommands;
import com.synopsys.integration.jenkins.detect.DetectRunner;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectDownloadStrategyService;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectExecutionStrategyFactory;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsBuildService;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.jenkins.service.JenkinsServicesFactory;
import com.synopsys.integration.jenkins.wrapper.JenkinsWrapper;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.WorkspaceList;

public class DetectCommandsFactory {
    private final JenkinsWrapper jenkinsWrapper;
    private final TaskListener listener;
    private final EnvVars envVars;
    private final ThrowingSupplier<FilePath, AbortException> validatedWorkspace;

    private DetectCommandsFactory(JenkinsWrapper jenkinsWrapper, TaskListener listener, EnvVars envVars, FilePath workspace) {
        this.jenkinsWrapper = jenkinsWrapper;
        this.listener = listener;
        this.envVars = envVars;
        this.validatedWorkspace = () -> Optional.ofNullable(workspace).orElseThrow(() -> new AbortException("Detect cannot be executed when the workspace is null"));
    }

    public static DetectFreestyleCommands fromPostBuild(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        DetectCommandsFactory detectCommandsFactory = new DetectCommandsFactory(JenkinsWrapper.initializeFromJenkinsJVM(), listener, build.getEnvironment(listener), build.getWorkspace());

        JenkinsServicesFactory jenkinsServicesFactory = new JenkinsServicesFactory(detectCommandsFactory.getLogger(), build, build.getEnvironment(listener), launcher, listener, build.getBuiltOn(), build.getWorkspace());
        JenkinsBuildService jenkinsBuildService = jenkinsServicesFactory.createJenkinsBuildService();
        JenkinsConfigService jenkinsConfigService = jenkinsServicesFactory.createJenkinsConfigService();
        JenkinsRemotingService jenkinsRemotingService = jenkinsServicesFactory.createJenkinsRemotingService();

        return new DetectFreestyleCommands(jenkinsBuildService, detectCommandsFactory.createDetectRunner(jenkinsConfigService, jenkinsRemotingService));
    }

    public static DetectPipelineCommands fromPipeline(TaskListener listener, EnvVars envVars, Launcher launcher, Node node, FilePath workspace) throws AbortException {
        DetectCommandsFactory detectCommandsFactory = new DetectCommandsFactory(JenkinsWrapper.initializeFromJenkinsJVM(), listener, envVars, workspace);

        JenkinsServicesFactory jenkinsServicesFactory = new JenkinsServicesFactory(detectCommandsFactory.getLogger(), null, envVars, launcher, listener, node, workspace);
        JenkinsConfigService jenkinsConfigService = jenkinsServicesFactory.createJenkinsConfigService();
        JenkinsRemotingService jenkinsRemotingService = jenkinsServicesFactory.createJenkinsRemotingService();

        return new DetectPipelineCommands(detectCommandsFactory.createDetectRunner(jenkinsConfigService, jenkinsRemotingService), detectCommandsFactory.getLogger());
    }

    private DetectRunner createDetectRunner(JenkinsConfigService jenkinsConfigService, JenkinsRemotingService jenkinsRemotingService) throws AbortException {
        return new DetectRunner(createDetectEnvironmentService(jenkinsConfigService), createDetectStrategyService(jenkinsConfigService), createDetectExecutionStrategyFactory(jenkinsConfigService, jenkinsRemotingService));
    }

    private DetectArgumentService createDetectArgumentService() {
        return new DetectArgumentService(getLogger(), jenkinsWrapper.getVersionHelper());
    }

    private DetectEnvironmentService createDetectEnvironmentService(JenkinsConfigService jenkinsConfigService) {
        return new DetectEnvironmentService(getLogger(), jenkinsWrapper.getProxyHelper(), jenkinsWrapper.getVersionHelper(), jenkinsWrapper.getCredentialsHelper(), jenkinsConfigService, envVars);
    }

    private DetectDownloadStrategyService createDetectStrategyService(JenkinsConfigService jenkinsConfigService) {
        return new DetectDownloadStrategyService(getLogger(), jenkinsConfigService);
    }

    private DetectExecutionStrategyFactory createDetectExecutionStrategyFactory(JenkinsConfigService jenkinsConfigService, JenkinsRemotingService jenkinsRemotingService) throws AbortException {
        FilePath workspace = validatedWorkspace.get();
        FilePath workspaceTempDir = WorkspaceList.tempDir(workspace);

        return new DetectExecutionStrategyFactory(getLogger(), jenkinsWrapper.getProxyHelper(), workspaceTempDir.getRemote(), jenkinsConfigService, jenkinsRemotingService, createDetectArgumentService());
    }

    private JenkinsIntLogger getLogger() {
        return new JenkinsIntLogger(listener);
    }
}
