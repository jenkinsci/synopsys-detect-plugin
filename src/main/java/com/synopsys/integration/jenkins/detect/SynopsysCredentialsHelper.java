package com.synopsys.integration.jenkins.detect.extensions;

import java.util.Collections;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;

import com.cloudbees.plugins.credentials.CredentialsMatcher;
import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.impl.BaseStandardCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import com.cloudbees.plugins.credentials.matchers.IdMatcher;

import hudson.security.ACL;
import hudson.util.Secret;
import jenkins.model.Jenkins;

public class SynopsysCredentialsHelper {
    private static final Class<StringCredentialsImpl> API_TOKEN_CREDENTIALS_CLASS = StringCredentialsImpl.class;
    private static final Class<UsernamePasswordCredentialsImpl> USERNAME_PASSWORD_CREDENTIALS_CLASS = UsernamePasswordCredentialsImpl.class;
    public static final CredentialsMatcher SYNOPSYS_CREDENTIALS_CLASS_MATCHER = CredentialsMatchers.either(CredentialsMatchers.instanceOf(API_TOKEN_CREDENTIALS_CLASS), CredentialsMatchers.instanceOf(USERNAME_PASSWORD_CREDENTIALS_CLASS));

    public static Optional<String> getUsernameFromCredentials(final String credentialsId) {
        return getCredentials(credentialsId)
                   .filter(USERNAME_PASSWORD_CREDENTIALS_CLASS::isInstance)
                   .map(USERNAME_PASSWORD_CREDENTIALS_CLASS::cast)
                   .map(UsernamePasswordCredentialsImpl::getUsername);
    }

    public static Optional<String> getPasswordFromCredentials(final String credentialsId) {
        return getCredentials(credentialsId)
                   .filter(USERNAME_PASSWORD_CREDENTIALS_CLASS::isInstance)
                   .map(USERNAME_PASSWORD_CREDENTIALS_CLASS::cast)
                   .map(UsernamePasswordCredentialsImpl::getPassword)
                   .map(Secret::getPlainText);
    }

    public static Optional<String> getApiTokenFromCredentials(final String credentialsId) {
        return getCredentials(credentialsId)
                   .filter(API_TOKEN_CREDENTIALS_CLASS::isInstance)
                   .map(API_TOKEN_CREDENTIALS_CLASS::cast)
                   .map(StringCredentialsImpl::getSecret)
                   .map(Secret::getPlainText);
    }

    private static Optional<BaseStandardCredentials> getCredentials(final String credentialsId) {
        if (StringUtils.isBlank(credentialsId)) {
            return Optional.empty();
        }

        final IdMatcher idMatcher = new IdMatcher(credentialsId);

        return CredentialsProvider.lookupCredentials(BaseStandardCredentials.class, Jenkins.getInstance(), ACL.SYSTEM, Collections.emptyList()).stream()
                   .filter(idMatcher::matches)
                   .findAny();
    }
}
