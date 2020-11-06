package com.synopsys.integration.jenkins.detect.service.strategy;

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

import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.detect.extensions.AirGapDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.DetectDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.InheritFromGlobalDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.ScriptOrJarDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.global.DetectGlobalConfig;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;

import hudson.model.TaskListener;

public class DetectDownloadStrategyServiceTest {
    private static final AirGapDownloadStrategy AIRGAP_DOWNLOAD_STRATEGY = new AirGapDownloadStrategy();
    private static final InheritFromGlobalDownloadStrategy INHERIT_DOWNLOAD_STRATEGY = new InheritFromGlobalDownloadStrategy();
    private static final ScriptOrJarDownloadStrategy SCRIPTJAR_DOWNLOAD_STRATEGY = new ScriptOrJarDownloadStrategy();
    private static final String CONFIGURED_STRATEGY_MSG = "configured strategy: ";
    private static final String SYSTEM_STRATEGY_MSG = "configured system strategy: ";
    private static final String DEFAULT_STRATEGY_MSG = "default strategy: ";

    private ByteArrayOutputStream byteArrayOutputStream;
    private JenkinsConfigService jenkinsConfigServiceMock;
    private DetectGlobalConfig mockDetectGlobalConfig;
    private DetectDownloadStrategyService detectDownloadStrategyService;

    @BeforeEach
    public void setup() {
        TaskListener taskListener = Mockito.mock(TaskListener.class);
        byteArrayOutputStream = new ByteArrayOutputStream();
        Mockito.when(taskListener.getLogger()).thenReturn(new PrintStream(byteArrayOutputStream));
        JenkinsIntLogger logger = new JenkinsIntLogger(taskListener);

        jenkinsConfigServiceMock = Mockito.mock(JenkinsConfigService.class);
        mockDetectGlobalConfig = Mockito.mock(DetectGlobalConfig.class);
        assertNotNull(mockDetectGlobalConfig);
        Mockito.when(jenkinsConfigServiceMock.getGlobalConfiguration(DetectGlobalConfig.class)).thenReturn(Optional.ofNullable(mockDetectGlobalConfig));

        detectDownloadStrategyService = new DetectDownloadStrategyService(logger, jenkinsConfigServiceMock);
    }

    @Test
    public void testNullStrategy() throws DetectJenkinsException {
        Mockito.when(jenkinsConfigServiceMock.getGlobalConfiguration(DetectGlobalConfig.class)).thenReturn(Optional.empty());
        DetectJenkinsException exception = assertThrows(DetectJenkinsException.class, () -> detectDownloadStrategyService.determineCorrectDownloadStrategy(null));
        assertTrue(exception.getMessage().contains("Could not find Detect configuration. Check Jenkins System Configuration to ensure Detect is configured correctly."), "Stacktrace does not contain expected message.");
    }

    @Test
    public void testReturnEmptyGetGlobalConfiguration() {
        Mockito.when(jenkinsConfigServiceMock.getGlobalConfiguration(DetectGlobalConfig.class)).thenReturn(Optional.empty());
        DetectJenkinsException exception = assertThrows(DetectJenkinsException.class, () -> detectDownloadStrategyService.determineCorrectDownloadStrategy(INHERIT_DOWNLOAD_STRATEGY));
        assertTrue(exception.getMessage().contains("Could not find Detect configuration. Check Jenkins System Configuration to ensure Detect is configured correctly."), "Stacktrace does not contain expected message.");
    }

