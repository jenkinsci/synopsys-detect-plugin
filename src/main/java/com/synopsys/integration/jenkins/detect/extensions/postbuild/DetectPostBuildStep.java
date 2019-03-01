/**
 * synopsys-detect
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
package com.synopsys.integration.jenkins.detect.extensions.postbuild;

import java.io.IOException;
import java.io.Serializable;

import javax.annotation.Nonnull;

import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import com.synopsys.integration.jenkins.detect.steps.ExecuteDetectStep;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.JDK;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.tasks.SimpleBuildStep;

public class DetectPostBuildStep extends Recorder implements SimpleBuildStep {
    public static final String DISPLAY_NAME = "Synopsys Detect";
    public static final String PIPELINE_NAME = "synopsys_detect";
    private final String detectProperties;

    @DataBoundConstructor
    public DetectPostBuildStep(final String detectProperties) {
        this.detectProperties = detectProperties;
    }

    public String getDetectProperties() {
        return detectProperties;
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
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
        final String javaHome = getJavaHome(build, listener);
        final ExecuteDetectStep executeDetectStep = new ExecuteDetectStep(build.getBuiltOn(), listener, build.getWorkspace(), build.getEnvironment(listener), build, javaHome);
        executeDetectStep.executeDetect(detectProperties);
        return true;
    }

    // Pipeline
    @Override
    public void perform(@Nonnull final Run<?, ?> run, @Nonnull final FilePath workspace, @Nonnull final Launcher launcher, @Nonnull final TaskListener listener) throws InterruptedException, IOException {
        final ExecuteDetectStep executeDetectStep = new ExecuteDetectStep(workspace.toComputer().getNode(), listener, workspace, run.getEnvironment(listener), run, null);
        executeDetectStep.executeDetect(detectProperties);
    }

    private String getJavaHome(final AbstractBuild<?, ?> build, final BuildListener listener) throws IOException, InterruptedException {
        JDK jdk = build.getProject().getJDK();
        if (jdk == null) {
            return null;
        }
        jdk = build.getProject().getJDK().forNode(build.getBuiltOn(), listener);

        return jdk.getHome();
    }

    @Symbol(PIPELINE_NAME)
    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements Serializable {
        private static final long serialVersionUID = 9059602791947799261L;

        public DescriptorImpl() {
            super(DetectPostBuildStep.class);
            load();
        }

        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
            return true;
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

    }

}
