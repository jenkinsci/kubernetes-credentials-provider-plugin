/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.cloudbees.jenkins.plugins.k8sCredentialProvider.convertors;

import io.fabric8.kubernetes.api.model.Secret;
import hudson.Extension;
import com.cloudbees.jenkins.plugins.k8sCredentialProvider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.k8sCredentialProvider.SecretToCredentialConverter;
import com.cloudbees.jenkins.plugins.k8sCredentialProvider.SecretUtils;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

/**
 * SecretToCredentialConvertor that converts {@link UsernamePasswordCredentialsImpl}.
 */
@Extension
public class UsernamePasswordCredentialsConvertor extends SecretToCredentialConverter {

    @Override
    public boolean canConvert(String type) {
        return "usernamePassword".equals(type);
    }

    @Override
    public UsernamePasswordCredentialsImpl convert(Secret secret) throws CredentialsConvertionException {
        SecretUtils.requireNonNull(secret.getData(), "usernamePassword definition contains no data");

        String usernameBase64 = SecretUtils.getNonNullSecretData(secret, "username", "usernamePassword credential is missing the username");

        String passwordBase64 = SecretUtils.getNonNullSecretData(secret, "password", "usernamePassword credential is missing the password");

        String username = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(usernameBase64), "usernamePassword credential has an invalid username (must be base64 encoded UTF-8)");

        String password = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(passwordBase64), "usernamePassword credential has an invalid password (must be base64 encoded UTF-8)");

        return new UsernamePasswordCredentialsImpl(CredentialsScope.GLOBAL, SecretUtils.getCredentialId(secret), SecretUtils.getCredentialDescription(secret), username, password);

    }

}
