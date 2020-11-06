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
package com.synopsys.integration.jenkins.detect.service.strategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.DetectJenkinsEnvironmentVariable;
import com.synopsys.integration.jenkins.detect.service.DetectArgumentService;
import com.synopsys.integration.jenkins.service.JenkinsRemotingService;
import com.synopsys.integration.util.IntEnvironmentVariables;

import jenkins.security.MasterToSlaveCallable;

public abstract class DetectExecutionStrategy {
    protected final IntEnvironmentVariables intEnvironmentVariables;

    private final JenkinsRemotingService jenkinsRemotingService;
    private final DetectArgumentService detectArgumentService;

    public DetectExecutionStrategy(JenkinsRemotingService jenkinsRemotingService, DetectArgumentService detectArgumentService, IntEnvironmentVariables intEnvironmentVariables) {
        this.jenkinsRemotingService = jenkinsRemotingService;
        this.detectArgumentService = detectArgumentService;
        this.intEnvironmentVariables = intEnvironmentVariables;
    }

    protected abstract MasterToSlaveCallable<ArrayList<String>, IntegrationException> getSetupCallable() throws IntegrationException, IOException, InterruptedException;

    protected abstract Function<String, String> getArgumentEscaper();

    public int runStrategy(String detectArgumentString) throws InterruptedException, IntegrationException, IOException {
        MasterToSlaveCallable<ArrayList<String>, IntegrationException> setupCallable = getSetupCallable();
        ArrayList<String> detectCommands = jenkinsRemotingService.call(setupCallable);

        Function<String, String> argumentEscaper = getArgumentEscaper();
        boolean shouldEscape = Boolean.parseBoolean(intEnvironmentVariables.getValue(DetectJenkinsEnvironmentVariable.SHOULD_ESCAPE.stringValue(), "true"));
        if (!shouldEscape) {
            argumentEscaper = Function.identity();
        }

        List<String> detectArguments = detectArgumentService.getDetectArguments(intEnvironmentVariables, argumentEscaper, detectCommands, detectArgumentString);

        return jenkinsRemotingService.launch(intEnvironmentVariables, detectArguments);
    }
}
