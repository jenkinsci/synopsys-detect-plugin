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
package com.blackducksoftware.integration.detect.jenkins.pipeline;

import javax.inject.Inject;

import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.jenkinsci.plugins.workflow.steps.StepContextParameter;

import com.blackducksoftware.integration.detect.jenkins.common.DetectCommonStep;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Computer;
import hudson.model.Run;
import hudson.model.TaskListener;

public class DetectPipelineExecution extends AbstractSynchronousNonBlockingStepExecution<Void> {

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
        final DetectCommonStep detectCommonStep = new DetectCommonStep(computer.getNode(), launcher, listener, envVars, workspace, run);
        detectCommonStep.runCommonDetectStep();
        return null;
    }

}
