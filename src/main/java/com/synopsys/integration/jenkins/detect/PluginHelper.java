/**
 * blackduck-detect
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.jenkins.detect;

import com.synopsys.integration.jenkins.detect.extensions.global.DetectGlobalConfig;

import hudson.Plugin;
import hudson.PluginWrapper;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;

public class PluginHelper {
    public static final String UNKNOWN_VERSION = "UNKNOWN_VERSION";

    public static String getJenkinsVersion() {
        try {
            return Jenkins.getVersion().toString();
        } catch (final Exception e) {
            return UNKNOWN_VERSION;
        }
    }

    public static String getPluginVersion() {
        String pluginVersion = UNKNOWN_VERSION;
        final Jenkins jenkins = Jenkins.getInstanceOrNull();
        if (jenkins != null) {
            // Jenkins still active
            final Plugin p = jenkins.getPlugin("blackduck-detect");
            if (p != null) {
                // plugin found
                final PluginWrapper pw = p.getWrapper();
                if (pw != null) {
                    pluginVersion = pw.getVersion();
                }
            }
        }
        pluginVersion = pluginVersion.split("\\s+")[0];

        return pluginVersion;
    }

    public static DetectGlobalConfig getDetectGlobalConfig() {
        return GlobalConfiguration.all().get(DetectGlobalConfig.class);
    }

}
