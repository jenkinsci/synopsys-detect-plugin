package com.synopsys.integration.jenkins.detect.service.strategy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

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

    @BeforeEach
    public void setUpMocks() {
        TaskListener mockedTaskListener = Mockito.mock(TaskListener.class);
        Mockito.when(mockedTaskListener.getLogger()).thenReturn(System.out);
        defaultLogger = new JenkinsIntLogger(mockedTaskListener);
        defaultProxyHelper = new JenkinsProxyHelper();
    }

    @Test
    public void testNoProxyDetermineable() {
        DetectScriptStrategy detectScriptStrategy = new DetectScriptStrategy(defaultLogger, defaultProxyHelper, OperatingSystemType.LINUX, null);

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

    @Test
    public void testDownloadShellScript() {
        downloadAndValidateScript(OperatingSystemType.LINUX);
    }

    @Test
    public void testDownloadPowershellScript() {
        downloadAndValidateScript(OperatingSystemType.WINDOWS);
    }

    @Test
    public void testFailureToDownload() {
        String toolsDirectory = createTemporaryDownloadDirectory();
        assumeTrue(new File(toolsDirectory).setReadOnly(), "Skipping test because we can't modify file permissions.");

        DetectScriptStrategy detectScriptStrategy = new DetectScriptStrategy(defaultLogger, defaultProxyHelper, OperatingSystemType.determineFromSystem(), toolsDirectory);
        assertThrows(IntegrationException.class, detectScriptStrategy.getSetupCallable()::call);
    }

    private void downloadAndValidateScript(OperatingSystemType operatingSystemType) {
        try {
            String toolsDirectory = createTemporaryDownloadDirectory();
            String expectedScriptPath = new File(toolsDirectory, DetectScriptStrategy.DETECT_INSTALL_DIRECTORY).getPath();

            DetectScriptStrategy detectScriptStrategy = new DetectScriptStrategy(defaultLogger, defaultProxyHelper, operatingSystemType, toolsDirectory);
            String remoteScriptPath = detectScriptStrategy.getSetupCallable().call();
            File remoteScriptFile = new File(remoteScriptPath);

            assertEquals(expectedScriptPath, remoteScriptFile.getParent(), "Script should have been downloaded to " + expectedScriptPath + " but wasn't.");
            assertTrue(remoteScriptFile.exists(), "A script should exist at " + remoteScriptPath + " but it doesn't");
        } catch (IntegrationException e) {
            fail("Unexpected exception occurred: ", e);
        }
    }

    private String createTemporaryDownloadDirectory() {
        String tempDirectory = "";

        try {
            File downloadDirectory = Files.createTempDirectory("testDetectScriptStrategy").toFile();
            downloadDirectory.deleteOnExit();
            tempDirectory = downloadDirectory.getCanonicalPath();
        } catch (IOException e) {
            assumeTrue(false, "Skipping test, could not create temporary directory: " + e.getMessage());
        }

        return tempDirectory;
    }

}
