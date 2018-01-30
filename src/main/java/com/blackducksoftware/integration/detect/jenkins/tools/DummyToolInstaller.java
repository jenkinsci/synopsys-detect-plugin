/**
 * Black Duck Detect Plugin
 *
 * Copyright (C) 2018 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.blackducksoftware.integration.detect.jenkins.tools;

import java.io.IOException;

import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;

public class DummyToolInstaller extends ToolInstaller {
    public DummyToolInstaller() {
        super(null);
    }

    @Override
    public FilePath performInstallation(final ToolInstallation tool, final Node node, final TaskListener log) throws IOException, InterruptedException {
        return null;
    }

    @Override
    public ToolInstallerDescriptor<?> getDescriptor() {

        return new DummyToolInstallerDescriptor();
    }

    public FilePath getToolDir(final ToolInstallation tool, final Node node) {
        final FilePath toolsDir = preferredLocation(tool, node);
        // preferredLocation will return {root}/tools/descriptorId/installationName
        // and we want to return {root}/tools
        return toolsDir.getParent().getParent();
    }

    public static class DummyToolInstallerDescriptor extends ToolInstallerDescriptor<DummyToolInstaller> {

        @Override
        public String getDisplayName() {
            return null;
        }

    }

}
