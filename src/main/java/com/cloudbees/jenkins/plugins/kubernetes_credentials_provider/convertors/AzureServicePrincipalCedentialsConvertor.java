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
package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.convertors;


import org.jenkinsci.plugins.variant.OptionalExtension;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretToCredentialConverter;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretUtils;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.microsoft.azure.util.AzureCredentials;

import io.fabric8.kubernetes.api.model.Secret;

/**
 * SecretToCredentialConvertor that converts {@link om.microsoft.azure.util.AzureCredentials.ServicePrincipal}.
 */
@OptionalExtension(requirePlugins={"azure-credentials"})
public class AzureServicePrincipalCedentialsConvertor extends SecretToCredentialConverter {

    @Override
    public boolean canConvert(String type) {
        return "azureServicePrincipal".equals(type);
    }

    @Override
    public AzureCredentials convert(Secret secret) throws CredentialsConvertionException {
    	// ensure we have some data
        SecretUtils.requireNonNull(secret.getData(), "azureCredentials kubernetes definition contains no data");
        
        String credsId = SecretUtils.getCredentialId(secret);
        String description = SecretUtils.getCredentialDescription(secret);
        CredentialsScope scope = SecretUtils.getCredentialScope(secret);

        // Assuming this is a service principal creds type
        String subscriptionId = SecretUtils.base64DecodeToString(SecretUtils.getNonNullSecretData(secret, "subscripitonId", "azureCredentials service principal credential is missing the subscriptionId")).trim();
        String clientId = SecretUtils.base64DecodeToString(SecretUtils.getNonNullSecretData(secret, "clientId", "azureCredentials service principal credential is missing the clientId")).trim();
        String clientSecret = SecretUtils.base64DecodeToString(SecretUtils.getNonNullSecretData(secret, "clientSecret", "azureCredentials service principal credential is missing the clientSecret")).trim();
        String tenantId = SecretUtils.base64DecodeToString(SecretUtils.getNonNullSecretData(secret, "tenantId", "azureCredentials service principal credential is missing the tenantId")).trim();
        
        AzureCredentials azureCredentials = new AzureCredentials(scope, credsId, description, subscriptionId, clientId, hudson.util.Secret.fromString(clientSecret));
        // Configure credentials against the correct Azure environment
        azureCredentials.setTenant(tenantId);
        azureCredentials.setAzureEnvironmentName("Azure");

    	return azureCredentials;
    }

}
