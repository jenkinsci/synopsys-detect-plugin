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

import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;

public class DetectPostBuildStep extends Recorder {

    @DataBoundConstructor
    public DetectPostBuildStep() {

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
        // TODO call the common step
        return true;
    }

}
