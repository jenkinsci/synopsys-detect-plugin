/*
 * blackduck-detect
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.sca.integration.detect.service.strategy;

import com.sca.integration.detect.DetectJenkinsEnvironmentVariable;
import com.sca.integration.detect.exception.DetectJenkinsException;
import com.sca.integration.detect.extensions.AirGapDownloadStrategy;
import com.sca.integration.detect.extensions.DetectDownloadStrategy;
import com.sca.integration.detect.extensions.InheritFromGlobalDownloadStrategy;
import com.sca.integration.detect.extensions.global.DetectGlobalConfig;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.OperatingSystemType;
import org.apache.commons.lang3.StringUtils;

public class DetectStrategyService {
    private final JenkinsIntLogger logger;
    private final String remoteTempWorkspacePath;
    private final JenkinsProxyHelper jenkinsProxyHelper;
    private final JenkinsConfigService jenkinsConfigService;

    public DetectStrategyService(JenkinsIntLogger logger, JenkinsProxyHelper jenkinsProxyHelper, String remoteTempWorkspacePath, JenkinsConfigService jenkinsConfigService) {
        this.logger = logger;
        this.jenkinsProxyHelper = jenkinsProxyHelper;
        this.remoteTempWorkspacePath = remoteTempWorkspacePath;
        this.jenkinsConfigService = jenkinsConfigService;
    }

    public DetectExecutionStrategy getExecutionStrategy(
        IntEnvironmentVariables intEnvironmentVariables,
        OperatingSystemType operatingSystemType,
        String remoteJdkHome,
        DetectDownloadStrategy detectDownloadStrategy
    )
        throws IntegrationException {
        String loggingMessage = "Running Detect using configured strategy: ";

        if (detectDownloadStrategy == null || detectDownloadStrategy instanceof InheritFromGlobalDownloadStrategy) {
            DetectGlobalConfig detectGlobalConfig = jenkinsConfigService.getGlobalConfiguration(DetectGlobalConfig.class)
                .orElseThrow(() -> new DetectJenkinsException("Could not find Detect configuration. Check Jenkins System Configuration to ensure Detect is configured correctly."));
            detectDownloadStrategy = detectGlobalConfig.getDownloadStrategy();

            if (detectDownloadStrategy == null) {
                detectDownloadStrategy = detectGlobalConfig.getDefaultDownloadStrategy();
                loggingMessage = "System configured strategy not found, running Detect using default configured system strategy: ";
            } else {
                loggingMessage = "Running Detect using configured system strategy: ";
            }
        }

        logger.info(loggingMessage + detectDownloadStrategy.getDisplayName());

        String detectJarPath = intEnvironmentVariables.getValue(DetectJenkinsEnvironmentVariable.USER_PROVIDED_JAR_PATH.stringValue());
        DetectExecutionStrategy detectExecutionStrategy;

        if (detectDownloadStrategy instanceof AirGapDownloadStrategy) {
            detectExecutionStrategy = new DetectAirGapJarStrategy(
                logger,
                intEnvironmentVariables,
                remoteJdkHome,
                jenkinsConfigService,
                (AirGapDownloadStrategy) detectDownloadStrategy
            );
        } else if (StringUtils.isNotBlank(detectJarPath)) {
            detectExecutionStrategy = new DetectJarStrategy(logger, intEnvironmentVariables, remoteJdkHome, detectJarPath);
        } else {
            detectExecutionStrategy = new DetectScriptStrategy(logger, jenkinsProxyHelper, operatingSystemType, remoteTempWorkspacePath);
        }

        return detectExecutionStrategy;
    }

}
