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
package com.blackducksoftware.integration.detect.jenkins.tools;

import java.io.File;
import java.io.IOException;

import com.blackducksoftware.integration.detect.rest.github.GitHubRequestService;
import com.blackducksoftware.integration.exception.IntegrationException;
import com.blackducksoftware.integration.log.IntLogger;

public class DetectDownloadManager {
    public static final String DETECT_INSTALL_DIRECTORY = "Detect_Installation";
    private final IntLogger logger;
    private final String toolsDirectory;

    public DetectDownloadManager(final IntLogger logger, final String toolsDirectory) {
        this.logger = logger;
        this.toolsDirectory = toolsDirectory;
    }

    public File handleDownload(final String fileUrl) throws IntegrationException, IOException {
        if (shouldInstallDetect()) {
            final GitHubRequestService gitHubRequestService = new GitHubRequestService();
            // TODO handle caching
            // TODO handle using the default
            // TODO put the file in the tools directory
            return gitHubRequestService.downloadFile(fileUrl, getDetectFile());
        }
        return null;
    }

    public File getDetectFile() {

        return null;
    }

    public File getInstallationDirectory() {

        return null;
    }

    public boolean shouldInstallDetect() {

        return true;
    }

}
