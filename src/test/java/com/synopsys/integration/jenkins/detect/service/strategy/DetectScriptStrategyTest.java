package com.synopsys.integration.jenkins.detect.service.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.service.DetectArgumentService;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.wrapper.JenkinsVersionHelper;
import com.synopsys.integration.util.OperatingSystemType;

import hudson.model.TaskListener;

public class DetectScriptStrategyTest {
    private final String unescapedSpecialCharacters = "|&;<>()$`\\\"' \t\r\n\n*?[#~=%,";
    private JenkinsIntLogger logger;
    private JenkinsProxyHelper defaultProxyHelper = new JenkinsProxyHelper();
    private ByteArrayOutputStream logs;

    private DetectArgumentService detectArgumentService;

    private JenkinsRemotingService jenkinsRemotingServiceMock = Mockito.mock(JenkinsRemotingService.class);
    private JenkinsVersionHelper jenkinsVersionHelperMock = Mockito.mock(JenkinsVersionHelper.class);

    @BeforeEach
    public void setup() {
        logs = new ByteArrayOutputStream();
        TaskListener mockedTaskListener = Mockito.mock(TaskListener.class);
        Mockito.when(mockedTaskListener.getLogger()).thenReturn(new PrintStream(logs));
        logger = new JenkinsIntLogger(mockedTaskListener);

        detectArgumentService = new DetectArgumentService(logger, jenkinsVersionHelperMock);

    }

    @Test
    public void testNoProxyDeterminable() {
        String expectedExceptionMessage = "expected test message";

        JenkinsProxyHelper mockedProxyHelper = Mockito.mock(JenkinsProxyHelper.class);
        Mockito.when(mockedProxyHelper.getProxyInfo(Mockito.anyString())).thenThrow(new IllegalArgumentException(expectedExceptionMessage));
        DetectScriptStrategy detectScriptStrategy = new DetectScriptStrategy(jenkinsRemotingServiceMock, detectArgumentService, null, logger, mockedProxyHelper, OperatingSystemType.LINUX, null);

        try {
            detectScriptStrategy.getSetupCallable();
        } catch (IntegrationException e) {
            fail("An unexpected exception occurred: ", e);
        }

        assertTrue(logs.toString().contains(expectedExceptionMessage));
    }

    @Test
    public void testArgumentEscaperLinux() {
        DetectScriptStrategy detectScriptStrategy = new DetectScriptStrategy(jenkinsRemotingServiceMock, detectArgumentService, null, logger, defaultProxyHelper, OperatingSystemType.LINUX, null);
        String expectedEscapedString = "\\|\\&\\;\\<\\>\\(\\)\\$\\`\\\\\\\"\\'\\ \\\t\\*\\?\\[\\#\\~\\=\\%,";
        String escapedString = detectScriptStrategy.getArgumentEscaper().apply(unescapedSpecialCharacters);

        assertEquals(escapedString, expectedEscapedString);
    }

    @Test
    public void testArgumentEscaperMac() {
        DetectScriptStrategy detectScriptStrategy = new DetectScriptStrategy(jenkinsRemotingServiceMock, detectArgumentService, null, logger, defaultProxyHelper, OperatingSystemType.MAC, null);
        String expectedEscapedString = "\\|\\&\\;\\<\\>\\(\\)\\$\\`\\\\\\\"\\'\\ \\\t\\*\\?\\[\\#\\~\\=\\%,";
        String escapedString = detectScriptStrategy.getArgumentEscaper().apply(unescapedSpecialCharacters);

        assertEquals(escapedString, expectedEscapedString);
    }

    @Test
    public void testArgumentEscaperWindows() {
        DetectScriptStrategy detectScriptStrategy = new DetectScriptStrategy(jenkinsRemotingServiceMock, detectArgumentService, null, logger, defaultProxyHelper, OperatingSystemType.WINDOWS, null);
        String expectedEscapedString = "`|`&`;`<`>`(`)`$```\\`\"`'` `\t`*`?`[`#`~`=`%`,";
        String escapedString = detectScriptStrategy.getArgumentEscaper().apply(unescapedSpecialCharacters);

        assertEquals(expectedEscapedString, escapedString);
    }

}
