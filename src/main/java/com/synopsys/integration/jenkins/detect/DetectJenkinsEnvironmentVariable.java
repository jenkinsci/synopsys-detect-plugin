/*
 * blackduck-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.detect;

public enum DetectJenkinsEnvironmentVariable {
    USER_PROVIDED_JAR_PATH("DETECT_JAR"),
    SHOULD_ESCAPE("DETECT_PLUGIN_ESCAPING");

    private final String environmentVariable;

    DetectJenkinsEnvironmentVariable(String environmentVariable) {
        this.environmentVariable = environmentVariable;
    }

    public String stringValue() {
        return environmentVariable;
    }

}
