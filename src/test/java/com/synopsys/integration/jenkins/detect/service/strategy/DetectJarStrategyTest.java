package com.synopsys.integration.jenkins.detect.service.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.commons.lang3.SystemUtils;
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
    private static final String JAVA_EXECUTABLE = (SystemUtils.IS_OS_WINDOWS) ? "java.exe" : "java";
    private static final String REMOTE_JDK_HOME = new File("/test/java/home/path").getAbsolutePath();
    private static final String REMOTE_JAVA_RELATIVE_PATH = new File("/bin/" + JAVA_EXECUTABLE).getPath();
    private static final String EXPECTED_PATH = new File("/test/path/env").getAbsolutePath();
    private static final String DETECT_JAR_PATH = new File("/test/detect/jar/path/detect.jar").getAbsolutePath();
    private static final String EXPECTED_JAVA_FULL_PATH = new File(REMOTE_JDK_HOME + REMOTE_JAVA_RELATIVE_PATH).getAbsolutePath();

    private IntEnvironmentVariables environmentVariables;
    private JenkinsIntLogger logger;
    private ByteArrayOutputStream byteArrayOutputStream;

    public static Stream<Arguments> testSetupCallableJavaHomeSource() {
        return Stream.of(Arguments.of(" ", System.getProperty("user.dir") + File.separator + " " + REMOTE_JAVA_RELATIVE_PATH),
            Arguments.of("", new File(REMOTE_JAVA_RELATIVE_PATH).getAbsolutePath()),
            Arguments.of(null, "java"),
            Arguments.of(REMOTE_JDK_HOME, EXPECTED_JAVA_FULL_PATH));
    }

    @BeforeEach
    public void setup() {
        environmentVariables = IntEnvironmentVariables.empty();
        environmentVariables.put("PATH", EXPECTED_PATH);

        TaskListener taskListener = Mockito.mock(TaskListener.class);
        byteArrayOutputStream = new ByteArrayOutputStream();
        Mockito.when(taskListener.getLogger()).thenReturn(new PrintStream(byteArrayOutputStream));

        logger = new JenkinsIntLogger(taskListener);
    }

    @Test
    public void testArgumentEscaper() {
        DetectJarStrategy detectJarStrategy = new DetectJarStrategy(logger, environmentVariables, REMOTE_JDK_HOME, DETECT_JAR_PATH);
        assertEquals(Function.identity(), detectJarStrategy.getArgumentEscaper());
    }

    @ParameterizedTest
    @MethodSource("testSetupCallableJavaHomeSource")
    public void testSetupCallableJavaHome(String javaHome, String expectedJavaPath) {
        this.executeAndValidateSetupCallable(javaHome, expectedJavaPath);
        this.validateLogsPresentInfo();
    }

    @Test
    public void testSetupCallableInvalidJdkHome() {
        this.executeAndValidateSetupCallable("\u0000", "java");
        assertTrue(byteArrayOutputStream.toString().contains("Detect could not get Java Home from configured JDK,"), "Log does not contain message from IOException.");
        this.validateLogsPresentInfo();
    }

    @Test
    public void testSetupCallableWarnLogging() {
        logger.setLogLevel(LogLevel.WARN);
        this.executeAndValidateSetupCallable(REMOTE_JDK_HOME, EXPECTED_JAVA_FULL_PATH);
        this.validateLogsNotPresentInfo();
        this.validateLogsNotPresentDebug();
    }

    @Test
    public void testSetupCallableInfoLogging() {
        logger.setLogLevel(LogLevel.INFO);
        this.executeAndValidateSetupCallable(REMOTE_JDK_HOME, EXPECTED_JAVA_FULL_PATH);
        this.validateLogsPresentInfo();
        this.validateLogsNotPresentDebug();
    }

    @Test
    public void testSetupCallableDebugLogging() {
        logger.setLogLevel(LogLevel.DEBUG);
        this.executeAndValidateSetupCallable(REMOTE_JDK_HOME, EXPECTED_JAVA_FULL_PATH);
        this.validateLogsPresentInfo();
        this.validateLogsPresentDebug();
    }

    @Test
    public void testSetupCallableTraceLogging() {
        logger.setLogLevel(LogLevel.TRACE);
        this.executeAndValidateSetupCallable(REMOTE_JDK_HOME, EXPECTED_JAVA_FULL_PATH);
        this.validateLogsPresentInfo();
        this.validateLogsPresentDebug();
    }

    @Test
    public void testSetupCallableDebugLoggingJavaVersionFailed() {
        logger.setLogLevel(LogLevel.DEBUG);
        try {
            String badJavaHome = Files.createTempDirectory(null).toRealPath().toString();
            String expectedBadJavaPath = badJavaHome + REMOTE_JAVA_RELATIVE_PATH;
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

        assertTrue(byteArrayOutputStream.toString().contains("Java version: "), "Log does not contain entry for Java Version heading.");
    }

    private void executeAndValidateSetupCallable(String javaHomeInput, String expectedJavaPath) {
        try {
            DetectJarStrategy detectJarStrategy = new DetectJarStrategy(logger, environmentVariables, javaHomeInput, DETECT_JAR_PATH);
            MasterToSlaveCallable<ArrayList<String>, IntegrationException> setupCallable = detectJarStrategy.getSetupCallable();
            ArrayList<String> jarExecutionElements = setupCallable.call();
            String resolvedExpectedJavaPath = resolveDirectory(expectedJavaPath);

            assertEquals(resolvedExpectedJavaPath, jarExecutionElements.get(0));
            assertEquals("-jar", jarExecutionElements.get(1));
            assertEquals(DETECT_JAR_PATH, jarExecutionElements.get(2));
        } catch (IntegrationException e) {
            fail("An unexpected exception occurred: ", e);
        }
    }

    private String resolveDirectory(String inputDirectory) {
        try {
            return Paths.get(inputDirectory).toRealPath().toString();
        } catch (IOException e) {
            return inputDirectory;
        }
    }

    private void validateLogsNotPresentInfo() {
        assertFalse(byteArrayOutputStream.toString().contains("Running with JAVA: "), "Log contains entry for JAVA path and shouldn't.");
        assertFalse(byteArrayOutputStream.toString().contains("Detect jar configured: "), "Log contains entry for Detect path and shouldn't.");
    }

    private void validateLogsNotPresentDebug() {
        assertFalse(byteArrayOutputStream.toString().contains("PATH: "), "Log contains entry for PATH environment variable and shouldn't.");
    }

    private void validateLogsPresentInfo() {
        assertTrue(byteArrayOutputStream.toString().contains("Running with JAVA: "), "Log does not contain entry for JAVA path.");
        assertTrue(byteArrayOutputStream.toString().contains("Detect jar configured: "), "Log does not contain entry for Detect path.");
    }

    private void validateLogsPresentDebug() {
        assertTrue(byteArrayOutputStream.toString().contains("PATH: "), "Log does not contain entry for PATH environment variable.");
    }

}
