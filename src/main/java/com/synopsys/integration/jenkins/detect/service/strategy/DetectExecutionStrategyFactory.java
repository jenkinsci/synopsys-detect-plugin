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

import java.io.IOException;

import com.synopsys.integration.jenkins.detect.service.DetectArgumentService;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.OperatingSystemType;

/**
 * This class is responsible for producing a DetectExecutionStrategy.
 */
public class DetectExecutionStrategyFactory {
    private final JenkinsIntLogger logger;
    private final JenkinsProxyHelper jenkinsProxyHelper;
    private final String remoteTempWorkspacePath;
    private final JenkinsConfigService jenkinsConfigService;
    private final JenkinsRemotingService jenkinsRemotingService;
    private final DetectArgumentService detectArgumentService;

    public DetectExecutionStrategyFactory(JenkinsIntLogger logger, JenkinsProxyHelper jenkinsProxyHelper, String remoteTempWorkspacePath, JenkinsConfigService jenkinsConfigService,
        JenkinsRemotingService jenkinsRemotingService, DetectArgumentService detectArgumentService) {
        this.logger = logger;
        this.jenkinsProxyHelper = jenkinsProxyHelper;
        this.remoteTempWorkspacePath = remoteTempWorkspacePath;
        this.jenkinsConfigService = jenkinsConfigService;
        this.jenkinsRemotingService = jenkinsRemotingService;
        this.detectArgumentService = detectArgumentService;
    }

    public DetectExecutionStrategy createDetectExecutionStrategy(DetectExecutionStrategyOptions detectExecutionStrategyOptions, String remoteJdkHome) throws IOException, InterruptedException {
        IntEnvironmentVariables intEnvironmentVariables = detectExecutionStrategyOptions.getIntEnvironmentVariables();

        if (detectExecutionStrategyOptions.isAirGap()) {
            return new DetectAirGapJarStrategy(jenkinsRemotingService, detectArgumentService, intEnvironmentVariables, logger, remoteJdkHome, jenkinsConfigService, detectExecutionStrategyOptions.getAirGapStrategy());
        } else if (detectExecutionStrategyOptions.isJar()) {
            return new DetectJarStrategy(jenkinsRemotingService, detectArgumentService, intEnvironmentVariables, logger, remoteJdkHome, detectExecutionStrategyOptions.getJarPath());
        } else {
            OperatingSystemType operatingSystemType = jenkinsRemotingService.getRemoteOperatingSystemType();
            return new DetectScriptStrategy(jenkinsRemotingService, detectArgumentService, intEnvironmentVariables, logger, jenkinsProxyHelper, operatingSystemType, remoteTempWorkspacePath);
        }
    }
}
