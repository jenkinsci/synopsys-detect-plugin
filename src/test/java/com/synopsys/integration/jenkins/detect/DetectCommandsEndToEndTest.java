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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.builder.BuilderPropertyKey;
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

    @Test
    public void testPerformJar() {
        final String detectPath = "/tmp/detect.jar";
        Map<String, String> environment = new HashMap<>();
        environment.put(DetectJenkinsEnvironmentVariable.USER_PROVIDED_JAR_PATH.stringValue(), detectPath);

        List<String> actualCmd = doTest(environment, OperatingSystemType.LINUX, detectPath, JAVA_HOME_VALUE);

        int i = 0;
        assertEquals(JDK_HOME, actualCmd.get(i++));
        assertEquals("-jar", actualCmd.get(i++));
        assertEquals(detectPath, actualCmd.get(i++));
        assertEquals("--detect.docker.passthrough.service.timeout=120", actualCmd.get(i++));
        assertEquals("--detect.cleanup=false", actualCmd.get(i++));
        assertEquals("--detect.project.name=Test Project'", actualCmd.get(i++));
        assertEquals("--logging.level.com.synopsys.integration=INFO", actualCmd.get(i++));
        assertTrue(actualCmd.get(i++).startsWith("--detect.phone.home.passthrough.jenkins.version="));
        assertTrue(actualCmd.get(i).startsWith("--detect.phone.home.passthrough.jenkins.plugin.version="));
    }

    @Test
    public void testPerformShell() {
        final String detectPath = "/tmp/detect.sh";

        List<String> actualCmd = doTest(new HashMap<>(), OperatingSystemType.LINUX, detectPath, JAVA_HOME_VALUE);

        int i = 0;
        assertEquals("bash", actualCmd.get(i++));
        assertEquals(detectPath, actualCmd.get(i++));
        assertEquals("--detect.docker.passthrough.service.timeout\\=120", actualCmd.get(i++));
        assertEquals("--detect.cleanup\\=false", actualCmd.get(i++));
        assertEquals("--detect.project.name\\=Test\\ Project\\'", actualCmd.get(i++));
        assertEquals("--logging.level.com.synopsys.integration\\=INFO", actualCmd.get(i++));
        assertTrue(actualCmd.get(i++).startsWith("--detect.phone.home.passthrough.jenkins.version\\="));
        assertTrue(actualCmd.get(i).startsWith("--detect.phone.home.passthrough.jenkins.plugin.version\\="));
    }

    @Test
    public void testPerformPowerShell() {
        final String detectPath = "/tmp/detect.ps1";

        List<String> actualCmd = doTest(new HashMap<>(), OperatingSystemType.WINDOWS, detectPath, JAVA_HOME_VALUE);

        int i = 0;
        assertEquals("powershell", actualCmd.get(i++));
        assertEquals("\"Import-Module '" + detectPath + "'; detect\"", actualCmd.get(i++));
        assertEquals("--detect.docker.passthrough.service.timeout`=120", actualCmd.get(i++));
        assertEquals("--detect.cleanup`=false", actualCmd.get(i++));
        assertEquals("--detect.project.name`=Test` Project`'", actualCmd.get(i++));
        assertEquals("--logging.level.com.synopsys.integration`=INFO", actualCmd.get(i++));
        assertTrue(actualCmd.get(i++).startsWith("--detect.phone.home.passthrough.jenkins.version`="));
        assertTrue(actualCmd.get(i).startsWith("--detect.phone.home.passthrough.jenkins.plugin.version`="));
    }

    private List<String> doTest(Map<String, String> environmentVariables, OperatingSystemType operatingSystemType, String detectPath, String javaHomePath) {
        try {
            System.out.println("Starting testPerform. Errors logged about missing Black Duck values are benign.");
            JenkinsIntLogger jenkinsIntLogger = new JenkinsIntLogger(null);
            Map<BuilderPropertyKey, String> builderEnvironmentVariables = new HashMap<>();
            builderEnvironmentVariables.put(BlackDuckServerConfigBuilder.TIMEOUT_KEY, "120");

            BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = Mockito.mock(BlackDuckServerConfigBuilder.class);
            Mockito.when(blackDuckServerConfigBuilder.getProperties()).thenReturn(builderEnvironmentVariables);

            DetectGlobalConfig detectGlobalConfig = Mockito.mock(DetectGlobalConfig.class);
            Mockito.when(detectGlobalConfig.getBlackDuckServerConfigBuilder(Mockito.any(), Mockito.any())).thenReturn(blackDuckServerConfigBuilder);

            JenkinsProxyHelper mockedProxyHelper = Mockito.mock(JenkinsProxyHelper.class);
            Mockito.when(mockedProxyHelper.getProxyInfo(Mockito.anyString())).thenReturn(ProxyInfo.NO_PROXY_INFO);

            JenkinsVersionHelper mockedVersionHelper = Mockito.mock(JenkinsVersionHelper.class);
            SynopsysCredentialsHelper mockedCredentialsHelper = Mockito.mock(SynopsysCredentialsHelper.class);

            JenkinsBuildService jenkinsBuildService = Mockito.mock(JenkinsBuildService.class);
            Mockito.when(jenkinsBuildService.getJDKRemoteHomeOrEmpty()).thenReturn(Optional.of("/tmp/jdk"));

            JenkinsRemotingService jenkinsRemotingService = Mockito.mock(JenkinsRemotingService.class);
            Mockito.when(jenkinsRemotingService.launch(Mockito.any(), Mockito.any())).thenReturn(0);
            Mockito.when(jenkinsRemotingService.call(Mockito.any(DetectJarStrategy.SetupCallableImpl.class))).thenReturn(JDK_HOME);
            Mockito.when(jenkinsRemotingService.call(Mockito.any(DetectScriptStrategy.SetupCallableImpl.class))).thenReturn(detectPath);
            Mockito.when(jenkinsRemotingService.getRemoteOperatingSystemType()).thenReturn(operatingSystemType);

            JenkinsConfigService jenkinsConfigService = Mockito.mock(JenkinsConfigService.class);
            Mockito.when(jenkinsConfigService.getGlobalConfiguration(DetectGlobalConfig.class)).thenReturn(Optional.of(detectGlobalConfig));

            DetectArgumentService detectArgumentService = new DetectArgumentService(jenkinsIntLogger, mockedVersionHelper);
            DetectEnvironmentService detectEnvironmentService = new DetectEnvironmentService(jenkinsIntLogger, mockedProxyHelper, mockedVersionHelper, mockedCredentialsHelper, jenkinsConfigService, environmentVariables);
            DetectStrategyService detectStrategyService = new DetectStrategyService(jenkinsIntLogger, mockedProxyHelper, WORKSPACE_TMP_REL_PATH);

            DetectServicesFactory detectServicesFactory = Mockito.mock(DetectServicesFactory.class);
            Mockito.when(detectServicesFactory.createJenkinsRemotingService()).thenReturn(jenkinsRemotingService);
            Mockito.when(detectServicesFactory.createJenkinsBuildService()).thenReturn(jenkinsBuildService);
            Mockito.when(detectServicesFactory.createJenkinsConfigService()).thenReturn(jenkinsConfigService);
            Mockito.when(detectServicesFactory.createDetectArgumentService()).thenReturn(detectArgumentService);
            Mockito.when(detectServicesFactory.createDetectEnvironmentService()).thenReturn(detectEnvironmentService);
            Mockito.when(detectServicesFactory.createDetectStrategyService()).thenReturn(detectStrategyService);

            // run the method we're testing
            DetectCommands detectCommands = new DetectCommands(detectServicesFactory);
            detectCommands.runDetectPostBuild(DETECT_PROPERTY_INPUT);

            // verify successful result
            Mockito.verify(jenkinsBuildService, Mockito.never()).markBuildAborted();
            Mockito.verify(jenkinsBuildService, Mockito.never()).markBuildUnstable(Mockito.any());
            Mockito.verify(jenkinsBuildService, Mockito.never()).markBuildFailed(Mockito.any(String.class));
            Mockito.verify(jenkinsBuildService, Mockito.never()).markBuildFailed(Mockito.any(Exception.class));

            // get the Detect command line that was constructed to return to calling test for validation
            ArgumentCaptor<List<String>> cmdsArgCapture = ArgumentCaptor.forClass(List.class);
            ArgumentCaptor<IntEnvironmentVariables> detectEnvCapture = ArgumentCaptor.forClass(IntEnvironmentVariables.class);
            Mockito.verify(jenkinsRemotingService).launch(detectEnvCapture.capture(), cmdsArgCapture.capture());

            // verify that the system env is NOT inherited (use JAVA_HOME, if set)
            assertNotEquals(javaHomePath, detectEnvCapture.getValue().getValue("JAVA_HOME"));

            return cmdsArgCapture.getValue();
        } catch (Throwable t) {
            fail("An unexpected exception was thrown by the test code. The test likely requires fixing: ", t);
            return Collections.emptyList();
        }
    }
}
