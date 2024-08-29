/*
 * blackduck-detect
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.sca.integration.detect.extensions.tool;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Util;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class DetectAirGapInstallation extends ToolInstallation implements NodeSpecific<DetectAirGapInstallation>, EnvironmentSpecific<DetectAirGapInstallation> {
    private static final long serialVersionUID = -3838254855454518440L;

    @DataBoundConstructor
    public DetectAirGapInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(Util.fixEmptyAndTrim(name), Util.fixEmptyAndTrim(home), properties);
    }

    @Override
    public DetectAirGapInstallation forNode(@Nonnull Node node, TaskListener log) throws IOException, InterruptedException {
        return new DetectAirGapInstallation(getName(), translateFor(node, log), getProperties().toList());
    }

    @Override
    public DetectAirGapInstallation forEnvironment(EnvVars environment) {
        return new DetectAirGapInstallation(getName(), environment.expand(getHome()), getProperties().toList());
    }

    @Extension
    @Symbol("detectAirGap")
    public static final class DescriptorImpl extends ToolDescriptor<DetectAirGapInstallation> {
        @Override
        public String getDisplayName() {
            return "Detect Air Gap";
        }

        @Override
        public DetectAirGapInstallation[] getInstallations() {
            load();
            return super.getInstallations();
        }

        @Override
        public void setInstallations(DetectAirGapInstallation... installations) {
            super.setInstallations(installations);
            save();
        }
    }

}