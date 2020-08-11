package com.synopsys.integration.jenkins.detect.service.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.model.TaskListener;
import jenkins.security.MasterToSlaveCallable;

public class DetectJarStrategyTest {
    private static final String detectJarPath = "/test/detect/jar/path/detect.jar";
    private static final String remoteJdkHome = "/test/java/home/path";
    private static final String expectedJdkJavaPath = remoteJdkHome + "/bin/java";
    private static final String expectedPath = "/test/path/env";

    private IntEnvironmentVariables environmentVariables;
    private JenkinsIntLogger logger;
    private ByteArrayOutputStream byteArrayOutputStream;

    public static Stream<Arguments> testSetupCallableJavaHomeSource() {
        return Stream.of(Arguments.of(" ", System.getProperty("user.dir") + "/ /bin/java"),
            Arguments.of("", "/bin/java"),
            Arguments.of(null, "java"),
            Arguments.of(remoteJdkHome, expectedJdkJavaPath));
    }

    @BeforeEach
    public void setup() {
        environmentVariables = new IntEnvironmentVariables(false);
        environmentVariables.put("PATH", expectedPath);

        TaskListener taskListener = Mockito.mock(TaskListener.class);
        byteArrayOutputStream = new ByteArrayOutputStream();
        Mockito.when(taskListener.getLogger()).thenReturn(new PrintStream(byteArrayOutputStream));

        logger = new JenkinsIntLogger(taskListener);
    }

    @Test
    public void testArgumentEscaper() {
        DetectJarStrategy detectJarStrategy = new DetectJarStrategy(logger, environmentVariables, remoteJdkHome, detectJarPath);
        assertEquals(Function.identity(), detectJarStrategy.getArgumentEscaper());
    }

    @Test
    public void testInvocationParameters() {
        DetectJarStrategy detectJarStrategy = new DetectJarStrategy(logger, environmentVariables, remoteJdkHome, detectJarPath);
        assertEquals(Arrays.asList(expectedJdkJavaPath, "-jar", detectJarPath), detectJarStrategy.getInitialArguments(expectedJdkJavaPath));
    }

    @ParameterizedTest
    @MethodSource("testSetupCallableJavaHomeSource")
    public void testSetupCallableJavaHome(String javaHome, String expectedJavaPath) {
        this.executeAndValidateSetupCallable(javaHome, expectedJavaPath);
    }

    @Test
    public void testSetupCallableWarnLogging() {
        logger.setLogLevel(LogLevel.WARN);
        this.executeAndValidateSetupCallable(remoteJdkHome, expectedJdkJavaPath);
        this.validateLogsNotPresentInfo(expectedJdkJavaPath);
        this.validateLogsNotPresentDebug();
    }

    @Test
    public void testSetupCallableInfoLogging() {
        logger.setLogLevel(LogLevel.INFO);
        this.executeAndValidateSetupCallable(remoteJdkHome, expectedJdkJavaPath);
        this.validateLogsPresentInfo(expectedJdkJavaPath);
        this.validateLogsNotPresentDebug();
    }

    @Test
    public void testSetupCallableDebugLogging() {
        logger.setLogLevel(LogLevel.DEBUG);
        this.executeAndValidateSetupCallable(remoteJdkHome, expectedJdkJavaPath);
        this.validateLogsPresentInfo(expectedJdkJavaPath);
        this.validateLogsPresentDebug();
    }

    @Test
    public void testSetupCallableTraceLogging() {
        logger.setLogLevel(LogLevel.TRACE);
        this.executeAndValidateSetupCallable(remoteJdkHome, expectedJdkJavaPath);
        this.validateLogsPresentInfo(expectedJdkJavaPath);
        this.validateLogsPresentDebug();
    }

    @Test
    public void testSetupCallableDebugLoggingJavaVersionFailed() {
        logger.setLogLevel(LogLevel.DEBUG);
        try {
            String badJavaHome = Files.createTempDirectory(null).toRealPath().toString();
            String expectedBadJavaPath = badJavaHome + "/bin/java";
            this.executeAndValidateSetupCallable(badJavaHome, expectedBadJavaPath);

            assertTrue(byteArrayOutputStream.toString().contains("Error printing the JAVA version: "), "Log does not contain error for printing Java version.");
        } catch (IOException e) {
            fail("Unexpected exception was thrown in test code: ", e);
        }
    }

    @Test
    public void testSetupCallableDebugLoggingJavaVersionSuccess() {
        logger.setLogLevel(LogLevel.DEBUG);
        this.executeAndValidateSetupCallable(null, "java");

        assertTrue(byteArrayOutputStream.toString().contains("Java version: \n"), "Log does not contain entry for Java Version heading.");
    }

    private void executeAndValidateSetupCallable(String javaHomeInput, String expectedJavaPath) {
        try {
            DetectJarStrategy detectJarStrategy = new DetectJarStrategy(logger, environmentVariables, javaHomeInput, detectJarPath);
            MasterToSlaveCallable<String, IntegrationException> setupCallable = detectJarStrategy.getSetupCallable();

            String actualJavaPath = setupCallable.call();
            assertEquals(expectedJavaPath, actualJavaPath);
        } catch (IntegrationException e) {
            fail("An unexpected exception occurred: ", e);
        }
    }

    private void validateLogsNotPresentInfo(String javaPath) {
        assertFalse(byteArrayOutputStream.toString().contains("Running with JAVA: " + javaPath), "Log contains entry for JAVA path and shouldn't.");
        assertFalse(byteArrayOutputStream.toString().contains("Detect configured: " + detectJarPath), "Log contains entry for Detect path and shouldn't.");
    }

    private void validateLogsNotPresentDebug() {
        assertFalse(byteArrayOutputStream.toString().contains("PATH: " + expectedPath), "Log contains entry for PATH environment variable and shouldn't.");
    }

    private void validateLogsPresentInfo(String javaPath) {
        assertTrue(byteArrayOutputStream.toString().contains("Running with JAVA: " + javaPath), "Log does not contain entry for JAVA path.");
        assertTrue(byteArrayOutputStream.toString().contains("Detect configured: " + detectJarPath), "Log does not contain entry for Detect path.");
    }

    private void validateLogsPresentDebug() {
        assertTrue(byteArrayOutputStream.toString().contains("PATH: " + expectedPath), "Log does not contain entry for PATH environment variable.");
    }

}
