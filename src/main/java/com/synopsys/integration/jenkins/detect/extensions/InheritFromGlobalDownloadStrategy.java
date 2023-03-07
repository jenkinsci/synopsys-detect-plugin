/*
 * blackduck-detect
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.detect.extensions;

import javax.annotation.Nonnull;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;

public class InheritFromGlobalDownloadStrategy extends DetectDownloadStrategy {
    private static final long serialVersionUID = -4187483154317201511L;
    public static final String DISPLAY_NAME = "Inherit behavior from system configuration";

    @DataBoundConstructor
    public InheritFromGlobalDownloadStrategy() {
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
    public static class DescriptorImpl extends DownloadStrategyDescriptor {
        public DescriptorImpl() {
            super(InheritFromGlobalDownloadStrategy.class);
            load();
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
