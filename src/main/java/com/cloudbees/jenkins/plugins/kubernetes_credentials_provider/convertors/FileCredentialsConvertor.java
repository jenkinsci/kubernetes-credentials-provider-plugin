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
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
import org.jenkinsci.plugins.variant.OptionalExtension;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretToCredentialConverter;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretUtils;
import com.cloudbees.plugins.credentials.SecretBytes;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;

/**
 * SecretToCredentialConvertor that converts {@link UsernamePasswordCredentialsImpl}.
 */
@OptionalExtension(requirePlugins={"plain-credentials"})
public class FileCredentialsConvertor extends SecretToCredentialConverter {

    @Override
    public boolean canConvert(String type) {
        return "secretFile".equals(type);
    }

    @Override
    public FileCredentialsImpl convert(Secret secret) throws CredentialsConvertionException {
        // check we have some data
        SecretUtils.requireNonNull(secret.getData(), "secretFile definition contains no data");

        String filenameBase64 = SecretUtils.getNonNullSecretData(secret, "filename", "secretFile credential is missing the filename"); 

        String dataBase64 = SecretUtils.getNonNullSecretData(secret, "data", "secretFile credential is missing the data");

        String filename = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(filenameBase64), "secretFile credential has an invalid filename (must be base64 encoded UTF-8)");

        byte[] _data = SecretUtils.requireNonNull(SecretUtils.base64Decode(dataBase64), "secretFile credential has an invalid data (must be base64 encoded data)");

        SecretBytes sb = SecretBytes.fromBytes(_data);
        return new FileCredentialsImpl(SecretUtils.getCredentialScope(secret), SecretUtils.getCredentialId(secret), SecretUtils.getCredentialDescription(secret), filename, sb);

    }

}
