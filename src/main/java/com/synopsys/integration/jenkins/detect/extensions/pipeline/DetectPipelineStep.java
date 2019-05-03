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
package com.synopsys.integration.jenkins.detect.extensions.pipeline;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import com.synopsys.integration.jenkins.detect.JenkinsDetectLogger;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.detect.steps.CreateDetectEnvironmentStep;
import com.synopsys.integration.jenkins.detect.steps.CreateDetectRunnerStep;
import com.synopsys.integration.jenkins.detect.steps.remote.DetectRemoteRunner;
import com.synopsys.integration.jenkins.detect.steps.remote.DetectResponse;
import com.synopsys.integration.jenkins.detect.tools.DummyToolInstaller;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

public class DetectPipelineStep extends Step implements Serializable {
    public static final String DISPLAY_NAME = "Synopsys Detect";
    public static final String PIPELINE_NAME = "synopsys_detect";
    private static final long serialVersionUID = 8126672300843832671L;
    private final String detectProperties;

    @DataBoundConstructor
    public DetectPipelineStep(final String detectProperties) {
        this.detectProperties = detectProperties;
    }

    public String getDetectProperties() {
        return detectProperties;
    }

    @Override
    public StepExecution start(final StepContext context) throws Exception {
        return new Execution(context);
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return new HashSet<>(Arrays.asList(TaskListener.class, EnvVars.class, Computer.class, FilePath.class, Run.class));
        }

        @Override
        public String getFunctionName() {
            return PIPELINE_NAME;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

    }

    public class Execution extends SynchronousNonBlockingStepExecution {
        private static final long serialVersionUID = -5807577350749324767L;
        private transient TaskListener listener;
        private transient EnvVars envVars;
        private transient Computer computer;
        private transient FilePath workspace;

        protected Execution(@Nonnull final StepContext context) throws InterruptedException, IOException {
            super(context);
            listener = context.get(TaskListener.class);
            envVars = context.get(EnvVars.class);
            computer = context.get(Computer.class);
            workspace = context.get(FilePath.class);
        }

        @Override
        protected Integer run() throws Exception {
            final JenkinsDetectLogger logger = new JenkinsDetectLogger(listener);
            final CreateDetectEnvironmentStep createDetectEnvironmentStep = new CreateDetectEnvironmentStep(logger);
            final IntEnvironmentVariables intEnvironmentVariables = createDetectEnvironmentStep.setDetectEnvironment(envVars);

            final CreateDetectRunnerStep createDetectRunnerStep = new CreateDetectRunnerStep(logger);
            final Node node = computer.getNode();
            final String remoteWorkspacePath = workspace.getRemote();
            final String remoteToolsDirectory = new DummyToolInstaller().getToolDir(node).getRemote();
            final DetectRemoteRunner detectRemoteRunner = createDetectRunnerStep.createAppropriateDetectRemoteRunner(intEnvironmentVariables, detectProperties, null, remoteWorkspacePath, remoteToolsDirectory);

            final VirtualChannel caller = node.getChannel();
            final DetectResponse detectResponse = caller.call(detectRemoteRunner);

            if (detectResponse.getExitCode() > 0) {
                logger.error("Detect failed with exit code: " + detectResponse.getExitCode());
            } else if (null != detectResponse.getException()) {
                throw new DetectJenkinsException("Detect encountered an exception", detectResponse.getException());
            }
            return detectResponse.getExitCode();
        }

        @Override
        public void stop(@Nonnull final Throwable cause) throws Exception {
            getContext().onFailure(cause);
            // Interrupt process?
        }

    }

}
