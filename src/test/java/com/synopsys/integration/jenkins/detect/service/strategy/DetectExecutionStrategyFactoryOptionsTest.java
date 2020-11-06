package com.synopsys.integration.jenkins.detect.service.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.jenkins.detect.DetectJenkinsEnvironmentVariable;
import com.synopsys.integration.jenkins.detect.extensions.AirGapDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.ScriptOrJarDownloadStrategy;
import com.synopsys.integration.jenkins.detect.service.DetectArgumentService;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.wrapper.JenkinsVersionHelper;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.model.TaskListener;

public class DetectExecutionStrategyFactoryOptionsTest {
    private static final AirGapDownloadStrategy AIRGAP_DOWNLOAD_STRATEGY = new AirGapDownloadStrategy();
    private static final ScriptOrJarDownloadStrategy SCRIPTJAR_DOWNLOAD_STRATEGY = new ScriptOrJarDownloadStrategy();
    private static final String DETECT_JAR = "detectJar";
    private static final String REMOTE_JDK_HOME = "remoteJdkHome";
    private final String REMOTE_TEMP_WORKSPACE_PATH = "remoteTempWorkspacePath";

    private IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.includeSystemEnv();

    private final JenkinsIntLogger logger = new JenkinsIntLogger(Mockito.mock(TaskListener.class));
    private final JenkinsProxyHelper jenkinsProxyHelper = new JenkinsProxyHelper();

    private final JenkinsConfigService jenkinsConfigServiceMock = Mockito.mock(JenkinsConfigService.class);
    private final JenkinsRemotingService jenkinsRemotingServiceMock = Mockito.mock(JenkinsRemotingService.class);
    private final JenkinsVersionHelper jenkinsVersionHelperMock = Mockito.mock(JenkinsVersionHelper.class);
    private final DetectArgumentService detectArgumentService = new DetectArgumentService(logger, jenkinsVersionHelperMock);
    private final DetectExecutionStrategyFactory detectExecutionStrategyFactory = new DetectExecutionStrategyFactory(logger, jenkinsProxyHelper, REMOTE_TEMP_WORKSPACE_PATH, jenkinsConfigServiceMock, jenkinsRemotingServiceMock,
        detectArgumentService);

    @Test
    public void testAirGapStrategy() {
        DetectExecutionStrategyOptions detectExecutionStrategyOptions = new DetectExecutionStrategyOptions(intEnvironmentVariables, AIRGAP_DOWNLOAD_STRATEGY);
        assertEquals(intEnvironmentVariables, detectExecutionStrategyOptions.getIntEnvironmentVariables());
        assertTrue(detectExecutionStrategyOptions.isAirGap(), "isAirGap() should be true");
        assertFalse(detectExecutionStrategyOptions.isJar(), "isJar() should be false");
        assertNull(detectExecutionStrategyOptions.getJarPath(), "getJarPath() should be null");
        assertEquals(AIRGAP_DOWNLOAD_STRATEGY, detectExecutionStrategyOptions.getAirGapStrategy());

        try {
            DetectExecutionStrategy detectExecutionStrategy = detectExecutionStrategyFactory.createDetectExecutionStrategy(detectExecutionStrategyOptions, REMOTE_JDK_HOME);
            assertTrue(detectExecutionStrategy instanceof DetectAirGapJarStrategy, String.format("Expected an instance of DetectAirGapJarStrategy and received %s instead", detectExecutionStrategy.getClass().getSimpleName()));
        } catch (Exception e) {
            fail("Unexpected exception was thrown in test code: ", e);
        }
    }

    @Test
    public void testJarStrategy() {
        intEnvironmentVariables.put(DetectJenkinsEnvironmentVariable.USER_PROVIDED_JAR_PATH.stringValue(), DETECT_JAR);
        DetectExecutionStrategyOptions detectExecutionStrategyOptions = new DetectExecutionStrategyOptions(intEnvironmentVariables, SCRIPTJAR_DOWNLOAD_STRATEGY);
        assertEquals(intEnvironmentVariables, detectExecutionStrategyOptions.getIntEnvironmentVariables());
        assertFalse(detectExecutionStrategyOptions.isAirGap(), "isAirGap() should be false");
        assertTrue(detectExecutionStrategyOptions.isJar(), "isJar() should be true");
        assertEquals(DETECT_JAR, detectExecutionStrategyOptions.getJarPath());
        assertNull(detectExecutionStrategyOptions.getAirGapStrategy(), "getAirGapStrategy() should be null");

        try {
            DetectExecutionStrategy detectExecutionStrategy = detectExecutionStrategyFactory.createDetectExecutionStrategy(detectExecutionStrategyOptions, REMOTE_JDK_HOME);
            assertTrue(detectExecutionStrategy instanceof DetectJarStrategy, String.format("Expected an instance of DetectJarStrategy and received %s instead", detectExecutionStrategy.getClass().getSimpleName()));
        } catch (Exception e) {
            fail("Unexpected exception was thrown in test code: ", e);
        }
    }

    @Test
    public void testScriptStrategy() {
        DetectExecutionStrategyOptions detectExecutionStrategyOptions = new DetectExecutionStrategyOptions(intEnvironmentVariables, SCRIPTJAR_DOWNLOAD_STRATEGY);
        assertEquals(intEnvironmentVariables, detectExecutionStrategyOptions.getIntEnvironmentVariables());
        assertFalse(detectExecutionStrategyOptions.isAirGap(), "isAirGap() should be false");
        assertFalse(detectExecutionStrategyOptions.isJar(), "isJar() should be false");
        assertNull(detectExecutionStrategyOptions.getJarPath(), "getJarPath() should be null");
        assertNull(detectExecutionStrategyOptions.getAirGapStrategy(), "getAirGapStrategy() should be null");

        try {
            DetectExecutionStrategy detectExecutionStrategy = detectExecutionStrategyFactory.createDetectExecutionStrategy(detectExecutionStrategyOptions, REMOTE_JDK_HOME);
            assertTrue(detectExecutionStrategy instanceof DetectScriptStrategy, String.format("Expected an instance of DetectScriptStrategy and received %s instead", detectExecutionStrategy.getClass().getSimpleName()));
        } catch (Exception e) {
            fail("Unexpected exception was thrown in test code: ", e);
        }
    }
}
