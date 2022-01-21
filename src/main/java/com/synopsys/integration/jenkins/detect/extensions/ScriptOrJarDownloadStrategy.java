/*
 * blackduck-detect
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.detect.extensions;

import javax.annotation.Nonnull;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

public class ScriptOrJarDownloadStrategy extends DetectDownloadStrategy {
    private static final long serialVersionUID = 3453314100205960797L;
    public static final String DISPLAY_NAME = "Download via scripts or use DETECT_JAR";

    @DataBoundConstructor
    public ScriptOrJarDownloadStrategy() {
        // Left empty intentionally. -- rotte SEP 2020
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Extension
    public static class DescriptorImpl extends DetectDownloadStrategy.DownloadStrategyDescriptor {
        public DescriptorImpl() {
            super(ScriptOrJarDownloadStrategy.class);
            load();
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
