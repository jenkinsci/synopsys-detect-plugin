/**
 * blackduck-detect
 *
 * Copyright (c) 2020 Synopsys, Inc.
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

import org.kohsuke.stapler.DataBoundConstructor;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.annotations.HelpMarkdown;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.detect.substeps.DetectJenkinsSubStepCoordinator;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Result;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

public class DetectPostBuildStep extends Recorder {
    public static final String DISPLAY_NAME = "Synopsys Detect";

    @HelpMarkdown("The command line options to pass to Synopsys Detect")
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
        final JenkinsIntLogger logger = new JenkinsIntLogger(listener);

        try {
            final FilePath workspace = build.getWorkspace();
            if (workspace == null) {
                throw new DetectJenkinsException("Detect cannot be executed when the workspace is null");
            }

            final Node node = build.getBuiltOn();
            final EnvVars envVars = build.getEnvironment(listener);

            final DetectJenkinsSubStepCoordinator detectJenkinsSubStepCoordinator = new DetectJenkinsSubStepCoordinator(logger, workspace, envVars, getJavaHome(build, node, listener), launcher, listener, detectProperties);
            final int exitCode = detectJenkinsSubStepCoordinator.runDetect();

            if (exitCode > 0) {
                logger.error("Detect failed with exit code " + exitCode);
                build.setResult(Result.FAILURE);
            }
        } catch (final Exception e) {
            if (e instanceof InterruptedException) {
                logger.error("Detect thread was interrupted", e);
                build.setResult(Result.ABORTED);
                Thread.currentThread().interrupt();
            } else if (e instanceof IntegrationException) {
                logger.error(e.getMessage());
                logger.debug(e.getMessage(), e);
                build.setResult(Result.UNSTABLE);
            } else {
                logger.error(e.getMessage(), e);
                build.setResult(Result.UNSTABLE);
            }
        }
        return true;
    }

    private String getJavaHome(final AbstractBuild<?, ?> build, final Node node, final BuildListener listener) throws IOException, InterruptedException {
        final JDK jdk = build.getProject().getJDK();
        if (jdk == null) {
            return null;
        }
        final JDK nodeJdk = jdk.forNode(node, listener);

        return nodeJdk.getHome();
    }

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
