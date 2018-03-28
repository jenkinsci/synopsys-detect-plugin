/**
 * blackduck-detect
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
package com.blackducksoftware.integration.detect.jenkins.dsl;

import com.blackducksoftware.integration.detect.jenkins.post.DetectPostBuildStep;

import hudson.Extension;
import javaposse.jobdsl.dsl.helpers.step.StepContext;
import javaposse.jobdsl.plugin.ContextExtensionPoint;
import javaposse.jobdsl.plugin.DslExtensionMethod;

@Extension(optional = true)
public class DetectDslExtension extends ContextExtensionPoint {
    @DslExtensionMethod(context = StepContext.class)
    public Object detect(final String detectProperties) {
        return new DetectPostBuildStep(detectProperties);
    }

    @DslExtensionMethod(context = StepContext.class)
    public Object detect(final Runnable closure) {
        final DetectDslContext context = new DetectDslContext();
        executeInContext(closure, context);

        return new DetectPostBuildStep(context.getDetectProperties());
    }

}
