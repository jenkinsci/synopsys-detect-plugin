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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.log.LogLevel;

public class RemoteJavaService {
    private final JenkinsIntLogger logger;
    private final String remoteJdkHome;
    private final Map<String, String> environmentVariables;

    public RemoteJavaService(JenkinsIntLogger logger, String remoteJdkHome, Map<String, String> environmentVariables) {
        this.logger = logger;
        this.remoteJdkHome = remoteJdkHome;
        this.environmentVariables = environmentVariables;
    }

    public String calculateJavaExecutablePath() {
        String javaExecutablePath = "java";
        if (remoteJdkHome != null) {
            File remoteJdkJava = new File(remoteJdkHome);
            remoteJdkJava = new File(remoteJdkJava, "bin");
            if (SystemUtils.IS_OS_WINDOWS) {
                remoteJdkJava = new File(remoteJdkJava, "java.exe");
            } else {
                remoteJdkJava = new File(remoteJdkJava, "java");
            }
            try {
                javaExecutablePath = remoteJdkJava.getCanonicalPath();
            } catch (IOException e) {
                logger.warn("Detect could not get Java Home from configured JDK, falling back to java on path: " + e.getMessage());
            }
        }

        logger.info("Running with JAVA: " + javaExecutablePath);
        logDebugData(javaExecutablePath);

        return javaExecutablePath;
    }

    private void logDebugData(String javaExecutablePath) {
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
            } catch (InterruptedException e) {
                logger.debug("Error printing the JAVA version: " + e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
    }
}
