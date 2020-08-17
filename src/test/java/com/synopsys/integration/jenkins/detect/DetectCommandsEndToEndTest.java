package com.synopsys.integration.jenkins.detect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.builder.BuilderPropertyKey;
import com.synopsys.integration.jenkins.detect.exception.DetectJenkinsException;
import com.synopsys.integration.jenkins.detect.extensions.global.DetectGlobalConfig;
import com.synopsys.integration.jenkins.detect.service.DetectArgumentService;
import com.synopsys.integration.jenkins.detect.service.DetectEnvironmentService;
import com.synopsys.integration.jenkins.detect.service.DetectServicesFactory;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectJarStrategy;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectScriptStrategy;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectStrategyService;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsBuildService;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.wrapper.JenkinsVersionHelper;
import com.synopsys.integration.jenkins.wrapper.SynopsysCredentialsHelper;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.OperatingSystemType;

public class DetectCommandsEndToEndTest {
    private static final String DETECT_PROPERTY_INPUT = "--detect.docker.passthrough.service.timeout=$BLACKDUCK_TIMEOUT --detect.cleanup=false --detect.project.name=\"Test Project'\"";
    private static final String WORKSPACE_TMP_REL_PATH = "out/test/DetectPostBuildStepTest/testPerform/workspace@tmp";
    private static final String JDK_HOME = "/tmp/jdk/bin/java";
    private static final String JAVA_HOME_VALUE = System.getenv("JAVA_HOME");
    private static final String DETECT_JAR_PATH = "/tmp/detect.jar";
    private static final String DETECT_SHELL_PATH = "/tmp/detect.sh";
    private static final String DETECT_POWERSHELL_PATH = "/tmp/detect.ps1";

    private JenkinsRemotingService jenkinsRemotingService;
    private DetectServicesFactory detectServicesFactory;
    private JenkinsProxyHelper mockedProxyHelper;
    private JenkinsVersionHelper mockedVersionHelper;
    private JenkinsConfigService jenkinsConfigService;
    private JenkinsIntLogger jenkinsIntLogger;

