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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.util.IntEnvironmentVariables;

import jenkins.security.MasterToSlaveCallable;

public class DetectJarStrategy extends DetectExecutionStrategy {
    private final JenkinsIntLogger logger;
    private final String detectJarPath;
    private final IntEnvironmentVariables intEnvironmentVariables;
    private final String remoteJdkHome;

    public DetectJarStrategy(JenkinsIntLogger logger, IntEnvironmentVariables intEnvironmentVariables, String remoteJdkHome, String detectJarPath) {
        this.logger = logger;
        this.intEnvironmentVariables = intEnvironmentVariables;
        this.remoteJdkHome = remoteJdkHome;
        this.detectJarPath = detectJarPath;
    }

    @Override
    public Function<String, String> getArgumentEscaper() {
        return Function.identity();
    }

    @Override
    public List<String> getInitialArguments(String javaExecutablePath) {
        return new ArrayList<>(Arrays.asList(javaExecutablePath, "-jar", detectJarPath));
    }

    @Override
    public MasterToSlaveCallable<String, IntegrationException> getSetupCallable() {
        return new SetupCallableImpl(logger, intEnvironmentVariables.getVariables(), detectJarPath, remoteJdkHome);
    }

    public static class SetupCallableImpl extends MasterToSlaveCallable<String, IntegrationException> {
        private static final long serialVersionUID = -8326836838838706367L;
        private final JenkinsIntLogger logger;
        private final Map<String, String> environmentVariables;
        private final String detectJarPath;
        private final String remoteJdkHome;

        public SetupCallableImpl(JenkinsIntLogger logger, Map<String, String> environmentVariables, String detectJarPath, String remoteJdkHome) {
            this.logger = logger;
            this.environmentVariables = environmentVariables;
            this.detectJarPath = detectJarPath;
            this.remoteJdkHome = remoteJdkHome;
        }

        @Override
        public String call() {
            String javaExecutablePath = this.calculateJavaExecutablePath();

            logger.info("Running with JAVA: " + javaExecutablePath);
            logger.info("Detect configured: " + detectJarPath);
            this.logDebugData(javaExecutablePath);

            return javaExecutablePath;
        }

        private String calculateJavaExecutablePath() {
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
                    logger.warn("Dtect could not get Java Home from configured JDK, falling back to java on path: " + e.getMessage());
                }
            }
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

}
