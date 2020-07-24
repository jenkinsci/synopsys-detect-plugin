package com.synopsys.integration.jenkins.detect.services;

import java.io.IOException;

import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.services.JenkinsBuildService;
import com.synopsys.integration.jenkins.services.JenkinsRemotingService;
import com.synopsys.integration.jenkins.wrapper.JenkinsWrapper;

import hudson.EnvVars;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.JDK;
import hudson.model.TaskListener;
import hudson.slaves.WorkspaceList;

public class DetectServicesFactory {
    private final JenkinsWrapper jenkinsWrapper;
    private final TaskListener listener;
    private final EnvVars envVars;
    private final Launcher launcher;
    private final FilePath workspace;
    private final AbstractBuild<?, ?> build;

    // TODO: Validate the pieces that we need to validate
    private DetectServicesFactory(JenkinsWrapper jenkinsWrapper, TaskListener listener, EnvVars envVars, Launcher launcher, FilePath workspace, AbstractBuild<?, ?> build) {
        this.jenkinsWrapper = jenkinsWrapper;
        this.listener = listener;
        this.envVars = envVars;
        this.launcher = launcher;
        this.workspace = workspace;
        this.build = build;
    }

    public static DetectServicesFactory fromPostBuild(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) throws IOException, InterruptedException {
        return new DetectServicesFactory(JenkinsWrapper.initializeFromJenkinsJVM(), listener, build.getEnvironment(listener), launcher, build.getWorkspace(), build);
    }

    public static DetectServicesFactory fromPipeline(JenkinsWrapper jenkinsWrapper, TaskListener listener, EnvVars envVars, Launcher launcher, FilePath workspace) {
        return new DetectServicesFactory(jenkinsWrapper, listener, envVars, launcher, workspace, null);
    }

    public DetectArgumentService createDetectArgumentService() {
        return new DetectArgumentService(getLogger(), jenkinsWrapper.getVersionHelper());
    }

    public DetectEnvironmentService createDetectEnvironmentService() {
        return new DetectEnvironmentService(getLogger(), jenkinsWrapper.getProxyHelper(), jenkinsWrapper.getVersionHelper(), jenkinsWrapper.getCredentialsHelper(), envVars);
    }

    public DetectWorkspaceService createDetectWorkspaceService() throws IOException, InterruptedException {
        return new DetectWorkspaceService(getLogger(), jenkinsWrapper.getProxyHelper(), createJenkinsRemotingService(), getJavaHomeFromBuildJDK(), WorkspaceList.tempDir(workspace).getRemote());
    }

    public JenkinsRemotingService createJenkinsRemotingService() {
        return new JenkinsRemotingService(launcher, workspace, listener);
    }

    public JenkinsBuildService createJenkinsBuildService() {
        return new JenkinsBuildService(getLogger(), build);
    }

    public JenkinsIntLogger getLogger() {
        return new JenkinsIntLogger(listener);
    }

    private String getJavaHomeFromBuildJDK() throws IOException, InterruptedException {
        if (build == null) {
            return null;
        }
        JDK jdk = build.getProject().getJDK();
        if (jdk == null) {
            return null;
        }
        JDK nodeJdk = jdk.forNode(build.getBuiltOn(), listener);

        return nodeJdk.getHome();
    }
}
