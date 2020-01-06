package com.synopsys.integration.jenkins.detect.extensions.postbuild;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.powermock.configuration.ConfigurationType.PowerMock;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;

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

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.PluginManager;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Node;

@PowerMockIgnore({"javax.crypto.*" })
@RunWith(PowerMockRunner.class)
@PrepareForTest(AbstractBuild.class)
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

        final AbstractBuild<?, ?> build = PowerMockito.mock(AbstractBuild.class);
        final Launcher launcher = Mockito.mock(Launcher.class);

        final BuildListener listener = Mockito.mock(BuildListener.class);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream printStream = new PrintStream(baos, true, "UTF-8");
        Mockito.when(listener.getLogger()).thenReturn(printStream);

        // final FilePath workspace = build.getWorkspace()
        final File workspace = new File(WORKSPACE_REL_PATH);
        final File workspaceTempDir = new File(WORKSPACE_TEMP_DIR_REL_PATH);
        workspaceTempDir.mkdirs();
        final FilePath workspaceFilePath = new FilePath(workspace);
//        final FilePath workspaceTempDirFilePath = new FilePath(workspaceTempDir);

        Mockito.when(build.getWorkspace()).thenReturn(workspaceFilePath);

        // final Node node = build.getBuiltOn();
        final Node node = Mockito.mock(Node.class);
        Mockito.when(build.getBuiltOn()).thenReturn(node);

        // final EnvVars envVars = build.getEnvironment(listener);
        final EnvVars envVars = Mockito.mock(EnvVars.class);
        Mockito.when(build.getEnvironment(listener)).thenReturn(envVars);

        // final String remoteTempWorkspacePath = WorkspaceList.tempDir(workspace).getRemote();
        // ws.getName()

        // ws.sibling(ws.getName() + COMBINATOR + "tmp");


        final boolean succeeded = detectPostBuildStep.perform(build, launcher, listener);
        assertTrue(succeeded);
        System.out.println("Done!!");
    }
}
