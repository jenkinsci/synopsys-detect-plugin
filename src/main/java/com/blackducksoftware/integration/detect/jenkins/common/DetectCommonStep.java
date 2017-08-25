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
package com.blackducksoftware.integration.detect.jenkins.common;

import java.util.Map;

import com.blackducksoftware.integration.detect.jenkins.JenkinsDetectLogger;
import com.blackducksoftware.integration.detect.jenkins.exception.DetectJenkinsException;
import com.blackducksoftware.integration.detect.jenkins.remote.DetectRemoteRunner;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;

public class DetectCommonStep {
    private final Node node;
    private final Launcher launcher;
    private final TaskListener listener;
    private final EnvVars envVars;
    private final FilePath workspace;
    private final Run run;

    public DetectCommonStep(final Node node, final Launcher launcher, final TaskListener listener, final EnvVars envVars, final FilePath workspace, final Run run) {
        this.node = node;
        this.launcher = launcher;
        this.listener = listener;
        this.envVars = envVars;
        this.workspace = workspace;
        this.run = run;
    }

    public void runCommonDetectStep() {
        final JenkinsDetectLogger logger = new JenkinsDetectLogger(listener);
        try {
            final DetectRemoteRunner detectRemoteRunner = new DetectRemoteRunner();
            node.getChannel().call(detectRemoteRunner);
        } catch (final Exception e) {
            logger.error(e.getMessage(), e);
            run.setResult(Result.UNSTABLE);
        }
    }

    public String handleVariableReplacement(final Map<String, String> variables, final String value) throws DetectJenkinsException {
        if (value != null) {
            final String newValue = Util.replaceMacro(value, variables);
            if (newValue.contains("$")) {
                throw new DetectJenkinsException("Variable was not properly replaced. Value : " + value + ", Result : " + newValue + ". Make sure the variable has been properly defined.");
            }
            return newValue;
        } else {
            return null;
        }
    }

}
