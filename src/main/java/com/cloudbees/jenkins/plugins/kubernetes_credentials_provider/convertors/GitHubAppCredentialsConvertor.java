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

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretToCredentialConverter;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretUtils;
import com.cloudbees.plugins.credentials.SecretBytes;
import hudson.Extension;
import io.fabric8.kubernetes.api.model.Secret;
import java.io.UnsupportedEncodingException;
import org.jenkinsci.plugins.github_branch_source.GitHubAppCredentials;

/**
 * SecretToCredentialConvertor that converts {@link GitHubAppCredentials}.
 */
@Extension
public class GitHubAppCredentialsConvertor extends SecretToCredentialConverter {

    @Override
    public boolean canConvert(String type) {
        return "gitHubApp".equals(type);
    }

    @Override
    public GitHubAppCredentials convert(Secret secret) throws CredentialsConvertionException {
        SecretUtils.requireNonNull(secret.getData(), "gitHubApp definition contains no data");

        String appIDBase64 = SecretUtils.getNonNullSecretData(secret, "appID", "gitHubApp credential is missing the appID");

        String privateKeyBase64 = SecretUtils.getNonNullSecretData(secret, "privateKey", "gitHubApp credential is missing the privateKey");

        String appID = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(appIDBase64), "gitHubApp credential has an invalid appID (must be base64 encoded UTF-8)");

        byte[] privateKey = SecretUtils.requireNonNull(SecretUtils.base64Decode(privateKeyBase64), "gitHubApp credential has an invalid privateKey (must be base64 encoded data)");

        hudson.util.Secret privateKeySecret = null;
        try {
            privateKeySecret = hudson.util.Secret.fromString(new String(privateKey, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new CredentialsConvertionException("unable to convert privateKey", e);
        }

        return new GitHubAppCredentials(SecretUtils.getCredentialScope(secret), SecretUtils.getCredentialId(secret), SecretUtils.getCredentialDescription(secret), appID, privateKeySecret);

    }

}
