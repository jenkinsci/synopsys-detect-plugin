package com.synopsys.integration.jenkins.detect.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import hudson.AbortException;
import hudson.EnvVars;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.Node;
import hudson.model.TaskListener;

public class DetectCommandsFactoryTest {
    private static TaskListener mockedTaskListener;
    private static Launcher mockedLauncher;
    private static Node mockedNode;
    private static AbstractBuild<?, ?> mockedAbstractBuild;
    private static BuildListener mockedBuildListener;
    private static final EnvVars emptyEnvVars = new EnvVars();

    @BeforeEach
    public void setUp() {
        mockedTaskListener = Mockito.mock(TaskListener.class);
        mockedLauncher = Mockito.mock(Launcher.class);
        mockedNode = Mockito.mock(Node.class);
        mockedAbstractBuild = Mockito.mock(AbstractBuild.class);
        mockedBuildListener = Mockito.mock(BuildListener.class);
    }

    @Test
    public void testPipelineNullWorkspace() {
        AbortException exception = assertThrows(AbortException.class, () -> DetectCommandsFactory.fromPipeline(mockedTaskListener, emptyEnvVars, mockedLauncher, mockedNode, null));
        assertEquals(DetectCommandsFactory.NULL_WORKSPACE, exception.getMessage());
    }

    @Test
    public void testPostBuildNullWorkspace() throws IOException, InterruptedException {
        Mockito.doReturn(emptyEnvVars).when(mockedAbstractBuild).getEnvironment(mockedBuildListener);
        Mockito.doReturn(null).when(mockedAbstractBuild).getWorkspace();
        AbortException exception = assertThrows(AbortException.class, () -> DetectCommandsFactory.fromPostBuild(mockedAbstractBuild, mockedLauncher, mockedBuildListener));
        assertEquals(DetectCommandsFactory.NULL_WORKSPACE, exception.getMessage());
    }

}
