/**
 * Black Duck Detect Plugin for Jenkins
 *
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.blackducksoftware.integration.detect.jenkins.common;

import java.util.Map;

import com.blackducksoftware.integration.detect.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.detect.jenkins.JenkinsDetectLogger;
import com.blackducksoftware.integration.detect.jenkins.exception.DetectJenkinsException;
import com.blackducksoftware.integration.detect.jenkins.remote.DetectRemoteRunner;
import com.blackducksoftware.integration.detect.jenkins.tools.DummyToolInstallation;
import com.blackducksoftware.integration.detect.jenkins.tools.DummyToolInstaller;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.ProxyConfiguration;
import hudson.Util;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.Jenkins;

public class DetectCommonStep {
    private final Node node;
    private final Launcher launcher;
    private final TaskListener listener;
    private final EnvVars envVars;
    private final FilePath workspace;
    private final Run run;

    public DetectCommonStep(final Node node, final Launcher launcher, final TaskListener listener, final EnvVars envVars, final FilePath workspace, final Run run) {
        this.node = node;
        this.launcher = launcher;
        this.listener = listener;
        this.envVars = envVars;
        this.workspace = workspace;
        this.run = run;
    }

    public void runCommonDetectStep() {
        final JenkinsDetectLogger logger = new JenkinsDetectLogger(listener);
        try {
            final DummyToolInstaller dummyInstaller = new DummyToolInstaller();
            final String toolsDirectory = dummyInstaller.getToolDir(new DummyToolInstallation(), node).getRemote();

            final DetectRemoteRunner detectRemoteRunner = new DetectRemoteRunner(logger, HubServerInfoSingleton.getInstance().getHubUrl(), HubServerInfoSingleton.getInstance().getHubUsername(),
                    HubServerInfoSingleton.getInstance().getHubPassword(), HubServerInfoSingleton.getInstance().getHubTimeout(), HubServerInfoSingleton.getInstance().isImportSSLCerts(),
                    HubServerInfoSingleton.getInstance().getDetectDownloadUrl(), toolsDirectory);
            ProxyConfiguration proxyConfig = null;
            final Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null) {
                proxyConfig = jenkins.proxy;
            }
            if (proxyConfig != null) {
                detectRemoteRunner.setProxyHost(proxyConfig.name);
                detectRemoteRunner.setProxyPort(proxyConfig.port);
                detectRemoteRunner.setProxyNoHost(proxyConfig.noProxyHost);
                detectRemoteRunner.setProxyUsername(proxyConfig.getUserName());
                detectRemoteRunner.setProxyPassword(proxyConfig.getPassword());
            }
            node.getChannel().call(detectRemoteRunner);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            run.setResult(Result.UNSTABLE);
        }
    }

    public String handleVariableReplacement(final Map<String, String> variables, final String value) throws DetectJenkinsException {
        if (value != null) {
            final String newValue = Util.replaceMacro(value, variables);
            if (newValue.contains("$")) {
                throw new DetectJenkinsException("Variable was not properly replaced. Value : " + value + ", Result : " + newValue + ". Make sure the variable has been properly defined.");
            }
            return newValue;
        } else {
            return null;
        }
    }

}
