package com.synopsys.integration.jenkins.service;

import java.util.Optional;

import jenkins.model.GlobalConfiguration;

public class JenkinsConfigService {
    public <T extends GlobalConfiguration> Optional<T> getGlobalConfiguration(Class<T> configurationClass) {
        T globalConfig = GlobalConfiguration.all().get(configurationClass);
        return Optional.ofNullable(globalConfig);
    }
}
