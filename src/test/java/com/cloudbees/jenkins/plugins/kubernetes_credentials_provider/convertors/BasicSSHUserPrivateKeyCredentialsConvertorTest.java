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

import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsScope;
import hudson.util.HistoricalSecrets;
import io.fabric8.kubernetes.api.model.Secret;
import jenkins.security.ConfidentialStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.Issue;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests BasicSSHUserPrivateKeyCredentialsConvertor
 */
@ExtendWith(MockitoExtension.class)
class BasicSSHUserPrivateKeyCredentialsConvertorTest extends AbstractConverterTest {

	private static final String testkey = """
			-----BEGIN RSA PRIVATE KEY-----
			MIIEowIBAAKCAQEAngWMYnda9vD2utvbAdgCOLVNanA/MW50er5ROW21it/eph1u
			6RCuZ0CiuYUE5Eb8kOOQP7MTL3Ixyv9GW6hmMZwjyvcCamKj7cYuEHBYkn0X2Jgw
			syPGUWZwITgSxgb/VfjRKbAtUdvXNFjHxknUlaVd+G6gQpN5Lv3//O/EglmVqf1d
			CM2xAy9Ixk9roMSmBpgwC7lCsi1W9IGdLrjLAC96BrJkHX1EDQDdB8tWg8qLjZfr
			L1ioddG/NDH8lOUetWX9SB5WF4xi/oBRNvSCwmBAa8v2DvhS/TEwcWAsReclRCNW
			5eGAqhbb0Kl8E0hYJdFlEKYjQH3y5cZtqMAiuwIDAQABAoIBAGQK2TThoYpjRaFJ
			XZ8ONWHXjpqLU8akykOHR/8WsO+qCdibG8OcFv4xkpPnXhBzzKSiHYnmgofwQQvm
			j5GpzIEt/A8cUMAvkN8RL8qihcDAR5+Nwo83X+/a7bRqPqB2f6LbMvi0nAyOJPH0
			Hw4vYdIX7qVAzF855GfW0QE+fueSdtgWviJM8gZHdhCqe/zqYm016zNaavap530r
			tJ/+vhUW8WYqJqBW8+58laW5vTBusNsVjeL40yJF8X/XQQcdZ4XmthNcegx79oim
			j9ELzX0ttchiwAe/trLxTkdWb4rEFz+U50iAOMUdS8T0brb5bxhqNM/ByiqQ28W9
			2NJCVEkCgYEA0phCE9iKVWNZnvWX6+fHgr2NO2ShPexPeRfFxr0ugXGTQvyT0HnM
			/Q//V+LduPMX8b2AsOzI0rQh+4bjohOZvKmGKiuPv3eSvqpi/r6208ZVTBjjFvBO
			UQhMbPUyR6vO1ryFDwBMwMqQ06ldkXArhB+SG0dYnOKb/6g0nO2BVFUCgYEAwBeH
			HGNGuxwum63UAaqyX6lRSpGGm6XSCBhzvHUPnVphgq7nnZOGl0z3U49jreCvuuEc
			fA9YqxJjzoZy5870KOXY2kltlq/U/4Lrb0k75ag6ZVbi0oemACN6KCHtE+Zm2dac
			rW8oKWpRTbsvMOYUvSjF0u8BCrestpRUF977Ks8CgYEAicbLFCjK9+ozq+eJKPFO
			eZ6BU6YWR2je5Z5D6i3CyzT+3whXvECzd6yLpXfrDyEbPTB5jUacbB0lTmWFb3fb
			UK6n89bkCKO2Ab9/XKJxAkPzcgGmME+vLRx8w5v29STWAW78rj/H9ymPbqqTaJ82
			GQ5+jBI1Sw6GeNAW+8P2pLECgYAs/dXBimcosCMih4ZelZKN4WSO6KL0ldQp3UBO
			ZcSwgFjSeRD60XD2wyoywiUAtt2yEcPQMu/7saT63HbRYKHDaoJuLkCiyLBE4G8w
			c6C527tBvSYHVYpGAgk8mSWkQZTZdPDhlmV7vdEpOayF8X3uCDy9eQlvbzHe2cMQ
			jEOb9QKBgG3jSxGfqN/sD8W9BhpVrybCXh2RvhxOBJAFx58wSWTkRcYSwpdyvm7x
			wlMtcEdQgaSBeuBU3HPUdYE07bQNAlYO0p9MQnsLHzd2V9yiCX1Sq5iB6dQpHxyi
			sDZLY2Mym1nUJWfE47GAcxFZtrVh9ojKcmgiHo8qPTkWjFGY7xe/
			-----END RSA PRIVATE KEY-----
			""";

