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

import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.kohsuke.stapler.DataBoundConstructor;

public class DetectPipelineStep extends AbstractStepImpl {

    @DataBoundConstructor
    public DetectPipelineStep() {

    }

    @Override
    public DetectPipelineStepDescriptor getDescriptor() {
        return (DetectPipelineStepDescriptor) super.getDescriptor();
    }

}
