/**
 * blackduck-detect
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
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
package com.synopsys.integration.jenkins.detect.steps.remote;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.JenkinsDetectLogger;
import com.synopsys.integration.jenkins.detect.tools.DetectDownloadManager;

import hudson.EnvVars;
import hudson.Platform;

public class DetectRemoteScriptRunner extends DetectRemoteRunner {
    private static final long serialVersionUID = -3893076074803560801L;
    private final String toolsDirectory;
    private String detectScriptPath;

    public DetectRemoteScriptRunner(final JenkinsDetectLogger logger, final String toolsDirectory, final String workspacePath, final EnvVars envVars, final String jenkinsVersion, final String pluginVersion,
        final List<String> detectProperties) {
        super(logger, detectProperties, envVars, workspacePath, jenkinsVersion, pluginVersion);
        this.toolsDirectory = toolsDirectory;
    }

    @Override
    protected void setUp() throws IOException, IntegrationException {
        final String scriptUrl;
        if (Platform.current() == Platform.WINDOWS) {
            scriptUrl = DetectDownloadManager.LATEST_POWERSHELL_SCRIPT_URL;
        } else {
            scriptUrl = DetectDownloadManager.LATEST_SHELL_SCRIPT_URL;
        }

        final DetectDownloadManager detectDownloadManager = new DetectDownloadManager(logger, toolsDirectory);

        detectScriptPath = detectDownloadManager.downloadScript(scriptUrl).getCanonicalPath();

        if (StringUtils.isBlank(detectScriptPath)) {
            throw new IntegrationException("[ERROR] The Detect script was not downloaded successfully.");
        }
    }

    @Override
    protected List<String> getInvocationParameters() throws IntegrationException {
        if (Platform.current() == Platform.WINDOWS) {
            return Arrays.asList("powershell", String.format("\"%s | iex; detect\"", detectScriptPath));
        } else {
            return Arrays.asList("bash", detectScriptPath);
        }
    }

}
