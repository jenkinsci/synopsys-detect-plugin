package com.synopsys.integration.jenkins.detect;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.extensions.AirGapDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.DetectDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.ScriptOrJarDownloadStrategy;
import com.synopsys.integration.jenkins.detect.extensions.tool.DetectAirGapInstallation;
import com.synopsys.integration.jenkins.detect.service.DetectArgumentService;
import com.synopsys.integration.jenkins.detect.service.DetectEnvironmentService;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectAirGapJarStrategy;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectDownloadStrategyService;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectExecutionStrategyFactory;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectJarStrategy;
import com.synopsys.integration.jenkins.detect.service.strategy.DetectScriptStrategy;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.wrapper.JenkinsVersionHelper;
import com.synopsys.integration.jenkins.wrapper.SynopsysCredentialsHelper;
import com.synopsys.integration.rest.proxy.ProxyInfo;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.OperatingSystemType;

import hudson.model.TaskListener;

public class DetectRunnerTest {
    private static final String DETECT_ARGUMENT_STRING = "--dummy.detect.argument";
    private static final String DETECT_JAR_PATH = "/tmp/airgap/detect.jar";
    private static final String REMOTE_JDK_HOME = "/remote/jdk/home";

    private static final ArrayList<String> EXPECTED_CMDS_JAR = new ArrayList<>(Arrays.asList(REMOTE_JDK_HOME, "-jar", DETECT_JAR_PATH));
    private static final ArrayList<String> EXPECTED_DETECT_CMDS_JAR = new ArrayList<>(
        Arrays.asList(DETECT_ARGUMENT_STRING, "--logging.level.com.synopsys.integration=INFO", "--detect.phone.home.passthrough.jenkins.version=<unknown>", "--detect.phone.home.passthrough.jenkins.plugin.version=<unknown>"));

    private static final ArrayList<String> EXPECTED_SCRIPT_CMDS_WINDOWS = new ArrayList<>(Arrays.asList("powershell", "\"Import-Module C:\\power\\shell; detect\"", "C:\\power\\shell"));
    private static final ArrayList<String> EXPECTED_DETECT_CMDS_WINDOWS = new ArrayList<>(
        Arrays.asList(DETECT_ARGUMENT_STRING, "--logging.level.com.synopsys.integration`=INFO", "--detect.phone.home.passthrough.jenkins.version`=`<unknown`>", "--detect.phone.home.passthrough.jenkins.plugin.version`=`<unknown`>"));

    private static final ArrayList<String> EXPECTED_SCRIPT_CMDS_LINUX = new ArrayList<>(Arrays.asList("bash", "/tmp/airgap/detect.sh"));
    private static final ArrayList<String> EXPECTED_DETECT_CMDS_LINUX = new ArrayList<>(
        Arrays.asList(DETECT_ARGUMENT_STRING, "--logging.level.com.synopsys.integration\\=INFO", "--detect.phone.home.passthrough.jenkins.version\\=\\<unknown\\>", "--detect.phone.home.passthrough.jenkins.plugin.version\\=\\<unknown\\>"));

    private final JenkinsProxyHelper jenkinsProxyHelperMock = Mockito.mock(JenkinsProxyHelper.class);
    private final JenkinsVersionHelper jenkinsVersionHelperMock = Mockito.mock(JenkinsVersionHelper.class);
    private final SynopsysCredentialsHelper synopsysCredentialsHelperMock = Mockito.mock(SynopsysCredentialsHelper.class);
    private final JenkinsRemotingService jenkinsRemotingServiceMock = Mockito.mock(JenkinsRemotingService.class);
    private final DetectAirGapInstallation detectAirGapInstallationMock = Mockito.mock(DetectAirGapInstallation.class);

    private static DetectRunner detectRunner;

    @Mock
    private JenkinsConfigService jenkinsConfigServiceMock;

