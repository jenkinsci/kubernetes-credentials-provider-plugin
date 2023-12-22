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
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.convertors.azure.AzureEnvironments;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.microsoft.azure.util.AzureImdsCredentials;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.Client;

/**
 * SecretToCredentialConvertor that converts {@link om.microsoft.azure.util.azureManagedIdentity.ServicePrincipal}.
 */
@OptionalExtension(requirePlugins={"azure-credentials"})
public class AzureManagedIdentityCedentialsConvertor extends SecretToCredentialConverter {

    @Override
    public boolean canConvert(String type) {
        return "azureManagedIdentity".equals(type);
    }

    @Override
    public AzureImdsCredentials convert(Secret secret) throws CredentialsConvertionException {
    	// ensure we have some data
        SecretUtils.requireNonNull(secret.getData(), "azureManagedIdentity kubernetes definition contains no data");
        
        String credsId = SecretUtils.getCredentialId(secret);
        String description = SecretUtils.getCredentialDescription(secret);
        CredentialsScope scope = SecretUtils.getCredentialScope(secret);

        // Assuming this is a service principal creds type
        String subscriptionIdBase64 = SecretUtils.getNonNullSecretData(secret, "subscripitonId", "azureManagedIdentity service principal credential is missing the subscriptionId");
        String subscriptionId       = (subscriptionIdBase64 != null && subscriptionIdBase64.length() > 0) ? SecretUtils.base64DecodeToString(subscriptionIdBase64) : "";
        if (subscriptionId != null && subscriptionId.length() > 0) {
        	subscriptionId = subscriptionId.trim();
        } else {
        	throw new CredentialsConvertionException("Can't continue as subscriptionId is empty");
        }
        
        String clientIdBase64 = SecretUtils.getNonNullSecretData(secret, "clientId", "azureManagedIdentity service principal credential is missing the clientId");
        String clientId       = (clientIdBase64 != null && clientIdBase64.length() > 0) ? SecretUtils.base64DecodeToString(clientIdBase64) : "";
        if (clientId != null && clientId.length() > 0) {
        	clientId = clientId.trim();
        } else {
        	throw new CredentialsConvertionException("Can't continue as clientId is empty");
        }
        
        AzureImdsCredentials azureImdsCredentials;
        
        String azureEnvironment = "";
        try {
        	azureEnvironment = SecretUtils.base64DecodeToString(SecretUtils.getNonNullSecretData(secret, "azureEnvironment", 
        			"azureManagedIdentity service principal credential is missing the azureEnvironment. Defaults to \"Azure\""));
        	if (azureEnvironment != null && azureEnvironment.length() > 0) {
        		azureEnvironment = azureEnvironment.trim();
        	} else {
        		// We defaults to "Azure"
        		azureEnvironment = AzureEnvironments.AZURE.label;
        	}
        } catch (CredentialsConvertionException convertionException) {
        	azureEnvironment = AzureEnvironments.AZURE.label;
		}
        azureImdsCredentials = new AzureImdsCredentials(scope, credsId, description, azureEnvironment);
        
        azureImdsCredentials.setClientId(clientId.trim());
        azureImdsCredentials.setSubscriptionId(subscriptionId.trim());
        
    	return azureImdsCredentials;
    }

}
