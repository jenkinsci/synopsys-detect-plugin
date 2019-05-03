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
package com.synopsys.integration.jenkins.detect.extensions.postbuild;

import java.io.IOException;
import java.io.Serializable;
import java.util.function.Consumer;

import javax.annotation.Nonnull;

import org.kohsuke.stapler.DataBoundConstructor;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.JenkinsDetectLogger;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.detect.steps.CreateDetectEnvironmentStep;
import com.synopsys.integration.jenkins.detect.steps.CreateDetectRunnerStep;
import com.synopsys.integration.jenkins.detect.steps.remote.DetectRemoteRunner;
import com.synopsys.integration.jenkins.detect.steps.remote.DetectResponse;
import com.synopsys.integration.jenkins.detect.tools.DummyToolInstaller;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Result;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;

public class DetectPostBuildStep extends Recorder {
    public static final String DISPLAY_NAME = "Synopsys Detect";
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
        final JenkinsDetectLogger logger = new JenkinsDetectLogger(listener);

        try {
            final CreateDetectEnvironmentStep createDetectEnvironmentStep = new CreateDetectEnvironmentStep(logger);
            final IntEnvironmentVariables intEnvironmentVariables = createDetectEnvironmentStep.setDetectEnvironment(build.getEnvironment(listener));

            final CreateDetectRunnerStep createDetectRunnerStep = new CreateDetectRunnerStep(logger);
            final Node node = build.getBuiltOn();
            final String javaHome = getJavaHome(build, node, listener);
            final String remoteWorkspacePath = build.getWorkspace().getRemote();
            final String remoteToolsDirectory = new DummyToolInstaller().getToolDir(node).getRemote();
            final DetectRemoteRunner detectRemoteRunner = createDetectRunnerStep.createAppropriateDetectRemoteRunner(intEnvironmentVariables, detectProperties, javaHome, remoteWorkspacePath, remoteToolsDirectory);

            final VirtualChannel caller = node.getChannel();
            final DetectResponse detectResponse = caller.call(detectRemoteRunner);

            if (detectResponse.getExitCode() > 0) {
                logger.error("Detect failed with exit code: " + detectResponse.getExitCode());
                build.setResult(Result.FAILURE);
            } else if (null != detectResponse.getException()) {
                throw new DetectJenkinsException("Detect encountered an exception", detectResponse.getException());
            }
        } catch (final Exception e) {
            setBuildStatusFromException(logger, e, build::setResult);
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

    private void setBuildStatusFromException(final JenkinsDetectLogger logger, final Exception exception, final Consumer<Result> resultConsumer) {
        if (exception instanceof InterruptedException) {
            logger.error("Detect thread was interrupted", exception);
            resultConsumer.accept(Result.ABORTED);
            Thread.currentThread().interrupt();
        } else if (exception instanceof IntegrationException) {
            logger.error(exception.getMessage());
            logger.debug(exception.getMessage(), exception);
            resultConsumer.accept(Result.UNSTABLE);
        } else {
            logger.error(exception.getMessage(), exception);
            resultConsumer.accept(Result.UNSTABLE);
        }
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
