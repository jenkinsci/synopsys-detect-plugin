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
package com.synopsys.integration.jenkins.detect;

import java.io.IOException;
import java.util.List;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.detect.services.DetectArgumentService;
import com.synopsys.integration.jenkins.detect.services.DetectEnvironmentService;
import com.synopsys.integration.jenkins.detect.services.DetectExecutionManager;
import com.synopsys.integration.jenkins.detect.services.DetectServicesFactory;
import com.synopsys.integration.jenkins.detect.services.DetectSetupResponse;
import com.synopsys.integration.jenkins.detect.services.DetectWorkspaceService;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.services.JenkinsBuildService;
import com.synopsys.integration.jenkins.services.JenkinsRemotingService;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.OperatingSystemType;

public class DetectCommands {
    private final DetectServicesFactory detectServicesFactory;

    public DetectCommands(DetectServicesFactory detectServicesFactory) {
        this.detectServicesFactory = detectServicesFactory;
    }

    private int runDetect(String remoteJdkHome, String detectArguments) throws IOException, InterruptedException, IntegrationException {
        DetectEnvironmentService detectEnvironmentService = detectServicesFactory.createDetectEnvironmentService();
        DetectWorkspaceService detectWorkspaceService = detectServicesFactory.createDetectWorkspaceService();
        DetectArgumentService detectArgumentService = detectServicesFactory.createDetectArgumentService();
        JenkinsRemotingService remotingService = detectServicesFactory.createJenkinsRemotingService();

        IntEnvironmentVariables intEnvironmentVariables = detectEnvironmentService.createDetectEnvironment();
        OperatingSystemType operatingSystemType = remotingService.call(OperatingSystemType::determineFromSystem);
        DetectExecutionManager detectExecutionManager = detectWorkspaceService.determineExecutionManager(intEnvironmentVariables, operatingSystemType, remoteJdkHome);
        DetectSetupResponse detectSetupResponse = remotingService.call(detectExecutionManager);
        List<String> detectCmds = detectArgumentService.parseDetectArguments(intEnvironmentVariables, detectSetupResponse, detectArguments);

        return remotingService.launch(intEnvironmentVariables, detectCmds);
    }

    public void runDetectPostBuild(String detectArguments) {
        JenkinsBuildService jenkinsBuildService = detectServicesFactory.createJenkinsBuildService();

        try {
            String remoteJdkHome = jenkinsBuildService.getJDKRemoteHomeOrEmpty().orElse(null);
            int exitCode = runDetect(remoteJdkHome, detectArguments);
            if (exitCode > 0) {
                jenkinsBuildService.markBuildFailed("Detect failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            jenkinsBuildService.markBuildInterrupted();
        } catch (IntegrationException e) {
            jenkinsBuildService.markBuildUnstable(e);
        } catch (Exception e) {
            jenkinsBuildService.markBuildFailed(e);
        }
    }

    public int runDetectPipeline(boolean returnStatus, String detectArguments) throws IOException, IntegrationException, InterruptedException {
        int exitCode = runDetect(null, detectArguments);
        if (exitCode > 0) {
            String errorMsg = "Detect failed with exit code " + exitCode;
            if (returnStatus) {
                JenkinsIntLogger logger = detectServicesFactory.getLogger();
                logger.error(errorMsg);
            } else {
                throw new DetectJenkinsException(errorMsg);
            }
        }

        return exitCode;
    }

}
