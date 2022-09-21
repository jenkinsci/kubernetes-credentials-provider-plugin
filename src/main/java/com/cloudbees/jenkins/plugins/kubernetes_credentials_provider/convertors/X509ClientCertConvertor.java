/*
 * The MIT License
 *
 * Copyright 2022 CloudBees, Inc., @sgybas
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
import io.fabric8.kubernetes.api.model.Secret;
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerCredentials;
import org.jenkinsci.plugins.variant.OptionalExtension;

@OptionalExtension(requirePlugins = {"docker-commons"})
public class X509ClientCertConvertor extends SecretToCredentialConverter {

    @Override
    public boolean canConvert(String type) {
        return "x509ClientCert".equals(type);
    }

    @Override
    public DockerServerCredentials convert(Secret secret) throws CredentialsConvertionException {

        SecretUtils.requireNonNull(secret.getData(), "X.509 client certificate definition contains no data");

        String clientCertificateBase64 = SecretUtils.getNonNullSecretData(secret, "clientCertificate", "X.509 client certificate is missing the clientCertificate entry");
        String clientCertificate = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(clientCertificateBase64), "X.509 client certificate has an invalid clientCertificate entry (must be base64 encoded UTF-8)");

        String clientKeySecretBase64 = SecretUtils.getNonNullSecretData(secret, "clientKeySecret", "X.509 client certificate is missing the clientKeySecret entry");
        String clientKeySecret = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(clientKeySecretBase64), "X.509 client certificate has an invalid clientKeySecret entry (must be base64 encoded UTF-8)");

        String serverCaCertificateBase64 = SecretUtils.getNonNullSecretData(secret, "serverCaCertificate", "X.509 client certificate is missing the serverCaCertificate entry");
        String serverCaCertificate = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(serverCaCertificateBase64), "X.509 client certificate has an invalid serverCaCertificate entry (must be base64 encoded UTF-8)");

        return new DockerServerCredentials(
                // scope
                SecretUtils.getCredentialScope(secret),
                // id
                SecretUtils.getCredentialId(secret),
                // description
                SecretUtils.getCredentialDescription(secret),
                // clientKeySecret
                hudson.util.Secret.fromString(clientKeySecret),
                // clientCertificate
                clientCertificate,
                // serverCaCertificate
                serverCaCertificate);
    }
}
