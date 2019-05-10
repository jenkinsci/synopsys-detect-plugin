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
package com.synopsys.integration.jenkins.detect;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import com.synopsys.integration.log.LogLevel;

public class JavaExecutableManager {
    private final DetectJenkinsLogger logger;
    private final Map<String, String> environmentVariables;

    public JavaExecutableManager(final DetectJenkinsLogger logger, final Map<String, String> environmentVariables) {
        this.logger = logger;
        this.environmentVariables = environmentVariables;
    }

    public String calculateJavaExecutablePath(final String javaHome) throws IOException {
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

    public void logJavaVersion() {
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
