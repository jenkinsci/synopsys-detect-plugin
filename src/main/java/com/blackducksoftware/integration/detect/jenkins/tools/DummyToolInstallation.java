/**
 * blackduck-detect-plugin
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

import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;

public class DummyToolInstallation extends ToolInstallation {
    private static final long serialVersionUID = -5953801725055668150L;

    public DummyToolInstallation() {
        super("Dummy Tool Name", null, null);
    }

    @Override
    public ToolDescriptor<?> getDescriptor() {

        return new DummyToolInstallationDescriptor();
    }

    public static class DummyToolInstallationDescriptor extends ToolDescriptor<DummyToolInstallation> {

        public DummyToolInstallationDescriptor() {
        }

        @Override
        public String getDisplayName() {
            return null;
        }

        @Override
        public String getId() {
            return "Dummy_Tool_Installation_Descriptor";
        }
    }
}
