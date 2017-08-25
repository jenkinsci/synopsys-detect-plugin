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
package com.blackducksoftware.integration.detect.jenkins.post;

import java.io.Serializable;

import com.blackducksoftware.integration.detect.jenkins.Messages;

import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Publisher;

public class DetectPostBuildStepDescriptor extends BuildStepDescriptor<Publisher> implements Serializable {

    @Override
    public boolean isApplicable(final Class<? extends AbstractProject> jobType) {
        return true;
    }

    @Override
    public String getDisplayName() {
        return Messages.DetectPostBuildStep_getDisplayName();
    }

}
