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
package com.synopsys.integration.jenkins.service;

import java.io.IOException;
import java.util.Optional;

import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Executor;
import hudson.model.JDK;
import hudson.model.Result;

public class JenkinsBuildService {
    private final JenkinsIntLogger logger;
    private final AbstractBuild<?, ?> build;

    public JenkinsBuildService(JenkinsIntLogger logger, AbstractBuild<?, ?> build) {
        this.logger = logger;
        this.build = build;
    }

    public void markBuildFailed(String message) {
        logger.error(message);
        build.setResult(Result.FAILURE);
    }

    public void markBuildFailed(Exception e) {
        logger.error(e.getMessage(), e);
        build.setResult(Result.FAILURE);
    }

    public void markBuildUnstable(Exception e) {
        logger.error(e.getMessage(), e);
        build.setResult(Result.UNSTABLE);
    }

    public Optional<String> getJDKRemoteHomeOrEmpty() throws InterruptedException {
        Optional<JDK> possibleJdk = Optional.ofNullable(build)
                                        .map(AbstractBuild::getProject)
                                        .map(AbstractProject::getJDK);

        if (possibleJdk.isPresent()) {
            try {
                JDK jdk = possibleJdk.get();
                JDK nodeJdk = jdk.forNode(build.getBuiltOn(), logger.getTaskListener());
                return Optional.ofNullable(nodeJdk.getHome());
            } catch (IOException ignored) {
                // If we can't get it, just return empty
            }
        }

        return Optional.empty();
    }

    public void markBuildAborted() {
        build.setResult(Result.ABORTED);
    }

    public void markBuildInterrupted() {
        Executor executor = build.getExecutor();
        if (executor == null) {
            markBuildAborted();
        } else {
            build.setResult(executor.abortResult());
        }
    }

}
