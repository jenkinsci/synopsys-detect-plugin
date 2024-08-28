/*
 * blackduck-detect
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.detect.extensions.pipeline;

import com.blackduck.integration.detect.extensions.DetectDownloadStrategy;
import com.blackduck.integration.detect.extensions.InheritFromGlobalDownloadStrategy;
import com.blackduck.integration.detect.service.DetectCommandsFactory;
import com.synopsys.integration.jenkins.annotations.HelpMarkdown;
import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Node;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DetectPipelineStep extends Step implements Serializable {
    public static final String DISPLAY_NAME = "Synopsys Detect";
    public static final String PIPELINE_NAME = "synopsys_detect";
    private static final long serialVersionUID = 8126672300843832671L;

    @HelpMarkdown("The command line options to pass to Synopsys Detect")
    private final String detectProperties;

    @HelpMarkdown("If true (checked), returns the status code of the Detect run instead of throwing an exception")
    private boolean returnStatus = false;

    @Nullable
    private DetectDownloadStrategy downloadStrategyOverride;

    @DataBoundConstructor
    public DetectPipelineStep(String detectProperties) {
        this.detectProperties = detectProperties;
    }

    public String getDetectProperties() {
        return detectProperties;
    }

    public DetectDownloadStrategy getDownloadStrategyOverride() {
        return downloadStrategyOverride;
    }

    @DataBoundSetter
    public void setDownloadStrategyOverride(DetectDownloadStrategy downloadStrategyOverride) {
        this.downloadStrategyOverride = downloadStrategyOverride;
    }

    public DetectDownloadStrategy getDefaultDownloadStrategyOverride() {
        return new InheritFromGlobalDownloadStrategy();
    }

    public boolean getReturnStatus() {
        return returnStatus;
    }

    @DataBoundSetter
    public void setReturnStatus(boolean returnStatus) {
        this.returnStatus = returnStatus;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new Execution(context);
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends StepDescriptor {
        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return new HashSet<>(Arrays.asList(TaskListener.class, EnvVars.class, FilePath.class, Launcher.class, Node.class));
        }

        @Override
        public String getFunctionName() {
            return PIPELINE_NAME;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return DISPLAY_NAME;
        }

    }

    public class Execution extends SynchronousNonBlockingStepExecution<Integer> {
        private static final long serialVersionUID = -5807577350749324767L;
        private final transient TaskListener listener;
        private final transient EnvVars envVars;
        private final transient FilePath workspace;
        private final transient Launcher launcher;
        private final transient Node node;

        protected Execution(@Nonnull StepContext context) throws InterruptedException, IOException {
            super(context);
            listener = context.get(TaskListener.class);
            envVars = context.get(EnvVars.class);
            workspace = context.get(FilePath.class);
            launcher = context.get(Launcher.class);
            node = context.get(Node.class);
        }

        @Override
        protected Integer run() throws Exception {
            return DetectCommandsFactory.fromPipeline(listener, envVars, launcher, node, workspace)
                       .runDetect(returnStatus, detectProperties, downloadStrategyOverride);
        }

    }

}
