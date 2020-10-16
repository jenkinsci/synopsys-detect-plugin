package com.synopsys.integration.jenkins.detect.service.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.DetectJenkinsEnvironmentVariable;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.detect.extensions.AirGapDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.DetectDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.InheritFromGlobalDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.ScriptOrJarDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.global.DetectGlobalConfig;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.model.TaskListener;

public class DetectStrategyServiceTest {
    private static final AirGapDownloadStrategy AIRGAP_DOWNLOAD_STRATEGY = new AirGapDownloadStrategy();
    private static final InheritFromGlobalDownloadStrategy INHERIT_DOWNLOAD_STRATEGY = new InheritFromGlobalDownloadStrategy();
    private static final ScriptOrJarDownloadStrategy SCRIPTJAR_DOWNLOAD_STRATEGY = new ScriptOrJarDownloadStrategy();

    private IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.empty();

    private ByteArrayOutputStream byteArrayOutputStream;
    private DetectStrategyService detectStrategyService;
    private JenkinsConfigService jenkinsConfigService;

    @BeforeEach
    public void setup() {
        TaskListener taskListener = Mockito.mock(TaskListener.class);
        byteArrayOutputStream = new ByteArrayOutputStream();
        Mockito.when(taskListener.getLogger()).thenReturn(new PrintStream(byteArrayOutputStream));
        JenkinsIntLogger logger = new JenkinsIntLogger(taskListener);

        jenkinsConfigService = Mockito.mock(JenkinsConfigService.class);
        detectStrategyService = new DetectStrategyService(logger, null, null, jenkinsConfigService);
    }

    @Test
    public void testInheritFromGlobalStrategy() {
        DetectGlobalConfig mockDetectGlobalConfig = Mockito.mock(DetectGlobalConfig.class);
        Mockito.when(jenkinsConfigService.getGlobalConfiguration(DetectGlobalConfig.class)).thenReturn(Optional.ofNullable(mockDetectGlobalConfig));
        assertNotNull(mockDetectGlobalConfig);
        Mockito.when(mockDetectGlobalConfig.getDownloadStrategy()).thenReturn(AIRGAP_DOWNLOAD_STRATEGY);

        DetectExecutionStrategy detectExecutionStrategy = testGetExecutionStrategy(intEnvironmentVariables, INHERIT_DOWNLOAD_STRATEGY);
        assertEquals(DetectAirGapJarStrategy.class, detectExecutionStrategy.getClass());
        assertTrue(byteArrayOutputStream.toString().contains(AirGapDownloadStrategy.DISPLAY_NAME), "Log does not contain message with correct download strategy.");

        System.out.println(byteArrayOutputStream.toString());
    }

    @Test
    public void testInheritDefaultGlobalStrategy() {
        DetectGlobalConfig mockDetectGlobalConfig = Mockito.mock(DetectGlobalConfig.class);
        Mockito.when(jenkinsConfigService.getGlobalConfiguration(DetectGlobalConfig.class)).thenReturn(Optional.ofNullable(mockDetectGlobalConfig));
        assertNotNull(mockDetectGlobalConfig);
        Mockito.when(mockDetectGlobalConfig.getDownloadStrategy()).thenReturn(null);
        Mockito.when(mockDetectGlobalConfig.getDefaultDownloadStrategy()).thenReturn(SCRIPTJAR_DOWNLOAD_STRATEGY);

        DetectExecutionStrategy detectExecutionStrategy = testGetExecutionStrategy(intEnvironmentVariables, INHERIT_DOWNLOAD_STRATEGY);
        assertEquals(DetectScriptStrategy.class, detectExecutionStrategy.getClass());
        assertTrue(byteArrayOutputStream.toString().contains(ScriptOrJarDownloadStrategy.DISPLAY_NAME), "Log does not contain message with correct download strategy.");

        System.out.println(byteArrayOutputStream.toString());
    }

    @Test
    public void testInheritFromGlobalStrategyFailure() throws IllegalAccessException, IntegrationException, NoSuchFieldException {
        assertThrows(DetectJenkinsException.class, () -> detectStrategyService.getExecutionStrategy(intEnvironmentVariables, null, null, INHERIT_DOWNLOAD_STRATEGY));
    }

    @Test
    public void testNullStrategyFailure() {
        assertThrows(DetectJenkinsException.class, () -> detectStrategyService.getExecutionStrategy(intEnvironmentVariables, null, null, null));
    }

    @Test
    public void testGetAirGapJarStrategy() {
        DetectExecutionStrategy detectExecutionStrategy = testGetExecutionStrategy(intEnvironmentVariables, AIRGAP_DOWNLOAD_STRATEGY);
        assertEquals(DetectAirGapJarStrategy.class, detectExecutionStrategy.getClass());
        assertTrue(byteArrayOutputStream.toString().contains(AirGapDownloadStrategy.DISPLAY_NAME), "Log does not contain message with correct download strategy.");

        System.out.println(byteArrayOutputStream.toString());
    }

    @Test
    public void testGetJarStrategy() {
        intEnvironmentVariables.put(DetectJenkinsEnvironmentVariable.USER_PROVIDED_JAR_PATH.stringValue(), "/tmp/path/to/detect.jar");

        DetectExecutionStrategy detectExecutionStrategy = testGetExecutionStrategy(intEnvironmentVariables, SCRIPTJAR_DOWNLOAD_STRATEGY);
        assertEquals(DetectJarStrategy.class, detectExecutionStrategy.getClass());
        assertTrue(byteArrayOutputStream.toString().contains(ScriptOrJarDownloadStrategy.DISPLAY_NAME), "Log does not contain message with correct download strategy.");

        System.out.println(byteArrayOutputStream.toString());
    }

    @Test
    public void testGetScriptStrategy() {
        DetectExecutionStrategy detectExecutionStrategy = testGetExecutionStrategy(intEnvironmentVariables, SCRIPTJAR_DOWNLOAD_STRATEGY);
        assertEquals(DetectScriptStrategy.class, detectExecutionStrategy.getClass());
        assertTrue(byteArrayOutputStream.toString().contains(ScriptOrJarDownloadStrategy.DISPLAY_NAME), "Log does not contain message with correct download strategy.");

        System.out.println(byteArrayOutputStream.toString());
    }

    public DetectExecutionStrategy testGetExecutionStrategy(IntEnvironmentVariables intEnvironmentVariables, DetectDownloadStrategy downloadStrategy) {
        DetectExecutionStrategy executionStrategy = null;
        try {
            executionStrategy = detectStrategyService.getExecutionStrategy(intEnvironmentVariables, null, null, downloadStrategy);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        return executionStrategy;
    }
}
