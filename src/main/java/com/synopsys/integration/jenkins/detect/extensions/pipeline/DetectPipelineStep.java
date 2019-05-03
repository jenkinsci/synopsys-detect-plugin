package com.synopsys.integration.jenkins.detect.extensions.postbuild;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nonnull;

import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;

import com.synopsys.integration.jenkins.detect.steps.ExecuteDetectStep;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Computer;
import hudson.model.Run;
import hudson.model.TaskListener;

public class DetectPipelineStep extends Step {
    public static final String DISPLAY_NAME = "Synopsys Detect";
    public static final String PIPELINE_NAME = "synopsys_detect";
    private final String detectProperties;

    @DataBoundConstructor
    public DetectPipelineStep(final String detectProperties) {
        this.detectProperties = detectProperties;
    }

    public String getDetectProperties() {
        return detectProperties;
    }

    @Override
    public StepExecution start(final StepContext context) throws Exception {
        return new Execution(context);
    }

    @Extension(optional = true)
    public static final class DescriptorImpl extends StepDescriptor implements Serializable {
        private static final long serialVersionUID = 9059602791947799261L;

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return new HashSet<>(Arrays.asList(DetectPipelineStep.class, TaskListener.class, EnvVars.class, Computer.class, FilePath.class, Run.class));
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

    public class Execution extends StepExecution {
        private static final long serialVersionUID = -5807577350749324767L;
        private transient DetectPipelineStep pipelineStep;
        private transient TaskListener listener;
        private transient EnvVars envVars;
        private transient Computer computer;
        private transient FilePath workspace;
        private transient Run run;

        protected Execution(@Nonnull final StepContext context) throws InterruptedException, IOException {
            super(context);
            pipelineStep = context.get(DetectPipelineStep.class);
            listener = context.get(TaskListener.class);
            envVars = context.get(EnvVars.class);
            computer = context.get(Computer.class);
            workspace = context.get(FilePath.class);
            run = context.get(Run.class);
        }

        @Override
        public boolean start() throws Exception {
            final ExecuteDetectStep executeDetectStep = new ExecuteDetectStep(computer.getNode(), listener, workspace, envVars, run, null);
            executeDetectStep.executeDetect(pipelineStep.getDetectProperties());
            return false;
        }

        @Override
        public void stop(@Nonnull final Throwable cause) throws Exception {
            getContext().onFailure(cause);
            // Interrupt process?
        }

    }

}
