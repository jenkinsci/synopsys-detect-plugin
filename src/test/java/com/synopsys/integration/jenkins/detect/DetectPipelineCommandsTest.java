package com.synopsys.integration.jenkins.detect;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.detect.extensions.ScriptOrJarDownloadStrategy;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;

public class DetectPipelineCommandsTest {
    private JenkinsIntLogger mockedLogger;
    private DetectRunner mockedDetectRunner;
    private static final ScriptOrJarDownloadStrategy DOWNLOAD_STRATEGY = new ScriptOrJarDownloadStrategy();

    @BeforeEach
    public void setupMocks() {
        try {
            mockedDetectRunner = Mockito.mock(DetectRunner.class);
            mockedLogger = Mockito.mock(JenkinsIntLogger.class);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }
    }

    @Test
    public void testRunDetectPipelineExceptionFailure() {
        try {
            Mockito.when(mockedDetectRunner.runDetect(Mockito.any(), Mockito.anyString(), Mockito.any(ScriptOrJarDownloadStrategy.class))).thenThrow(new IOException());
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        DetectPipelineCommands detectCommands = new DetectPipelineCommands(mockedDetectRunner, mockedLogger);

        assertThrows(IOException.class, () -> detectCommands.runDetect(false, StringUtils.EMPTY, DOWNLOAD_STRATEGY));
    }

    @Test
    public void testRunDetectPipelineSuccess() {
        try {
            Mockito.when(mockedDetectRunner.runDetect(Mockito.any(), Mockito.anyString(), Mockito.any(ScriptOrJarDownloadStrategy.class))).thenReturn(0);

            DetectPipelineCommands detectCommands = new DetectPipelineCommands(mockedDetectRunner, mockedLogger);
            detectCommands.runDetect(false, StringUtils.EMPTY, DOWNLOAD_STRATEGY);

            Mockito.verify(mockedLogger, Mockito.never()).error(Mockito.anyString());
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }
    }

    @Test
    public void testRunDetectPipelineExitCodeExceptionFailure() {
        try {
            Mockito.when(mockedDetectRunner.runDetect(Mockito.any(), Mockito.anyString(), Mockito.any(ScriptOrJarDownloadStrategy.class))).thenReturn(1);
        } catch (Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }

        DetectPipelineCommands detectCommands = new DetectPipelineCommands(mockedDetectRunner, mockedLogger);
        assertThrows(DetectJenkinsException.class, () -> detectCommands.runDetect(false, StringUtils.EMPTY, DOWNLOAD_STRATEGY));

        Mockito.verify(mockedLogger, Mockito.never()).error(Mockito.anyString());
    }

    @Test
    public void testRunDetectPipelineReturnExitCodeFailure() {
        try {
            Mockito.when(mockedDetectRunner.runDetect(Mockito.any(), Mockito.anyString(), Mockito.any(ScriptOrJarDownloadStrategy.class))).thenReturn(1);

            DetectPipelineCommands detectCommands = new DetectPipelineCommands(mockedDetectRunner, mockedLogger);
            detectCommands.runDetect(true, StringUtils.EMPTY, DOWNLOAD_STRATEGY);

            Mockito.verify(mockedLogger).error(Mockito.anyString());
        } catch (
              Exception e) {
            fail("An unexpected exception occurred in the test code: ", e);
        }
    }
}