	@Mock
	private ConfidentialStore confidentialStore;
	@Mock
	private MockedStatic<ConfidentialStore> confidentialStoreMockedStatic;

	// return null rather than go looking up Jenkins.getInstance....
	@Mock
	private MockedStatic<HistoricalSecrets> historicalSecretsMockedStatic;

	private final BasicSSHUserPrivateKeyCredentialsConvertor convertor = new BasicSSHUserPrivateKeyCredentialsConvertor();

	@BeforeEach
	void setup() {
		confidentialStoreMockedStatic.when(ConfidentialStore::get).thenReturn(confidentialStore);
	}

	@Test
	void canConvert() {
		assertThat("correct registration of valid type", convertor.canConvert("basicSSHUserPrivateKey"), is(true));
		assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
	}

	@Test
	void canConvertAValidSecretWithoutPassphrase() throws Exception {
		Secret secret = getSecret("valid.yaml");
		BasicSSHUserPrivateKey credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("jenkins-key"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("basic user private key credential from Kubernetes"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
		assertThat("credential username is mapped correctly", credential.getUsername(), is("jenkins"));
		assertThat("credential privateKey is mapped correctly", credential.getPrivateKey(), is(testkey));
		assertThat("credential passphrase is mapped correctly", credential.getPassphrase(), nullValue());
	}

	@Test
	void canConvertAValidSecretWithPassphrase() throws Exception {
		Secret secret = getSecret("validPassphrase.yaml");
		BasicSSHUserPrivateKey credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("jenkins-key"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("basic user private key credential from Kubernetes"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
		assertThat("credential username is mapped correctly", credential.getUsername(), is("jenkins"));
		assertThat("credential privateKey is mapped correctly", credential.getPrivateKey(), is(testkey));
		assertThat("credential passphrase is mapped correctly", credential.getPassphrase(), is(hudson.util.Secret.fromString("mypassphrase")));
	}

	@Test
	void canConvertAValidMappedSecretWithoutPassphrase() throws Exception {
		Secret secret = getSecret("validMapped.yaml");
		BasicSSHUserPrivateKey credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("jenkins-key"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("basic user private key credential from Kubernetes"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
		assertThat("credential username is mapped correctly", credential.getUsername(), is("jenkins"));
		assertThat("credential privateKey is mapped correctly", credential.getPrivateKey(), is(testkey));
		assertThat("credential passphrase is mapped correctly", credential.getPassphrase(), nullValue());
	}

	@Test
	void canConvertAValidMappedSecretWithPassphrase() throws Exception {
		Secret secret = getSecret("validPassphraseMapped.yaml");
		assertThat("The Secret was loaded correctly from disk", notNullValue());
		BasicSSHUserPrivateKey credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("jenkins-key"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("basic user private key credential from Kubernetes"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
		assertThat("credential username is mapped correctly", credential.getUsername(), is("jenkins"));
		assertThat("credential privateKey is mapped correctly", credential.getPrivateKey(), is(testkey));
		assertThat("credential passphrase is mapped correctly", credential.getPassphrase(), is(hudson.util.Secret.fromString("mypassphrase")));
	}

	@Issue("JENKINS-53105")
	@Test
	void canConvertAValidScopedSecret() throws Exception {
		Secret secret = getSecret("validScoped.yaml");
		BasicSSHUserPrivateKey credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("jenkins-key"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("basic user private key credential from Kubernetes"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
		assertThat("credential username is mapped correctly", credential.getUsername(), is("jenkins"));
		assertThat("credential privateKey is mapped correctly", credential.getPrivateKey(), is(testkey));
		assertThat("credential passphrase is mapped correctly", credential.getPassphrase(), nullValue());
	}

	@Test
	void failsToConvertWhenUsernameMissing() throws Exception {
		testMissingField(convertor, "username");
	}

	@Test
	void failsToConvertWhenPrivateKeyMissing() throws Exception {
		testMissingField(convertor, "privateKey");
	}

	@Test
	void failsToConvertWhenUsernameCorrupt() throws Exception {
		testCorruptField(convertor, "username");
	}

	@Test
	void failsToConvertWhenPrivateKeyCorrupt() throws Exception {
		testCorruptField(convertor, "privateKey");
	}

	@Test
	void failsToConvertWhenPassphraseCorrupt() throws Exception {
		testCorruptField(convertor, "passphrase");
	}

	@Test
	void failsToConvertWhenDataEmpty() throws Exception {
		testNoData(convertor);
	}
}
