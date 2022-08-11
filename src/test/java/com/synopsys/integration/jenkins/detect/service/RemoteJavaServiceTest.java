package com.synopsys.integration.jenkins.detect.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.jenkins.detect.service.strategy.RemoteJavaService;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.model.TaskListener;

public class RemoteJavaServiceTest {

    private JenkinsIntLogger logger;
    private ByteArrayOutputStream byteArrayOutputStream;

    @BeforeEach
    public void setup() {
        TaskListener taskListener = Mockito.mock(TaskListener.class);
        byteArrayOutputStream = new ByteArrayOutputStream();
        Mockito.when(taskListener.getLogger()).thenReturn(new PrintStream(byteArrayOutputStream));

        logger = JenkinsIntLogger.logToListener(taskListener);
        logger.setLogLevel(LogLevel.DEBUG);
    }

    @Test
    public void testRemoteJdkHome() {
        String remoteJdkHome = "/test/remote/jdk/home";
        String expectedJavaExecutablePath = remoteJdkHome + "/bin/java";

        IntEnvironmentVariables environmentVariables = IntEnvironmentVariables.empty();
        RemoteJavaService remoteJavaService = new RemoteJavaService(logger, remoteJdkHome, environmentVariables.getVariables());

        assertEquals(expectedJavaExecutablePath, remoteJavaService.getJavaExecutablePath(), "Could not set Java path by using " + remoteJdkHome);
        assertTrue(byteArrayOutputStream.toString().contains(expectedJavaExecutablePath), "Log message does not contain correct Java path.");
        assertTrue(byteArrayOutputStream.toString().contains("Node environment"), "Log message does not contain correct 'based on' message.");
    }

    @Test
    public void testDetectJavaPath() {
        String detectJavaPath = "/test/detect/java/path";

        IntEnvironmentVariables environmentVariables = IntEnvironmentVariables.empty();
        environmentVariables.put(RemoteJavaService.DETECT_JAVA_PATH, detectJavaPath);
        RemoteJavaService remoteJavaService = new RemoteJavaService(logger, null, environmentVariables.getVariables());

        assertEquals(detectJavaPath, remoteJavaService.getJavaExecutablePath(), "Could not set Java path by using " + RemoteJavaService.DETECT_JAVA_PATH);
        assertTrue(byteArrayOutputStream.toString().contains(detectJavaPath), "Log message does not contain correct Java path.");
        assertTrue(byteArrayOutputStream.toString().contains("DETECT_JAVA_PATH environment variable"), "Log message does not contain correct 'based on' message.");
    }

    @Test
    public void testJavaPath() {
        String javaPath = "/test/java/path";
        String expectedJavaExecutablePath = javaPath + "/bin/java";

        IntEnvironmentVariables environmentVariables = IntEnvironmentVariables.empty();
        environmentVariables.put(RemoteJavaService.JAVA_HOME, javaPath);
        RemoteJavaService remoteJavaService = new RemoteJavaService(logger, null, environmentVariables.getVariables());

        assertEquals(expectedJavaExecutablePath, remoteJavaService.getJavaExecutablePath(), "Could not set Java path by using " + RemoteJavaService.JAVA_HOME);
        assertTrue(byteArrayOutputStream.toString().contains(expectedJavaExecutablePath), "Log message does not contain correct Java path.");
        assertTrue(byteArrayOutputStream.toString().contains("JAVA_HOME environment variable"), "Log message does not contain correct 'based on' message.");
    }

    @Test
    public void testContainAllOptions() {
        String remoteJdkHome = "/test/remote/jdk/home";
        String expectedJavaExecutablePath = remoteJdkHome + "/bin/java";
        String detectJavaPath = "/test/detect/java/path";
        String javaPath = "/test/java/path";

        IntEnvironmentVariables environmentVariables = IntEnvironmentVariables.empty();
        environmentVariables.put(RemoteJavaService.DETECT_JAVA_PATH, detectJavaPath);
        environmentVariables.put(RemoteJavaService.JAVA_HOME, javaPath);
        RemoteJavaService remoteJavaService = new RemoteJavaService(logger, remoteJdkHome, environmentVariables.getVariables());

        assertEquals(expectedJavaExecutablePath, remoteJavaService.getJavaExecutablePath(), "Could not set Java path by using " + remoteJdkHome);
        assertTrue(byteArrayOutputStream.toString().contains(expectedJavaExecutablePath), "Log message does not contain correct Java path.");
        assertTrue(byteArrayOutputStream.toString().contains("Node environment"), "Log message does not contain correct 'based on' message.");
    }

    @Test
    public void testContainBothEnvVars() {
        String detectJavaPath = "/test/detect/java/path";
        String javaPath = "/test/java/path";

        IntEnvironmentVariables environmentVariables = IntEnvironmentVariables.empty();
        environmentVariables.put(RemoteJavaService.DETECT_JAVA_PATH, detectJavaPath);
        environmentVariables.put(RemoteJavaService.JAVA_HOME, javaPath);
        RemoteJavaService remoteJavaService = new RemoteJavaService(logger, null, environmentVariables.getVariables());

        assertEquals(detectJavaPath, remoteJavaService.getJavaExecutablePath(), "Could not set Java path by using " + RemoteJavaService.DETECT_JAVA_PATH);
        assertTrue(byteArrayOutputStream.toString().contains(detectJavaPath), "Log message does not contain correct Java path.");
        assertTrue(byteArrayOutputStream.toString().contains("DETECT_JAVA_PATH environment variable"), "Log message does not contain correct 'based on' message.");
    }
}
