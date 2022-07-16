package com.synopsys.integration.jenkins.detect.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.wrapper.JenkinsVersionHelper;
import com.synopsys.integration.log.LogLevel;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.model.TaskListener;

public class DetectArgumentServiceTest {
    private final JenkinsVersionHelper jenkinsVersionHelper = Mockito.mock(JenkinsVersionHelper.class);

    private static final String errorMessage = "Output Detect Command does not contain: ";
    private static final String loggingLevelKey = "--logging.level.com.synopsys.integration";
    private static final String expectedTestInvocationParameter = "TestInvocationParameter";
    private static final String expectedJenkinsVersion = "JenkinsVersion";
    private static final String pluginName = "blackduck-detect";
    private static final String expectedJenkinsPluginVersion = "JenkinsPluginVersion";
    private static final String jenkinsVersionParam = "--detect.phone.home.passthrough.jenkins.version";
    private static final String pluginVersionParam = "--detect.phone.home.passthrough.jenkins.plugin.version";

    private final Function<String, String> strategyEscaper = Function.identity();
    private final List<String> invocationParameters = Collections.singletonList(expectedTestInvocationParameter);

    private IntEnvironmentVariables intEnvironmentVariables;
    private ByteArrayOutputStream byteArrayOutputStream;

    private DetectArgumentService detectArgumentService;
    private Map<String, String> inputDetectProperties;
    private Map<String, String> expectedVisibleDetectProperties;
    private Map<String, String> expectedHiddenDetectProperties;

    @BeforeEach
    public void setUp() {
        // Setup logger
        TaskListener taskListener = Mockito.mock(TaskListener.class);
        JenkinsIntLogger jenkinsIntLogger = JenkinsIntLogger.logToListener(taskListener);

        jenkinsIntLogger.setLogLevel(LogLevel.DEBUG);
        intEnvironmentVariables = IntEnvironmentVariables.includeSystemEnv();
        intEnvironmentVariables.put("DETECT_PLUGIN_ESCAPING", "true");

        // Set default expected values. Tests will apply different if needed
        byteArrayOutputStream = new ByteArrayOutputStream();
        Mockito.when(taskListener.getLogger()).thenReturn(new PrintStream(byteArrayOutputStream));
        Mockito.when(jenkinsVersionHelper.getJenkinsVersion()).thenReturn(Optional.of(expectedJenkinsVersion));
        Mockito.when(jenkinsVersionHelper.getPluginVersion(pluginName)).thenReturn(Optional.of(expectedJenkinsPluginVersion));

        // These are the standard input detect properties
        inputDetectProperties = new LinkedHashMap<>();
        inputDetectProperties.put("--detect.docker.passthrough.service.timeout", "1234567890");
        inputDetectProperties.put("--detect.cleanup", "ThisIsFalse");

        // These are the standard expected detect properties. Tests will update if needed.
        // They SHOULD be visible in the output log message and be within the return List
        expectedVisibleDetectProperties = new LinkedHashMap<>();
        expectedVisibleDetectProperties.putAll(inputDetectProperties);
        expectedVisibleDetectProperties.put(loggingLevelKey, jenkinsIntLogger.getLogLevel().toString());

        // These are the automatically appended detect properties added by DetectArgumentService(). Tests will update if needed.
        // They should NOT be visible in the output log message but should be within the return List
        expectedHiddenDetectProperties = new LinkedHashMap<>();
        expectedHiddenDetectProperties.put(jenkinsVersionParam, expectedJenkinsVersion);
        expectedHiddenDetectProperties.put(pluginVersionParam, expectedJenkinsPluginVersion);

        // Create object in test
        detectArgumentService = new DetectArgumentService(jenkinsIntLogger, jenkinsVersionHelper);
    }

    private String createDetectPropertiesInputString() {
        return inputDetectProperties.keySet().stream()
            .map(key -> key + "=" + inputDetectProperties.get(key))
            .collect(Collectors.joining(" "));
    }

    @Test
    public void testGetDetectArguments() {
        List<String> detectCommandLine = detectArgumentService.getDetectArguments(
            intEnvironmentVariables,
            strategyEscaper,
            invocationParameters,
            createDetectPropertiesInputString()
        );

        assertEquals(6, detectCommandLine.size()); // Invocation args (1) + passed args (2) + auto added args (3)
        commonValidation(detectCommandLine, expectedVisibleDetectProperties, expectedHiddenDetectProperties);
    }

    @Test
    public void testShouldEscapeFalse() {
        intEnvironmentVariables.put("DETECT_PLUGIN_ESCAPING", "false");

        List<String> detectCommandLine = detectArgumentService.getDetectArguments(
            intEnvironmentVariables,
            strategyEscaper,
            invocationParameters,
            createDetectPropertiesInputString()
        );

        assertEquals(6, detectCommandLine.size()); // Invocation args (1) + passed args (2) + auto added args (3)
        commonValidation(detectCommandLine, expectedVisibleDetectProperties, expectedHiddenDetectProperties);
    }

