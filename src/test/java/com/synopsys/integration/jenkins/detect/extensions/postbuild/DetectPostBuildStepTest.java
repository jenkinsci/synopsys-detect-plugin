package com.synopsys.integration.jenkins.detect.extensions.postbuild;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.builder.BuilderPropertyKey;
import com.synopsys.integration.jenkins.detect.extensions.global.DetectGlobalConfig;
import com.synopsys.integration.jenkins.detect.substeps.DetectSetupResponse;
import com.synopsys.integration.jenkins.detect.substeps.SetUpDetectWorkspaceCallable;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfigBuilder;

import hudson.EnvVars;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;
import jenkins.model.GlobalConfiguration;

@PowerMockIgnore({ "javax.crypto.*" })
@RunWith(PowerMockRunner.class)
@PrepareForTest({ AbstractBuild.class, JDK.class, Launcher.class, Launcher.ProcStarter.class, GlobalConfiguration.class })
public class DetectPostBuildStepTest {
    private static final String DETECT_PROPERTY_INPUT = "--detect.docker.passthrough.service.timeout=$POLARIS_TIMEOUT --detect.cleanup=false --detect.project.name=\"Test Project'\"";
    private static final String WORKSPACE_REL_PATH = "out/test/DetectPostBuildStepTest/testPerform/workspace";
    private static final String JAVA_HOME_VALUE = System.getenv("JAVA_HOME");