    @Test
    public void testConfiguredAirGapStrategy() {
        DetectDownloadStrategy correctDownloadStrategy = null;
        try {
            correctDownloadStrategy = detectDownloadStrategyService.determineCorrectDownloadStrategy(AIRGAP_DOWNLOAD_STRATEGY);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        assertTrue(correctDownloadStrategy instanceof AirGapDownloadStrategy);
        assertTrue(byteArrayOutputStream.toString().contains(AirGapDownloadStrategy.DISPLAY_NAME), "Log does not contain correct download strategy name.");
        assertTrue(byteArrayOutputStream.toString().contains(CONFIGURED_STRATEGY_MSG), "Log does not contain correct download strategy label.");
    }

    @Test
    public void testConfiguredScriptJarStrategy() {
        DetectDownloadStrategy correctDownloadStrategy = null;
        try {
            correctDownloadStrategy = detectDownloadStrategyService.determineCorrectDownloadStrategy(SCRIPTJAR_DOWNLOAD_STRATEGY);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        assertTrue(correctDownloadStrategy instanceof ScriptOrJarDownloadStrategy);
        assertTrue(byteArrayOutputStream.toString().contains(ScriptOrJarDownloadStrategy.DISPLAY_NAME), "Log does not contain correct download strategy name.");
        assertTrue(byteArrayOutputStream.toString().contains(CONFIGURED_STRATEGY_MSG), "Log does not contain correct download strategy label.");
    }

    @Test
    public void testAirGapFromGlobal() {
        Mockito.when(mockDetectGlobalConfig.getDownloadStrategy()).thenReturn(AIRGAP_DOWNLOAD_STRATEGY);
        DetectDownloadStrategy correctDownloadStrategy = null;
        try {
            correctDownloadStrategy = detectDownloadStrategyService.determineCorrectDownloadStrategy(INHERIT_DOWNLOAD_STRATEGY);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        assertTrue(correctDownloadStrategy instanceof AirGapDownloadStrategy);
        assertTrue(byteArrayOutputStream.toString().contains(AirGapDownloadStrategy.DISPLAY_NAME), "Log does not contain correct download strategy name.");
        assertTrue(byteArrayOutputStream.toString().contains(SYSTEM_STRATEGY_MSG), "Log does not contain correct download strategy label.");
    }

    @Test
    public void testScriptJarFromGlobal() {
        Mockito.when(mockDetectGlobalConfig.getDownloadStrategy()).thenReturn(SCRIPTJAR_DOWNLOAD_STRATEGY);
        DetectDownloadStrategy correctDownloadStrategy = null;
        try {
            correctDownloadStrategy = detectDownloadStrategyService.determineCorrectDownloadStrategy(INHERIT_DOWNLOAD_STRATEGY);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        assertTrue(correctDownloadStrategy instanceof ScriptOrJarDownloadStrategy);
        assertTrue(byteArrayOutputStream.toString().contains(ScriptOrJarDownloadStrategy.DISPLAY_NAME), "Log does not contain correct download strategy name.");
        assertTrue(byteArrayOutputStream.toString().contains(SYSTEM_STRATEGY_MSG), "Log does not contain correct download strategy label.");
    }

    @Test
    public void testAirGapFromDefault() {
        Mockito.when(mockDetectGlobalConfig.getDownloadStrategy()).thenReturn(null);
        Mockito.when(mockDetectGlobalConfig.getDefaultDownloadStrategy()).thenReturn(AIRGAP_DOWNLOAD_STRATEGY);

        DetectDownloadStrategy correctDownloadStrategy = null;
        try {
            correctDownloadStrategy = detectDownloadStrategyService.determineCorrectDownloadStrategy(INHERIT_DOWNLOAD_STRATEGY);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        assertTrue(correctDownloadStrategy instanceof AirGapDownloadStrategy);
        assertTrue(byteArrayOutputStream.toString().contains(AirGapDownloadStrategy.DISPLAY_NAME), "Log does not contain correct download strategy name.");
        assertTrue(byteArrayOutputStream.toString().contains(DEFAULT_STRATEGY_MSG), "Log does not contain correct download strategy label.");
    }

    @Test
    public void testScriptJarFromDefault() {
        Mockito.when(mockDetectGlobalConfig.getDownloadStrategy()).thenReturn(null);
        Mockito.when(mockDetectGlobalConfig.getDefaultDownloadStrategy()).thenReturn(SCRIPTJAR_DOWNLOAD_STRATEGY);

        DetectDownloadStrategy correctDownloadStrategy = null;
        try {
            correctDownloadStrategy = detectDownloadStrategyService.determineCorrectDownloadStrategy(INHERIT_DOWNLOAD_STRATEGY);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        assertTrue(correctDownloadStrategy instanceof ScriptOrJarDownloadStrategy);
        assertTrue(byteArrayOutputStream.toString().contains(ScriptOrJarDownloadStrategy.DISPLAY_NAME), "Log does not contain correct download strategy name.");
        assertTrue(byteArrayOutputStream.toString().contains(DEFAULT_STRATEGY_MSG), "Log does not contain correct download strategy label.");
    }
}