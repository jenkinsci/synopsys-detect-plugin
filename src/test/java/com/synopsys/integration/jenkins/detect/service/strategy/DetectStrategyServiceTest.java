package com.synopsys.integration.jenkins.detect.service.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.jenkins.detect.DetectJenkinsEnvironmentVariable;
import com.synopsys.integration.jenkins.detect.extensions.ScriptOrJarDownloadStrategy;
import com.synopsys.integration.util.IntEnvironmentVariables;

public class DetectStrategyServiceTest {
    private final ScriptOrJarDownloadStrategy DOWNLOAD_STRATEGY = new ScriptOrJarDownloadStrategy();

    @Test
    public void testGetJarStrategy() {
        IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.empty();
        intEnvironmentVariables.put(DetectJenkinsEnvironmentVariable.USER_PROVIDED_JAR_PATH.stringValue(), "/tmp/path/to/detect.jar");

        DetectExecutionStrategy detectExecutionStrategy = testGetExecutionStrategy(intEnvironmentVariables, DOWNLOAD_STRATEGY);
        assertEquals(DetectJarStrategy.class, detectExecutionStrategy.getClass());
    }

    @Test
    public void testGetScriptStrategy() {
        IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.empty();

        DetectExecutionStrategy detectExecutionStrategy = testGetExecutionStrategy(intEnvironmentVariables, DOWNLOAD_STRATEGY);
        assertEquals(DetectScriptStrategy.class, detectExecutionStrategy.getClass());
    }

    public DetectExecutionStrategy testGetExecutionStrategy(IntEnvironmentVariables intEnvironmentVariables, ScriptOrJarDownloadStrategy downloadStrategy) {
        DetectStrategyService detectStrategyService = new DetectStrategyService(null, null, null);

        return detectStrategyService.getExecutionStrategy(intEnvironmentVariables, null, null, downloadStrategy);
    }
}
