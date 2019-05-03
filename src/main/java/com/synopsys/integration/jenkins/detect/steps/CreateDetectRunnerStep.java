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
package com.synopsys.integration.jenkins.detect.steps;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.jenkins.detect.JenkinsDetectLogger;
import com.synopsys.integration.jenkins.detect.PluginHelper;
import com.synopsys.integration.jenkins.detect.steps.remote.DetectRemoteJarRunner;
import com.synopsys.integration.jenkins.detect.steps.remote.DetectRemoteRunner;
import com.synopsys.integration.jenkins.detect.steps.remote.DetectRemoteScriptRunner;
import com.synopsys.integration.util.IntEnvironmentVariables;

public class ExecuteDetectStep {
    private final JenkinsDetectLogger logger;

    public ExecuteDetectStep(final JenkinsDetectLogger logger) {
        this.logger = logger;
    }

    public DetectRemoteRunner createAppropriateDetectRemoteRunner(final IntEnvironmentVariables intEnvironmentVariables, final String detectProperties, final String remoteJavaHome, final String remoteWorkspacePath,
        final String remoteToolsDirectory) {

        final DetectRemoteRunner detectRunner;
        final String pathToDetectJar = intEnvironmentVariables.getValue("DETECT_JAR");

        final String jenkinsVersion = PluginHelper.getJenkinsVersion();
        final String pluginVersion = PluginHelper.getPluginVersion();

        if (StringUtils.isNotBlank(pathToDetectJar)) {
            detectRunner = new DetectRemoteJarRunner(logger, intEnvironmentVariables, remoteWorkspacePath, jenkinsVersion, pluginVersion, remoteJavaHome, pathToDetectJar, detectProperties);
        } else {
            detectRunner = new DetectRemoteScriptRunner(logger, remoteToolsDirectory, remoteWorkspacePath, intEnvironmentVariables, jenkinsVersion, pluginVersion, detectProperties);
        }

        return detectRunner;
    }

}
