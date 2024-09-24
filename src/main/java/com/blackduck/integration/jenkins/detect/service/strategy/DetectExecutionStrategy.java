/*
 * blackduck-detect
 *
 * Copyright (c) 2024 Black Duck Software, Inc.
 *
 * Use subject to the terms and conditions of the Black Duck Software End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.jenkins.detect.service.strategy;

import com.blackduck.integration.exception.IntegrationException;
import jenkins.security.MasterToSlaveCallable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Function;

public abstract class DetectExecutionStrategy {
    public abstract MasterToSlaveCallable<ArrayList<String>, IntegrationException> getSetupCallable() throws IntegrationException, IOException, InterruptedException;

    public abstract Function<String, String> getArgumentEscaper();
}
