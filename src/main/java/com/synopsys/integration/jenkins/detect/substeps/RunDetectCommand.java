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
package com.synopsys.integration.jenkins.detect.substeps;

import java.io.IOException;
import java.util.List;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.JenkinsVersionHelper;
import com.synopsys.integration.jenkins.SynopsysCredentialsHelper;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.slaves.WorkspaceList;
import jenkins.model.Jenkins;

public class RunDetectCommand {
    private final JenkinsIntLogger logger;
    private final FilePath workspace;
    private final EnvVars envVars;
    private final String remoteJavaHome;
    private final Launcher launcher;
    private final TaskListener listener;
    private final String detectProperties;

    public RunDetectCommand(JenkinsIntLogger logger, FilePath workspace, EnvVars envVars, String remoteJavaHome, Launcher launcher, TaskListener listener, String detectProperties) {
        this.logger = logger;
        this.workspace = workspace;
        this.envVars = envVars;
        this.remoteJavaHome = remoteJavaHome;
        this.launcher = launcher;
        this.listener = listener;
        this.detectProperties = detectProperties;
    }

    public int execute() throws IOException, InterruptedException, IntegrationException {
        JenkinsVersionHelper jenkinsVersionHelper = new JenkinsVersionHelper(Jenkins.getInstanceOrNull());
        JenkinsProxyHelper jenkinsProxyHelper = JenkinsProxyHelper.fromJenkins(Jenkins.getInstanceOrNull());
        SynopsysCredentialsHelper synopsysCredentialsHelper = new SynopsysCredentialsHelper(Jenkins.getInstanceOrNull());

        DetectEnvironmentService detectEnvironmentService = new DetectEnvironmentService(logger, jenkinsProxyHelper, jenkinsVersionHelper, synopsysCredentialsHelper, envVars);
        IntEnvironmentVariables intEnvironmentVariables = detectEnvironmentService.createDetectEnvironment();

        DetectWorkspaceService detectWorkspaceService = new DetectWorkspaceService(logger, launcher.getChannel(), intEnvironmentVariables, remoteJavaHome, WorkspaceList.tempDir(workspace).getRemote());
        DetectSetupResponse detectSetupResponse = detectWorkspaceService.setUpDetectWorkspace();

        DetectArgumentService detectArgumentService = new DetectArgumentService(logger, intEnvironmentVariables, jenkinsVersionHelper, detectSetupResponse, detectProperties);
        List<String> detectCmds = detectArgumentService.parseDetectArguments();

        return launcher.launch()
                   .cmds(detectCmds)
                   .envs(intEnvironmentVariables.getVariables())
                   .pwd(workspace)
                   .stdout(listener)
                   .quiet(true)
                   .join();
    }
}