    @Captor
    ArgumentCaptor<ArrayList<String>> cmdsArgCapture;

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
    }

    public static Stream<Object> shouldEscapeTestSource() {
        String shouldEscapeKey = DetectJenkinsEnvironmentVariable.SHOULD_ESCAPE.stringValue();

        return Stream.of(Arguments.of(Collections.singletonMap(shouldEscapeKey, "true")),
            Arguments.of(Collections.singletonMap(shouldEscapeKey, "false")),
            Arguments.of(Collections.singletonMap(shouldEscapeKey, "")),
            Arguments.of(Collections.singletonMap(shouldEscapeKey, null)),
            Arguments.of(new HashMap<>()));
    }

    private void requiredObjectSetup(Map<String, String> environmentVariables) {
        TaskListener taskListener = Mockito.mock(TaskListener.class);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Mockito.when(taskListener.getLogger()).thenReturn(new PrintStream(byteArrayOutputStream));
        JenkinsIntLogger logger = new JenkinsIntLogger(taskListener);

        DetectEnvironmentService detectEnvironmentService = new DetectEnvironmentService(logger, jenkinsProxyHelperMock, jenkinsVersionHelperMock, synopsysCredentialsHelperMock, jenkinsConfigServiceMock, environmentVariables);
        DetectDownloadStrategyService detectDownloadStrategyService = new DetectDownloadStrategyService(logger, jenkinsConfigServiceMock);
        DetectArgumentService detectArgumentService = new DetectArgumentService(logger, jenkinsVersionHelperMock);
        DetectExecutionStrategyFactory detectExecutionStrategyFactory = new DetectExecutionStrategyFactory(logger, jenkinsProxyHelperMock, "/remote/temp/workspace/path", jenkinsConfigServiceMock, jenkinsRemotingServiceMock,
            detectArgumentService);
        detectRunner = new DetectRunner(detectEnvironmentService, detectDownloadStrategyService, detectExecutionStrategyFactory);
    }

    private List<String> runDetectAndCaptureCommand(DetectDownloadStrategy detectDownloadStrategy) throws InterruptedException, IntegrationException, IOException {
        detectRunner.runDetect(REMOTE_JDK_HOME, DETECT_ARGUMENT_STRING, detectDownloadStrategy);

        //cmdsArgCapture = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<IntEnvironmentVariables> detectEnvCapture = ArgumentCaptor.forClass(IntEnvironmentVariables.class);
        Mockito.verify(jenkinsRemotingServiceMock).launch(detectEnvCapture.capture(), cmdsArgCapture.capture());
        return cmdsArgCapture.getValue();
    }

    @ParameterizedTest
    @MethodSource("shouldEscapeTestSource")
    public void testRunDetectAirGapJar(Map<String, String> environmentVariables) throws IntegrationException, InterruptedException, IOException {
        String airgapToolName = "DetectAirGapTool";
        String airgapToolHome = "/tmp/airgap";

        AirGapDownloadStrategy airGapDownloadStrategy = new AirGapDownloadStrategy();
        AirGapDownloadStrategy spiedAirGapDownloadStrategy = Mockito.spy(airGapDownloadStrategy);

        requiredObjectSetup(environmentVariables);

        Mockito.when(spiedAirGapDownloadStrategy.getAirGapInstallationName()).thenReturn(airgapToolName);
        Mockito.doReturn(Optional.of(detectAirGapInstallationMock)).when(jenkinsConfigServiceMock).getInstallationForNodeAndEnvironment(DetectAirGapInstallation.DescriptorImpl.class, airgapToolName);
        Mockito.when(detectAirGapInstallationMock.getHome()).thenReturn(airgapToolHome);
        Mockito.when(jenkinsRemotingServiceMock.call(Mockito.any(DetectAirGapJarStrategy.SetupCallableImpl.class))).thenReturn(EXPECTED_CMDS_JAR);

        List<String> actualDetectCommand = runDetectAndCaptureCommand(spiedAirGapDownloadStrategy);
        assertEquals(Stream.concat(EXPECTED_CMDS_JAR.stream(), EXPECTED_DETECT_CMDS_JAR.stream()).collect(Collectors.toList()), actualDetectCommand);
    }

    @Test
    public void testRunDetectShellWindows() throws IntegrationException, InterruptedException, IOException {
        ScriptOrJarDownloadStrategy scriptOrJarDownloadStrategy = new ScriptOrJarDownloadStrategy();

        requiredObjectSetup(new HashMap<>());

        Mockito.when(jenkinsRemotingServiceMock.getRemoteOperatingSystemType()).thenReturn(OperatingSystemType.WINDOWS);
        Mockito.when(jenkinsProxyHelperMock.getProxyInfo(DetectScriptStrategy.LATEST_POWERSHELL_SCRIPT_URL)).thenReturn(ProxyInfo.NO_PROXY_INFO);
        Mockito.when(jenkinsRemotingServiceMock.call(Mockito.any(DetectScriptStrategy.SetupCallableImpl.class))).thenReturn(EXPECTED_SCRIPT_CMDS_WINDOWS);

        List<String> actualDetectCommand = runDetectAndCaptureCommand(scriptOrJarDownloadStrategy);
        assertEquals(Stream.concat(EXPECTED_SCRIPT_CMDS_WINDOWS.stream(), EXPECTED_DETECT_CMDS_WINDOWS.stream()).collect(Collectors.toList()), actualDetectCommand);
    }

    @Test
    public void testRunDetectShellUnix() throws IntegrationException, InterruptedException, IOException {
        ScriptOrJarDownloadStrategy scriptOrJarDownloadStrategy = new ScriptOrJarDownloadStrategy();

        requiredObjectSetup(new HashMap<>());

        Mockito.when(jenkinsRemotingServiceMock.getRemoteOperatingSystemType()).thenReturn(OperatingSystemType.LINUX);
        Mockito.when(jenkinsProxyHelperMock.getProxyInfo(DetectScriptStrategy.LATEST_SHELL_SCRIPT_URL)).thenReturn(ProxyInfo.NO_PROXY_INFO);
        Mockito.when(jenkinsRemotingServiceMock.call(Mockito.any(DetectScriptStrategy.SetupCallableImpl.class))).thenReturn(EXPECTED_SCRIPT_CMDS_LINUX);

        List<String> actualDetectCommand = runDetectAndCaptureCommand(scriptOrJarDownloadStrategy);
        assertEquals(Stream.concat(EXPECTED_SCRIPT_CMDS_LINUX.stream(), EXPECTED_DETECT_CMDS_LINUX.stream()).collect(Collectors.toList()), actualDetectCommand);
    }

    @Test
    public void testRunDetectJarUnix() throws IntegrationException, InterruptedException, IOException {
        ScriptOrJarDownloadStrategy scriptOrJarDownloadStrategy = new ScriptOrJarDownloadStrategy();

        requiredObjectSetup(Collections.singletonMap(DetectJenkinsEnvironmentVariable.USER_PROVIDED_JAR_PATH.stringValue(), DETECT_JAR_PATH));

        Mockito.when(jenkinsRemotingServiceMock.getRemoteOperatingSystemType()).thenReturn(OperatingSystemType.LINUX);
        Mockito.when(jenkinsProxyHelperMock.getProxyInfo(DetectScriptStrategy.LATEST_SHELL_SCRIPT_URL)).thenReturn(ProxyInfo.NO_PROXY_INFO);
        Mockito.when(jenkinsRemotingServiceMock.call(Mockito.any(DetectJarStrategy.SetupCallableImpl.class))).thenReturn(EXPECTED_DETECT_CMDS_JAR);

        List<String> actualDetectCommand = runDetectAndCaptureCommand(scriptOrJarDownloadStrategy);
        assertEquals(Stream.concat(EXPECTED_DETECT_CMDS_JAR.stream(), EXPECTED_DETECT_CMDS_JAR.stream()).collect(Collectors.toList()), actualDetectCommand);
    }
}
