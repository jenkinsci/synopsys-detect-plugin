package com.synopsys.integration.jenkins.detect.service.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.jenkins.detect.DetectJenkinsEnvironmentVariable;
import com.synopsys.integration.jenkins.detect.extensions.ScriptOrJarDownloadStrategy;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.model.TaskListener;

public class DetectStrategyServiceTest {
    private final ScriptOrJarDownloadStrategy DOWNLOAD_STRATEGY = new ScriptOrJarDownloadStrategy();

    private JenkinsIntLogger logger;
    private ByteArrayOutputStream byteArrayOutputStream;

    @BeforeEach
    public void setup() {
        TaskListener taskListener = Mockito.mock(TaskListener.class);
        byteArrayOutputStream = new ByteArrayOutputStream();
        Mockito.when(taskListener.getLogger()).thenReturn(new PrintStream(byteArrayOutputStream));

        logger = new JenkinsIntLogger(taskListener);
    }

    @Test
    public void testGetJarStrategy() {
        IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.empty();
        intEnvironmentVariables.put(DetectJenkinsEnvironmentVariable.USER_PROVIDED_JAR_PATH.stringValue(), "/tmp/path/to/detect.jar");

        DetectExecutionStrategy detectExecutionStrategy = testGetExecutionStrategy(intEnvironmentVariables, DOWNLOAD_STRATEGY);
        assertEquals(DetectJarStrategy.class, detectExecutionStrategy.getClass());
        assertTrue(byteArrayOutputStream.toString().contains("ScriptOrJarDownloadStrategy"), "Log does not contain message with correct download strategy.");

        System.out.println(byteArrayOutputStream.toString());
    }

    @Test
    public void testGetScriptStrategy() {
        IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.empty();

        DetectExecutionStrategy detectExecutionStrategy = testGetExecutionStrategy(intEnvironmentVariables, DOWNLOAD_STRATEGY);
        assertEquals(DetectScriptStrategy.class, detectExecutionStrategy.getClass());
        assertTrue(byteArrayOutputStream.toString().contains("ScriptOrJarDownloadStrategy"), "Log does not contain message with correct download strategy.");

        System.out.println(byteArrayOutputStream.toString());
    }

    public DetectExecutionStrategy testGetExecutionStrategy(IntEnvironmentVariables intEnvironmentVariables, ScriptOrJarDownloadStrategy downloadStrategy) {
        JenkinsConfigService jenkinsConfigService = Mockito.mock(JenkinsConfigService.class);
        DetectStrategyService detectStrategyService = new DetectStrategyService(logger, null, null, jenkinsConfigService);

        DetectExecutionStrategy executionStrategy = null;
        try {
            executionStrategy = detectStrategyService.getExecutionStrategy(intEnvironmentVariables, null, null, downloadStrategy);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        return executionStrategy;
    }
}
