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

import com.cloudbees.plugins.credentials.CredentialsScope;
import hudson.util.Secret;
import org.jenkinsci.plugins.docker.commons.credentials.DockerServerCredentials;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class DockerServerCredentialsConvertorTest extends AbstractConverterTest {

	private static final String clientCertificate = """
			-----BEGIN CERTIFICATE-----
			MIIBYjCCAQygAwIBAgIJAKZlQzqGGWu9MA0GCSqGSIb3DQEBBQUAMEExCzAJBgNV
			BAYTAlhYMQswCQYDVQQIDAJYWDELMAkGA1UEBwwCWFgxCzAJBgNVBAoMAlhYMQsw
			CQYDVQQDDAJjYTAeFw0yMjA5MjEwNzQzMzVaFw0yMjEwMjEwNzQzMzVaMBExDzAN
			BgNVBAMMBmNsaWVudDBcMA0GCSqGSIb3DQEBAQUAA0sAMEgCQQDB/x6RULr5QOYl
			ulbzZI+8wPZMnrDPwMpP3Kh1MzxJwm1E0LJcI1nY3ePsoIGGQVITNNnjfBbEuYU6
			01sljo5/AgMBAAGjFzAVMBMGA1UdJQQMMAoGCCsGAQUFBwMCMA0GCSqGSIb3DQEB
			BQUAA0EAVP35oeWUOiRaIv9zCDt+3VRMQd6eggmmsx5qyy6ee/mLPpdUWUSt8Ayf
			AiwAD2dca4XziVtJYVK++VnFGG/5EQ==
			-----END CERTIFICATE-----""";

	private static final String clientKeySecret = """
			-----BEGIN PRIVATE KEY-----
			MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAwf8ekVC6+UDmJbpW
			82SPvMD2TJ6wz8DKT9yodTM8ScJtRNCyXCNZ2N3j7KCBhkFSEzTZ43wWxLmFOtNb
			JY6OfwIDAQABAkAU3CDmUT75pE/bCLFm1I5cJoeVb47ll/5pHfoDODIoYA5LnQy9
			/z4PNYCyw3Cq9m3+nf+HSRs8JcWuU7u93BaBAiEA/9mPwDrTlDhpILnmbsbIxXkq
			zeUgypmM1cxQnhtYS78CIQDCHEPgHCdWYCLPnMxUjwrzXtyrIlWJ89j04uOVyN1t
			QQIhANI4mFYRv/Fk3HSIax+QdD1Vzub4opX1zvOI+qC+xTEPAiBiM/KS+ytbo594
			8ZbeYM/leGSjn+cut9NXcUI6kTiVAQIgd/FTmiUryLcSUxzz6YqmU+wU1+ebSHmx
			U87XDZwmb40=
			-----END PRIVATE KEY-----
			""";

	private static final String serverCaCertificate = """
			-----BEGIN CERTIFICATE-----
			MIIBdDCCAR4CCQCgzKd3hWBmXTANBgkqhkiG9w0BAQsFADBBMQswCQYDVQQGEwJY
			WDELMAkGA1UECAwCWFgxCzAJBgNVBAcMAlhYMQswCQYDVQQKDAJYWDELMAkGA1UE
			AwwCY2EwHhcNMjIwOTIxMDc0MzM1WhcNMjIxMDIxMDc0MzM1WjBBMQswCQYDVQQG
			EwJYWDELMAkGA1UECAwCWFgxCzAJBgNVBAcMAlhYMQswCQYDVQQKDAJYWDELMAkG
			A1UEAwwCY2EwXDANBgkqhkiG9w0BAQEFAANLADBIAkEAruIT3of/2lUvYPY7Azsj
			AtKZnV6gthB6K70AsgKPp63xdlBrMgg5CYH7Xe7VmLXb7xhHLBHBnRJ3vPbH/m7h
			swIDAQABMA0GCSqGSIb3DQEBCwUAA0EAfWb62RJ21i7tlbSttmu7by/k4fML31FQ
			XoR7JjrHmbI+f1BkwSbMVxxadAWpSkk/NNI1+SHR/nYSv/loQ3UjmA==
			-----END CERTIFICATE-----""";

	private final DockerServerCredentialsConvertor convertor = new DockerServerCredentialsConvertor();

	@Test
	void canConvert() {
		assertThat("correct registration of valid type", convertor.canConvert("x509ClientCert"), is(true));
		assertThat("incorrect type is rejected", convertor.canConvert("somethingElse"), is(false));
	}

	@Test
	void canConvertAValidSecret() throws Exception {
		testExpectedCredentials("valid.yaml", "x509-valid", "Valid X.509 client certificate", CredentialsScope.GLOBAL);
	}

	@Test
	void canConvertAValidSecretWithNoDescription() throws Exception {
		testExpectedCredentials("validNoDescription.yaml", "x509-valid-no-description", "", CredentialsScope.GLOBAL);
	}

	@Test
	void canConvertAValidMappedSecret() throws Exception {
		testExpectedCredentials("validMapped.yaml", "x509-valid-mapped", "Valid mapped X.509 client certificate", CredentialsScope.GLOBAL);
	}

	@Test
	void canConvertAValidScopedSecret() throws Exception {
		testExpectedCredentials("validScoped.yaml", "x509-valid-scoped", "Valid scoped X.509 client certificate", CredentialsScope.SYSTEM);
	}

	@Test
	void failsToConvertWhenDataIsEmpty() throws Exception {
		testNoData(convertor);
	}

	@Test
	void failsToConvertWhenClientCertificateIsCorrupt() throws Exception {
		testCorruptField(convertor, "clientCertificate");
	}

	@Test
	void failsToConvertWhenClientKeySecretIsCorrupt() throws Exception {
		testCorruptField(convertor, "clientKeySecret");
	}

	@Test
	void failsToConvertWhenServerCaCertificatIsCorrupt() throws Exception {
		testCorruptField(convertor, "serverCaCertificate");
	}

	@Test
	void failsToConvertWhenClientCertificateIsMissing() throws Exception {
		testMissingField(convertor, "clientCertificate");
	}

	@Test
	void failsToConvertWhenClientKeySecretIsMissing() throws Exception {
		testMissingField(convertor, "clientKeySecret");
	}

	@Test
	void failsToConvertWhenServerCaCertificateIsMissing() throws Exception {
		testMissingField(convertor, "serverCaCertificate");
	}

	private void testExpectedCredentials(String resource, String id, String description, CredentialsScope scope) throws Exception {
		DockerServerCredentials credential = convertor.convert(getSecret(resource));

		assertThat("credential id is mapped correctly", credential.getId(), is(id));
		assertThat("credential description is mapped correctly", credential.getDescription(), is(description));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(scope));
		assertThat("credential clientCertificate is mapped correctly", credential.getClientCertificate(), is(clientCertificate));
		assertThat("credential clientKeySecret is mapped correctly", Secret.toString(credential.getClientKeySecret()), is(clientKeySecret));
		assertThat("credential serverCaCertificate is mapped correctly", credential.getServerCaCertificate(), is(serverCaCertificate));
	}
}
