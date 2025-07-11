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

import com.cloudbees.plugins.credentials.CredentialsScope;
import hudson.util.HistoricalSecrets;
import io.fabric8.kubernetes.api.model.Secret;
import jenkins.security.ConfidentialStore;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
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

import java.nio.charset.StandardCharsets;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests FileCredentialsConvertor
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FileCredentialsConvertorTest extends AbstractConverterTest {

	@Mock
	private ConfidentialStore confidentialStore;
	@Mock
	private MockedStatic<ConfidentialStore> confidentialStoreMockedStatic;

	// return null rather than go looking up Jenkins.getInstance....
	@Mock
	private MockedStatic<HistoricalSecrets> historicalSecretsMockedStatic;

	private final FileCredentialsConvertor convertor = new FileCredentialsConvertor();

	@BeforeEach
	void setup() {
		confidentialStoreMockedStatic.when(ConfidentialStore::get).thenReturn(confidentialStore);
		Mockito.when(confidentialStore.randomBytes(ArgumentMatchers.anyInt())).thenAnswer(it -> new byte[(Integer) (it.getArguments()[0])]);
	}

	@Test
	void canConvert() {
		assertThat("correct registration of valid type", convertor.canConvert("secretFile"), is(true));
		assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
	}

	@Test
	void canConvertAValidSecret() throws Exception {
		Secret secret = getSecret("valid.yaml");
		FileCredentialsImpl credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("another-test-file"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("secret file credential from Kubernetes"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
		assertThat("credential username is mapped correctly", credential.getFileName(), is("mySecret.txt"));
		assertThat("credential password is mapped correctly", credential.getSecretBytes().getPlainData(), is("Hello World!".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void canConvertAValidMappedSecret() throws Exception {
		Secret secret = getSecret("validMapped.yaml");
		FileCredentialsImpl credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("another-test-file"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("secret file credential from Kubernetes"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
		assertThat("credential username is mapped correctly", credential.getFileName(), is("mySecret.txt"));
		assertThat("credential password is mapped correctly", credential.getSecretBytes().getPlainData(), is("Hello World!".getBytes(StandardCharsets.UTF_8)));
	}

	@Issue("JENKINS-53105")
	@Test
	void canConvertAValidScopedSecret() throws Exception {
		Secret secret = getSecret("validScoped.yaml");
		FileCredentialsImpl credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("another-test-file"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("secret file credential from Kubernetes"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
		assertThat("credential username is mapped correctly", credential.getFileName(), is("mySecret.txt"));
		assertThat("credential password is mapped correctly", credential.getSecretBytes().getPlainData(), is("Hello World!".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void failsToConvertWhenFilenameMissing() throws Exception {
		testMissingField(convertor, "filename");
	}

	@Test
	void failsToConvertWhenDataMissing() throws Exception {
		testMissingField(convertor, "data");
	}

	@Test
	void failsToConvertWhenFilenameCorrupt() throws Exception {
		testCorruptField(convertor, "filename");
	}

	@Test
	void failsToConvertWhenDataCorrupt() throws Exception {
		testCorruptField(convertor, "data");
	}

	@Test
	void failsToConvertWhenDataEmpty() throws Exception {
		testNoData(convertor);
	}
}