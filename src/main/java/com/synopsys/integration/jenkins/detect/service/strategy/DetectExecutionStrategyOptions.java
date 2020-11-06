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

import com.synopsys.integration.jenkins.detect.DetectJenkinsEnvironmentVariable;
import com.synopsys.integration.jenkins.detect.extensions.AirGapDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.DetectDownloadStrategy;
import com.synopsys.integration.util.IntEnvironmentVariables;

public class DetectExecutionStrategyOptions {
    private IntEnvironmentVariables intEnvironmentVariables;
    private String detectJarPath;
    private AirGapDownloadStrategy airGapDownloadStrategy;

    public DetectExecutionStrategyOptions(IntEnvironmentVariables intEnvironmentVariables, DetectDownloadStrategy detectDownloadStrategy) {
        this.intEnvironmentVariables = intEnvironmentVariables;
        this.detectJarPath = intEnvironmentVariables.getValue(DetectJenkinsEnvironmentVariable.USER_PROVIDED_JAR_PATH.stringValue());
        if (detectDownloadStrategy instanceof AirGapDownloadStrategy) {
            airGapDownloadStrategy = (AirGapDownloadStrategy) detectDownloadStrategy;
        }
    }

    public IntEnvironmentVariables getIntEnvironmentVariables() {
        return intEnvironmentVariables;
    }

    public boolean isAirGap() {
        return null != airGapDownloadStrategy;
    }

    public boolean isJar() {
        return StringUtils.isNotBlank(detectJarPath);
    }

    public String getJarPath() {
        return detectJarPath;
    }

    public AirGapDownloadStrategy getAirGapStrategy() {
        return airGapDownloadStrategy;
    }
}
