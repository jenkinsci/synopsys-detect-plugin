/*
 * blackduck-detect
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.jenkins.detect;

import com.blackduck.integration.jenkins.detect.extensions.DetectDownloadStrategy;
import com.blackduck.integration.jenkins.detect.service.DetectArgumentService;
import com.blackduck.integration.jenkins.detect.service.DetectEnvironmentService;
import com.blackduck.integration.jenkins.detect.service.strategy.DetectExecutionStrategy;
import com.blackduck.integration.jenkins.detect.service.strategy.DetectStrategyService;
import com.blackduck.integration.exception.IntegrationException;
import com.blackduck.integration.jenkins.extensions.JenkinsIntLogger;
import com.blackduck.integration.jenkins.service.JenkinsRemotingService;
import com.blackduck.integration.util.IntEnvironmentVariables;
import com.blackduck.integration.util.OperatingSystemType;

import java.io.IOException;
import java.util.List;

public class DetectRunner {
    public static final String ASTERISKS = "******************************************************************************";

    private final DetectEnvironmentService detectEnvironmentService;
    private final JenkinsRemotingService remotingService;
    private final DetectStrategyService detectStrategyService;
    private final DetectArgumentService detectArgumentService;
    private final JenkinsIntLogger logger;

    public DetectRunner(
        DetectEnvironmentService detectEnvironmentService,
        JenkinsRemotingService remotingService,
        DetectStrategyService detectStrategyService,
        DetectArgumentService detectArgumentService,
        JenkinsIntLogger logger
    ) {
        this.detectEnvironmentService = detectEnvironmentService;
        this.remotingService = remotingService;
        this.detectStrategyService = detectStrategyService;
        this.detectArgumentService = detectArgumentService;
        this.logger = logger;
    }

    public int runDetect(String remoteJdkHome, String detectArgumentString, DetectDownloadStrategy detectDownloadStrategy)
        throws IOException, InterruptedException, IntegrationException {
        IntEnvironmentVariables intEnvironmentVariables = detectEnvironmentService.createDetectEnvironment();
        OperatingSystemType operatingSystemType = remotingService.getRemoteOperatingSystemType();
        DetectExecutionStrategy detectExecutionStrategy = detectStrategyService.getExecutionStrategy(
            intEnvironmentVariables,
            operatingSystemType,
            remoteJdkHome,
            detectDownloadStrategy
        );

        List<String> initialArguments = remotingService.call(detectExecutionStrategy.getSetupCallable());

        List<String> detectCommands = detectArgumentService.getDetectArguments(
            intEnvironmentVariables,
            detectExecutionStrategy.getArgumentEscaper(),
            initialArguments,
            detectArgumentString
        );

        logger.info(ASTERISKS);
        logger.info("START OF DETECT");
        logger.info(ASTERISKS);

        int detectRun = remotingService.launch(intEnvironmentVariables, detectCommands);

        logger.info(ASTERISKS);
        logger.info("END OF DETECT");
        logger.info(ASTERISKS);

        return detectRun;
    }
}
