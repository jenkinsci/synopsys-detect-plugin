package com.synopsys.integration.jenkins.detect.steps.remote;

import java.io.Serializable;

import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jenkins.detect.JenkinsDetectLogger;

import hudson.remoting.Callable;

public abstract class DetectRemoteCallable<T extends Serializable> implements Callable<T, IntegrationException> {
    private static final long serialVersionUID = -4754831395795794586L;
    protected final JenkinsDetectLogger logger;

    public DetectRemoteCallable(final JenkinsDetectLogger logger) {
        this.logger = logger;
    }

    @Override
    public abstract T call() throws IntegrationException;

    @Override
    public void checkRoles(final RoleChecker checker) throws SecurityException {
        checker.check(this, new Role(this.getClass()));
    }
}
