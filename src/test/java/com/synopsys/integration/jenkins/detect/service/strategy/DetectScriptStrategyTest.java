package com.synopsys.integration.jenkins.detect.service.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
        defaultLogger = new JenkinsIntLogger(mockedTaskListener);
        defaultProxyHelper = new JenkinsProxyHelper();
    }

    @Test
    public void testNoProxyDetermineable() {
        String expectedExceptionMessage = "expected test message";

        JenkinsProxyHelper mockedProxyHelper = Mockito.mock(JenkinsProxyHelper.class);
        Mockito.when(mockedProxyHelper.getProxyInfo(Mockito.anyString())).thenThrow(new IllegalArgumentException(expectedExceptionMessage));
        DetectScriptStrategy detectScriptStrategy = new DetectScriptStrategy(defaultLogger, mockedProxyHelper, OperatingSystemType.LINUX, null);
        detectScriptStrategy.getSetupCallable();

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

    @Test
    public void testInitialArgumentsLinux() {
        DetectScriptStrategy detectScriptStrategy = new DetectScriptStrategy(defaultLogger, defaultProxyHelper, OperatingSystemType.LINUX, null);
        String remoteExecutablePath = "/path/to/detect.sh";

        List<String> expectedInitialArguments = Arrays.asList("bash", remoteExecutablePath);
        assertEquals(expectedInitialArguments, detectScriptStrategy.getInitialArguments(remoteExecutablePath));
    }

    @Test
    public void testInitialArgumentsMac() {
        DetectScriptStrategy detectScriptStrategy = new DetectScriptStrategy(defaultLogger, defaultProxyHelper, OperatingSystemType.MAC, null);
        String remoteExecutablePath = "/path/to/detect.sh";

        List<String> expectedInitialArguments = Arrays.asList("bash", remoteExecutablePath);
        assertEquals(expectedInitialArguments, detectScriptStrategy.getInitialArguments(remoteExecutablePath));
    }

    @Test
    public void testInitialArgumentsWindows() {
        DetectScriptStrategy detectScriptStrategy = new DetectScriptStrategy(defaultLogger, defaultProxyHelper, OperatingSystemType.WINDOWS, null);
        String remoteExecutablePath = "C:\\path\\to\\detect.ps1";

        List<String> expectedInitialArguments = Arrays.asList("powershell", "\"Import-Module 'C:\\path\\to\\detect.ps1'; detect\"");
        assertEquals(expectedInitialArguments, detectScriptStrategy.getInitialArguments(remoteExecutablePath));
    }

}
