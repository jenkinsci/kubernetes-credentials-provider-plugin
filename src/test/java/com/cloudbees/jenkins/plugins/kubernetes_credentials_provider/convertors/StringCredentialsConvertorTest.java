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
import io.fabric8.kubernetes.api.model.Secret;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests StringCredentialCredentialsConvertor
 */
class StringCredentialsConvertorTest extends AbstractConverterTest {

	private final StringCredentialConvertor convertor = new StringCredentialConvertor();

	@Test
	void canConvert() {
		assertThat("correct registration of valid type", convertor.canConvert("secretText"), is(true));
		assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
	}

	@Test
	void canConvertAValidSecret() throws Exception {
		Secret secret = getSecret("valid.yaml");
		StringCredentialsImpl credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("a-test-secret"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("secret text credential from Kubernetes"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
		assertThat("credential text is mapped correctly", credential.getSecret().getPlainText(), is("mySecret!"));
	}

	@Test
	void canConvertAValidMappedSecret() throws Exception {
		Secret secret = getSecret("valid.yaml");
		StringCredentialsImpl credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("a-test-secret"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("secret text credential from Kubernetes"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
		assertThat("credential text is mapped correctly", credential.getSecret().getPlainText(), is("mySecret!"));
	}

	@Issue("JENKINS-53105")
	@Test
	void canConvertAValidScopedSecret() throws Exception {
		Secret secret = getSecret("validScoped.yaml");
		StringCredentialsImpl credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("a-test-secret"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("secret text credential from Kubernetes"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
		assertThat("credential text is mapped correctly", credential.getSecret().getPlainText(), is("mySecret!"));
	}

	@Test
	void failsToConvertWhenTextMissing() throws Exception {
		testMissingField(convertor, "text");
	}

	@Test
	void failsToConvertWhenTextCorrupt() throws Exception {
		testCorruptField(convertor, "text");
	}

	@Test
	void failsToConvertWhenDataEmpty() throws Exception {
		testNoData(convertor);
	}
}