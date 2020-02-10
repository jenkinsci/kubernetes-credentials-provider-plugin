/*
 * The MIT License
 *
 * Copyright 2020 CloudBees, Inc., @MichalAugustyn
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
package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.convertors;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretToCredentialConverter;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretUtils;
import com.cloudbees.plugins.credentials.CredentialsScope;

import io.fabric8.kubernetes.api.model.Secret;

import jenkins.plugins.openstack.compute.auth.OpenstackCredentialv3;

import org.jenkinsci.plugins.variant.OptionalExtension;


/**
 * SecretToCredentialConvertor that converts {@link OpenstackCredentialv3}.
 */
@OptionalExtension(requirePlugins={"openstack-cloud"})
public class OpenstackCredentialv3Convertor extends SecretToCredentialConverter {

    @Override
    public boolean canConvert(String type) {
        return "openstackCredentialv3".equals(type);
    }

    @Override
    public OpenstackCredentialv3 convert(Secret secret) throws CredentialsConvertionException {
        SecretUtils.requireNonNull(secret.getData(), "openstackCredentialv3 definition contains no data");

        String userNameBase64 = SecretUtils.getNonNullSecretData(secret, "userName", "openstackCredentialv3 credential is missing the userName");
        String userName = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(userNameBase64), "openstackCredentialv3 credential has an invalid userName (must be base64 encoded UTF-8)");

        String userDomainBase64 = SecretUtils.getNonNullSecretData(secret, "userDomain", "openstackCredentialv3 credential is missing the userDomain");
        String userDomain = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(userDomainBase64), "openstackCredentialv3 credential has an invalid userDomain (must be base64 encoded UTF-8)");

        String projectNameBase64 = SecretUtils.getNonNullSecretData(secret, "projectName", "openstackCredentialv3 credential is missing the projectName");
        String projectName = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(projectNameBase64), "openstackCredentialv3 credential has an invalid projectName (must be base64 encoded UTF-8)");

        String projectDomainBase64 = SecretUtils.getNonNullSecretData(secret, "projectDomain", "openstackCredentialv3 credential is missing the projectDomain");
        String projectDomain = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(projectDomainBase64), "openstackCredentialv3 credential has an invalid projectDomain (must be base64 encoded UTF-8)");

        String passwordBase64 = SecretUtils.getNonNullSecretData(secret, "password", "openstackCredentialv3 credential is missing the password");
        String password = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(passwordBase64), "openstackCredentialv3 credential has an invalid password (must be base64 encoded UTF-8)");

        return new OpenstackCredentialv3(CredentialsScope.GLOBAL, SecretUtils.getCredentialId(secret), SecretUtils.getCredentialDescription(secret), userName, userDomain, projectName, projectDomain, password);

    }

}