    @Test
    public void testVariableReplaced() {
        // Set up intEnvironmentVariables to only contain custom variable to verify search and replace of the same
        IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.empty();
        intEnvironmentVariables.put("TRUST_CERT", "false");
        inputDetectProperties.put("--blackduck.trust.cert", "$TRUST_CERT");
        expectedVisibleDetectProperties.put("--blackduck.trust.cert", "false");

        List<String> detectCommandLine = detectArgumentService.getDetectArguments(
            intEnvironmentVariables,
            strategyEscaper,
            invocationParameters,
            createDetectPropertiesInputString()
        );

        assertEquals(7, detectCommandLine.size()); // Invocation args (1) + passed args (3) + auto added args (3)
        commonValidation(detectCommandLine, expectedVisibleDetectProperties, expectedHiddenDetectProperties);
    }

    @Test
    public void testVariableNotReplaced() {
        inputDetectProperties.put("$VISIBLE", "visible");

        List<String> detectCommandLine = detectArgumentService.getDetectArguments(
            intEnvironmentVariables,
            strategyEscaper,
            invocationParameters,
            createDetectPropertiesInputString()
        );

        assertEquals(7, detectCommandLine.size()); // Invocation args (1) + passed args (3) + auto added args (3)
        commonValidation(detectCommandLine, expectedVisibleDetectProperties, expectedHiddenDetectProperties);

        assertTrue(
            byteArrayOutputStream.toString().contains("A variable may not have been properly replaced in resolved argument: $VISIBLE"),
            "Log should contain message about unable to replace variable."
        );
    }

    @Test
    public void testCustomLogLevel() {
        String expectedCustomLogLevel = "TestLogLevel";
        inputDetectProperties.put(loggingLevelKey, expectedCustomLogLevel);
        expectedVisibleDetectProperties.replace(loggingLevelKey, expectedCustomLogLevel);

        List<String> detectCommandLine = detectArgumentService.getDetectArguments(
            intEnvironmentVariables,
            strategyEscaper,
            invocationParameters,
            createDetectPropertiesInputString()
        );

        assertEquals(6, detectCommandLine.size()); // Invocation args (1) + passed args (3) + auto added args (2)
        commonValidation(detectCommandLine, expectedVisibleDetectProperties, expectedHiddenDetectProperties);
    }

    @Test
    public void testNoDetectArguments() {
        List<String> detectCommandLine = detectArgumentService.getDetectArguments(intEnvironmentVariables, strategyEscaper, invocationParameters, "\t");

        assertEquals(4, detectCommandLine.size()); // Invocation args (1) + passed args (0) + auto added args (3)
        commonValidation(detectCommandLine, new LinkedHashMap<>(), expectedHiddenDetectProperties);
    }

    @Test
    public void testUnknownJenkinsVersion() {
        Mockito.when(jenkinsVersionHelper.getJenkinsVersion()).thenReturn(Optional.empty());
        Mockito.when(jenkinsVersionHelper.getPluginVersion(pluginName)).thenReturn(Optional.empty());
        expectedHiddenDetectProperties.replace(jenkinsVersionParam, expectedJenkinsVersion, "<unknown>");
        expectedHiddenDetectProperties.replace(pluginVersionParam, expectedJenkinsPluginVersion, "<unknown>");

        List<String> detectCommandLine = detectArgumentService.getDetectArguments(
            intEnvironmentVariables,
            strategyEscaper,
            invocationParameters,
            createDetectPropertiesInputString()
        );

        assertEquals(6, detectCommandLine.size()); // Invocation args (1) + passed args (2) + auto added args (3)
        commonValidation(detectCommandLine, expectedVisibleDetectProperties, expectedHiddenDetectProperties);
    }

    @Test
    public void testEmptyIntEnvironmentVariables() {
        IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.empty();

        List<String> detectCommandLine = detectArgumentService.getDetectArguments(
            intEnvironmentVariables,
            strategyEscaper,
            invocationParameters,
            createDetectPropertiesInputString()
        );

        assertEquals(6, detectCommandLine.size()); // Invocation args (1) + passed args (2) + auto added args (3)
        commonValidation(detectCommandLine, expectedVisibleDetectProperties, expectedHiddenDetectProperties);
    }

    private void commonValidation(List<String> detectCommandLine, Map<String, String> expectedVisibleDetectProperties, Map<String, String> expectedHiddenDetectProperties) {
        assertEquals(expectedTestInvocationParameter, detectCommandLine.get(0));

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
