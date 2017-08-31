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
package com.blackducksoftware.integration.detect.jenkins.post;

import java.io.IOException;

import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.detect.jenkins.common.DetectCommonStep;

import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.JDK;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Recorder;

public class DetectPostBuildStep extends Recorder {
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
    public DetectPostBuildStepDescriptor getDescriptor() {
        return (DetectPostBuildStepDescriptor) super.getDescriptor();
    }

    @Override
    public boolean perform(final AbstractBuild<?, ?> build, final Launcher launcher, final BuildListener listener) throws InterruptedException, IOException {
        final String javaHome = getJavaExecutable(build, listener);
        final DetectCommonStep detectCommonStep = new DetectCommonStep(build.getBuiltOn(), launcher, listener, build.getEnvironment(listener), getWorkingDirectory(build), build, javaHome);
        detectCommonStep.runCommonDetectStep(detectProperties);
        return true;
    }

    private String getJavaExecutable(final AbstractBuild<?, ?> build, final BuildListener listener) throws IOException, InterruptedException {
        final JDK jdk = build.getProject().getJDK().forNode(build.getBuiltOn(), listener);
        String javaHome = null;
        if (jdk != null) {
            javaHome = jdk.getHome();
        }

        return javaHome;
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
