package com.synopsys.integration.jenkins.detect.extensions.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import hudson.tools.ToolInstallation;

public class AbortDetectAirGapInstallExceptionTest {

    private final String toolName = "Test Tool Name";
    private final String reason = "Test Reason";
    private final String exceptionMessage = String.format("Cannot install Detect AirGap Installation %s because: %s", toolName, reason);

    @Test
    public void testException() {
        ToolInstallation mockToolInstallation = Mockito.mock(ToolInstallation.class);
        Mockito.when(mockToolInstallation.getName()).thenReturn(toolName);
        AbortDetectAirGapInstallException abortDetectAirGapInstallException = new AbortDetectAirGapInstallException(mockToolInstallation, reason);
        assertEquals(exceptionMessage, abortDetectAirGapInstallException.getMessage());
    }
}
