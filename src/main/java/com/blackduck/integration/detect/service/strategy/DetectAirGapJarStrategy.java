/*
 * blackduck-detect
 *
 * Copyright (c) 2023 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.blackduck.integration.detect.service.strategy;

import com.blackduck.integration.detect.extensions.AirGapDownloadStrategy;
import com.blackduck.integration.detect.exception.DetectJenkinsException;
import com.blackduck.integration.detect.extensions.tool.DetectAirGapInstallation;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.extensions.JenkinsIntLogger;
import com.synopsys.integration.jenkins.service.JenkinsConfigService;
import com.synopsys.integration.util.IntEnvironmentVariables;
import jenkins.security.MasterToSlaveCallable;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

public class DetectAirGapJarStrategy extends DetectExecutionStrategy {
    public static final String DETECT_JAR_PREFIX = "synopsys-detect-";
    public static final String DETECT_JAR_SUFFIX = ".jar";

    private final JenkinsIntLogger logger;
    private final IntEnvironmentVariables intEnvironmentVariables;
    private final String remoteJdkHome;
    private final JenkinsConfigService jenkinsConfigService;
    private final AirGapDownloadStrategy airGapDownloadStrategy;

    public DetectAirGapJarStrategy(
        JenkinsIntLogger logger,
        IntEnvironmentVariables intEnvironmentVariables,
        String remoteJdkHome,
        JenkinsConfigService jenkinsConfigService,
        AirGapDownloadStrategy airGapDownloadStrategy
    ) {
        this.logger = logger;
        this.intEnvironmentVariables = intEnvironmentVariables;
        this.remoteJdkHome = remoteJdkHome;
        this.jenkinsConfigService = jenkinsConfigService;
        this.airGapDownloadStrategy = airGapDownloadStrategy;
    }

    @Override
    public Function<String, String> getArgumentEscaper() {
        return Function.identity();
    }

    private String getOrDownloadAirGapJar() throws DetectJenkinsException {
        DetectAirGapInstallation airGapInstallation;
        try {
            String airGapInstallationName = airGapDownloadStrategy.getAirGapInstallationName();
            airGapInstallation = jenkinsConfigService.getInstallationForNodeAndEnvironment(DetectAirGapInstallation.DescriptorImpl.class, airGapInstallationName).orElseThrow(
                () -> new DetectJenkinsException(
                    String.format(
                        "Problem encountered getting Detect Air Gap tool with the name %s from global tool configuration. Check Jenkins plugin and tool configuration.",
                        airGapInstallationName
                    )));
        } catch (IOException e) {
            throw new DetectJenkinsException("Problem encountered while interacting with Jenkins environment. Check Jenkins and environment.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DetectJenkinsException("Getting Detect Air Gap tool was interrupted. Check Jenkins and environment.", e);
        }

        return airGapInstallation.getHome();
    }

    @Override
    public MasterToSlaveCallable<ArrayList<String>, IntegrationException> getSetupCallable() throws DetectJenkinsException {
        String airGapBaseDir = getOrDownloadAirGapJar();

        if (airGapBaseDir == null) {
            throw new DetectJenkinsException("Detect AirGap installation directory is null. Check Jenkins tool configuration for installation directory.");
        }

        return new SetupCallableImpl(logger, intEnvironmentVariables.getVariables(), remoteJdkHome, airGapBaseDir);
    }

    public static class SetupCallableImpl extends MasterToSlaveCallable<ArrayList<String>, IntegrationException> {
        private static final long serialVersionUID = -8326836838838706367L;

        private final JenkinsIntLogger logger;
        private final Map<String, String> environmentVariables;
        private final String remoteJdkHome;
        private final String airGapBaseDir;

        public SetupCallableImpl(JenkinsIntLogger logger, Map<String, String> environmentVariables, String remoteJdkHome, String airGapBaseDir) {
            this.logger = logger;
            this.environmentVariables = environmentVariables;
            this.remoteJdkHome = remoteJdkHome;
            this.airGapBaseDir = airGapBaseDir;
        }

        @Override
        public ArrayList<String> call() throws DetectJenkinsException {
            String airGapJar = getAirGapJar(airGapBaseDir);
            RemoteJavaService remoteJavaService = new RemoteJavaService(logger, remoteJdkHome, environmentVariables);
            String javaExecutablePath = remoteJavaService.getJavaExecutablePath();

            logger.info("Detect AirGap jar configured: " + airGapJar);

            return new ArrayList<>(Arrays.asList(javaExecutablePath, "-jar", airGapJar));
        }

        private String getAirGapJar(String airGapBaseDir) throws DetectJenkinsException {
            FileFilter fileFilter = file -> file.getName().startsWith(DETECT_JAR_PREFIX) && file.getName().endsWith(DETECT_JAR_SUFFIX);
            File[] foundAirGapJars = new File(airGapBaseDir).listFiles(fileFilter);

            if (foundAirGapJars == null || foundAirGapJars.length == 0) {
                throw new DetectJenkinsException(String.format(
                    "Expected 1 jar from Detect Air Gap tool installation at <%s> and did not find any. Check your Jenkins plugin and tool configuration.",
                    airGapBaseDir
                ));
            } else if (foundAirGapJars.length > 1) {
                throw new DetectJenkinsException(
                    String.format(
                        "Expected 1 jar from Detect Air Gap tool installation at <%s> and instead found %d jars. Check your Jenkins plugin and tool configuration.",
                        airGapBaseDir,
                        foundAirGapJars.length
                    ));
            } else {
                return foundAirGapJars[0].toString();
            }
        }
    }

}
