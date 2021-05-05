/*
 * blackduck-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.detect.extensions.postbuild;

import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.synopsys.integration.jenkins.annotations.HelpMarkdown;
import com.synopsys.integration.jenkins.detect.extensions.DetectDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.InheritFromGlobalDownloadStrategy;
import com.synopsys.integration.jenkins.detect.service.DetectCommandsFactory;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

public class DetectPostBuildStep extends Recorder {
    public static final String DISPLAY_NAME = "Synopsys Detect";

    @HelpMarkdown("The command line options to pass to Synopsys Detect")
    private final String detectProperties;

    @Nullable
    private DetectDownloadStrategy downloadStrategyOverride;

    @DataBoundConstructor
    public DetectPostBuildStep(String detectProperties) {
        this.detectProperties = detectProperties;
    }

    public String getDetectProperties() {
        return detectProperties;
    }

    public DetectDownloadStrategy getDownloadStrategyOverride() {
        return downloadStrategyOverride;
    }

    @DataBoundSetter
    public void setDownloadStrategyOverride(DetectDownloadStrategy downloadStrategyOverride) {
        this.downloadStrategyOverride = downloadStrategyOverride;
    }

    public DetectDownloadStrategy getDefaultDownloadStrategyOverride() {
        return new InheritFromGlobalDownloadStrategy();
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    // Freestyle
    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws InterruptedException, IOException {
        DetectCommandsFactory.fromPostBuild(build, launcher, listener)
            .runDetect(detectProperties, downloadStrategyOverride);
        return true;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements Serializable {
        private static final long serialVersionUID = 9059602791947799261L;

        public DescriptorImpl() {
            super(DetectPostBuildStep.class);
            load();
        }

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

    }

}
