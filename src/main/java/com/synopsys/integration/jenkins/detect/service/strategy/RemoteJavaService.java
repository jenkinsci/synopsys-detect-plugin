/*
 * blackduck-detect
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.detect.service.strategy;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

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
            logger.debug("PATH: " + environmentVariables.get("PATH"));
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(Arrays.asList(javaExecutablePath, "-version"));
                processBuilder.environment().putAll(environmentVariables);
                Process process = processBuilder.start();
                process.waitFor();
                logJavaVersion(process);
            } catch (IOException e) {
                logger.debug("Error starting process to get Java version: " + e.getMessage(), e);
            } catch (InterruptedException e) {
                logger.debug("Error running process to get Java version: " + e.getMessage(), e);
                Thread.currentThread().interrupt();
            }
        }
    }

    private void logJavaVersion(Process process) {
        try (InputStream errorStream = Objects.requireNonNull(process).getErrorStream();
            InputStream inputSteam = Objects.requireNonNull(process).getInputStream()) {
            logger.debug("Java version: ");
            IOUtils.copy(errorStream, logger.getTaskListener().getLogger());
            IOUtils.copy(inputSteam, logger.getTaskListener().getLogger());
        } catch (IOException e) {
            logger.debug("Error printing the JAVA version: " + e.getMessage(), e);
        }
    }
}
