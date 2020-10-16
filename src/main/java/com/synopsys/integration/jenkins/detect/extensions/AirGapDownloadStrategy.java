/**
 * blackduck-detect
 *
 * Copyright (c) 2020 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
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
    public static String DISPLAY_NAME = "Install AirGapped Detect as a Tool Installation";
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
