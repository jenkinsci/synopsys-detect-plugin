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
import org.kohsuke.stapler.DataBoundSetter;

import com.synopsys.integration.jenkins.annotations.HelpMarkdown;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.detect.substeps.DetectJenkinsSubStepCoordinator;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Run;
import hudson.model.TaskListener;

public class DetectPipelineStep extends Step implements Serializable {
    public static final String DISPLAY_NAME = "Synopsys Detect";
    public static final String PIPELINE_NAME = "synopsys_detect";
    private static final long serialVersionUID = 8126672300843832671L;

    @HelpMarkdown("The command line options to pass to Synopsys Detect")
    private final String detectProperties;

    @HelpMarkdown("If true (checked), returns the status code of the Detect run instead of throwing an exception")
    private boolean returnStatus = false;

    @DataBoundConstructor
    public DetectPipelineStep(final String detectProperties) {
        this.detectProperties = detectProperties;
    }

    public String getDetectProperties() {
        return detectProperties;
    }

    public boolean getReturnStatus() {
        return returnStatus;
    }

    @DataBoundSetter
    public void setReturnStatus(final boolean returnStatus) {
        this.returnStatus = returnStatus;
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
        private transient FilePath workspace;
        private transient Launcher launcher;

        protected Execution(@Nonnull final StepContext context) throws InterruptedException, IOException {
            super(context);
            listener = context.get(TaskListener.class);
            envVars = context.get(EnvVars.class);
            workspace = context.get(FilePath.class);
            launcher = context.get(Launcher.class);
        }

        @Override
        protected Integer run() throws Exception {
            final JenkinsIntLogger logger = new JenkinsIntLogger(listener);

            final DetectJenkinsSubStepCoordinator detectJenkinsSubStepCoordinator = new DetectJenkinsSubStepCoordinator(logger, workspace, envVars, null, launcher, listener, detectProperties);
            final int exitCode = detectJenkinsSubStepCoordinator.runDetect();

            if (exitCode > 0) {
                final String errorMsg = "Detect failed with exit code " + exitCode;
                if (returnStatus) {
                    logger.error(errorMsg);
                } else {
                    throw new DetectJenkinsException(errorMsg);
                }
            }

            return exitCode;
        }

    }

}
