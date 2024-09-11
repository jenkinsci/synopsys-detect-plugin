package com.blackduck.integration.jenkins.detect.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.blackduck.integration.jenkins.detect.service.strategy.RemoteJavaService;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.model.TaskListener;

public class RemoteJavaServiceTest {
    private static final String JAVA_EXECUTABLE_BIN = (SystemUtils.IS_OS_WINDOWS) ? "\\bin\\java.exe" : "/bin/java";

    public String testRemoteJdkHome = "/test/remote/jdk/home";
    public String expectedTestRemoteJdkHome = testRemoteJdkHome + JAVA_EXECUTABLE_BIN;
    public String expectedDetectJavaPath = "/test/detect/java/path";
    public String testJavaPath = "/test/java/path";
    public String expectedTestJavaPath = testJavaPath + JAVA_EXECUTABLE_BIN;

    private final IntEnvironmentVariables environmentVariables = IntEnvironmentVariables.empty();
    private JenkinsIntLogger logger;
    private ByteArrayOutputStream byteArrayOutputStream;

    @BeforeEach
    public void setup() {
        TaskListener taskListener = Mockito.mock(TaskListener.class);
        byteArrayOutputStream = new ByteArrayOutputStream();
        Mockito.when(taskListener.getLogger()).thenReturn(new PrintStream(byteArrayOutputStream));

        logger = JenkinsIntLogger.logToListener(taskListener);
        logger.setLogLevel(LogLevel.DEBUG);

        if (SystemUtils.IS_OS_WINDOWS) {
            testRemoteJdkHome = "T:\\test\\remote\\jdk\\home";
            expectedTestRemoteJdkHome = testRemoteJdkHome + JAVA_EXECUTABLE_BIN;
            expectedDetectJavaPath = "T:\\test\\detect\\java\\path";
            testJavaPath = "T:\\test\\java\\path";
            expectedTestJavaPath = testJavaPath + JAVA_EXECUTABLE_BIN;
        }
    }

    @Test
    public void testRemoteJdkHomeSet() {
        RemoteJavaService remoteJavaService = new RemoteJavaService(logger, testRemoteJdkHome, environmentVariables.getVariables());

        assertEquals(expectedTestRemoteJdkHome, remoteJavaService.getJavaExecutablePath(), "Could not set Java path by using " + testRemoteJdkHome);
        assertTrue(byteArrayOutputStream.toString().contains(expectedTestRemoteJdkHome), "Log message does not contain correct Java path.");
        assertTrue(byteArrayOutputStream.toString().contains("Node environment"), "Log message does not contain correct 'based on' message.");
    }

    @Test
    public void testDetectJavaPathSet() {
        environmentVariables.put(RemoteJavaService.DETECT_JAVA_PATH, expectedDetectJavaPath);
        RemoteJavaService remoteJavaService = new RemoteJavaService(logger, null, environmentVariables.getVariables());

        assertEquals(expectedDetectJavaPath, remoteJavaService.getJavaExecutablePath(), "Could not set Java path by using " + RemoteJavaService.DETECT_JAVA_PATH);
        assertTrue(byteArrayOutputStream.toString().contains(expectedDetectJavaPath), "Log message does not contain correct Java path.");
        assertTrue(byteArrayOutputStream.toString().contains("DETECT_JAVA_PATH environment variable"), "Log message does not contain correct 'based on' message.");
    }

    @Test
    public void testJavaPathSet() {
        environmentVariables.put(RemoteJavaService.JAVA_HOME, testJavaPath);
        RemoteJavaService remoteJavaService = new RemoteJavaService(logger, null, environmentVariables.getVariables());

        assertEquals(expectedTestJavaPath, remoteJavaService.getJavaExecutablePath(), "Could not set Java path by using " + RemoteJavaService.JAVA_HOME);
        assertTrue(byteArrayOutputStream.toString().contains(expectedTestJavaPath), "Log message does not contain correct Java path.");
        assertTrue(byteArrayOutputStream.toString().contains("JAVA_HOME environment variable"), "Log message does not contain correct 'based on' message.");
    }

    @Test
    public void testContainAllOptions() {
        environmentVariables.put(RemoteJavaService.DETECT_JAVA_PATH, expectedDetectJavaPath);
        environmentVariables.put(RemoteJavaService.JAVA_HOME, testJavaPath);
        RemoteJavaService remoteJavaService = new RemoteJavaService(logger, testRemoteJdkHome, environmentVariables.getVariables());

        assertEquals(expectedTestRemoteJdkHome, remoteJavaService.getJavaExecutablePath(), "Could not set Java path by using " + testRemoteJdkHome);
        assertTrue(byteArrayOutputStream.toString().contains(expectedTestRemoteJdkHome), "Log message does not contain correct Java path.");
        assertTrue(byteArrayOutputStream.toString().contains("Node environment"), "Log message does not contain correct 'based on' message.");
    }

    @Test
    public void testContainBothEnvVars() {
        environmentVariables.put(RemoteJavaService.DETECT_JAVA_PATH, expectedDetectJavaPath);
        environmentVariables.put(RemoteJavaService.JAVA_HOME, testJavaPath);
        RemoteJavaService remoteJavaService = new RemoteJavaService(logger, null, environmentVariables.getVariables());

        assertEquals(expectedDetectJavaPath, remoteJavaService.getJavaExecutablePath(), "Could not set Java path by using " + RemoteJavaService.DETECT_JAVA_PATH);
        assertTrue(byteArrayOutputStream.toString().contains(expectedDetectJavaPath), "Log message does not contain correct Java path.");
        assertTrue(byteArrayOutputStream.toString().contains("DETECT_JAVA_PATH environment variable"), "Log message does not contain correct 'based on' message.");
    }
}
