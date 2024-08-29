package com.sca.integration.jenkins.detect;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import com.sca.integration.detect.DetectFreestyleCommands;
import com.sca.integration.detect.DetectRunner;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.exception.IntegrationException;
import com.sca.integration.detect.extensions.ScriptOrJarDownloadStrategy;
import com.synopsys.integration.jenkins.service.JenkinsBuildService;

public class DetectFreestyleCommandsTest {
    private JenkinsBuildService mockedBuildService;
    private DetectRunner mockedDetectRunner;
    private static final ScriptOrJarDownloadStrategy DOWNLOAD_STRATEGY = new ScriptOrJarDownloadStrategy();

    @BeforeEach
    public void setupMocks() {
        try {
            mockedDetectRunner = Mockito.mock(DetectRunner.class);
            mockedBuildService = Mockito.mock(JenkinsBuildService.class);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }
    }

    @Test
    public void testRunDetectSuccess() {
        try {
            Mockito.when(mockedDetectRunner.runDetect(Mockito.any(), Mockito.anyString(), Mockito.any(ScriptOrJarDownloadStrategy.class))).thenReturn(0);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        DetectFreestyleCommands detectCommands = new DetectFreestyleCommands(mockedBuildService, mockedDetectRunner);
        detectCommands.runDetect(StringUtils.EMPTY, DOWNLOAD_STRATEGY);

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAborted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(String.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
    }

    @Test
    public void testRunDetectExitCodeFailure() {
        try {
            Mockito.when(mockedDetectRunner.runDetect(Mockito.any(), Mockito.anyString(), Mockito.any(ScriptOrJarDownloadStrategy.class))).thenReturn(1);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        DetectFreestyleCommands detectCommands = new DetectFreestyleCommands(mockedBuildService, mockedDetectRunner);
        detectCommands.runDetect(StringUtils.EMPTY, DOWNLOAD_STRATEGY);

        Mockito.verify(mockedBuildService).markBuildFailed(Mockito.any(String.class));

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAborted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
    }

    @Test
    public void testRunDetectIntegrationFailure() {
        try {
            Mockito.when(mockedDetectRunner.runDetect(Mockito.any(), Mockito.anyString(), Mockito.any(ScriptOrJarDownloadStrategy.class))).thenThrow(new IntegrationException());
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        DetectFreestyleCommands detectCommands = new DetectFreestyleCommands(mockedBuildService, mockedDetectRunner);
        detectCommands.runDetect(StringUtils.EMPTY, DOWNLOAD_STRATEGY);

        Mockito.verify(mockedBuildService).markBuildFailed(Mockito.any(IntegrationException.class));

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAborted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(String.class));
    }

    @Test
    public void testRunDetectInterrupted() {
        try {
            Mockito.when(mockedDetectRunner.runDetect(Mockito.any(), Mockito.anyString(), Mockito.any(ScriptOrJarDownloadStrategy.class))).thenThrow(new InterruptedException());
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        DetectFreestyleCommands detectCommands = new DetectFreestyleCommands(mockedBuildService, mockedDetectRunner);
        detectCommands.runDetect(StringUtils.EMPTY, DOWNLOAD_STRATEGY);

        Mockito.verify(mockedBuildService).markBuildInterrupted();

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAborted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(String.class));
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
    }

    @Test
    public void testRunDetectExceptionFailure() {
        try {
            Mockito.when(mockedDetectRunner.runDetect(Mockito.any(), Mockito.anyString(), Mockito.any(ScriptOrJarDownloadStrategy.class))).thenThrow(new IOException());
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        DetectFreestyleCommands detectCommands = new DetectFreestyleCommands(mockedBuildService, mockedDetectRunner);
        detectCommands.runDetect(StringUtils.EMPTY, DOWNLOAD_STRATEGY);

        Mockito.verify(mockedBuildService).markBuildFailed(Mockito.any(IOException.class));

        Mockito.verify(mockedBuildService, Mockito.never()).markBuildAborted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildInterrupted();
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
        Mockito.verify(mockedBuildService, Mockito.never()).markBuildFailed(Mockito.any(String.class));
    }

}
