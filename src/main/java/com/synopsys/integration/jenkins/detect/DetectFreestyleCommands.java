/*
 * blackduck-detect
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.detect;

import com.synopsys.integration.jenkins.detect.extensions.DetectDownloadStrategy;
import com.synopsys.integration.jenkins.service.JenkinsBuildService;

public class DetectFreestyleCommands {
    private final JenkinsBuildService jenkinsBuildService;
    private final DetectRunner detectRunner;

    public DetectFreestyleCommands(JenkinsBuildService jenkinsBuildService, DetectRunner detectRunner) {
        this.jenkinsBuildService = jenkinsBuildService;
        this.detectRunner = detectRunner;
    }

    public void runDetect(String detectArgumentString, DetectDownloadStrategy detectDownloadStrategy) {
        try {
            String remoteJdkHome = jenkinsBuildService.getJDKRemoteHomeOrEmpty().orElse(null);
            int exitCode = detectRunner.runDetect(remoteJdkHome, detectArgumentString, detectDownloadStrategy);
            if (exitCode > 0) {
                jenkinsBuildService.markBuildFailed("Detect failed with exit code " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            jenkinsBuildService.markBuildInterrupted();
        } catch (Exception e) {
            jenkinsBuildService.markBuildFailed(e);
        }
    }

}
