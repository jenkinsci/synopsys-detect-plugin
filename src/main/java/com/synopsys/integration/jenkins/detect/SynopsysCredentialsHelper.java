/**
 * blackduck-detect
 *
 * Copyright (c) 2019 Synopsys, Inc.
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
package com.synopsys.integration.jenkins.detect;

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
    public static final CredentialsMatcher API_TOKEN_CREDENTIALS = CredentialsMatchers.instanceOf(API_TOKEN_CREDENTIALS_CLASS);
    private static final Class<UsernamePasswordCredentialsImpl> USERNAME_PASSWORD_CREDENTIALS_CLASS = UsernamePasswordCredentialsImpl.class;
    public static final CredentialsMatcher API_TOKEN_OR_USERNAME_PASSWORD_CREDENTIALS = CredentialsMatchers
                                                                                            .either(CredentialsMatchers.instanceOf(API_TOKEN_CREDENTIALS_CLASS), CredentialsMatchers.instanceOf(USERNAME_PASSWORD_CREDENTIALS_CLASS));

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
