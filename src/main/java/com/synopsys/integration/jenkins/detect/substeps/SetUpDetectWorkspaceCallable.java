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

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.detect.DetectDownloadManager;
import com.synopsys.integration.jenkins.detect.JavaExecutableManager;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.rest.proxy.ProxyInfo;

import hudson.Platform;
import hudson.remoting.Callable;
import jenkins.model.Jenkins;

public class SetUpDetectWorkspaceCallable implements Callable<DetectSetupResponse, IntegrationException> {
    private static final long serialVersionUID = -4754831395795794586L;
    private final JenkinsIntLogger logger;
    private final HashMap<String, String> environmentVariables;
    private final String workspaceTempPath;
    private final String remoteJavaHome;

    public SetUpDetectWorkspaceCallable(JenkinsIntLogger logger, Map<String, String> environmentVariables, String workspaceTempPath, String remoteJavaHome) {
        this.logger = logger;
        this.environmentVariables = new HashMap<>(environmentVariables);
        this.workspaceTempPath = workspaceTempPath;
        this.remoteJavaHome = remoteJavaHome;
    }

    @Override
    public DetectSetupResponse call() throws IntegrationException {
        try {
            String pathToDetectJar = environmentVariables.get("DETECT_JAR");
            DetectSetupResponse.ExecutionStrategy executionStrategy = determineExecutionStrategy(pathToDetectJar);
            if (executionStrategy == DetectSetupResponse.ExecutionStrategy.JAR) {
                return setUpForJarExecution(pathToDetectJar, remoteJavaHome);
            } else {
                return setUpForScriptExecution(executionStrategy);
            }
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new DetectJenkinsException("Could not set up Detect environment", e);
        }
    }

    @Override
    public void checkRoles(RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(this.getClass()));
    }

    private DetectSetupResponse.ExecutionStrategy determineExecutionStrategy(String pathToDetectJar) {
        if (StringUtils.isNotBlank(pathToDetectJar)) {
            return DetectSetupResponse.ExecutionStrategy.JAR;
        } else {
            if (Platform.current() == Platform.WINDOWS) {
                return DetectSetupResponse.ExecutionStrategy.POWERSHELL_SCRIPT;
            } else {
                return DetectSetupResponse.ExecutionStrategy.SHELL_SCRIPT;
            }
        }
    }

    private DetectSetupResponse setUpForJarExecution(String pathToDetectJar, String javaHome) throws IOException, InterruptedException {
        JavaExecutableManager javaExecutableManager = new JavaExecutableManager(logger, environmentVariables);

        String javaExecutablePath = javaExecutableManager.calculateJavaExecutablePath(javaHome);
        logger.info("Running with JAVA: " + javaExecutablePath);
        logger.info("Detect configured: " + pathToDetectJar);
        javaExecutableManager.logJavaVersion();

        Path detectJar = Paths.get(pathToDetectJar);

        logger.info("Running Detect: " + detectJar.getFileName());
        return new DetectSetupResponse(DetectSetupResponse.ExecutionStrategy.JAR, javaExecutablePath, pathToDetectJar);
    }

    private DetectSetupResponse setUpForScriptExecution(DetectSetupResponse.ExecutionStrategy executionStrategy) throws IOException, IntegrationException {
        String detectRemotePath;

        String scriptUrl;
        if (executionStrategy == DetectSetupResponse.ExecutionStrategy.POWERSHELL_SCRIPT) {
            scriptUrl = DetectDownloadManager.LATEST_POWERSHELL_SCRIPT_URL;
        } else {
            scriptUrl = DetectDownloadManager.LATEST_SHELL_SCRIPT_URL;
        }

        // TODO: This will always return NO_PROXY_INFO, because Jenkins will always be null. We must fix this for 3.0.0. --rotte JUN 2020
        JenkinsProxyHelper jenkinsProxyHelper = JenkinsProxyHelper.fromJenkins(Jenkins.getInstanceOrNull());
        ProxyInfo proxyInfo = jenkinsProxyHelper.getProxyInfo(scriptUrl);
        DetectDownloadManager detectDownloadManager = new DetectDownloadManager(logger, proxyInfo, workspaceTempPath);

        detectRemotePath = detectDownloadManager.downloadScript(scriptUrl).toRealPath().toString();

        if (StringUtils.isBlank(detectRemotePath)) {
            throw new IntegrationException("[ERROR] The Detect script was not downloaded successfully.");
        }

        return new DetectSetupResponse(executionStrategy, detectRemotePath);
    }

}