    @Test
    public void testPerformJar() throws Exception {
        final String detectPath = "/tmp/detect.jar";

        List<String> actualCmd = doTest(DetectSetupResponse.ExecutionStrategy.JAR, detectPath, JAVA_HOME_VALUE);

        int i = 0;
        assertEquals("/tmp/jdk/bin/java", actualCmd.get(i++));
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
    public void testPerformShell() throws Exception {
        final String detectPath = "/tmp/detect.sh";

        List<String> actualCmd = doTest(DetectSetupResponse.ExecutionStrategy.SHELL_SCRIPT, detectPath, JAVA_HOME_VALUE);

        int i = 0;
        assertEquals("bash", actualCmd.get(i++));
        assertEquals(detectPath, actualCmd.get(i++));
        assertEquals("--detect.docker.passthrough.service.timeout\\=120", actualCmd.get(i++));
        assertEquals("--detect.cleanup\\=false", actualCmd.get(i++));
        assertEquals("--detect.project.name\\=Test\\ Project\\'", actualCmd.get(i++));
        assertEquals("--logging.level.com.synopsys.integration=INFO", actualCmd.get(i++));
        assertTrue(actualCmd.get(i++).startsWith("--detect.phone.home.passthrough.jenkins.version="));
        assertTrue(actualCmd.get(i).startsWith("--detect.phone.home.passthrough.jenkins.plugin.version="));
    }

    @Test
    public void testPerformPowerShell() throws Exception {
        final String detectPath = "/tmp/detect.ps1";

        List<String> actualCmd = doTest(DetectSetupResponse.ExecutionStrategy.POWERSHELL_SCRIPT, detectPath, JAVA_HOME_VALUE);

        int i = 0;
        assertEquals("powershell", actualCmd.get(i++));
        assertEquals("\"Import-Module '" + detectPath + "'; detect\"", actualCmd.get(i++));
        assertEquals("--detect.docker.passthrough.service.timeout`=120", actualCmd.get(i++));
        assertEquals("--detect.cleanup`=false", actualCmd.get(i++));
        assertEquals("--detect.project.name`=Test` Project`'", actualCmd.get(i++));
        assertEquals("--logging.level.com.synopsys.integration=INFO", actualCmd.get(i++));
        assertTrue(actualCmd.get(i++).startsWith("--detect.phone.home.passthrough.jenkins.version="));
        assertTrue(actualCmd.get(i).startsWith("--detect.phone.home.passthrough.jenkins.plugin.version="));
    }

    private List<String> doTest(DetectSetupResponse.ExecutionStrategy executionStrategy, String detectPath, String javaHomePath) throws Exception {
        System.out.println("Starting testPerform. Errors logged about missing Black Duck or Polaris values are benign.");
        DetectPostBuildStep detectPostBuildStep = new DetectPostBuildStep(DETECT_PROPERTY_INPUT);
        AbstractBuild<FreeStyleProject, FreeStyleBuild> build = PowerMockito.mock(AbstractBuild.class);
        Launcher launcher = PowerMockito.mock(Launcher.class);

        BuildListener buildListener = PowerMockito.mock(BuildListener.class);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(baos, true, "UTF-8");
        Mockito.when(buildListener.getLogger()).thenReturn(printStream);

        ExtensionList<GlobalConfiguration> extensionList = Mockito.mock(ExtensionList.class);
        PowerMockito.mockStatic(GlobalConfiguration.class);
        Mockito.when(GlobalConfiguration.all()).thenReturn(extensionList);
        DetectGlobalConfig detectGlobalConfig = Mockito.mock(DetectGlobalConfig.class);
        PolarisServerConfigBuilder polarisServerConfigBuilder = Mockito.mock(PolarisServerConfigBuilder.class);
        BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = Mockito.mock(BlackDuckServerConfigBuilder.class);

        Mockito.when(extensionList.get(Mockito.any())).thenReturn(detectGlobalConfig);
        Mockito.when(detectGlobalConfig.getPolarisServerConfigBuilder(Mockito.any(), Mockito.any())).thenReturn(polarisServerConfigBuilder);
        Mockito.when(detectGlobalConfig.getBlackDuckServerConfigBuilder(Mockito.any(), Mockito.any())).thenReturn(blackDuckServerConfigBuilder);
        Map<BuilderPropertyKey, String> environmentVariables = new HashMap<>();
        environmentVariables.put(PolarisServerConfigBuilder.TIMEOUT_KEY, String.valueOf(PolarisServerConfigBuilder.DEFAULT_TIMEOUT_SECONDS));
        Mockito.when(polarisServerConfigBuilder.getProperties()).thenReturn(environmentVariables);

        FilePath workspaceFilePath = new FilePath(new File(WORKSPACE_REL_PATH));
        Mockito.when(build.getWorkspace()).thenReturn(workspaceFilePath);

        Node node = Mockito.mock(Node.class);
        Mockito.when(build.getBuiltOn()).thenReturn(node);

        EnvVars envVars = Mockito.mock(EnvVars.class);
        Mockito.when(build.getEnvironment(buildListener)).thenReturn(envVars);

        FreeStyleProject project = PowerMockito.mock(FreeStyleProject.class);
        JDK jdk = PowerMockito.mock(JDK.class);
        Mockito.when(build.getProject()).thenReturn(project);
        Mockito.when(project.getJDK()).thenReturn(jdk);
        Mockito.when(jdk.forNode(Mockito.any(Node.class), Mockito.any(TaskListener.class))).thenReturn(jdk);
        Mockito.when(jdk.getHome()).thenReturn("/tmp/jdk");

        VirtualChannel channel = PowerMockito.mock(hudson.remoting.VirtualChannel.class);
        DetectSetupResponse detectSetupResponse = PowerMockito.mock(DetectSetupResponse.class);
        Mockito.when(launcher.getChannel()).thenReturn(channel);
        Mockito.when(channel.call(Mockito.any(SetUpDetectWorkspaceCallable.class))).thenReturn(detectSetupResponse);
        Mockito.when(detectSetupResponse.getExecutionStrategy()).thenReturn(executionStrategy);
        Mockito.when(detectSetupResponse.getDetectRemotePath()).thenReturn(detectPath);

        // This getHome() method returns the path to the java exe
        Mockito.when(detectSetupResponse.getRemoteJavaHome()).thenReturn("/tmp/jdk/bin/java");

        Launcher.ProcStarter procStarter = PowerMockito.mock(Launcher.ProcStarter.class);
        Mockito.when(launcher.launch()).thenReturn(procStarter);
        Mockito.when(procStarter.envs(Mockito.anyMap())).thenReturn(procStarter);
        Mockito.when(procStarter.cmds(Mockito.anyList())).thenReturn(procStarter);
        Mockito.when(procStarter.pwd(Mockito.any(FilePath.class))).thenReturn(procStarter);
        Mockito.when(procStarter.stdout(Mockito.any(TaskListener.class))).thenReturn(procStarter);
        Mockito.when(procStarter.quiet(Mockito.anyBoolean())).thenReturn(procStarter);
        Mockito.when(procStarter.join()).thenReturn(0);

        // run the method we're testing
        boolean succeeded = detectPostBuildStep.perform(build, launcher, buildListener);

        // verify successful result
        assertTrue(succeeded);
        Mockito.verify(build, Mockito.never()).setResult(Result.ABORTED);
        Mockito.verify(build, Mockito.never()).setResult(Result.UNSTABLE);

        // get the Detect command line that was constructed to return to calling test for validation
        ArgumentCaptor<List<String>> cmdsArgCapture = ArgumentCaptor.forClass(List.class);
        Mockito.verify(procStarter).cmds(cmdsArgCapture.capture());
        List<String> actualCmd = cmdsArgCapture.getValue();

        // verify that the system env is NOT inherited (use JAVA_HOME, if set)
        if (StringUtils.isNotBlank(javaHomePath)) {
            ArgumentCaptor<Map<String, String>> detectEnvCapture = ArgumentCaptor.forClass(Map.class);
            Mockito.verify(procStarter).envs(detectEnvCapture.capture());
            Map<String, String> actualDetectEnv = detectEnvCapture.getValue();
            assertNotEquals(javaHomePath, actualDetectEnv.get("JAVA_HOME"));
        }

        // verify that workspace was passed to procStarter.pwd() [DetectJenkinsSubStepCoordinator]
        ArgumentCaptor<FilePath> detectPwdCapture = ArgumentCaptor.forClass(FilePath.class);
        Mockito.verify(procStarter).pwd(detectPwdCapture.capture());
        FilePath actualDetectPwd = detectPwdCapture.getValue();
        assertEquals(WORKSPACE_REL_PATH, actualDetectPwd.getRemote());

        // verify that buildListener was passed to procStarter.stdout() [DetectJenkinsSubStepCoordinator]
        ArgumentCaptor<BuildListener> detectStdoutCapture = ArgumentCaptor.forClass(BuildListener.class);
        Mockito.verify(procStarter).stdout(detectStdoutCapture.capture());
        BuildListener actualDetectStdout = detectStdoutCapture.getValue();
        assertEquals(buildListener, actualDetectStdout);

        return actualCmd;
    }
}
