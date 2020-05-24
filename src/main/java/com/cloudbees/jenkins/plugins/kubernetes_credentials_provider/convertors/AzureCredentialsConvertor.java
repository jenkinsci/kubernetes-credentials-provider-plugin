/*
 * The MIT License
 *
 * Copyright 2019 CloudBees, Inc., @hugomcfonseca
 * Copyright 2020 @luka5
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
import com.microsoft.azure.util.AzureCredentials;
import org.jenkinsci.plugins.variant.OptionalExtension;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretToCredentialConverter;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretUtils;
import com.cloudbees.plugins.credentials.CredentialsScope;

/**
 * SecretToCredentialConvertor that converts {@link AzureCredentials}.
 */
@OptionalExtension(requirePlugins={"azure-credentials"})
public class AzureCredentialsConvertor extends SecretToCredentialConverter {

    @Override
    public boolean canConvert(String type) {
        return "azure".equals(type);
    }

    @Override
    public AzureCredentials convert(Secret secret) throws CredentialsConvertionException {

        SecretUtils.requireNonNull(secret.getData(), "azure definition contains no data");

        String subscriptionIdBase64 = SecretUtils.getNonNullSecretData(secret, "subscriptionId", "azure credential is missing the subscriptionId");

        String clientIdBase64 = SecretUtils.getNonNullSecretData(secret, "clientId", "azure credential is missing the clientId");

        String clientSecretBase64 = SecretUtils.getNonNullSecretData(secret, "clientSecret", "azure credential is missing the clientSecret");

        String subscriptionId = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(subscriptionIdBase64), "azure credential has an invalid subscriptionId (must be base64 encoded UTF-8)");

        String clientId = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(clientIdBase64), "azure credential has an invalid clientId (must be base64 encoded UTF-8)");

        String clientSecret = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(clientSecretBase64), "azure credential has an invalid clientSecret (must be base64 encoded UTF-8)");

        return new AzureCredentials(
                // Scope
                CredentialsScope.GLOBAL,
                // ID
                SecretUtils.getCredentialId(secret),
                // Desc
                SecretUtils.getCredentialDescription(secret),
                // SubscriptionId
                subscriptionId,
                // ClientId
                clientId,
                // ClientSecret
                clientSecret
        );

    }

}
