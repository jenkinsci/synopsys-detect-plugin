/*
 * blackduck-detect
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.sca.integration.detect.service;

import com.sca.integration.detect.extensions.global.DetectGlobalConfig;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.jenkins.wrapper.JenkinsProxyHelper;
import com.synopsys.integration.jenkins.wrapper.JenkinsVersionHelper;
import com.synopsys.integration.jenkins.wrapper.SynopsysCredentialsHelper;
import com.synopsys.integration.util.IntEnvironmentVariables;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

public class DetectEnvironmentService {
    public static final String TIMEOUT = "DETECT_TIMEOUT";

    private final JenkinsIntLogger logger;
    private final JenkinsProxyHelper jenkinsProxyHelper;
    private final JenkinsVersionHelper jenkinsVersionHelper;
    private final SynopsysCredentialsHelper synopsysCredentialsHelper;
    private final Map<String, String> environmentVariables;
    private final JenkinsConfigService jenkinsConfigService;

    public DetectEnvironmentService(
        JenkinsIntLogger logger,
        JenkinsProxyHelper jenkinsProxyHelper,
        JenkinsVersionHelper jenkinsVersionHelper,
        SynopsysCredentialsHelper synopsysCredentialsHelper,
        JenkinsConfigService jenkinsConfigService,
        Map<String, String> environmentVariables
    ) {
        this.logger = logger;
        this.jenkinsProxyHelper = jenkinsProxyHelper;
        this.jenkinsVersionHelper = jenkinsVersionHelper;
        this.jenkinsConfigService = jenkinsConfigService;
        this.synopsysCredentialsHelper = synopsysCredentialsHelper;
        this.environmentVariables = environmentVariables;
    }

    public IntEnvironmentVariables createDetectEnvironment() {
        IntEnvironmentVariables intEnvironmentVariables = IntEnvironmentVariables.empty();
        intEnvironmentVariables.putAll(environmentVariables);
        logger.setLogLevel(intEnvironmentVariables);

        populateAllBlackDuckEnvironmentVariables(intEnvironmentVariables::put);

        Optional<String> pluginVersion = jenkinsVersionHelper.getPluginVersion("blackduck-detect");
        if (pluginVersion.isPresent()) {
            logger.info("Running Synopsys Detect Plugin for Jenkins version: " + pluginVersion.get());
        } else {
            logger.info("Running Synopsys Detect Plugin for Jenkins");
        }

        return intEnvironmentVariables;
    }

    private void populateAllBlackDuckEnvironmentVariables(BiConsumer<String, String> environmentPutter) {
        Optional<DetectGlobalConfig> detectGlobalConfig = jenkinsConfigService.getGlobalConfiguration(DetectGlobalConfig.class);
        if (!detectGlobalConfig.isPresent()) {
            return;
        }

        BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = detectGlobalConfig.get().getBlackDuckServerConfigBuilder(jenkinsProxyHelper, synopsysCredentialsHelper);

        blackDuckServerConfigBuilder.getProperties()
            .forEach((builderPropertyKey, propertyValue) -> updateAndFilterVariables(environmentPutter, builderPropertyKey.getKey(), propertyValue));
    }

    private void updateAndFilterVariables(BiConsumer<String, String> environmentPutter, String key, String value) {
        String filteredKey = BlackDuckServerConfigBuilder.TIMEOUT_KEY.getKey().equals(key) ? TIMEOUT : key;

        if (StringUtils.isNoneBlank(filteredKey, value)) {
            environmentPutter.accept(filteredKey, value);
        }
    }

}
