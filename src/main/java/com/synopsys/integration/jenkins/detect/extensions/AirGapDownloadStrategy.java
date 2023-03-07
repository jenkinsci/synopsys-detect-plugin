/*
 * blackduck-detect
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.detect.extensions;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import com.synopsys.integration.jenkins.detect.extensions.tool.DetectAirGapInstallation;

import hudson.Extension;
import hudson.tools.ToolInstallation;
import hudson.util.ListBoxModel;

public class AirGapDownloadStrategy extends DetectDownloadStrategy {
    private static final long serialVersionUID = -8683774675699706747L;
    public static final String DISPLAY_NAME = "Install AirGapped Detect as a Tool Installation";
    @Nullable
    private String airGapInstallationName;

    @DataBoundConstructor
    public AirGapDownloadStrategy() {
        // Left empty intentionally. -- rotte SEP 2020
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getAirGapInstallationName() {
        return airGapInstallationName;
    }

    @DataBoundSetter
    public void setAirGapInstallationName(String airGapInstallationName) {
        this.airGapInstallationName = airGapInstallationName;
    }

    @Override
    public String getDisplayName() {
        return DISPLAY_NAME;
    }

    @Extension
    public static class DescriptorImpl extends DetectDownloadStrategy.DownloadStrategyDescriptor {
        public DescriptorImpl() {
            super(AirGapDownloadStrategy.class);
            load();
        }

        public ListBoxModel doFillAirGapInstallationNameItems() {
            DetectAirGapInstallation.DescriptorImpl detectAirGapInstallationDescriptor = ToolInstallation.all().get(DetectAirGapInstallation.DescriptorImpl.class);

            if (detectAirGapInstallationDescriptor == null) {
                return new ListBoxModel();
            }

            return Stream.of(detectAirGapInstallationDescriptor.getInstallations())
                       .map(DetectAirGapInstallation::getName)
                       .map(ListBoxModel.Option::new)
                       .collect(Collectors.toCollection(ListBoxModel::new));
        }

        @Override
        @Nonnull
        public String getDisplayName() {
            return DISPLAY_NAME;
        }
    }
}
