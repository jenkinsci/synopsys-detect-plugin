package com.synopsys.integration.jenkins.detect.substeps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

public class DetectSetupResponseTest {

    private static final String detectRemotePath = "Detect Remote Path";
    private static final String remoteJavaHome = "Remote Java Path";

    @ParameterizedTest
    @ValueSource(strings = { "SHELL_SCRIPT", "POWERSHELL_SCRIPT", "JAR" })
    public void testConstructorNoJava(String executionStrategyString) {
        DetectSetupResponse detectSetupResponse = new DetectSetupResponse(DetectSetupResponse.ExecutionStrategy.valueOf(executionStrategyString), detectRemotePath);
        assertEquals(executionStrategyString, detectSetupResponse.getExecutionStrategy().name());
        assertEquals(detectRemotePath, detectSetupResponse.getDetectRemotePath());
        assertNull(detectSetupResponse.getRemoteJavaHome());
    }

    @ParameterizedTest
    @ValueSource(strings = { "SHELL_SCRIPT", "POWERSHELL_SCRIPT", "JAR" })
    public void testConstructorWithJava(String executionStrategyString) {
        DetectSetupResponse detectSetupResponse = new DetectSetupResponse(DetectSetupResponse.ExecutionStrategy.valueOf(executionStrategyString), remoteJavaHome, detectRemotePath);
        assertEquals(executionStrategyString, detectSetupResponse.getExecutionStrategy().name());
        assertEquals(detectRemotePath, detectSetupResponse.getDetectRemotePath());
        assertEquals(remoteJavaHome, detectSetupResponse.getRemoteJavaHome());
    }

}
