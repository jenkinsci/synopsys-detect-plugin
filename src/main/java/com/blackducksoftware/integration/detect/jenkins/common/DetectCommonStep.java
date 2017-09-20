/**
 * Black Duck Detect Plugin
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.tools.ant.types.Commandline;

import com.blackducksoftware.integration.detect.jenkins.HubServerInfoSingleton;
import com.blackducksoftware.integration.detect.jenkins.JenkinsDetectLogger;
import com.blackducksoftware.integration.detect.jenkins.JenkinsProxyHelper;
import com.blackducksoftware.integration.detect.jenkins.PluginHelper;
import com.blackducksoftware.integration.detect.jenkins.exception.DetectJenkinsException;
import com.blackducksoftware.integration.detect.jenkins.remote.DetectRemoteRunner;
import com.blackducksoftware.integration.detect.jenkins.tools.DummyToolInstallation;
import com.blackducksoftware.integration.detect.jenkins.tools.DummyToolInstaller;
import com.blackducksoftware.integration.hub.builder.HubServerConfigBuilder;
import com.blackducksoftware.integration.hub.dataservice.phonehome.PhoneHomeDataService;
import com.blackducksoftware.integration.hub.global.HubServerConfig;
import com.blackducksoftware.integration.hub.rest.RestConnection;
import com.blackducksoftware.integration.hub.service.HubServicesFactory;
import com.blackducksoftware.integration.phonehome.PhoneHomeRequestBodyBuilder;
import com.blackducksoftware.integration.phonehome.enums.BlackDuckName;
import com.blackducksoftware.integration.util.CIEnvironmentVariables;

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
    private final String javaHome;

    public DetectCommonStep(final Node node, final Launcher launcher, final TaskListener listener, final EnvVars envVars, final FilePath workspace, final Run run, final String javaHome) {
        this.node = node;
        this.launcher = launcher;
        this.listener = listener;
        this.envVars = envVars;
        this.workspace = workspace;
        this.run = run;
        this.javaHome = javaHome;
    }

    public void runCommonDetectStep(final String detectProperties) {
        final JenkinsDetectLogger logger = new JenkinsDetectLogger(listener);
        final CIEnvironmentVariables variables = new CIEnvironmentVariables();
        variables.putAll(envVars);
        logger.setLogLevel(variables);
        try {
            final DummyToolInstaller dummyInstaller = new DummyToolInstaller();
            final String toolsDirectory = dummyInstaller.getToolDir(new DummyToolInstallation(), node).getRemote();
            final String hubUrl = HubServerInfoSingleton.getInstance().getHubUrl();
            final String hubUsername = HubServerInfoSingleton.getInstance().getHubUsername();
            final String hubPassword = HubServerInfoSingleton.getInstance().getHubPassword();
            final int hubTimeout = HubServerInfoSingleton.getInstance().getHubTimeout();
            final boolean trustSSLCertificates = HubServerInfoSingleton.getInstance().isTrustSSLCertificates();

            final DetectRemoteRunner detectRemoteRunner = new DetectRemoteRunner(logger, javaHome, hubUrl, hubUsername, hubPassword, hubTimeout, trustSSLCertificates, HubServerInfoSingleton.getInstance().getDetectDownloadUrl(),
                    toolsDirectory, getCorrectedParameters(detectProperties), variables);
            ProxyConfiguration proxyConfig = null;
            final Jenkins jenkins = Jenkins.getInstance();
            if (jenkins != null) {
                proxyConfig = jenkins.proxy;
            }
            if (proxyConfig != null) {
                if (JenkinsProxyHelper.shouldUseProxy(hubUrl, proxyConfig.noProxyHost)) {
                    detectRemoteRunner.setProxyHost(proxyConfig.name);
                    detectRemoteRunner.setProxyPort(proxyConfig.port);
                    detectRemoteRunner.setProxyUsername(proxyConfig.getUserName());
                    detectRemoteRunner.setProxyPassword(proxyConfig.getPassword());
                }
            }

            try {
                // Phone Home
                final HubServerConfigBuilder hubServerConfigBuilder = new HubServerConfigBuilder();
                if (StringUtils.isNotBlank(hubUrl)) {
                    hubServerConfigBuilder.setHubUrl(hubUrl);
                    hubServerConfigBuilder.setUsername(hubUsername);
                    hubServerConfigBuilder.setPassword(hubPassword);
                    hubServerConfigBuilder.setTimeout(hubTimeout);
                    hubServerConfigBuilder.setAlwaysTrustServerCertificate(trustSSLCertificates);

                    if (JenkinsProxyHelper.shouldUseProxy(hubUrl, proxyConfig.noProxyHost)) {
                        hubServerConfigBuilder.setProxyHost(proxyConfig.name);
                        hubServerConfigBuilder.setProxyPort(proxyConfig.port);
                        hubServerConfigBuilder.setProxyUsername(proxyConfig.getUserName());
                        hubServerConfigBuilder.setProxyPassword(proxyConfig.getPassword());
                    }

                    final HubServerConfig hubServerConfig = hubServerConfigBuilder.build();
                    final RestConnection restConnection = hubServerConfig.createCredentialsRestConnection(logger);
                    final HubServicesFactory servicesFactory = new HubServicesFactory(restConnection);
                    final PhoneHomeDataService phoneHomeDataService = servicesFactory.createPhoneHomeDataService();

                    final String thirdPartyVersion = Jenkins.getVersion().toString();
                    final String pluginVersion = PluginHelper.getPluginVersion();

                    final PhoneHomeRequestBodyBuilder phoneHomeRequestBodyBuilder = phoneHomeDataService.createInitialPhoneHomeRequestBodyBuilder();
                    phoneHomeRequestBodyBuilder.setBlackDuckName(BlackDuckName.HUB);
                    phoneHomeRequestBodyBuilder.setThirdPartyName("Jenkins-Detect");
                    phoneHomeRequestBodyBuilder.setThirdPartyVersion(thirdPartyVersion);
                    phoneHomeRequestBodyBuilder.setPluginVersion(pluginVersion);

                    phoneHomeDataService.phoneHome(phoneHomeRequestBodyBuilder);
                }
            } catch (final Exception e) {
                logger.debug("Phone Home failed : " + e.getMessage(), e);
            }

            node.getChannel().call(detectRemoteRunner);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            run.setResult(Result.UNSTABLE);
        }
    }

    public List<String> getCorrectedParameters(final String commandLineParameters) throws DetectJenkinsException {
        final String[] separatedParameters = Commandline.translateCommandline(commandLineParameters);
        final List<String> correctedParameters = new ArrayList<>();
        for (final String parameter : separatedParameters) {
            correctedParameters.add(handleVariableReplacement(envVars, parameter));
        }
        return correctedParameters;
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
