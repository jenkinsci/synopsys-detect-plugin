package com.synopsys.integration.jenkins.detect;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.detect.service.DetectArgumentService;
import com.synopsys.integration.jenkins.detect.service.DetectEnvironmentService;
import com.synopsys.integration.jenkins.detect.service.DetectServicesFactory;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectExecutionStrategy;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectStrategyService;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsBuildService;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;

import joptsimple.internal.Strings;

public class DetectCommandsTest {
    private DetectServicesFactory mockedServicesFactory;
    private JenkinsRemotingService mockedRemotingService;
    private JenkinsBuildService mockedBuildService;
    private JenkinsIntLogger mockedLogger;

    @BeforeEach
    public void setupMocks() {
        mockedServicesFactory = Mockito.mock(DetectServicesFactory.class);
        try {
            mockedRemotingService = Mockito.mock(JenkinsRemotingService.class);
            mockedBuildService = Mockito.mock(JenkinsBuildService.class);
            mockedLogger = Mockito.mock(JenkinsIntLogger.class);
            JenkinsConfigService jenkinsConfigService = Mockito.mock(JenkinsConfigService.class);
            DetectArgumentService detectArgumentService = Mockito.mock(DetectArgumentService.class);
            DetectEnvironmentService detectEnvironmentService = Mockito.mock(DetectEnvironmentService.class);
            DetectStrategyService detectStrategyService = Mockito.mock(DetectStrategyService.class);
            DetectExecutionStrategy detectExecutionStrategy = Mockito.mock(DetectExecutionStrategy.class);

            Mockito.when(mockedServicesFactory.createJenkinsRemotingService()).thenReturn(mockedRemotingService);
            Mockito.when(mockedServicesFactory.createJenkinsBuildService()).thenReturn(mockedBuildService);
            Mockito.when(mockedServicesFactory.getLogger()).thenReturn(mockedLogger);
            Mockito.when(mockedServicesFactory.createJenkinsConfigService()).thenReturn(jenkinsConfigService);
            Mockito.when(mockedServicesFactory.createDetectArgumentService()).thenReturn(detectArgumentService);
            Mockito.when(mockedServicesFactory.createDetectEnvironmentService()).thenReturn(detectEnvironmentService);
            Mockito.when(mockedServicesFactory.createDetectStrategyService()).thenReturn(detectStrategyService);
            Mockito.when(detectStrategyService.getExecutionStrategy(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(detectExecutionStrategy);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }
    }

    @Test
    public void testRunDetectPostBuildSuccess() {
        try {
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(0);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        DetectCommands detectCommands = new DetectCommands(mockedServicesFactory);
        detectCommands.runDetectPostBuild(Strings.EMPTY);

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAborted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(String.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
    }

    @Test
    public void testRunDetectPostBuildExitCodeFailure() {
        try {
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(1);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        DetectCommands detectCommands = new DetectCommands(mockedServicesFactory);
        detectCommands.runDetectPostBuild(Strings.EMPTY);

        Mockito.verify(mockedBuildService).markBuildFailed(Mockito.any(String.class));

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAborted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
    }

    @Test
    public void testRunDetectPostBuildIntegrationFailure() {
        try {
            Mockito.when(mockedRemotingService.call(Mockito.any())).thenThrow(new IntegrationException());
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        DetectCommands detectCommands = new DetectCommands(mockedServicesFactory);
        detectCommands.runDetectPostBuild(Strings.EMPTY);

        Mockito.verify(mockedBuildService).markBuildUnstable(Mockito.any());

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAborted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(String.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
    }

    @Test
    public void testRunDetectPostBuildInterrupted() {
        try {
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenThrow(new InterruptedException());
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        DetectCommands detectCommands = new DetectCommands(mockedServicesFactory);
        detectCommands.runDetectPostBuild(Strings.EMPTY);

        Mockito.verify(mockedBuildService).markBuildInterrupted();

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAborted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(String.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
    }

    @Test
    public void testRunDetectPostBuildExceptionFailure() {
        try {
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenThrow(new IOException());
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        DetectCommands detectCommands = new DetectCommands(mockedServicesFactory);
        detectCommands.runDetectPostBuild(Strings.EMPTY);

        Mockito.verify(mockedBuildService).markBuildFailed(Mockito.any(Exception.class));

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAborted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(String.class));
    }

    @Test
    public void testRunDetectPipelineExceptionFailure() {
        try {
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenThrow(new IOException());
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        DetectCommands detectCommands = new DetectCommands(mockedServicesFactory);

        assertThrows(IOException.class, () -> detectCommands.runDetectPipeline(false, Strings.EMPTY));
    }

    @Test
    public void testRunDetectPipelineSuccess() {
        try {
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(0);

            DetectCommands detectCommands = new DetectCommands(mockedServicesFactory);
            detectCommands.runDetectPipeline(false, Strings.EMPTY);

            Mockito.verify(mockedLogger, Mockito.never()).error(Mockito.anyString());
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }
    }

    @Test
    public void testRunDetectPipelineExitCodeExceptionFailure() {
        try {
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(1);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        DetectCommands detectCommands = new DetectCommands(mockedServicesFactory);
        assertThrows(DetectJenkinsException.class, () -> detectCommands.runDetectPipeline(false, Strings.EMPTY));

        Mockito.verify(mockedLogger, Mockito.never()).error(Mockito.anyString());
    }

    @Test
    public void testRunDetectPipelineReturnExitCodeFailure() {
        try {
            Mockito.when(mockedRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(1);

            DetectCommands detectCommands = new DetectCommands(mockedServicesFactory);
            detectCommands.runDetectPipeline(true, Strings.EMPTY);

            Mockito.verify(mockedLogger).error(Mockito.anyString());
        } catch (
              Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }
    }
}
