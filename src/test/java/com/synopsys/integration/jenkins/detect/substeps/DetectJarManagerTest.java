package com.synopsys.integration.jenkins.detect.substeps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.log.LogLevel;

import hudson.model.TaskListener;

public class DetectJarManagerTest {

    private final static String detectJarPathInput = "/test/detect/jar/path/detect.jar";
    private final static String javaHomeInput = "/test/java/home/path";
    private final static String javaBinPath = "/bin/java";
    private final static String javaPath = javaHomeInput + javaBinPath;
    private final static String javaPathSystem = System.getProperty("user.dir") + "/ " + javaBinPath;
    private final static String pathEnvVarInput = "/test/path/env";

    private final static Map<String, String> environmentVariables = new HashMap<>();

    private final TaskListener taskListener = Mockito.mock(TaskListener.class);
    private final JenkinsIntLogger jenkinsIntLogger = new JenkinsIntLogger(taskListener);

    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    @Test
    public void testSetUpForExecutionEmptySpaceStringInput() {
        jenkinsIntLogger.setLogLevel(LogLevel.DEBUG);
        this.createAndTestDetectSetupResponse(" ", " ", javaPathSystem);
        this.debugLogLevelValidation(javaPathSystem, " ");
        this.invalidJavaExecValidation();
    }

    @Test
    public void testSetUpForExecutionEmptyStringInput() {
        jenkinsIntLogger.setLogLevel(LogLevel.DEBUG);
        this.createAndTestDetectSetupResponse("", "", javaBinPath);
        this.debugLogLevelValidation(javaBinPath, "");
        this.invalidJavaExecValidation();
    }

    @Test
    public void testSetUpForExecutionNullInput() {
        jenkinsIntLogger.setLogLevel(LogLevel.DEBUG);
        this.createAndTestDetectSetupResponse(null, null, "java");
        this.debugLogLevelValidation("java", null);
        this.validJavaExecValidation();
    }

    @Test
    public void testSetUpForExecutionLogTrace() {
        jenkinsIntLogger.setLogLevel(LogLevel.TRACE);
        this.createAndTestDetectSetupResponse(javaHomeInput, detectJarPathInput, javaPath);
        this.debugLogLevelValidation(javaPath, detectJarPathInput);
        this.invalidJavaExecValidation();
    }

    @Test
    public void testSetUpForExecutionLogDebug() {
        jenkinsIntLogger.setLogLevel(LogLevel.DEBUG);
        this.createAndTestDetectSetupResponse(javaHomeInput, detectJarPathInput, javaPath);
        this.debugLogLevelValidation(javaPath, detectJarPathInput);
        this.invalidJavaExecValidation();
    }

    @Test
    public void testSetUpForExecutionLogInfo() {
        jenkinsIntLogger.setLogLevel(LogLevel.INFO);
        this.createAndTestDetectSetupResponse(javaHomeInput, detectJarPathInput, javaPath);
        this.infoLogLevelValidation(javaPath, detectJarPathInput);
    }

    @Test
    public void testSetUpForExecutionLogWarn() {
        jenkinsIntLogger.setLogLevel(LogLevel.WARN);
        this.createAndTestDetectSetupResponse(javaHomeInput, detectJarPathInput, javaPath);
    }

    @Test
    public void testSetUpForExecutionLogUnset() {
        this.createAndTestDetectSetupResponse("", "", javaBinPath);
        this.infoLogLevelValidation(javaBinPath, "");
    }

    private void createAndTestDetectSetupResponse(String javaHomeInput, String detectJarPathInput, String expectedJavaPath) {
        environmentVariables.put("PATH", pathEnvVarInput);
        Mockito.when(taskListener.getLogger()).thenReturn(new PrintStream(byteArrayOutputStream));

        DetectJarManager detectJarManager = new DetectJarManager(jenkinsIntLogger, javaHomeInput, environmentVariables, detectJarPathInput);
        DetectSetupResponse detectSetupResponse = null;

        try {
            detectSetupResponse = detectJarManager.setUpForExecution();
        } catch (IOException | InterruptedException e) {
            fail("Unexpected exception thrown", e);
        }

        assertEquals(DetectSetupResponse.ExecutionStrategy.JAR, detectSetupResponse.getExecutionStrategy());
        assertEquals(detectJarPathInput, detectSetupResponse.getDetectRemotePath());
        assertEquals(expectedJavaPath, detectSetupResponse.getRemoteJavaHome());
    }

    private void infoLogLevelValidation(String javaPath, String detectJarPathInput) {
        assertTrue(byteArrayOutputStream.toString().contains("Running with JAVA: " + javaPath), "Log does not contain entry for JAVA path.");
        assertTrue(byteArrayOutputStream.toString().contains("Detect configured: " + detectJarPathInput), "Log does not contain entry for Detect path.");
    }

    private void debugLogLevelValidation(String javaPath, String detectJarPathInput) {
        this.infoLogLevelValidation(javaPath, detectJarPathInput);
        assertTrue(byteArrayOutputStream.toString().contains("PATH: " + pathEnvVarInput), "Log does not contain entry for PATH environment variable");
    }

    private void invalidJavaExecValidation() {
        assertTrue(byteArrayOutputStream.toString().contains("Error printing the JAVA version: "), "Log does not contain error for printing Java version.");
    }

    private void validJavaExecValidation() {
        assertTrue(byteArrayOutputStream.toString().contains("Java version: \n"), "Log does not contain entry for Java Version heading.");
    }

}
