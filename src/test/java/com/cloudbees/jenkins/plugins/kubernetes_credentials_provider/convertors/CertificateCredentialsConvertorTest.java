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
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.CertificateCredentialsImpl;
import hudson.util.HistoricalSecrets;
import io.fabric8.kubernetes.api.model.Secret;
import jenkins.security.ConfidentialStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.Issue;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.security.KeyStore;
import java.security.cert.X509Certificate;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests CertificateCredentialsConvertor
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CertificateCredentialsConvertorTest extends AbstractConverterTest {

	@Mock
	private ConfidentialStore confidentialStore;
	@Mock
	private MockedStatic<ConfidentialStore> confidentialStoreMockedStatic;

	// return null rather than go looking up Jenkins.getInstance....
	@Mock
	private MockedStatic<HistoricalSecrets> historicalSecretsMockedStatic;

	private final CertificateCredentialsConvertor convertor = new CertificateCredentialsConvertor();

	@BeforeEach
	void setup() {
		confidentialStoreMockedStatic.when(ConfidentialStore::get).thenReturn(confidentialStore);
		Mockito.when(confidentialStore.randomBytes(ArgumentMatchers.anyInt())).thenAnswer(it -> new byte[(Integer) (it.getArguments()[0])]);
	}

	@Test
	void canConvert() {
		assertThat("correct registration of valid type", convertor.canConvert("certificate"), is(true));
		assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
	}

	@Test
	void canConvertAValidSecret() throws Exception {
		Secret secret = getSecret("valid.yaml");
		CertificateCredentialsImpl credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("a-test-certificate"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("certificate credential from Kubernetes"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
		assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("testPassword"));
		KeyStore ks = credential.getKeyStore();
		// credential.getKeyStore never returns null so we need to check the Keystore contains our certificate
		assertThat("credential certificate mapped correctly ", ks.containsAlias("myKey"), is(true));
		// TODO what can we check here to see this is valid
		X509Certificate cert = (X509Certificate) ks.getCertificate("myKey");
		assertThat("Correct cert", cert.getSubjectX500Principal().getName(), is("CN=A Test,OU=Dev,O=CloudBees,L=Around The World,ST=Cool,C=earth"));
	}

	@Test
	void canConvertAValidSecretWithMapping() throws Exception {
		Secret secret = getSecret("validMapped.yaml");
		CertificateCredentialsImpl credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("a-test-certificate"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("certificate credential from Kubernetes"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
		assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("testPassword"));
		KeyStore ks = credential.getKeyStore();
		// credential.getKeyStore never returns null so we need to check the Keystore contains our certificate
		assertThat("credential certificate mapped correctly ", ks.containsAlias("myKey"), is(true));
		// TODO what can we check here to see this is valid
		X509Certificate cert = (X509Certificate) ks.getCertificate("myKey");
		assertThat("Correct cert", cert.getSubjectX500Principal().getName(), is("CN=A Test,OU=Dev,O=CloudBees,L=Around The World,ST=Cool,C=earth"));
	}

	@Issue("JENKINS-53105")
	@Test
	void canConvertAValidScopedSecret() throws Exception {
		Secret secret = getSecret("validScoped.yaml");
		CertificateCredentialsImpl credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("a-test-certificate"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("certificate credential from Kubernetes"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
		assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("testPassword"));
		KeyStore ks = credential.getKeyStore();
		// credential.getKeyStore never returns null so we need to check the Keystore contains our certificate
		assertThat("credential certificate mapped correctly ", ks.containsAlias("myKey"), is(true));
		// TODO what can we check here to see this is valid
		X509Certificate cert = (X509Certificate) ks.getCertificate("myKey");
		assertThat("Correct cert", cert.getSubjectX500Principal().getName(), is("CN=A Test,OU=Dev,O=CloudBees,L=Around The World,ST=Cool,C=earth"));
	}

	@Test
	void failsToConvertWhenCertificateMissing() throws Exception {
		testMissingField(convertor, "certificate");
	}

	@Test
	void failsToConvertWhenPasswordMissing() throws Exception {
		testMissingField(convertor, "password");
	}

	/**
	 * Tests when the certificate is not a valid certificate
	 */
	@Test
	void failsToConvertWhenCertificateCorruptPKCS12() throws Exception {
		Secret secret = getSecret("corruptCertificatePKCS12.yaml");
		CredentialsConvertionException cex = assertThrows(CredentialsConvertionException.class, () -> convertor.convert(secret));
		assertThat(cex.getMessage(), allOf(containsString("invalid certificate"), containsString("PKCS#12")));
	}

	/**
	 * Tests when the certificate is not valid base64
	 */
	@Test
	void failsToConvertWhenCertificateCorruptBase64() throws Exception {
		Secret secret = getSecret("corruptBase64Certificate.yaml");
		CredentialsConvertionException cex = assertThrows(CredentialsConvertionException.class, () -> convertor.convert(secret));
		assertThat(cex.getMessage(), allOf(containsString("invalid certificate"), containsString("base64")));
	}

	@Test
	void failsToConvertWhenPasswordCorrupt() throws Exception {
		testCorruptField(convertor, "password");
	}

	@Test
	void failsToConvertWhenDataEmpty() throws Exception {
		testNoData(convertor);
	}
}