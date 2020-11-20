/*
 * The MIT License
 *
 * Copyright 2019 CloudBees, Inc., @hugomcfonseca
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

import io.fabric8.kubernetes.api.model.Secret;
import java.util.Optional;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl;
import org.jenkinsci.plugins.variant.OptionalExtension;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretToCredentialConverter;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretUtils;
import com.cloudbees.plugins.credentials.CredentialsScope;

/**
 * SecretToCredentialConvertor that converts {@link AWSCredentialsImpl}.
 */
@OptionalExtension(requirePlugins={"aws-credentials"})
public class AWSCredentialsConvertor extends SecretToCredentialConverter {

    @Override
    public boolean canConvert(String type) {
        return "aws".equals(type);
    }

    @Override
    public AWSCredentialsImpl convert(Secret secret) throws CredentialsConvertionException {

        SecretUtils.requireNonNull(secret.getData(), "aws definition contains no data");

        String accessKeyBase64 = SecretUtils.getNonNullSecretData(secret, "accessKey", "aws credential is missing the accessKey");

        String secretKeyBase64 = SecretUtils.getNonNullSecretData(secret, "secretKey", "aws credential is missing the secretKey");

        String accessKey = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(accessKeyBase64), "aws credential has an invalid accessKey (must be base64 encoded UTF-8)");

        String secretKey = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(secretKeyBase64), "aws credential has an invalid secretKey (must be base64 encoded UTF-8)");

        Optional<String> iamRoleArnBase64 = SecretUtils.getOptionalSecretData(secret, "iamRoleArn", "aws credential: failed to retrieve optional parameter iamRoleArn");
        String iamRoleArn = null;

        if (iamRoleArnBase64.isPresent()) {
            iamRoleArn = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(iamRoleArnBase64.get()), "aws credential has an invalid iamRoleArn (must be base64 encoded UTF-8)");
        }

        Optional<String> iamMfaSerialNumberBase64 = SecretUtils.getOptionalSecretData(secret, "iamMfaSerialNumber", "aws credential: failed to retrieve optional parameter iamMfaSerialNumber");
        String iamMfaSerialNumber = null;

        if (iamMfaSerialNumberBase64.isPresent()) {
            iamMfaSerialNumber = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(iamMfaSerialNumberBase64.get()), "aws credential has an invalid iamMfaSerialNumber (must be base64 encoded UTF-8)");
        }

        return new AWSCredentialsImpl(
                // Scope
                SecretUtils.getScope(secret),
                // ID
                SecretUtils.getCredentialId(secret),
                // AccessKey
                accessKey,
                // SecretKey
                secretKey,
                // Desc
                SecretUtils.getCredentialDescription(secret),
                // IAM Role ARN
                iamRoleArn,
                // MFA
                iamMfaSerialNumber
        );

    }

}
