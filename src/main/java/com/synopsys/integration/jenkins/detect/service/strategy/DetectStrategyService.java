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
package com.synopsys.integration.jenkins.detect.service.strategy;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.DetectJenkinsEnvironmentVariable;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.detect.extensions.AirGapDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.DetectDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.InheritFromGlobalDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.global.DetectGlobalConfig;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.OperatingSystemType;

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

    public DetectExecutionStrategy getExecutionStrategy(IntEnvironmentVariables intEnvironmentVariables, OperatingSystemType operatingSystemType, String remoteJdkHome, DetectDownloadStrategy detectDownloadStrategy)
        throws IntegrationException {
        if (detectDownloadStrategy == null || detectDownloadStrategy instanceof InheritFromGlobalDownloadStrategy) {
            DetectGlobalConfig detectGlobalConfig = jenkinsConfigService.getGlobalConfiguration(DetectGlobalConfig.class)
                                                        .orElseThrow(() -> new DetectJenkinsException("Unable to identify Detect Global Configuration."));
            detectDownloadStrategy = detectGlobalConfig.getDownloadStrategy();
        }

        logger.info("Running Detect using strategy: " + (detectDownloadStrategy != null ? detectDownloadStrategy.getClass().getSimpleName() : null));

        String detectJarPath = intEnvironmentVariables.getValue(DetectJenkinsEnvironmentVariable.USER_PROVIDED_JAR_PATH.stringValue());
        DetectExecutionStrategy detectExecutionStrategy;

        if (detectDownloadStrategy instanceof AirGapDownloadStrategy) {
            detectExecutionStrategy = new DetectAirGapJarStrategy(logger, intEnvironmentVariables, remoteJdkHome, jenkinsConfigService, (AirGapDownloadStrategy) detectDownloadStrategy);
        } else if (StringUtils.isNotBlank(detectJarPath)) {
            detectExecutionStrategy = new DetectJarStrategy(logger, intEnvironmentVariables, remoteJdkHome, detectJarPath);
        } else {
            detectExecutionStrategy = new DetectScriptStrategy(logger, jenkinsProxyHelper, operatingSystemType, remoteTempWorkspacePath);
        }

        return detectExecutionStrategy;
    }

}
