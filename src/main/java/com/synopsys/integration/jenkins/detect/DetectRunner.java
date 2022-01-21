/*
 * blackduck-detect
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
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

        List<String> initialArguments = remotingService.call(detectExecutionStrategy.getSetupCallable());

        List<String> detectCommands = detectArgumentService.getDetectArguments(intEnvironmentVariables, detectExecutionStrategy.getArgumentEscaper(), initialArguments, detectArgumentString);

        return remotingService.launch(intEnvironmentVariables, detectCommands);
    }
}
