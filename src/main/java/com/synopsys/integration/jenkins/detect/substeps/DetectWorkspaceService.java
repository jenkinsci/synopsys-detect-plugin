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

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.detect.DetectJenkinsEnvironmentVariable;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.OperatingSystemType;

import hudson.remoting.VirtualChannel;
import jenkins.model.Jenkins;
import jenkins.security.MasterToSlaveCallable;

public class DetectWorkspaceService {
    private final JenkinsIntLogger logger;
    private final VirtualChannel virtualChannel;
    private final IntEnvironmentVariables intEnvironmentVariables;
    private final String remoteJavaHome;
    private final String remoteTempWorkspacePath;

    public DetectWorkspaceService(JenkinsIntLogger logger, VirtualChannel virtualChannel, IntEnvironmentVariables intEnvironmentVariables, String remoteJavaHome, String remoteTempWorkspacePath) {
        this.logger = logger;
        this.virtualChannel = virtualChannel;
        this.intEnvironmentVariables = intEnvironmentVariables;
        this.remoteJavaHome = remoteJavaHome;
        this.remoteTempWorkspacePath = remoteTempWorkspacePath;
    }

    public DetectSetupResponse setUpDetectWorkspace() throws IntegrationException {
        try {
            DetectExecutionManager detectExecutionManager;
            String detectJarPath = intEnvironmentVariables.getValue(DetectJenkinsEnvironmentVariable.USER_PROVIDED_JAR_PATH.stringValue());

            if (StringUtils.isNotBlank(detectJarPath)) {
                detectExecutionManager = new DetectJarManager(logger, remoteJavaHome, intEnvironmentVariables.getVariables(), detectJarPath);
            } else {
                OperatingSystemType operatingSystemType = virtualChannel.call(new GetOperatingSystemTypeCallable());
                String scriptUrl;
                if (operatingSystemType == OperatingSystemType.WINDOWS) {
                    scriptUrl = DetectScriptManager.LATEST_POWERSHELL_SCRIPT_URL;
                } else {
                    scriptUrl = DetectScriptManager.LATEST_SHELL_SCRIPT_URL;
                }
                ProxyInfo proxyInfo = getProxyInfo(scriptUrl);
                detectExecutionManager = new DetectScriptManager(logger, scriptUrl, proxyInfo, remoteTempWorkspacePath);
            }

            return virtualChannel.call(detectExecutionManager);
        } catch (IOException e) {
            throw new DetectJenkinsException("Could not set up Detect environment", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DetectJenkinsException("Could not set up Detect environment", e);
        }
    }

    private ProxyInfo getProxyInfo(String scriptUrl) {
        if (StringUtils.isBlank(scriptUrl)) {
            return ProxyInfo.NO_PROXY_INFO;
        }

        JenkinsProxyHelper jenkinsProxyHelper = JenkinsProxyHelper.fromJenkins(Jenkins.getInstanceOrNull());
        ProxyInfo proxyInfo;
        try {
            proxyInfo = jenkinsProxyHelper.getProxyInfo(scriptUrl);
        } catch (IllegalArgumentException e) {
            logger.warn("Synopsys Detect for Jenkins could not resolve proxy info from Jenkins because: " + e.getMessage());
            logger.warn("Continuing without proxy...");
            logger.trace("Stack trace:", e);
            proxyInfo = ProxyInfo.NO_PROXY_INFO;
        }

        return proxyInfo;
    }

    private static class GetOperatingSystemTypeCallable extends MasterToSlaveCallable<OperatingSystemType, RuntimeException> {
        private static final long serialVersionUID = -2750322419510141246L;

        @Override
        public OperatingSystemType call() {
            return OperatingSystemType.determineFromSystem();
        }
    }
}
