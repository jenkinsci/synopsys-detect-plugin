package com.synopsys.integration.jenkins.detect.substeps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.jenkins.JenkinsVersionHelper;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.model.TaskListener;

public class ParseDetectArgumentsTest {
    private final static JenkinsVersionHelper jenkinsVersionHelper = Mockito.mock(JenkinsVersionHelper.class);

    private final static String errorMessage = "Output Detect Command does not contain: ";
    private final static String loggingLevelKey = "--logging.level.com.synopsys.integration";
    private final static String expectedJavaHome = "/usr/bin/java";
    private final static String expectedDetectPath = "/usr/bin/detect.jar";
    private final static String expectedJenkinsVersion = "JenkinsVersion";
    private final static String expectedJenkinsPluginVersion = "JenkinsPluginVersion";
    private final static String expectedCustomLogLevel = "TestLogLevel";

    private final static String jenkinsVersionParam = "--detect.phone.home.passthrough.jenkins.version";
    private final static String pluginVersionParam = "--detect.phone.home.passthrough.jenkins.plugin.version";

    private final IntEnvironmentVariables intEnvironmentVariables = new IntEnvironmentVariables();
    private final TaskListener taskListener = Mockito.mock(TaskListener.class);
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final JenkinsIntLogger jenkinsIntLogger = new JenkinsIntLogger(taskListener);

    private Map<String, String> inputDetectProperties = new LinkedHashMap<>();
    private Map<String, String> expectedVisibleDetectProperties = new LinkedHashMap<>();
    private Map<String, String> expectedHiddenDetectProperties = new LinkedHashMap<>();

    @BeforeEach
    public void setUp() {
        jenkinsIntLogger.setLogLevel(LogLevel.DEBUG);
        intEnvironmentVariables.put("DETECT_PLUGIN_ESCAPING", "true");

        // Set default expected values. Tests will apply different if needed
        Mockito.when(taskListener.getLogger()).thenReturn(new PrintStream(byteArrayOutputStream));
        Mockito.when(jenkinsVersionHelper.getJenkinsVersion()).thenReturn(Optional.of(expectedJenkinsVersion));
        Mockito.when(jenkinsVersionHelper.getPluginVersion("blackduck-detect")).thenReturn(Optional.of(expectedJenkinsPluginVersion));

        // These are the standard input detect properties
        inputDetectProperties.put("--detect.docker.passthrough.service.timeout", "1234567890");
        inputDetectProperties.put("--detect.cleanup", "ThisIsFalse");

        // These are the standard expected detect properties. Tests will update if needed.
        // They SHOULD be visible in the output log message and be within the return List
        expectedVisibleDetectProperties.putAll(inputDetectProperties);
        expectedVisibleDetectProperties.put(loggingLevelKey, jenkinsIntLogger.getLogLevel().toString());

        // These are the automatically appended detect properties added by parseDetectArguments().
        // They should NOT be visible in the output log message but should be within the return List
        expectedHiddenDetectProperties.put(jenkinsVersionParam, expectedJenkinsVersion);
        expectedHiddenDetectProperties.put(pluginVersionParam, expectedJenkinsPluginVersion);
    }

    private String createDetectPropertiesInputString() {
        return inputDetectProperties.keySet().stream()
                   .map(key -> key + "=" + inputDetectProperties.get(key) + " ")
                   .collect(Collectors.joining());
    }

    @Test
    public void testParseDetectArgumentsJar() {
        DetectSetupResponse detectSetupResponse = new DetectSetupResponse(DetectSetupResponse.ExecutionStrategy.JAR, expectedJavaHome, expectedDetectPath);
        ParseDetectArguments parseDetectArguments = new ParseDetectArguments(jenkinsIntLogger, intEnvironmentVariables, jenkinsVersionHelper, detectSetupResponse, createDetectPropertiesInputString());
        List<String> detectCommandLine = parseDetectArguments.parseDetectArguments();

        assertEquals(8, detectCommandLine.size()); // Jar args (3) + passed args (2) + auto added args (3)
        assertEquals(expectedJavaHome, detectCommandLine.get(0));
        assertEquals("-jar", detectCommandLine.get(1));
        assertEquals(expectedDetectPath, detectCommandLine.get(2));
        commonValidation(detectCommandLine);
    }

    @Test
    public void testParseDetectArgumentsShellScript() {
        DetectSetupResponse detectSetupResponse = new DetectSetupResponse(DetectSetupResponse.ExecutionStrategy.SHELL_SCRIPT, expectedJavaHome, expectedDetectPath);
        ParseDetectArguments parseDetectArguments = new ParseDetectArguments(jenkinsIntLogger, intEnvironmentVariables, jenkinsVersionHelper, detectSetupResponse, createDetectPropertiesInputString());
        List<String> detectCommandLine = parseDetectArguments.parseDetectArguments();

        assertEquals(7, detectCommandLine.size()); // Shell args (2) + passed args (2) + auto added args (3)
        assertEquals("bash", detectCommandLine.get(0));
        assertEquals(expectedDetectPath, detectCommandLine.get(1));
        commonValidation(detectCommandLine);
    }

    @Test
    public void testParseDetectArgumentsPowerShellScript() {
        DetectSetupResponse detectSetupResponse = new DetectSetupResponse(DetectSetupResponse.ExecutionStrategy.POWERSHELL_SCRIPT, expectedJavaHome, expectedDetectPath);
        ParseDetectArguments parseDetectArguments = new ParseDetectArguments(jenkinsIntLogger, intEnvironmentVariables, jenkinsVersionHelper, detectSetupResponse, createDetectPropertiesInputString());
        List<String> detectCommandLine = parseDetectArguments.parseDetectArguments();
        String expectedPowerShellJavaCommand = "\"Import-Module '" + expectedDetectPath + "'; detect\"";

        assertEquals(7, detectCommandLine.size()); // PowerShell args (2) + passed args (2) + auto added args (3)
        assertEquals("powershell", detectCommandLine.get(0));
        assertEquals(expectedPowerShellJavaCommand, detectCommandLine.get(1));
        commonValidation(detectCommandLine);
    }

