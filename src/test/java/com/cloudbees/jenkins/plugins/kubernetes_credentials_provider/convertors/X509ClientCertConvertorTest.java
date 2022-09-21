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
import com.cloudbees.plugins.credentials.CredentialsScope;
import hudson.util.Secret;
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerCredentials;
import org.junit.Test;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class X509ClientCertConvertorTest extends AbstractConverterTest {

    private final String clientCertificate = "-----BEGIN CERTIFICATE-----\n" +
            "MIIBYjCCAQygAwIBAgIJAKZlQzqGGWu9MA0GCSqGSIb3DQEBBQUAMEExCzAJBgNV\n" +
            "BAYTAlhYMQswCQYDVQQIDAJYWDELMAkGA1UEBwwCWFgxCzAJBgNVBAoMAlhYMQsw\n" +
            "CQYDVQQDDAJjYTAeFw0yMjA5MjEwNzQzMzVaFw0yMjEwMjEwNzQzMzVaMBExDzAN\n" +
            "BgNVBAMMBmNsaWVudDBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQDB/x6RULr5QOYl\n" +
            "ulbzZI+8wPZMnrDPwMpP3Kh1MzxJwm1E0LJcI1nY3ePsoIGGQVITNNnjfBbEuYU6\n" +
            "01sljo5/AgMBAAGjFzAVMBMGA1UdJQQMMAoGCCsGAQUFBwMCMA0GCSqGSIb3DQEB\n" +
            "BQUAA0EAVP35oeWUOiRaIv9zCDt+3VRMQd6eggmmsx5qyy6ee/mLPpdUWUSt8Ayf\n" +
            "AiwAD2dca4XziVtJYVK++VnFGG/5EQ==\n" +
            "-----END CERTIFICATE-----";

    private final String clientKeySecret = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAwf8ekVC6+UDmJbpW\n" +
            "82SPvMD2TJ6wz8DKT9yodTM8ScJtRNCyXCNZ2N3j7KCBhkFSEzTZ43wWxLmFOtNb\n" +
            "JY6OfwIDAQABAkAU3CDmUT75pE/bCLFm1I5cJoeVb47ll/5pHfoDODIoYA5LnQy9\n" +
            "/z4PNYCyw3Cq9m3+nf+HSRs8JcWuU7u93BaBAiEA/9mPwDrTlDhpILnmbsbIxXkq\n" +
            "zeUgypmM1cxQnhtYS78CIQDCHEPgHCdWYCLPnMxUjwrzXtyrIlWJ89j04uOVyN1t\n" +
            "QQIhANI4mFYRv/Fk3HSIax+QdD1Vzub4opX1zvOI+qC+xTEPAiBiM/KS+ytbo594\n" +
            "8ZbeYM/leGSjn+cut9NXcUI6kTiVAQIgd/FTmiUryLcSUxzz6YqmU+wU1+ebSHmx\n" +
            "U87XDZwmb40=\n" +
            "-----END PRIVATE KEY-----\n";

    private final String serverCaCertificate = "-----BEGIN CERTIFICATE-----\n" +
            "MIIBdDCCAR4CCQCgzKd3hWBmXTANBgkqhkiG9w0BAQsFADBBMQswCQYDVQQGEwJY\n" +
            "WDELMAkGA1UECAwCWFgxCzAJBgNVBAcMAlhYMQswCQYDVQQKDAJYWDELMAkGA1UE\n" +
            "AwwCY2EwHhcNMjIwOTIxMDc0MzM1WhcNMjIxMDIxMDc0MzM1WjBBMQswCQYDVQQG\n" +
            "EwJYWDELMAkGA1UECAwCWFgxCzAJBgNVBAcMAlhYMQswCQYDVQQKDAJYWDELMAkG\n" +
            "A1UEAwwCY2EwXDANBgkqhkiG9w0BAQEFAANLADBIAkEAruIT3of/2lUvYPY7Azsj\n" +
            "AtKZnV6gthB6K70AsgKPp63xdlBrMgg5CYH7Xe7VmLXb7xhHLBHBnRJ3vPbH/m7h\n" +
            "swIDAQABMA0GCSqGSIb3DQEBCwUAA0EAfWb62RJ21i7tlbSttmu7by/k4fML31FQ\n" +
            "XoR7JjrHmbI+f1BkwSbMVxxadAWpSkk/NNI1+SHR/nYSv/loQ3UjmA==\n" +
            "-----END CERTIFICATE-----";

    private X509ClientCertConvertor convertor = new X509ClientCertConvertor();

    @Test
    public void canConvert() throws Exception {
        assertThat("correct registration of valid type", convertor.canConvert("x509ClientCert"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("somethingElse"), is(false));
    }

    @Test
    public void canConvertAValidSecret() throws CredentialsConvertionException, IOException {
        testExpectedCredentials("valid.yaml", "x509-valid", "Valid X.509 client certificate", CredentialsScope.GLOBAL);
    }

    @Test
    public void canConvertAValidSecretWithNoDescription() throws CredentialsConvertionException, IOException {
        testExpectedCredentials("validNoDescription.yaml", "x509-valid-no-description", "", CredentialsScope.GLOBAL);
    }

    @Test
    public void canConvertAValidMappedSecret() throws CredentialsConvertionException, IOException {
        testExpectedCredentials("validMapped.yaml", "x509-valid-mapped", "Valid mapped X.509 client certificate", CredentialsScope.GLOBAL);
    }

    @Test
    public void canConvertAValidScopedSecret() throws CredentialsConvertionException, IOException {
        testExpectedCredentials("validScoped.yaml", "x509-valid-scoped", "Valid scoped X.509 client certificate", CredentialsScope.SYSTEM);
    }

    @Test
    public void failsToConvertWhenDataIsEmpty() throws IOException {
        testNoData(convertor);
    }

    @Test
    public void failsToConvertWhenClientCertificatIsCorrupt() throws IOException {
        testCorruptField(convertor, "clientCertificate");
    }

    @Test
    public void failsToConvertWhenClientKeySecretIsCorrupt() throws IOException {
        testCorruptField(convertor, "clientKeySecret");
    }

    @Test
    public void failsToConvertWhenServerCaCertificatIsCorrupt() throws IOException {
        testCorruptField(convertor, "serverCaCertificate");
    }

    @Test
    public void failsToConvertWhenClientCertificateIsMissing() throws IOException {
        testMissingField(convertor, "clientCertificate");
    }

    @Test
    public void failsToConvertWhenClientKeySecretIsMissing() throws IOException {
        testMissingField(convertor, "clientKeySecret");
    }

    @Test
    public void failsToConvertWhenServerCaCertificateIsMissing() throws IOException {
        testMissingField(convertor, "serverCaCertificate");
    }

    private void testExpectedCredentials(String resource, String id, String description, CredentialsScope scope) throws CredentialsConvertionException, IOException {
        DockerServerCredentials credential = convertor.convert(getSecret(resource));

        assertThat("credential id is mapped correctly", credential.getId(), is(id));
        assertThat("credential description is mapped correctly", credential.getDescription(), is(description));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(scope));
        assertThat("credential clientCertificate is mapped correctly", credential.getClientCertificate(), is(clientCertificate));
        assertThat("credential clientKeySecret is mapped correctly", Secret.toString(credential.getClientKeySecret()), is(clientKeySecret));
        assertThat("credential serverCaCertificate is mapped correctly", credential.getServerCaCertificate(), is(serverCaCertificate));
    }
}
