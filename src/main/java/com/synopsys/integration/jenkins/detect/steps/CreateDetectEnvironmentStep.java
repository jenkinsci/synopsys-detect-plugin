package com.synopsys.integration.jenkins.detect.steps;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;

import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfigBuilder;
import com.synopsys.integration.jenkins.detect.JenkinsDetectLogger;
import com.synopsys.integration.jenkins.detect.PluginHelper;
import com.synopsys.integration.polaris.common.configuration.PolarisServerConfigBuilder;
import com.synopsys.integration.util.IntEnvironmentVariables;

public class SetDetectEnvironmentStep {
    private final JenkinsDetectLogger logger;

    public SetDetectEnvironmentStep(final JenkinsDetectLogger logger) {
        this.logger = logger;
    }

    public IntEnvironmentVariables setDetectEnvironment(final Map<String, String> environmentVariables) {
        final IntEnvironmentVariables intEnvironmentVariables = new IntEnvironmentVariables();
        intEnvironmentVariables.putAll(environmentVariables);
        logger.setLogLevel(intEnvironmentVariables);

        populateAllBlackDuckEnvironmentVariables(intEnvironmentVariables::put);
        populateAllPolarisEnvironmentVariables(intEnvironmentVariables::put);

        final String pluginVersion = PluginHelper.getPluginVersion();
        logger.info("Running Detect jenkins plugin version: " + pluginVersion);

        return intEnvironmentVariables;
    }

    private void populateAllBlackDuckEnvironmentVariables(final BiConsumer<String, String> environmentPutter) {
        final BlackDuckServerConfigBuilder blackDuckServerConfigBuilder = PluginHelper.getDetectGlobalConfig().getBlackDuckServerConfigBuilder();

        Arrays.stream(BlackDuckServerConfigBuilder.Property.values())
            .forEach(property -> environmentPutter.accept(property.getBlackDuckEnvironmentVariableKey(), blackDuckServerConfigBuilder.get(property)));
    }

    // TODO: Replace these puts with a cleaner impl (see populateAllBlackDuckEnvironmentVariables) when Polaris Common supports it
    private void populateAllPolarisEnvironmentVariables(final BiConsumer<String, String> environmentPutter) {
        final PolarisServerConfigBuilder polarisServerConfigBuilder = PluginHelper.getDetectGlobalConfig().getPolarisServerConfigBuilder();

        environmentPutter.accept("POLARIS_TIMEOUT_IN_SECONDS", String.valueOf(polarisServerConfigBuilder.getTimeoutSeconds()));
        environmentPutter.accept("POLARIS_TRUST_CERT", String.valueOf(polarisServerConfigBuilder.isTrustCert()));
        environmentPutter.accept("POLARIS_PROXY_HOST", polarisServerConfigBuilder.getProxyHost());
        environmentPutter.accept("POLARIS_PROXY_USERNAME", polarisServerConfigBuilder.getProxyUsername());
        environmentPutter.accept("POLARIS_PROXY_PASSWORD", polarisServerConfigBuilder.getProxyPassword());
        environmentPutter.accept("POLARIS_PROXY_NTLM_DOMAIN", polarisServerConfigBuilder.getProxyNtlmDomain());
        environmentPutter.accept("POLARIS_PROXY_NTLM_WORKSTATION", polarisServerConfigBuilder.getProxyNtlmWorkstation());
        if (polarisServerConfigBuilder.getProxyPort() != -1) {
            environmentPutter.accept("POLARIS_PROXY_PORT", String.valueOf(polarisServerConfigBuilder.getProxyPort()));
        }

        try {
            polarisServerConfigBuilder.build().populateEnvironmentVariables(environmentPutter);
        } catch (final Exception ignored) {
            // If this doesn't work, Detect will throw an exception later on.
        }

    }
}
