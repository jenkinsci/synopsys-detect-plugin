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
