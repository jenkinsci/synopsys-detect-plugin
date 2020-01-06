package com.synopsys.integration.jenkins.detect.extensions.postbuild;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.configuration.ConfigurationType.PowerMock;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.recipes.WithPlugin;
import org.mockito.Mockito;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.synopsys.integration.jenkins.detect.substeps.DetectSetupResponse;
import com.synopsys.integration.jenkins.detect.substeps.SetUpDetectWorkspaceCallable;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.PluginManager;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Build;
import hudson.model.BuildListener;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.JDK;
import hudson.model.Node;
import hudson.model.Project;
import hudson.model.Result;
import hudson.model.TaskListener;
import hudson.remoting.VirtualChannel;

@PowerMockIgnore({"javax.crypto.*" })
@RunWith(PowerMockRunner.class)
@PrepareForTest({AbstractBuild.class, JDK.class, Launcher.class, Launcher.ProcStarter.class})
public class DetectPostBuildStepTest {
    private static final String DETECT_PROPERTY_INPUT = "--detect.docker.passthrough.service.timeout=240000 --detect.cleanup=false";
    private static final String WORKSPACE_REL_PATH = "out/test/DetectPostBuildStepTest/testPerform/workspace";
    private static final String WORKSPACE_TEMP_DIR_REL_PATH = WORKSPACE_REL_PATH + "/tmp";

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    //fails: @WithPlugin({"credentials", "plain-credentials"})
    @Ignore
    @Test
    public void test() throws Exception {
        System.out.println("Starting test()!");
        for (final PluginManager.FailedPlugin failedPlugin : jenkinsRule.getPluginManager().getFailedPlugins()) {
            System.out.printf("Failed plugin: %s: %s\n",
                failedPlugin.name, failedPlugin.cause.getMessage());
        }
        //jenkinsRule.configRoundtrip();
        System.out.println("Done!");
    }

    @Ignore
    @Test
    public void testNull() {
        System.out.println("Done!!");
    }

    @Ignore
    @Test
    public void testGetProperties() throws Exception {
        System.out.println("Starting testGetProperties()!");
        final DetectPostBuildStep detectPostBuildStep = new DetectPostBuildStep(DETECT_PROPERTY_INPUT);
//        final String displayName = detectPostBuildStep.getDescriptor().getDisplayName();
//        System.out.printf("displayName: %s\n", displayName);

        System.out.printf("read back properties: %s\n", detectPostBuildStep.getDetectProperties());
        assertEquals(DETECT_PROPERTY_INPUT, detectPostBuildStep.getDetectProperties());
        System.out.println("Done!!");
    }

    //@Ignore
    @Test
    public void testPerform() throws Exception {
        System.out.println("Starting testPerform()!");
        final DetectPostBuildStep detectPostBuildStep = new DetectPostBuildStep(DETECT_PROPERTY_INPUT);
        // TODO make build a FreeStyleBuild
        final AbstractBuild<FreeStyleProject, FreeStyleBuild> build = PowerMockito.mock(AbstractBuild.class);
        final Launcher launcher = PowerMockito.mock(Launcher.class);

        final BuildListener listener = PowerMockito.mock(BuildListener.class);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(baos, true, "UTF-8");
        Mockito.when(listener.getLogger()).thenReturn(printStream);

        final File workspace = new File(WORKSPACE_REL_PATH);
        final File workspaceTempDir = new File(WORKSPACE_TEMP_DIR_REL_PATH);
        workspaceTempDir.mkdirs();
        final FilePath workspaceFilePath = new FilePath(workspace);

        Mockito.when(build.getWorkspace()).thenReturn(workspaceFilePath);

        final Node node = Mockito.mock(Node.class);
        Mockito.when(build.getBuiltOn()).thenReturn(node);

        final EnvVars envVars = Mockito.mock(EnvVars.class);
        Mockito.when(build.getEnvironment(listener)).thenReturn(envVars);

        final FreeStyleProject project = PowerMockito.mock(FreeStyleProject.class);
        Mockito.when(build.getProject()).thenReturn(project);
        final JDK jdk = PowerMockito.mock(JDK.class);
        Mockito.when(project.getJDK()).thenReturn(jdk);
        Mockito.when(jdk.forNode(Mockito.any(Node.class), Mockito.any(TaskListener.class))).thenReturn(jdk);
        Mockito.when(jdk.getHome()).thenReturn("/tmp/jdk");

        final VirtualChannel channel = PowerMockito.mock(hudson.remoting.VirtualChannel.class);
        Mockito.when(launcher.getChannel()).thenReturn(channel);
        final DetectSetupResponse detectSetupResponse = PowerMockito.mock(DetectSetupResponse.class);
        Mockito.when(channel.call(Mockito.any(SetUpDetectWorkspaceCallable.class))).thenReturn(detectSetupResponse);


        Mockito.when(detectSetupResponse.getExecutionStrategy()).thenReturn(DetectSetupResponse.ExecutionStrategy.JAR);

        Mockito.when(detectSetupResponse.getDetectRemotePath()).thenReturn("/tmp/detect.jar");

        // This getHome() method actually returns the path to the java exe
        Mockito.when(detectSetupResponse.getRemoteJavaHome()).thenReturn("/tmp/jdk/bin/java");

        final Launcher.ProcStarter procStarter = PowerMockito.mock(Launcher.ProcStarter.class);
        Mockito.when(launcher.launch()).thenReturn(procStarter);
        Mockito.when(procStarter.envs(Mockito.anyMap())).thenReturn(procStarter);
        Mockito.when(procStarter.cmds(Mockito.anyList())).thenReturn(procStarter);
        Mockito.when(procStarter.pwd(Mockito.any(FilePath.class))).thenReturn(procStarter);
        Mockito.when(procStarter.stdout(Mockito.any(TaskListener.class))).thenReturn(procStarter);
        Mockito.when(procStarter.quiet(Mockito.anyBoolean())).thenReturn(procStarter);
        Mockito.when(procStarter.join()).thenReturn(0);

        final boolean succeeded = detectPostBuildStep.perform(build, launcher, listener);
        assertTrue(succeeded);
        Mockito.verify(build, Mockito.never()).setResult(Result.ABORTED);
        Mockito.verify(build, Mockito.never()).setResult(Result.UNSTABLE);

        final List<String> expectedCmds = Arrays.asList("/tmp/jdk/bin/java", "-jar", "/tmp/detect.jar", "--detect.docker.passthrough.service.timeout=240000", "--detect.cleanup=false",
            "--logging.level.com.synopsys.integration=INFO", "--detect.phone.home.passthrough.jenkins.version=2.138.4", "--detect.phone.home.passthrough.jenkins.plugin.version=2.1.1-SNAPSHOT");
        Mockito.verify(procStarter).cmds(expectedCmds);
        System.out.println("Done!!");
    }
}
