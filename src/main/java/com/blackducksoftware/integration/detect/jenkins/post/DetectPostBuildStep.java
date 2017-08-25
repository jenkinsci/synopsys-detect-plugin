/*
 * Copyright (C) 2017 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Black Duck Software.
 */
package com.blackducksoftware.integration.detect.jenkins.post;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.detect.jenkins.common.DetectCommonStep;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;

public class DetectPostBuildStep extends Recorder {

    @DataBoundConstructor
    public DetectPostBuildStep() {
        // TODO Get User configuration
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public DetectPostBuildStepDescriptor getDescriptor() {
        return (DetectPostBuildStepDescriptor) super.getDescriptor();
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
        final DetectCommonStep detectCommonStep = new DetectCommonStep(build.getBuiltOn(), launcher, listener, build.getEnvironment(listener), getWorkingDirectory(build), build);
        detectCommonStep.runCommonDetectStep();
        return true;
    }

    public FilePath getWorkingDirectory(final AbstractBuild<?, ?> build) throws InterruptedException {
        String workingDirectory = "";
        if (build.getWorkspace() == null) {
            // might be using custom workspace
            workingDirectory = build.getProject().getCustomWorkspace();
        } else {
            workingDirectory = build.getWorkspace().getRemote();
        }
        return new FilePath(build.getBuiltOn().getChannel(), workingDirectory);
    }

}
