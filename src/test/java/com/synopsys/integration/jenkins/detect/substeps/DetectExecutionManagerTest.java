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

    @Test
    public void testCallInterruptedException() {
        DetectExecutionManager detectExecutionManager = new DetectExecutionManager() {
            @Override
            public DetectSetupResponse setUpForExecution() throws InterruptedException {
                throw new InterruptedException();
            }
        };

        assertFalse(Thread.currentThread().isInterrupted(), "Thread should not be interrupted until an InterruptedException is thrown.");
        assertThrows(DetectJenkinsException.class, detectExecutionManager::call);
        assertTrue(Thread.currentThread().isInterrupted());
    }

    @Test
    public void testCallIOException() {
        DetectExecutionManager detectExecutionManager = new DetectExecutionManager() {
            @Override
            public DetectSetupResponse setUpForExecution() throws IOException {
                throw new IOException();
            }
        };

        assertThrows(DetectJenkinsException.class, detectExecutionManager::call);
    }

    @Test
    public void testCallNoException() {
        DetectSetupResponse detectSetupResponseMock = Mockito.mock(DetectSetupResponse.class);

        DetectExecutionManager detectExecutionManager = new DetectExecutionManager() {
            @Override
            public DetectSetupResponse setUpForExecution() {
                return detectSetupResponseMock;
            }
        };

        try {
            assertEquals(detectSetupResponseMock, detectExecutionManager.call());
        } catch (IntegrationException e) {
            fail("Unexpected exception thrown", e);
        }
    }
}
