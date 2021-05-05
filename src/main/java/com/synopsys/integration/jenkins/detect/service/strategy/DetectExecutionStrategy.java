/*
 * blackduck-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.jenkins.detect.service.strategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Function;

import com.synopsys.integration.exception.IntegrationException;

import jenkins.security.MasterToSlaveCallable;

public abstract class DetectExecutionStrategy {
    public abstract MasterToSlaveCallable<ArrayList<String>, IntegrationException> getSetupCallable() throws IntegrationException, IOException, InterruptedException;

    public abstract Function<String, String> getArgumentEscaper();
}
