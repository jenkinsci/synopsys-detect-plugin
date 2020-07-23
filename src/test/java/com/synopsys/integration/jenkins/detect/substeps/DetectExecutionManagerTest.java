package com.synopsys.integration.jenkins.detect.substeps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;

import org.junit.Test;
import org.mockito.Mockito;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;

public class DetectExecutionManagerTest {

    private static DetectJarManager detectJarManager = new DetectJarManager(null, null, null, null);
    private static DetectJarManager spiedDetectJarManager = Mockito.spy(detectJarManager);

    @Test
    public void testCallInterruptedException() {
        try {
            Mockito.doThrow(new InterruptedException()).when(spiedDetectJarManager).setUpForExecution();
        } catch (IOException | InterruptedException e) {
            fail("Unexpected exception thrown", e);
        }

        assertFalse(Thread.currentThread().isInterrupted());
        assertThrows(DetectJenkinsException.class, spiedDetectJarManager::call);
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    public void testCallIOException() {
        try {
            Mockito.doThrow(new IOException()).when(spiedDetectJarManager).setUpForExecution();
        } catch (IOException | InterruptedException e) {
            fail("Unexpected exception thrown", e);
        }

        assertThrows(DetectJenkinsException.class, spiedDetectJarManager::call);
    }

    @Test
    public void testCallNoException() {
        DetectSetupResponse detectSetupResponseMock = Mockito.mock(DetectSetupResponse.class);

        try {
            Mockito.doReturn(detectSetupResponseMock).when(spiedDetectJarManager).setUpForExecution();
            assertEquals(detectSetupResponseMock, spiedDetectJarManager.call());
        } catch (IOException | InterruptedException | IntegrationException e) {
            fail("Unexpected exception thrown", e);
        }
    }
}
