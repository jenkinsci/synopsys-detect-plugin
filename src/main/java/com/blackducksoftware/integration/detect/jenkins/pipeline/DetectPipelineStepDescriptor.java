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

import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;

import com.blackducksoftware.integration.detect.jenkins.Messages;

import hudson.Extension;

@Extension(optional = true)
public class DetectPipelineStepDescriptor extends AbstractStepDescriptorImpl {

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
