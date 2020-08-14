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
import com.synopsys.integration.jenkins.detect.service.DetectArgumentService;
import com.synopsys.integration.jenkins.detect.service.DetectEnvironmentService;
import com.synopsys.integration.jenkins.detect.service.DetectServicesFactory;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectExecutionStrategy;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectStrategyService;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsBuildService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.OperatingSystemType;

public class DetectCommands {
    private final DetectServicesFactory detectServicesFactory;

    public DetectCommands(DetectServicesFactory detectServicesFactory) {
        this.detectServicesFactory = detectServicesFactory;
    }

    public void runDetectPostBuild(String detectArgumentString) {
        JenkinsBuildService jenkinsBuildService = detectServicesFactory.createJenkinsBuildService();

        try {
            String remoteJdkHome = jenkinsBuildService.getJDKRemoteHomeOrEmpty().orElse(null);
            int exitCode = runDetect(remoteJdkHome, detectArgumentString);
            if (exitCode > 0) {
                jenkinsBuildService.markBuildFailed("Detect failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            jenkinsBuildService.markBuildInterrupted();
        } catch (IntegrationException e) {
            jenkinsBuildService.markBuildUnstable(e);
        } catch (Exception e) {
            jenkinsBuildService.markBuildFailed(e);
        }
    }

    public int runDetectPipeline(boolean returnStatus, String detectArgumentString) throws IOException, IntegrationException, InterruptedException {
        int exitCode = runDetect(null, detectArgumentString);
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

    private int runDetect(String remoteJdkHome, String detectArgumentString) throws IOException, InterruptedException, IntegrationException {
        DetectEnvironmentService detectEnvironmentService = detectServicesFactory.createDetectEnvironmentService();
        DetectStrategyService detectStrategyService = detectServicesFactory.createDetectStrategyService();
        DetectArgumentService detectArgumentService = detectServicesFactory.createDetectArgumentService();
        JenkinsRemotingService remotingService = detectServicesFactory.createJenkinsRemotingService();

        IntEnvironmentVariables intEnvironmentVariables = detectEnvironmentService.createDetectEnvironment();
        OperatingSystemType operatingSystemType = remotingService.getRemoteOperatingSystemType();
        DetectExecutionStrategy detectExecutionStrategy = detectStrategyService.getExecutionStrategy(intEnvironmentVariables, operatingSystemType, remoteJdkHome);

        String setupResponse = remotingService.call(detectExecutionStrategy.getSetupCallable());
        List<String> initialArguments = detectExecutionStrategy.getInitialArguments(setupResponse);

        List<String> detectCmds = detectArgumentService.getDetectArguments(intEnvironmentVariables, detectExecutionStrategy.getArgumentEscaper(), initialArguments, detectArgumentString);

        return remotingService.launch(intEnvironmentVariables, detectCmds);
    }

}
