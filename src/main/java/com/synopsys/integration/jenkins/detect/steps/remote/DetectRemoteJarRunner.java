/**
 * blackduck-detect
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.jenkins.detect.steps.remote;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import com.synopsys.integration.jenkins.detect.JenkinsDetectLogger;
import com.synopsys.integration.log.LogLevel;

public class DetectRemoteJarRunner extends DetectRemoteRunner {
    private static final long serialVersionUID = -3893076074803560801L;
    private final String javaHome;
    private final String pathToDetectJar;
    private String javaExecutablePath;

    public DetectRemoteJarRunner(final JenkinsDetectLogger logger, final HashMap<String, String> environmentVariables, final String workspacePath, final String jenkinsVersion, final String pluginVersion, final String javaHome,
        final String pathToDetectJar, final String detectProperties) {
        super(logger, detectProperties, environmentVariables, workspacePath, jenkinsVersion, pluginVersion);
        this.javaHome = javaHome;
        this.pathToDetectJar = pathToDetectJar;
    }

    @Override
    protected void setUp() throws IOException {
        javaExecutablePath = calculateJavaExecutablePath(javaHome);
        logger.info("Running with JAVA: " + javaExecutablePath);
        logger.info("Detect configured: " + pathToDetectJar);
        logJavaVersion();
    }

    @Override
    protected List<String> getInvocationParameters() throws IOException {
        final File detectJar = new File(pathToDetectJar);
        logger.info("Running Detect: " + detectJar.getName());

        return Arrays.asList(javaExecutablePath, "-jar", detectJar.getCanonicalPath());
    }

    @Override
    protected Function<String, String> getEscapingFunction() {
        return Function.identity();
    }

    private String calculateJavaExecutablePath(final String javaHome) throws IOException {
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

    private void logJavaVersion() {
        logger.debug("PATH: " + environmentVariables.get("PATH"));
        if (LogLevel.DEBUG == logger.getLogLevel()) {
            try {
                logger.info("Java version: ");
                final ProcessBuilder processBuilder = new ProcessBuilder(Arrays.asList("java", "-version"));
                processBuilder.environment().putAll(environmentVariables);
                final Process process = processBuilder.start();
                process.waitFor();
                IOUtils.copy(process.getErrorStream(), logger.getJenkinsListener().getLogger());
                IOUtils.copy(process.getInputStream(), logger.getJenkinsListener().getLogger());
            } catch (final InterruptedException | IOException e) {
                logger.debug("Error printing the JAVA version: " + e.getMessage(), e);
            }
        }
    }

}
