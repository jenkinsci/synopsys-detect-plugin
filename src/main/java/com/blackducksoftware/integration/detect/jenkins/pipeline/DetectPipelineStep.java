/**
 * Black Duck Detect Plugin
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
package com.blackducksoftware.integration.detect.jenkins.pipeline;

import javax.inject.Inject;

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;
import org.kohsuke.stapler.DataBoundConstructor;

import com.blackducksoftware.integration.detect.jenkins.Messages;
import com.blackducksoftware.integration.detect.jenkins.common.DetectCommonStep;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Run;
import hudson.model.TaskListener;

public class DetectPipelineStep extends AbstractStepImpl {
    private final String detectProperties;

    @DataBoundConstructor
    public DetectPipelineStep(final String detectProperties) {
        this.detectProperties = detectProperties;
    }

    public String getDetectProperties() {
        return detectProperties;
    }

    @Override
    public DetectPipelineStepDescriptor getDescriptor() {
        return (DetectPipelineStepDescriptor) super.getDescriptor();
    }

    @Extension(optional = true)
    public static final class DetectPipelineStepDescriptor extends AbstractStepDescriptorImpl {
        public DetectPipelineStepDescriptor() {
            super(DetectPipelineExecution.class);
        }

        @Override
        public String getFunctionName() {
            return "hub_detect";
        }

        @Override
        public String getDisplayName() {
            return Messages.DetectPipelineStep_getDisplayName();
        }

    }

    public static final class DetectPipelineExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {
        @Inject
        private transient DetectPipelineStep detectPipelineStep;

        @StepContextParameter
        private transient Computer computer;

        @StepContextParameter
        transient Launcher launcher;

        @StepContextParameter
        transient TaskListener listener;

        @StepContextParameter
        transient EnvVars envVars;

        @StepContextParameter
        private transient FilePath workspace;

        @StepContextParameter
        private transient Run run;

        @Override
        protected Void run() throws Exception {
            final DetectCommonStep detectCommonStep = new DetectCommonStep(computer.getNode(), launcher, listener, envVars, workspace, run, null);
            detectCommonStep.runCommonDetectStep(detectPipelineStep.getDetectProperties());
            return null;
        }

    }

}
