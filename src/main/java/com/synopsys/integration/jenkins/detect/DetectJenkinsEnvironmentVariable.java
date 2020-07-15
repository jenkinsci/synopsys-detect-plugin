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
