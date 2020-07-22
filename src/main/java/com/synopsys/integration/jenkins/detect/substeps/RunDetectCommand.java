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
import com.synopsys.integration.util.IntEnvironmentVariables;

public class RunDetectCommand {
    private final DetectEnvironmentService detectEnvironmentService;
    private final DetectWorkspaceService detectWorkspaceService;
    private final DetectArgumentService detectArgumentService;
    private final JenkinsLauncherService launchService;
    private final String detectArgumentString;

    public RunDetectCommand(DetectEnvironmentService detectEnvironmentService, DetectWorkspaceService detectWorkspaceService, DetectArgumentService detectArgumentService, JenkinsLauncherService launchService,
        String detectArgumentString) {
        this.detectEnvironmentService = detectEnvironmentService;
        this.detectWorkspaceService = detectWorkspaceService;
        this.detectArgumentService = detectArgumentService;
        this.launchService = launchService;
        this.detectArgumentString = detectArgumentString;
    }

    public int execute() throws IOException, InterruptedException, IntegrationException {
        IntEnvironmentVariables intEnvironmentVariables = detectEnvironmentService.createDetectEnvironment();
        DetectSetupResponse detectSetupResponse = detectWorkspaceService.setUpDetectWorkspace(intEnvironmentVariables);
        List<String> detectCmds = detectArgumentService.parseDetectArguments(intEnvironmentVariables, detectSetupResponse, detectArgumentString);

        return launchService.executeWithEnvironment(intEnvironmentVariables, detectCmds);
    }
}
