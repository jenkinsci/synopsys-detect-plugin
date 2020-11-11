package com.synopsys.integration.jenkins.detect.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.builder.BuilderPropertyKey;
import com.synopsys.integration.jenkins.detect.extensions.global.DetectGlobalConfig;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.wrapper.JenkinsVersionHelper;
import com.synopsys.integration.jenkins.wrapper.JenkinsWrapper;
import com.synopsys.integration.jenkins.wrapper.SynopsysCredentialsHelper;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.model.TaskListener;

public class DetectEnvironmentServiceTest {
    private final static String expectedJenkinsPluginVersion = "JenkinsPluginVersion";

    private final TaskListener taskListenerMock = Mockito.mock(TaskListener.class);
    private final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    private final JenkinsIntLogger jenkinsIntLogger = new JenkinsIntLogger(taskListenerMock);

    private final JenkinsProxyHelper jenkinsProxyHelper = new JenkinsProxyHelper("proxyHost", 31339, "proxyUsername", "proxyPassword", Collections.emptyList(), "ntlmDomain", "ntlmWorkstation");
    private final JenkinsVersionHelper jenkinsVersionHelperMock = Mockito.mock(JenkinsVersionHelper.class);
    private final JenkinsWrapper jenkinsWrapper = JenkinsWrapper.initializeFromJenkinsJVM();
    private final SynopsysCredentialsHelper synopsysCredentialsHelper = new SynopsysCredentialsHelper(jenkinsWrapper);
    private final JenkinsConfigService jenkinsConfigServiceMock = Mockito.mock(JenkinsConfigService.class);
    private final DetectGlobalConfig detectGlobalConfig = Mockito.mock(DetectGlobalConfig.class);
    private final BlackDuckServerConfigBuilder blackDuckServerConfigBuilderMock = Mockito.mock(BlackDuckServerConfigBuilder.class);

    private DetectEnvironmentService detectEnvironmentService;

    private Map<String, String> environmentVariables = new HashMap<>();
    private Map<BuilderPropertyKey, String> builderEnvironmentVariables = new HashMap<>();
    private Map<String, String> expectedIntEnvironmentVariables = new HashMap<>();

    @BeforeEach
    public void setUp() {
        // Populate input environment variables passed to constructor
        for (int i = 1; i <= 5; i++) {
            environmentVariables.put("env_key_" + i, "env_value_" + i);
        }
        environmentVariables.put("BLACK_DUCK_LOG_LEVEL", "DEBUG");

        // Populate builder environment variables used as a return for mocking
        builderEnvironmentVariables.put(BlackDuckServerConfigBuilder.TIMEOUT_KEY, "120");

        // Setup mocked data
        Mockito.when(taskListenerMock.getLogger()).thenReturn(new PrintStream(byteArrayOutputStream));
        Mockito.when(jenkinsConfigServiceMock.getGlobalConfiguration(DetectGlobalConfig.class)).thenReturn(Optional.of(detectGlobalConfig));
        Mockito.when(detectGlobalConfig.getBlackDuckServerConfigBuilder(jenkinsProxyHelper, synopsysCredentialsHelper)).thenReturn(blackDuckServerConfigBuilderMock);
        Mockito.when(blackDuckServerConfigBuilderMock.getProperties()).thenReturn(builderEnvironmentVariables);

        // Populate expected return IntEnvironmentVariables
        expectedIntEnvironmentVariables.putAll(environmentVariables);
        builderEnvironmentVariables.forEach((key, value) -> expectedIntEnvironmentVariables.put(key.getKey(), value));

        detectEnvironmentService = new DetectEnvironmentService(jenkinsIntLogger, jenkinsProxyHelper, jenkinsVersionHelperMock, synopsysCredentialsHelper, jenkinsConfigServiceMock, environmentVariables);
    }

    @Test
    public void testNoPluginVersionInLog() {
        IntEnvironmentVariables intEnvironmentVariables = detectEnvironmentService.createDetectEnvironment();

        assertEquals(expectedIntEnvironmentVariables, intEnvironmentVariables.getVariables());
        assertTrue(byteArrayOutputStream.toString().contains("Running Synopsys Detect for Jenkins"), "Log should contain default message about what it is running.");
    }

    @Test
    public void testPluginVersionInLog() {
        Mockito.when(jenkinsVersionHelperMock.getPluginVersion("blackduck-detect")).thenReturn(Optional.of(expectedJenkinsPluginVersion));
        IntEnvironmentVariables intEnvironmentVariables = detectEnvironmentService.createDetectEnvironment();

        assertEquals(expectedIntEnvironmentVariables, intEnvironmentVariables.getVariables());
        assertTrue(byteArrayOutputStream.toString().contains(String.format("Running Synopsys Detect for Jenkins version: %s", expectedJenkinsPluginVersion)),
            "Log should contain message, with plugin version, about what it is running.");
    }

    @Test
    public void testNullInDetectGlobalConfig() {
        builderEnvironmentVariables.put(BlackDuckServerConfigBuilder.PASSWORD_KEY, null);
        IntEnvironmentVariables intEnvironmentVariables = detectEnvironmentService.createDetectEnvironment();

        assertEquals(expectedIntEnvironmentVariables, intEnvironmentVariables.getVariables());
    }

    @Test
    public void testNoValidDetectGlobalConfig() {
        Mockito.when(jenkinsConfigServiceMock.getGlobalConfiguration(DetectGlobalConfig.class)).thenReturn(Optional.empty());
        IntEnvironmentVariables intEnvironmentVariables = detectEnvironmentService.createDetectEnvironment();

        // Return should not contain values from builderEnvironmentVariables, so compare against environmentVariables only
        assertEquals(environmentVariables, intEnvironmentVariables.getVariables());
    }

}
