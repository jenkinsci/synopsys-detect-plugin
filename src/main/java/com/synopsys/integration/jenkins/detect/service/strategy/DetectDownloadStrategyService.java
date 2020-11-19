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

import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.detect.extensions.DetectDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.InheritFromGlobalDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.global.DetectGlobalConfig;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;

public class DetectDownloadStrategyService {
    public static final String DETECT_GLOBAL_CONFIGURATION_NOT_FOUND = "Could not find Detect configuration. Check Jenkins System Configuration to ensure Detect is configured correctly.";
    private final JenkinsIntLogger logger;
    private final JenkinsConfigService jenkinsConfigService;

    public DetectDownloadStrategyService(JenkinsIntLogger logger, JenkinsConfigService jenkinsConfigService) {
        this.logger = logger;
        this.jenkinsConfigService = jenkinsConfigService;
    }

    //TODO we should likely have an explicit object for HOW we are going to get detect and not reuse the user selection object, DetectDownloadStrategy (maybe build off of DetectExecutionStrategyOptions)
    public DetectDownloadStrategy determineCorrectDownloadStrategy(DetectDownloadStrategy initialDownloadStrategy) throws DetectJenkinsException {
        DetectDownloadStrategy correctDownloadStrategy = initialDownloadStrategy;
        String downloadStrategySource = "configured";
        if (initialDownloadStrategy == null || initialDownloadStrategy instanceof InheritFromGlobalDownloadStrategy) {
            DetectGlobalConfig detectGlobalConfig = jenkinsConfigService
                                                        .getGlobalConfiguration(DetectGlobalConfig.class)
                                                        .orElseThrow(() -> new DetectJenkinsException(DETECT_GLOBAL_CONFIGURATION_NOT_FOUND));
            correctDownloadStrategy = detectGlobalConfig.getDownloadStrategy();
            downloadStrategySource = "configured system";

            if (correctDownloadStrategy == null) {
                correctDownloadStrategy = detectGlobalConfig.getDefaultDownloadStrategy();
                downloadStrategySource = "default";
                logger.info("System configured strategy not found.");
            }
        }

        logger.info(String.format("Running Detect using %s strategy: %s", downloadStrategySource, correctDownloadStrategy.getDisplayName()));

        return correctDownloadStrategy;
    }

    private DetectGlobalConfig getValidDetectGlobalConfig() throws DetectJenkinsException {
        return jenkinsConfigService
                   .getGlobalConfiguration(DetectGlobalConfig.class)
                   .orElseThrow(() -> new DetectJenkinsException(DETECT_GLOBAL_CONFIGURATION_NOT_FOUND));
    }

}
