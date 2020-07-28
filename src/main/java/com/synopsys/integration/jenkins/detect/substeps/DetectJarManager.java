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
package com.synopsys.integration.jenkins.detect.substeps;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.log.LogLevel;

public class DetectJarManager extends DetectExecutionManager {
    private static final long serialVersionUID = -2061679469148948745L;
    private final JenkinsIntLogger logger;
    private final Map<String, String> environmentVariables;
    private final String detectJarPath;
    private final String javaHome;

    public DetectJarManager(JenkinsIntLogger logger, String javaHome, Map<String, String> environmentVariables, String detectJarPath) {
        this.logger = logger;
        this.javaHome = javaHome;
        this.environmentVariables = environmentVariables;
        this.detectJarPath = detectJarPath;
    }

    @Override
    public DetectSetupResponse setUpForExecution() throws IOException, InterruptedException {
        String javaExecutablePath = this.calculateJavaExecutablePath();

        logger.info("Running with JAVA: " + javaExecutablePath);
        logger.info("Detect configured: " + detectJarPath);
        this.logDebugData(javaExecutablePath);

        return new DetectSetupResponse(DetectSetupResponse.ExecutionStrategy.JAR, javaExecutablePath, detectJarPath);
    }

    private String calculateJavaExecutablePath() throws IOException {
        String javaExecutablePath = "java";
        if (javaHome != null) {
            File java = new File(javaHome);
            java = new File(java, "bin");
            if (SystemUtils.IS_OS_WINDOWS) {
                java = new File(java, "java.exe");
            } else {
                java = new File(java, "java");
            }
            javaExecutablePath = java.getCanonicalPath();
        }
        return javaExecutablePath;
    }

    private void logDebugData(String javaExecutablePath) throws InterruptedException {
        if (logger.getLogLevel().isLoggable(LogLevel.DEBUG)) {
            try {
                logger.debug("PATH: " + environmentVariables.get("PATH"));
                ProcessBuilder processBuilder = new ProcessBuilder(Arrays.asList(javaExecutablePath, "-version"));
                processBuilder.environment().putAll(environmentVariables);
                Process process = processBuilder.start();
                process.waitFor();
                logger.debug("Java version: ");
                IOUtils.copy(process.getErrorStream(), logger.getTaskListener().getLogger());
                IOUtils.copy(process.getInputStream(), logger.getTaskListener().getLogger());
            } catch (IOException e) {
                logger.debug("Error printing the JAVA version: " + e.getMessage(), e);
            }
        }
    }
}