    @Test
    public void testParseDetectArgumentsShouldEscapeFalse() {
        intEnvironmentVariables.put("DETECT_PLUGIN_ESCAPING", "false");

        DetectSetupResponse detectSetupResponse = new DetectSetupResponse(DetectSetupResponse.ExecutionStrategy.JAR, expectedJavaHome, expectedDetectPath);
        ParseDetectArguments parseDetectArguments = new ParseDetectArguments(jenkinsIntLogger, intEnvironmentVariables, jenkinsVersionHelper, detectSetupResponse, createDetectPropertiesInputString());
        List<String> detectCommandLine = parseDetectArguments.parseDetectArguments();

        assertEquals(8, detectCommandLine.size()); // Jar args (2) + passed args (2) + auto added args (3)
        assertEquals(expectedJavaHome, detectCommandLine.get(0));
        assertEquals("-jar", detectCommandLine.get(1));
        assertEquals(expectedDetectPath, detectCommandLine.get(2));
        commonValidation(detectCommandLine);
    }

    @Test
    public void testParseDetectArgumentsCustomLogLevel() {
        inputDetectProperties.put(loggingLevelKey, expectedCustomLogLevel);
        expectedVisibleDetectProperties.replace(loggingLevelKey, expectedCustomLogLevel);

        DetectSetupResponse detectSetupResponse = new DetectSetupResponse(DetectSetupResponse.ExecutionStrategy.JAR, expectedJavaHome, expectedDetectPath);
        ParseDetectArguments parseDetectArguments = new ParseDetectArguments(jenkinsIntLogger, intEnvironmentVariables, jenkinsVersionHelper, detectSetupResponse, createDetectPropertiesInputString());
        List<String> detectCommandLine = parseDetectArguments.parseDetectArguments();

        assertEquals(8, detectCommandLine.size()); // Jar args (2) + passed args (3) + auto added args (2)
        assertEquals(expectedJavaHome, detectCommandLine.get(0));
        assertEquals("-jar", detectCommandLine.get(1));
        assertEquals(expectedDetectPath, detectCommandLine.get(2));
        commonValidation(detectCommandLine);
    }

    @Test
    public void testParseDetectArgumentsUnknownJenkinsVersion() {
        Mockito.when(jenkinsVersionHelper.getJenkinsVersion()).thenReturn(Optional.empty());
        Mockito.when(jenkinsVersionHelper.getPluginVersion("blackduck-detect")).thenReturn(Optional.empty());

        DetectSetupResponse detectSetupResponse = new DetectSetupResponse(DetectSetupResponse.ExecutionStrategy.JAR, expectedJavaHome, expectedDetectPath);
        ParseDetectArguments parseDetectArguments = new ParseDetectArguments(jenkinsIntLogger, intEnvironmentVariables, jenkinsVersionHelper, detectSetupResponse, "");
        List<String> detectCommandLine = parseDetectArguments.parseDetectArguments();

        assertEquals(6, detectCommandLine.size()); // Jar args (3) + passed args (0) + auto added args (3)
        assertTrue(detectCommandLine.stream().anyMatch(argument -> argument.contains(jenkinsVersionParam + "=<unknown>")), errorMessage + "correct jenkins version");
        assertTrue(detectCommandLine.stream().anyMatch(argument -> argument.contains(pluginVersionParam + "=<unknown>")), errorMessage + "correct plugins version");
    }

    @Test
    public void testParseDetectArgumentsVariableNotReplaced() {
        // This is used to validate a log entry from calling handleVariableReplacement()
        inputDetectProperties.put("$VISIBLE", "visible");

        DetectSetupResponse detectSetupResponse = new DetectSetupResponse(DetectSetupResponse.ExecutionStrategy.JAR, expectedJavaHome, expectedDetectPath);
        ParseDetectArguments parseDetectArguments = new ParseDetectArguments(jenkinsIntLogger, intEnvironmentVariables, jenkinsVersionHelper, detectSetupResponse, createDetectPropertiesInputString());
        List<String> detectCommandLine = parseDetectArguments.parseDetectArguments();

        // Validate log entry from handleVariableReplacement()
        assertTrue(byteArrayOutputStream.toString().contains("Variable may not have been properly replaced. Argument: $VISIBLE=visible"), "Log should contain message about unable to replace variable.");
    }

    private void commonValidation(List<String> detectCommandLine) {
        // Validate that detectCommandLine DOES contain all the visible and hidden properties
        validateMapProperties(expectedVisibleDetectProperties, detectCommandLine);
        validateMapProperties(expectedHiddenDetectProperties, detectCommandLine);

        // Validate the log contents
        String log = byteArrayOutputStream.toString();
        assertTrue(log.contains("Running Detect command: "), "Output log should contain : Running Detect command: ");
        for (String key : expectedHiddenDetectProperties.keySet()) {
            assertFalse(log.contains(key), "Output log should not contain : " + key);
        }
    }

    private void validateMapProperties(Map<String, String> detectProperties, List<String> detectCommandLine) {
        detectProperties.forEach((key, value) -> {
            assertTrue(detectCommandLine.stream().anyMatch(argument -> argument.contains(key)), errorMessage + key);
            assertTrue(detectCommandLine.stream().anyMatch(argument -> argument.contains("=" + value)), errorMessage + "=" + value);
        });
    }

}
