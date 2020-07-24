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
package com.synopsys.integration.jenkins.detect.services;

import java.io.Serializable;

public class DetectSetupResponse implements Serializable {
    private static final long serialVersionUID = 6143610176077857305L;
    private final String detectRemotePath;
    private final ExecutionStrategy executionStrategy;
    private final String remoteJavaHome;

    public DetectSetupResponse(ExecutionStrategy executionStrategy, String detectRemotePath) {
        this.detectRemotePath = detectRemotePath;
        this.executionStrategy = executionStrategy;
        this.remoteJavaHome = null;
    }

    public DetectSetupResponse(ExecutionStrategy executionStrategy, String remoteJavaHome, String detectRemotePath) {
        this.detectRemotePath = detectRemotePath;
        this.executionStrategy = executionStrategy;
        this.remoteJavaHome = remoteJavaHome;
    }

    public String getDetectRemotePath() {
        return detectRemotePath;
    }

    public ExecutionStrategy getExecutionStrategy() {
        return executionStrategy;
    }

    public String getRemoteJavaHome() {
        return remoteJavaHome;
    }

    public enum ExecutionStrategy {
        SHELL_SCRIPT,
        POWERSHELL_SCRIPT,
        JAR
    }

}