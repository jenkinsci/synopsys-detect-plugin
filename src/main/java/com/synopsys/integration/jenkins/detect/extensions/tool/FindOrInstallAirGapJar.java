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
package com.synopsys.integration.jenkins.detect.extensions.tool;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Optional;

import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.detect.extensions.AirGapDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.DetectDownloadStrategy;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;

public class FindOrInstallAirGapJar {
    public static final String DETECT_JAR_PREFIX = "synopsys-detect-";
    public static final String DETECT_JAR_SUFFIX = ".jar";
    private final JenkinsIntLogger logger;
    private final JenkinsConfigService jenkinsConfigService;

    public FindOrInstallAirGapJar(JenkinsIntLogger logger, JenkinsConfigService jenkinsConfigService) {
        this.logger = logger;
        this.jenkinsConfigService = jenkinsConfigService;
    }

    public String getOrDownloadAirGapJar(DetectDownloadStrategy detectDownloadStrategy) throws IOException, InterruptedException, DetectJenkinsException {
        Optional<DetectAirGapInstallation> detectAirGapWithName = jenkinsConfigService
                                                                      .getInstallationForNodeAndEnvironment(DetectAirGapInstallation.DescriptorImpl.class, ((AirGapDownloadStrategy) detectDownloadStrategy).getAirGapInstallationName());

        if (!detectAirGapWithName.isPresent()) {
            throw new DetectJenkinsException(String.format("Detect AirGap cannot be executed: No DetectAirGap with the name %s could be found in the global tool configuration.", detectAirGapWithName));
        }

        return getAirGapJar(detectAirGapWithName.get());
    }

    private String getAirGapJar(DetectAirGapInstallation detectAirGapInstallation) throws DetectJenkinsException {
        String baseDir = detectAirGapInstallation.getHome();

        if (baseDir == null) {
            throw new DetectJenkinsException("Detect AirGap installation directory is null.");
        }

        File[] foundJars = new File(baseDir).listFiles(jarFileFilter);

        if (foundJars == null || foundJars.length != 1) {
            throw new DetectJenkinsException(String.format("Unable to identify Detect AirGap jar in directory %s", baseDir));
        }

        return foundJars[0].toString();
    }

    FileFilter jarFileFilter = new FileFilter() {
        @Override
        public boolean accept(File file) {
            String fileName = file.getName();
            return fileName.endsWith(DETECT_JAR_SUFFIX) && fileName.startsWith(DETECT_JAR_PREFIX);
        }
    };
}
