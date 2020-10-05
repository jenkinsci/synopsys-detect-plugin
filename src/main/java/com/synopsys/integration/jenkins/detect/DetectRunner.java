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
import com.synopsys.integration.jenkins.detect.extensions.DetectDownloadStrategy;
import com.synopsys.integration.jenkins.detect.service.DetectArgumentService;
import com.synopsys.integration.jenkins.detect.service.DetectEnvironmentService;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectExecutionStrategy;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectStrategyService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.OperatingSystemType;

public class DetectRunner {
    private final DetectEnvironmentService detectEnvironmentService;
    private final JenkinsRemotingService remotingService;
    private final DetectStrategyService detectStrategyService;
    private final DetectArgumentService detectArgumentService;

    public DetectRunner(DetectEnvironmentService detectEnvironmentService, JenkinsRemotingService remotingService, DetectStrategyService detectStrategyService, DetectArgumentService detectArgumentService) {
        this.detectEnvironmentService = detectEnvironmentService;
        this.remotingService = remotingService;
        this.detectStrategyService = detectStrategyService;
        this.detectArgumentService = detectArgumentService;
    }

    public int runDetect(String remoteJdkHome, String detectArgumentString, DetectDownloadStrategy detectDownloadStrategy) throws IOException, InterruptedException, IntegrationException {
        IntEnvironmentVariables intEnvironmentVariables = detectEnvironmentService.createDetectEnvironment();
        OperatingSystemType operatingSystemType = remotingService.getRemoteOperatingSystemType();
        DetectExecutionStrategy detectExecutionStrategy = detectStrategyService.getExecutionStrategy(intEnvironmentVariables, operatingSystemType, remoteJdkHome, detectDownloadStrategy);

        String setupResponse = remotingService.call(detectExecutionStrategy.getSetupCallable());
        List<String> initialArguments = detectExecutionStrategy.getInitialArguments(setupResponse);

        List<String> detectCommands = detectArgumentService.getDetectArguments(intEnvironmentVariables, detectExecutionStrategy.getArgumentEscaper(), initialArguments, detectArgumentString);

        return remotingService.launch(intEnvironmentVariables, detectCommands);
    }
}
