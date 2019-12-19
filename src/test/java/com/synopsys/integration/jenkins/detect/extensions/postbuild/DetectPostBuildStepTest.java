package com.synopsys.integration.jenkins.detect.extensions.postbuild;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.PluginManager;

public class DetectPostBuildStepTest {

    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Test
    public void test() throws Exception {
        for (final PluginManager.FailedPlugin failedPlugin : jenkinsRule.getPluginManager().getFailedPlugins()) {
            System.out.printf("Failed plugin: %s: %s\n",
                failedPlugin.name, failedPlugin.cause.getMessage());
        }
        jenkinsRule.configRoundtrip();
        System.out.println("Done.");
    }
}
