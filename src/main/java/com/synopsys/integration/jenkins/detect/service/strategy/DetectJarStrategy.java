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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.util.IntEnvironmentVariables;

import jenkins.security.MasterToSlaveCallable;

public class DetectJarStrategy extends DetectExecutionStrategy {
    private final JenkinsIntLogger logger;
    private final String detectJarPath;
    private final IntEnvironmentVariables intEnvironmentVariables;
    private final String remoteJdkHome;

    public DetectJarStrategy(JenkinsIntLogger logger, IntEnvironmentVariables intEnvironmentVariables, String remoteJdkHome, String detectJarPath) {
        this.logger = logger;
        this.intEnvironmentVariables = intEnvironmentVariables;
        this.remoteJdkHome = remoteJdkHome;
        this.detectJarPath = detectJarPath;
    }

    @Override
    public Function<String, String> getArgumentEscaper() {
        return Function.identity();
    }

    @Override
    public MasterToSlaveCallable<ArrayList<String>, IntegrationException> getSetupCallable() {
        return new SetupCallableImpl(logger, intEnvironmentVariables.getVariables(), detectJarPath, remoteJdkHome);
    }

    public static class SetupCallableImpl extends MasterToSlaveCallable<ArrayList<String>, IntegrationException> {
        private static final long serialVersionUID = -8326836838838706367L;
        private final JenkinsIntLogger logger;
        private final Map<String, String> environmentVariables;
        private final String detectJarPath;
        private final String remoteJdkHome;

        public SetupCallableImpl(JenkinsIntLogger logger, Map<String, String> environmentVariables, String detectJarPath, String remoteJdkHome) {
            this.logger = logger;
            this.environmentVariables = environmentVariables;
            this.detectJarPath = detectJarPath;
            this.remoteJdkHome = remoteJdkHome;
        }

        @Override
        public ArrayList<String> call() {
            RemoteJavaService remoteJavaService = new RemoteJavaService(logger, remoteJdkHome, environmentVariables);
            String javaExecutablePath = remoteJavaService.calculateJavaExecutablePath();

            logger.info("Detect jar configured: " + detectJarPath);

            return new ArrayList<>(Arrays.asList(javaExecutablePath, "-jar", detectJarPath));
        }
    }

}
