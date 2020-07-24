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
package com.synopsys.integration.jenkins.services;

import java.io.IOException;
import java.util.List;

import com.synopsys.integration.function.ThrowingSupplier;
import com.synopsys.integration.util.IntEnvironmentVariables;

import hudson.AbortException;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.remoting.Callable;
import hudson.remoting.VirtualChannel;
import jenkins.security.MasterToSlaveCallable;

public class JenkinsRemotingService {
    private final Launcher launcher;
    private final FilePath workspace;
    private final TaskListener listener;

    public JenkinsRemotingService(Launcher launcher, FilePath workspace, TaskListener listener) {
        this.launcher = launcher;
        this.workspace = workspace;
        this.listener = listener;
    }

    public int launch(IntEnvironmentVariables intEnvironmentVariables, List<String> commandLine) throws IOException, InterruptedException {
        return launcher.launch()
                   .cmds(commandLine)
                   .envs(intEnvironmentVariables.getVariables())
                   .pwd(workspace)
                   .stdout(listener)
                   .quiet(true)
                   .join();
    }

    public <T, E extends Throwable> T call(Callable<T, E> callable) throws E, IOException, InterruptedException {
        VirtualChannel virtualChannel = launcher.getChannel();
        if (virtualChannel == null) {
            throw new AbortException("");
        }
        return virtualChannel.call(callable);
    }

    public <T, E extends Throwable> T call(ThrowingSupplier<T, E> supplierToExecute) throws E, IOException, InterruptedException {
        Callable<T, E> wrappingCallable = new MasterToSlaveCallable<T, E>() {
            private static final long serialVersionUID = 1943720716430585353L;

            @Override
            public T call() throws E {
                return supplierToExecute.get();
            }
        };

        return call(wrappingCallable);
    }

}
