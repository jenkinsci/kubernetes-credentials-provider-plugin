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

import java.util.Optional;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretToCredentialConverter;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretUtils;
import com.datapipe.jenkins.vault.credentials.VaultAppRoleCredential;
import io.fabric8.kubernetes.api.model.Secret;
import org.jenkinsci.plugins.variant.OptionalExtension;

/**
 * SecretToCredentialConvertor that converts {@link com.datapipe.jenkins.vault.credentials.VaultAppRoleCredential}.
 */
@OptionalExtension(requirePlugins={"hashicorp-vault-plugin"})
public class VaultAppRoleCredentialsConvertor extends SecretToCredentialConverter {

    @Override
    public boolean canConvert(String type) {
        return "vaultAppRole".equals(type);
    }

    @Override
    public VaultAppRoleCredential convert(Secret secret) throws CredentialsConvertionException {
        SecretUtils.requireNonNull(secret.getData(), "vaultAppRole definition contains no data");

        String roleIdBase64 = SecretUtils.getNonNullSecretData(secret, "roleId", "vaultAppRole credential is missing the roleId");
        String secretIdBase64 = SecretUtils.getNonNullSecretData(secret, "secretId", "vaultAppRole credential is missing the secretId");
        Optional<String> pathBase64 = SecretUtils.getOptionalSecretData(secret, "path", "vaultAppRole credential is missing the path");
        Optional<String> namespaceBase64 = SecretUtils.getOptionalSecretData(secret, "namespace", "vaultAppRole credential is missing the namespace");

        String roleId = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(roleIdBase64), "vaultAppRole credential has an invalid roleId (must be base64 encoded UTF-8)");
        String secretId = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(secretIdBase64), "vaultAppRole credential has an invalid secretId (must be base64 encoded UTF-8)");
        String path = "approle";
        if (pathBase64.isPresent()) {
            path = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(pathBase64.get()), "vaultAppRole credential has an invalid path (must be base64 encoded UTF-8)");
        }

        VaultAppRoleCredential cred = new VaultAppRoleCredential(SecretUtils.getCredentialScope(secret),
                SecretUtils.getCredentialId(secret),
                SecretUtils.getCredentialDescription(secret),
                roleId,
                hudson.util.Secret.fromString(secretId),
                path);

        if (namespaceBase64.isPresent()) {
            String namespace = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(namespaceBase64.get()), "vaultAppRole credential has an invalid namespace (must be base64 encoded UTF-8)");
            cred.setNamespace(namespace);
        }

        return cred;
    }

}