    @BeforeEach
    public void setUpMocks() {
        try {
            jenkinsIntLogger = new JenkinsIntLogger(null);
            Map<BuilderPropertyKey, String> builderEnvironmentVariables = new HashMap<>();
            builderEnvironmentVariables.put(BlackDuckServerConfigBuilder.TIMEOUT_KEY, "120");

            BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = Mockito.mock(BlackDuckServerConfigBuilder.class);
            Mockito.when(blackDuckServerConfigBuilder.getProperties()).thenReturn(builderEnvironmentVariables);

            DetectGlobalConfig detectGlobalConfig = Mockito.mock(DetectGlobalConfig.class);
            Mockito.when(detectGlobalConfig.getBlackDuckServerConfigBuilder(Mockito.any(), Mockito.any())).thenReturn(blackDuckServerConfigBuilder);

            mockedProxyHelper = Mockito.mock(JenkinsProxyHelper.class);
            Mockito.when(mockedProxyHelper.getProxyInfo(Mockito.anyString())).thenReturn(ProxyInfo.NO_PROXY_INFO);

            mockedVersionHelper = Mockito.mock(JenkinsVersionHelper.class);

            jenkinsRemotingService = Mockito.mock(JenkinsRemotingService.class);
            Mockito.when(jenkinsRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(0);
            Mockito.when(jenkinsRemotingService.call(Mockito.any(DetectJarStrategy.SetupCallableImpl.class))).thenReturn(JDK_HOME);

            jenkinsConfigService = Mockito.mock(JenkinsConfigService.class);
            Mockito.when(jenkinsConfigService.getGlobalConfiguration(DetectGlobalConfig.class)).thenReturn(Optional.of(detectGlobalConfig));

            DetectArgumentService detectArgumentService = new DetectArgumentService(jenkinsIntLogger, mockedVersionHelper);
            DetectStrategyService detectStrategyService = new DetectStrategyService(jenkinsIntLogger, mockedProxyHelper, WORKSPACE_TMP_REL_PATH);

            detectServicesFactory = Mockito.mock(DetectServicesFactory.class);
            Mockito.when(detectServicesFactory.createJenkinsRemotingService()).thenReturn(jenkinsRemotingService);
            Mockito.when(detectServicesFactory.createJenkinsConfigService()).thenReturn(jenkinsConfigService);
            Mockito.when(detectServicesFactory.createDetectArgumentService()).thenReturn(detectArgumentService);
            Mockito.when(detectServicesFactory.createDetectStrategyService()).thenReturn(detectStrategyService);
        } catch (Throwable t) {
            fail("An unexpected exception was thrown by the test code. The test likely requires fixing: ", t);
        }
    }

    @Test
    public void testPostBuildJar() {
        Map<String, String> environment = new HashMap<>();
        environment.put(DetectJenkinsEnvironmentVariable.USER_PROVIDED_JAR_PATH.stringValue(), DETECT_JAR_PATH);

        mockEnvironment(environment);
        validateRunDetectPostBuild(OperatingSystemType.LINUX, DETECT_JAR_PATH);

        validateJarOutput(JAVA_HOME_VALUE);
    }

    @Test
    public void testPostBuildShell() {
        mockEnvironment(new HashMap<>());
        validateRunDetectPostBuild(OperatingSystemType.LINUX, DETECT_SHELL_PATH);

        validateShellOutput(JAVA_HOME_VALUE);
    }

    @Test
    public void testPostBuildPowerShell() {
        mockEnvironment(new HashMap<>());
        validateRunDetectPostBuild(OperatingSystemType.WINDOWS, DETECT_POWERSHELL_PATH);

        validatePowershellOutput(JAVA_HOME_VALUE);
    }

    @Test
    public void testPipelineJar() {
        Map<String, String> environment = new HashMap<>();
        environment.put(DetectJenkinsEnvironmentVariable.USER_PROVIDED_JAR_PATH.stringValue(), DETECT_JAR_PATH);

        mockEnvironment(environment);
        validateRunDetectPipeline(OperatingSystemType.LINUX, DETECT_JAR_PATH);

        validateJarOutput(JAVA_HOME_VALUE);
    }

    @Test
    public void testPipelineShell() {
        mockEnvironment(new HashMap<>());
        validateRunDetectPostBuild(OperatingSystemType.LINUX, DETECT_SHELL_PATH);

        validateShellOutput(JAVA_HOME_VALUE);
    }

    @Test
    public void testPipelinePowershell() {
        mockEnvironment(new HashMap<>());
        validateRunDetectPipeline(OperatingSystemType.WINDOWS, DETECT_POWERSHELL_PATH);

        validatePowershellOutput(JAVA_HOME_VALUE);
    }

    private void mockEnvironment(Map<String, String> environmentVariables) {
        SynopsysCredentialsHelper mockedCredentialsHelper = Mockito.mock(SynopsysCredentialsHelper.class);
        DetectEnvironmentService detectEnvironmentService = new DetectEnvironmentService(jenkinsIntLogger, mockedProxyHelper, mockedVersionHelper, mockedCredentialsHelper, jenkinsConfigService, environmentVariables);
        Mockito.when(detectServicesFactory.createDetectEnvironmentService()).thenReturn(detectEnvironmentService);
    }

    private void validateRunDetectPostBuild(OperatingSystemType operatingSystemType, String detectPath) {
        try {
            // Build service only matters for PostBuild
            JenkinsBuildService jenkinsBuildService = Mockito.mock(JenkinsBuildService.class);
            Mockito.when(jenkinsBuildService.getJDKRemoteHomeOrEmpty()).thenReturn(Optional.of("/tmp/jdk"));
            Mockito.when(detectServicesFactory.createJenkinsBuildService()).thenReturn(jenkinsBuildService);

            Mockito.when(jenkinsRemotingService.call(Mockito.any(DetectScriptStrategy.SetupCallableImpl.class))).thenReturn(detectPath);
            Mockito.when(jenkinsRemotingService.getRemoteOperatingSystemType()).thenReturn(operatingSystemType);

            // run the method we're testing
            DetectCommands detectCommands = new DetectCommands(detectServicesFactory);
            detectCommands.runDetectPostBuild(DETECT_PROPERTY_INPUT);

            // verify successful result
            Mockito.verify(jenkinsBuildService, Mockito.never()).markBuildAborted();
            Mockito.verify(jenkinsBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
            Mockito.verify(jenkinsBuildService, Mockito.never()).markBuildFailed(Mockito.any(String.class));
            Mockito.verify(jenkinsBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));
        } catch (Throwable t) {
            fail("An unexpected exception was thrown by the test code. The test likely requires fixing: ", t);
        }
    }

    private void validateRunDetectPipeline(OperatingSystemType operatingSystemType, String detectPath) {
        try {
            Mockito.when(jenkinsRemotingService.call(Mockito.any(DetectScriptStrategy.SetupCallableImpl.class))).thenReturn(detectPath);
            Mockito.when(jenkinsRemotingService.getRemoteOperatingSystemType()).thenReturn(operatingSystemType);

            // run the method we're testing
            DetectCommands detectCommands = new DetectCommands(detectServicesFactory);
            detectCommands.runDetectPipeline(false, DETECT_PROPERTY_INPUT);
        } catch (DetectJenkinsException e) {
            fail("The Detect execution returned a failing status code instead of a successful status code: " + e.getMessage());
        } catch (Throwable t) {
            fail("An unexpected exception was thrown by the test code. The test likely requires fixing: ", t);
        }
    }

    private void validateJarOutput(String javaHomePath) {
        List<String> actualCommand = captureDetectCommands(javaHomePath);

        int i = 0;
        assertEquals(JDK_HOME, actualCommand.get(i++));
        assertEquals("-jar", actualCommand.get(i++));
        assertEquals(DETECT_JAR_PATH, actualCommand.get(i++));
        assertEquals("--detect.docker.passthrough.service.timeout=120", actualCommand.get(i++));
        assertEquals("--detect.cleanup=false", actualCommand.get(i++));
        assertEquals("--detect.project.name=Test Project'", actualCommand.get(i++));
        assertEquals("--logging.level.com.synopsys.integration=INFO", actualCommand.get(i++));
        assertTrue(actualCommand.get(i++).startsWith("--detect.phone.home.passthrough.jenkins.version="));
        assertTrue(actualCommand.get(i).startsWith("--detect.phone.home.passthrough.jenkins.plugin.version="));
    }

    private void validateShellOutput(String javaHomePath) {
        List<String> actualCommand = captureDetectCommands(javaHomePath);

        int i = 0;
        assertEquals("bash", actualCommand.get(i++));
        assertEquals(DETECT_SHELL_PATH, actualCommand.get(i++));
        assertEquals("--detect.docker.passthrough.service.timeout\\=120", actualCommand.get(i++));
        assertEquals("--detect.cleanup\\=false", actualCommand.get(i++));
        assertEquals("--detect.project.name\\=Test\\ Project\\'", actualCommand.get(i++));
        assertEquals("--logging.level.com.synopsys.integration\\=INFO", actualCommand.get(i++));
        assertTrue(actualCommand.get(i++).startsWith("--detect.phone.home.passthrough.jenkins.version\\="));
        assertTrue(actualCommand.get(i).startsWith("--detect.phone.home.passthrough.jenkins.plugin.version\\="));
    }

    private void validatePowershellOutput(String javaHomePath) {
        List<String> actualCommand = captureDetectCommands(javaHomePath);

        int i = 0;
        assertEquals("powershell", actualCommand.get(i++));
        assertEquals("\"Import-Module '" + DETECT_POWERSHELL_PATH + "'; detect\"", actualCommand.get(i++));
        assertEquals("--detect.docker.passthrough.service.timeout`=120", actualCommand.get(i++));
        assertEquals("--detect.cleanup`=false", actualCommand.get(i++));
        assertEquals("--detect.project.name`=Test` Project`'", actualCommand.get(i++));
        assertEquals("--logging.level.com.synopsys.integration`=INFO", actualCommand.get(i++));
        assertTrue(actualCommand.get(i++).startsWith("--detect.phone.home.passthrough.jenkins.version`="));
        assertTrue(actualCommand.get(i).startsWith("--detect.phone.home.passthrough.jenkins.plugin.version`="));
    }

    private List<String> captureDetectCommands(String javaHomePath) {
        try {
            // get the Detect command line that was constructed to return to calling test for validation
            ArgumentCaptor<List<String>> cmdsArgCapture = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<IntEnvironmentVariables> detectEnvCapture = ArgumentCaptor.forClass(IntEnvironmentVariables.class);
            Mockito.verify(jenkinsRemotingService).launch(detectEnvCapture.capture(), cmdsArgCapture.capture());

            // verify that the system env is NOT inherited (use JAVA_HOME, if set)
            assertNotEquals(javaHomePath, detectEnvCapture.getValue().getValue("JAVA_HOME"));

            return cmdsArgCapture.getValue();
        } catch (Exception e) {
            fail("An unexpected exception was thrown by the test code. The test likely requires fixing: ", e);
            return Collections.emptyList();
        }
    }
}
