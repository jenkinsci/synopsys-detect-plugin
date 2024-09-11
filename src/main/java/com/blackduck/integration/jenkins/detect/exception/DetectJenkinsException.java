/*
 * blackduck-detect
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.jenkins.detect.exception;

import com.synopsys.integration.exception.IntegrationException;

public class DetectJenkinsException extends IntegrationException {
    private static final long serialVersionUID = -1172941819259598247L;

    public DetectJenkinsException() {
        super();
    }

    public DetectJenkinsException(String message) {
        super(message);
    }

    public DetectJenkinsException(Throwable cause) {
        super(cause);
    }

    public DetectJenkinsException(String message, Throwable cause) {
        super(message, cause);
    }

}