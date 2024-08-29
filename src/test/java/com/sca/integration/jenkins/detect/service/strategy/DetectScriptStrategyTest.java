package com.sca.integration.jenkins.detect.service.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.util.OperatingSystemType;

import hudson.model.TaskListener;

public class DetectScriptStrategyTest {
    private final String unescapedSpecialCharacters = "|&;<>()$`\\\"' \t\r\n\n*?[#~=%,";
    private JenkinsIntLogger defaultLogger;
    private JenkinsProxyHelper defaultProxyHelper;
    private ByteArrayOutputStream logs;

    @BeforeEach
    public void setUpMocks() {
        logs = new ByteArrayOutputStream();
        TaskListener mockedTaskListener = Mockito.mock(TaskListener.class);
        Mockito.when(mockedTaskListener.getLogger()).thenReturn(new PrintStream(logs));
        defaultLogger = JenkinsIntLogger.logToListener(mockedTaskListener);
        defaultProxyHelper = new JenkinsProxyHelper();
    }

    @Test
    public void testNoProxyDeterminable() {
        String expectedExceptionMessage = "expected test message";

        JenkinsProxyHelper mockedProxyHelper = Mockito.mock(JenkinsProxyHelper.class);
        Mockito.when(mockedProxyHelper.getProxyInfo(Mockito.anyString())).thenThrow(new IllegalArgumentException(expectedExceptionMessage));
        DetectScriptStrategy detectScriptStrategy = new DetectScriptStrategy(defaultLogger, mockedProxyHelper, OperatingSystemType.LINUX, null);

        try {
            detectScriptStrategy.getSetupCallable();
        } catch (IntegrationException e) {
            fail("An unexpected exception occurred: ", e);
        }

        assertTrue(logs.toString().contains(expectedExceptionMessage));
    }

    @Test
    public void testArgumentEscaperLinux() {
        DetectScriptStrategy detectScriptStrategy = new DetectScriptStrategy(defaultLogger, defaultProxyHelper, OperatingSystemType.LINUX, null);
        String expectedEscapedString = "\\|\\&\\;\\<\\>\\(\\)\\$\\`\\\\\\\"\\'\\ \\\t\\*\\?\\[\\#\\~\\=\\%,";

        String escapedString = detectScriptStrategy.getArgumentEscaper().apply(unescapedSpecialCharacters);

        assertEquals(escapedString, expectedEscapedString);
    }

    @Test
    public void testArgumentEscaperMac() {
        DetectScriptStrategy detectScriptStrategy = new DetectScriptStrategy(defaultLogger, defaultProxyHelper, OperatingSystemType.MAC, null);
        String expectedEscapedString = "\\|\\&\\;\\<\\>\\(\\)\\$\\`\\\\\\\"\\'\\ \\\t\\*\\?\\[\\#\\~\\=\\%,";

        String escapedString = detectScriptStrategy.getArgumentEscaper().apply(unescapedSpecialCharacters);

        assertEquals(escapedString, expectedEscapedString);
    }

    @Test
    public void testArgumentEscaperWindows() {
        DetectScriptStrategy detectScriptStrategy = new DetectScriptStrategy(defaultLogger, defaultProxyHelper, OperatingSystemType.WINDOWS, null);
        String expectedEscapedString = "`|`&`;`<`>`(`)`$```\\`\"`'` `\t`*`?`[`#`~`=`%`,";

        String escapedString = detectScriptStrategy.getArgumentEscaper().apply(unescapedSpecialCharacters);

        assertEquals(expectedEscapedString, escapedString);
    }

}
