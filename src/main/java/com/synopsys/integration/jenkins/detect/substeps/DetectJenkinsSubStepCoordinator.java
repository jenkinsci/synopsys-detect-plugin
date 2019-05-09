/**
 * blackduck-detect
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
import com.synopsys.integration.jenkins.detect.DetectJenkinsLogger;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.slaves.WorkspaceList;

public class DetectJenkinsSubStepCoordinator {
    private final DetectJenkinsLogger logger;
    private final FilePath workspace;
    private final EnvVars envVars;
    private final String remoteJavaHome;
    private final Launcher launcher;
    private final TaskListener listener;
    private final String detectProperties;

    public DetectJenkinsSubStepCoordinator(final DetectJenkinsLogger logger, final FilePath workspace, final EnvVars envVars, final String remoteJavaHome, final Launcher launcher, final TaskListener listener,
        final String detectProperties) {
        this.logger = logger;
        this.workspace = workspace;
        this.envVars = envVars;
        this.remoteJavaHome = remoteJavaHome;
        this.launcher = launcher;
        this.listener = listener;
        this.detectProperties = detectProperties;
    }

    public int runDetect() throws IOException, InterruptedException, IntegrationException {
        final String remoteTempWorkspacePath = WorkspaceList.tempDir(workspace).getRemote();

        final CreateDetectEnvironment createDetectEnvironment = new CreateDetectEnvironment(logger, envVars);
        final IntEnvironmentVariables intEnvironmentVariables = createDetectEnvironment.createDetectEnvironment();

        final SetUpDetectWorkspaceCallable setUpDetectWorkspaceCallable = new SetUpDetectWorkspaceCallable(logger, intEnvironmentVariables.getVariables(), remoteTempWorkspacePath, remoteJavaHome);
        final DetectSetupResponse detectSetupResponse = launcher.getChannel().call(setUpDetectWorkspaceCallable);

        final ParseDetectArguments parseDetectArguments = new ParseDetectArguments(logger, intEnvironmentVariables, detectSetupResponse, detectProperties);
        final List<String> detectCmds = parseDetectArguments.parseDetectArguments();

        return launcher.launch()
                   .cmds(detectCmds)
                   .envs(intEnvironmentVariables.getVariables())
                   .pwd(workspace)
                   .stdout(listener)
                   .quiet(true)
                   .join();
    }
}
