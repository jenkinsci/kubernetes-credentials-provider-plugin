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

import io.fabric8.kubernetes.api.model.Secret;
import java.util.Optional;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import org.jenkinsci.plugins.variant.OptionalExtension;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretToCredentialConverter;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretUtils;
import com.cloudbees.plugins.credentials.CredentialsScope;

/**
 * SecretToCredentialConvertor that converts {@link BasicSSHUserPrivateKey}.
 */
@OptionalExtension(requirePlugins={"ssh-credentials"})
public class BasicSSHUserPrivateKeyCredentialsConvertor extends SecretToCredentialConverter {

    @Override
    public boolean canConvert(String type) {
        return "basicSSHUserPrivateKey".equals(type);
    }

    @Override
    public BasicSSHUserPrivateKey convert(Secret secret) throws CredentialsConvertionException {

        SecretUtils.requireNonNull(secret.getData(), "basicSSHUserPrivateKey definition contains no data");

        String privateKeyBase64 = SecretUtils.getNonNullSecretData(secret, "privateKey", "basicSSHUserPrivateKey credential is missing the privateKey"); 
        String privateKey = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(privateKeyBase64), "basicSSHUserPrivateKey credential has an invalid privateKey (must be base64 encoded UTF-8)");

        String usernameBase64 = SecretUtils.getNonNullSecretData(secret, "username", "basicSSHUserPrivateKey credential is missing the username");
        String username = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(usernameBase64), "basicSSHUserPrivateKey credential has an invalid username (must be base64 encoded UTF-8)");

        Optional<String> optPassphraseBase64 = SecretUtils.getOptionalSecretData(secret, "passphrase", "basicSSHUserPrivateKey credential: failed to retrieve passphrase, assuming private key has an empty passphrase");
        String passphrase = null; 

        if (optPassphraseBase64.isPresent()) {
            passphrase = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(optPassphraseBase64.get()), "basicSSHUserPrivateKey credential has an invalid passphrase (must be base64 encoded UTF-8)");
        }

        return new BasicSSHUserPrivateKey(
            // Scope
            SecretUtils.getScope(secret),
            // ID
            SecretUtils.getCredentialId(secret),
            // Username
            username,
            // PrivateKeySource
            new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(privateKey),
            // Passphrase
            passphrase,
            // Desc
            SecretUtils.getCredentialDescription(secret)
        );

    }

}
