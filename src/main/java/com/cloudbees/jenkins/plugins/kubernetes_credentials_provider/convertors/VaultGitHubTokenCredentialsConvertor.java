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
import com.datapipe.jenkins.vault.credentials.VaultGithubTokenCredential;
import hudson.Extension;
import io.fabric8.kubernetes.api.model.Secret;

/**
 * SecretToCredentialConvertor that converts {@link com.datapipe.jenkins.vault.credentials.VaultGithubTokenCredential}.
 */
@Extension
public class VaultGitHubTokenCredentialsConvertor extends SecretToCredentialConverter {

    @Override
    public boolean canConvert(String type) {
        return "vaultGitHubToken".equals(type);
    }

    @Override
    public VaultGithubTokenCredential convert(Secret secret) throws CredentialsConvertionException {
        SecretUtils.requireNonNull(secret.getData(), "vaultGitHubToken definition contains no data");

        String accessTokenBase64 = SecretUtils.getNonNullSecretData(secret, "accessToken", "vaultGitHubToken credential is missing the accessToken");
        Optional<String> mountPathBase64 = SecretUtils.getOptionalSecretData(secret, "mountPath", "vaultGitHubToken credential is missing the mountPath");
        Optional<String> namespaceBase64 = SecretUtils.getOptionalSecretData(secret, "namespace", "vaultGitHubToken credential is missing the namespace");

        String accessToken = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(accessTokenBase64), "vaultGitHubToken credential has an invalid accessToken (must be base64 encoded UTF-8)");

        VaultGithubTokenCredential cred = new VaultGithubTokenCredential(SecretUtils.getCredentialScope(secret),
                SecretUtils.getCredentialId(secret),
                SecretUtils.getCredentialDescription(secret),
                hudson.util.Secret.fromString(accessToken));

        if (mountPathBase64.isPresent()) {
            String mountPath = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(mountPathBase64.get()), "vaultGitHubToken credential has an invalid mountPath (must be base64 encoded UTF-8)");
            cred.setMountPath(mountPath);
        }

        if (namespaceBase64.isPresent()) {
            String namespace = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(namespaceBase64.get()), "vaultGitHubToken credential has an invalid namespace (must be base64 encoded UTF-8)");
            cred.setNamespace(namespace);
        }

        return cred;
    }

}
