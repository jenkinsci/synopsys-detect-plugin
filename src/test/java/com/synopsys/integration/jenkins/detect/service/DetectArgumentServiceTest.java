package com.synopsys.integration.jenkins.detect.service;

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

import com.synopsys.integration.jenkins.detect.service.strategy.DetectJarStrategy;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.wrapper.JenkinsVersionHelper;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.model.TaskListener;

public class DetectArgumentServiceTest {
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

    private final DetectJarStrategy detectJarStrategy = new DetectJarStrategy(jenkinsIntLogger, intEnvironmentVariables, expectedJavaHome, expectedDetectPath);
    private final DetectArgumentService detectArgumentService = new DetectArgumentService(jenkinsIntLogger, jenkinsVersionHelper);

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

        // These are the automatically appended detect properties added by DetectArgumentService().
        // They should NOT be visible in the output log message but should be within the return List
        expectedHiddenDetectProperties.put(jenkinsVersionParam, expectedJenkinsVersion);
        expectedHiddenDetectProperties.put(pluginVersionParam, expectedJenkinsPluginVersion);

        DetectJarStrategy detectJarStrategy = new DetectJarStrategy(jenkinsIntLogger, intEnvironmentVariables, expectedJavaHome, expectedDetectPath);
        DetectArgumentService detectArgumentService = new DetectArgumentService(jenkinsIntLogger, jenkinsVersionHelper);
    }

    private String createDetectPropertiesInputString() {
        return inputDetectProperties.keySet().stream()
                   .map(key -> key + "=" + inputDetectProperties.get(key) + " ")
                   .collect(Collectors.joining());
    }

    @Test
    public void testDetectArgumentService() {
        List<String> detectCommandLine = detectArgumentService
                                             .parseDetectArgumentString(intEnvironmentVariables, detectJarStrategy.getArgumentEscaper(), detectJarStrategy.getInitialArguments(expectedJavaHome), createDetectPropertiesInputString());

        assertEquals(8, detectCommandLine.size()); // Jar args (3) + passed args (2) + auto added args (3)
        commonValidation(detectCommandLine);
    }

    @Test
    public void testShouldEscapeFalse() {
        intEnvironmentVariables.put("DETECT_PLUGIN_ESCAPING", "false");

        List<String> detectCommandLine = detectArgumentService
                                             .parseDetectArgumentString(intEnvironmentVariables, detectJarStrategy.getArgumentEscaper(), detectJarStrategy.getInitialArguments(expectedJavaHome), createDetectPropertiesInputString());

        assertEquals(8, detectCommandLine.size()); // Jar args (3) + passed args (2) + auto added args (3)
        commonValidation(detectCommandLine);
    }

    @Test
    public void testVariableReplaced() {
        // Set up intEnvironmentVariables to only contain custom variable to verify search and replace of the same
        IntEnvironmentVariables intEnvironmentVariables = new IntEnvironmentVariables(false);
        intEnvironmentVariables.put("TRUST_CERT", "false");
        inputDetectProperties.put("--blackduck.trust.cert", "$TRUST_CERT");
        expectedVisibleDetectProperties.put("--blackduck.trust.cert", "false");

        List<String> detectCommandLine = detectArgumentService
                                             .parseDetectArgumentString(intEnvironmentVariables, detectJarStrategy.getArgumentEscaper(), detectJarStrategy.getInitialArguments(expectedJavaHome), createDetectPropertiesInputString());

        assertEquals(9, detectCommandLine.size()); // Jar args (3) + passed args (3) + auto added args (3)
        commonValidation(detectCommandLine);
    }

    @Test
    public void testVariableNotReplaced() {
        // This is used to validate a log entry from calling handleVariableReplacement()
        inputDetectProperties.put("$VISIBLE", "visible");

        List<String> detectCommandLine = detectArgumentService
                                             .parseDetectArgumentString(intEnvironmentVariables, detectJarStrategy.getArgumentEscaper(), detectJarStrategy.getInitialArguments(expectedJavaHome), createDetectPropertiesInputString());

        assertEquals(9, detectCommandLine.size()); // Jar args (3) + passed args (3) + auto added args (3)
        commonValidation(detectCommandLine);

        // Validate log entry from handleVariableReplacement()
        assertTrue(byteArrayOutputStream.toString().contains("Variable may not have been properly replaced. Argument: $VISIBLE=visible"), "Log should contain message about unable to replace variable.");
    }

    @Test
    public void testCustomLogLevel() {
        inputDetectProperties.put(loggingLevelKey, expectedCustomLogLevel);
        expectedVisibleDetectProperties.replace(loggingLevelKey, expectedCustomLogLevel);

        List<String> detectCommandLine = detectArgumentService
                                             .parseDetectArgumentString(intEnvironmentVariables, detectJarStrategy.getArgumentEscaper(), detectJarStrategy.getInitialArguments(expectedJavaHome), createDetectPropertiesInputString());

        assertEquals(8, detectCommandLine.size()); // Jar args (3) + passed args (3) + auto added args (2)
        commonValidation(detectCommandLine);
    }

    @Test
    public void testNoDetectArguments() {
        expectedVisibleDetectProperties.clear();

        List<String> detectCommandLine = detectArgumentService
                                             .parseDetectArgumentString(intEnvironmentVariables, detectJarStrategy.getArgumentEscaper(), detectJarStrategy.getInitialArguments(expectedJavaHome), "");

        assertEquals(6, detectCommandLine.size()); // Jar args (3) + passed args (0) + auto added args (3)
        commonValidation(detectCommandLine);
    }

    @Test
    public void testUnknownJenkinsVersion() {
        // Mock and set expectedHiddenDetectProperties for <unknown> Jenkins version properties
        Mockito.when(jenkinsVersionHelper.getJenkinsVersion()).thenReturn(Optional.empty());
        Mockito.when(jenkinsVersionHelper.getPluginVersion("blackduck-detect")).thenReturn(Optional.empty());
        expectedHiddenDetectProperties.replace(jenkinsVersionParam, expectedJenkinsVersion, "<unknown>");
        expectedHiddenDetectProperties.replace(pluginVersionParam, expectedJenkinsPluginVersion, "<unknown>");

        List<String> detectCommandLine = detectArgumentService
                                             .parseDetectArgumentString(intEnvironmentVariables, detectJarStrategy.getArgumentEscaper(), detectJarStrategy.getInitialArguments(expectedJavaHome), createDetectPropertiesInputString());

        assertEquals(8, detectCommandLine.size()); // Jar args (3) + passed args (2) + auto added args (3)
        commonValidation(detectCommandLine);
    }

    @Test
    public void testEmptyIntEnvironmentVariables() {
        IntEnvironmentVariables intEnvironmentVariables = new IntEnvironmentVariables(false);

        List<String> detectCommandLine = detectArgumentService
                                             .parseDetectArgumentString(intEnvironmentVariables, detectJarStrategy.getArgumentEscaper(), detectJarStrategy.getInitialArguments(expectedJavaHome), createDetectPropertiesInputString());

        assertEquals(8, detectCommandLine.size()); // Jar args (3) + passed args (2) + auto added args (3)
        commonValidation(detectCommandLine);
    }

    private void commonValidation(List<String> detectCommandLine) {
        assertEquals(expectedJavaHome, detectCommandLine.get(0));
        assertEquals("-jar", detectCommandLine.get(1));
        assertEquals(expectedDetectPath, detectCommandLine.get(2));

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
