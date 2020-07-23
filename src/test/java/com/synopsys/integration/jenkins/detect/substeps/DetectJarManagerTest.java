package com.synopsys.integration.jenkins.detect.substeps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.log.LogLevel;

import hudson.model.TaskListener;

public class DetectJarManagerTest {

    private final static String testDetectJarPath = "/test/detect/jar/path/detect.jar";
    private final static String testJavaHome = "/test/java/home/path";
    private final static String testPathEnv = "/test/path/env";

    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(byteArrayOutputStream));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }

    private static Stream<Arguments> setUpForExecutionInput() {
        return Stream.of(
            Arguments.of(null, true, null, testPathEnv, LogLevel.DEBUG),
            Arguments.of(testJavaHome, false, testDetectJarPath, testPathEnv, LogLevel.WARN),
            Arguments.of(testJavaHome, false, testDetectJarPath, testPathEnv, LogLevel.INFO),
            Arguments.of(testJavaHome, false, testDetectJarPath, testPathEnv, LogLevel.DEBUG),
            Arguments.of(testJavaHome, false, testDetectJarPath, testPathEnv, LogLevel.TRACE),
            Arguments.of("", false, "", "", LogLevel.DEBUG),
            Arguments.of(" ", false, " ", " ", LogLevel.DEBUG),
            Arguments.of("", false, "", "", null)
        );
    }

    @ParameterizedTest
    @MethodSource({ "setUpForExecutionInput" })
    public void testSetUpForExecution(String javaHomeInput, Boolean isValidJavaHome, String detectJarPathInput, String pathEnvInput, LogLevel logLevel) {
        Map<String, String> environmentVariables = new HashMap<>();
        environmentVariables.put("PATH", pathEnvInput);

        TaskListener taskListener = Mockito.mock(TaskListener.class);
        Mockito.when(taskListener.getLogger()).thenReturn(new PrintStream(byteArrayOutputStream));
        JenkinsIntLogger jenkinsIntLogger = new JenkinsIntLogger(taskListener);

        if (logLevel != null) {
            jenkinsIntLogger.setLogLevel(logLevel);
        }

        try {
            // Perform same actions as calculateJavaExecutablePath().
            String adjustedJavaHome = javaHomeInput == null ? "java" : new File(javaHomeInput + "/bin/java").getCanonicalPath();

            // Call the method being tested
            DetectJarManager detectJarManager = new DetectJarManager(jenkinsIntLogger, javaHomeInput, environmentVariables, detectJarPathInput);
            DetectSetupResponse detectSetupResponse = detectJarManager.setUpForExecution();

            assertEquals(DetectSetupResponse.ExecutionStrategy.JAR, detectSetupResponse.getExecutionStrategy());
            assertEquals(detectJarPathInput, detectSetupResponse.getDetectRemotePath());
            assertEquals(adjustedJavaHome, detectSetupResponse.getRemoteJavaHome());

            if (jenkinsIntLogger.getLogLevel().isLoggable(LogLevel.INFO)) {
                assertTrue(byteArrayOutputStream.toString().contains("Running with JAVA: " + adjustedJavaHome), "Log does not contain entry for JAVA path.");
                assertTrue(byteArrayOutputStream.toString().contains("Detect configured: " + detectJarPathInput), "Log does not contain entry for Detect path.");
            }

            if (jenkinsIntLogger.getLogLevel().isLoggable(LogLevel.DEBUG)) {
                assertTrue(byteArrayOutputStream.toString().contains("PATH: " + pathEnvInput), "Log does not contain entry for PATH environment variable");

                if (isValidJavaHome) {
                    assertTrue(byteArrayOutputStream.toString().contains("Java version: \n"), "Log does not contain entry for Java Version heading.");
                } else {
                    assertTrue(byteArrayOutputStream.toString().contains("Error printing the JAVA version: "), "Log does not contain error for printing Java version.");
                }

            }

        } catch (IOException | InterruptedException e) {
            fail("Unexpected exception thrown", e);
        }

    }

}
