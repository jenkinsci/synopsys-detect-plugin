/*
 * blackduck-detect
 *
 * Copyright (c) 2022 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.detect.extensions;

import java.io.Serializable;

import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

public abstract class DetectDownloadStrategy extends AbstractDescribableImpl<DetectDownloadStrategy> implements Serializable {
    private static final long serialVersionUID = 8059287675134866543L;

    public abstract String getDisplayName();

    @Override
    public DownloadStrategyDescriptor getDescriptor() {
        return (DownloadStrategyDescriptor) super.getDescriptor();
    }

    public abstract static class DownloadStrategyDescriptor extends Descriptor<DetectDownloadStrategy> {
        public DownloadStrategyDescriptor(Class<? extends DetectDownloadStrategy> clazz) {
            super(clazz);
        }
    }
}
