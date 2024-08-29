/*
 * blackduck-detect
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.sca.integration.detect.service.strategy;

import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.log.LogLevel;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

public class RemoteJavaService {
    public static final String DETECT_JAVA_PATH = "DETECT_JAVA_PATH";
    public static final String JAVA_HOME = "JAVA_HOME";

    private final JenkinsIntLogger logger;
    private final String remoteJdkHome;
    private final Map<String, String> environmentVariables;

    public RemoteJavaService(JenkinsIntLogger logger, String remoteJdkHome, Map<String, String> environmentVariables) {
        this.logger = logger;
        this.remoteJdkHome = remoteJdkHome;
        this.environmentVariables = environmentVariables;
    }

    public String getJavaExecutablePath() {
        String javaExecutableName = "java";
        if (SystemUtils.IS_OS_WINDOWS) {
            javaExecutableName = "java.exe";
        }

        String javaExecutablePath = calculateJavaExecutablePath(javaExecutableName);
        logger.info("Running with JAVA: " + javaExecutablePath);

        logDebugData(javaExecutablePath);

        return javaExecutablePath;
    }

    private String calculateJavaExecutablePath(String javaExecutableName) {
        String fullPathToJava = null;
        String javaPathSourceLogging = "";

        File javaExecutablePath = null;
        if (remoteJdkHome != null) {
            javaExecutablePath = new File(remoteJdkHome, "bin");
            javaExecutablePath = new File(javaExecutablePath, javaExecutableName);
            javaPathSourceLogging = "Node environment";
        } else if (environmentVariables.containsKey(DETECT_JAVA_PATH)) {
            javaExecutablePath = new File(environmentVariables.get(DETECT_JAVA_PATH));
            javaPathSourceLogging = DETECT_JAVA_PATH + " environment variable";
        } else if (environmentVariables.containsKey(JAVA_HOME)) {
            javaExecutablePath = new File(environmentVariables.get(JAVA_HOME), "bin");
            javaExecutablePath = new File(javaExecutablePath, javaExecutableName);
            javaPathSourceLogging = JAVA_HOME + " environment variable";
        }

        try {
            fullPathToJava = Objects.requireNonNull(javaExecutablePath).getCanonicalPath();
            logger.debug("Path to Java executable is set based on: " + javaPathSourceLogging);
        } catch (IOException | NullPointerException e) {
            logger.warn("Could not set path to Java executable, falling back to PATH.");
        }

        return (null != fullPathToJava) ? fullPathToJava : javaExecutableName;
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
