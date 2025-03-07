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
import jenkins.plugins.openstack.compute.auth.OpenstackCredentialv3;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;

/**
 * Tests OpenstackCredentialv3Convertor
 */
@ExtendWith(MockitoExtension.class)
class OpenstackCredentialv3ConvertorTest extends AbstractConverterTest {

	@Mock
	private ConfidentialStore confidentialStore;
	@Mock
	private MockedStatic<ConfidentialStore> confidentialStoreMockedStatic;

	// return null rather than go looking up Jenkins.getInstance....
	@Mock
	private MockedStatic<HistoricalSecrets> historicalSecretsMockedStatic;

	private final OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

	@BeforeEach
	void setup() {
		confidentialStoreMockedStatic.when(ConfidentialStore::get).thenReturn(confidentialStore);
	}

	@Test
	void canConvert() {
		assertThat("correct registration of valid type", convertor.canConvert("openstackCredentialv3"), is(true));
		assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
	}

	@Test
	void canConvertAValidSecret() throws Exception {
		Secret secret = getSecret("valid.yaml");
		OpenstackCredentialv3 credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("test-openstack-credential-v3"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("openstack credentials for you Jenkins in Kubernetes!"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
		assertThat("credential userName is mapped correctly", credential.getUserName(), is("casualName"));
		assertThat("credential userDomain is mapped correctly", credential.getUserDomain(), is("meaningfulDomain"));
		assertThat("credential projectName is mapped correctly", credential.getProjectName(), is("simpleProject"));
		assertThat("credential projectDomain is mapped correctly", credential.getProjectDomain(), is("everSimplerDomain"));
		assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("s3cr3tPass"));
	}

	@Test
	void canConvertAValidMappedSecret() throws Exception {
		Secret secret = getSecret("validMapped.yaml");
		OpenstackCredentialv3 credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("test-openstack-credential-v3"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("openstack credentials for you Jenkins in Kubernetes!"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
		assertThat("credential userName is mapped correctly", credential.getUserName(), is("casualName"));
		assertThat("credential userDomain is mapped correctly", credential.getUserDomain(), is("meaningfulDomain"));
		assertThat("credential projectName is mapped correctly", credential.getProjectName(), is("simpleProject"));
		assertThat("credential projectDomain is mapped correctly", credential.getProjectDomain(), is("everSimplerDomain"));
		assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("s3cr3tPass"));
	}

	@Test
	void canConvertAValidSecretWithNoDescription() throws Exception {
		Secret secret = getSecret("valid-no-desc.yaml");
		OpenstackCredentialv3 credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("test-openstack-credential-v3"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is(emptyString()));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
		assertThat("credential userName is mapped correctly", credential.getUserName(), is("casualName"));
		assertThat("credential userDomain is mapped correctly", credential.getUserDomain(), is("meaningfulDomain"));
		assertThat("credential projectName is mapped correctly", credential.getProjectName(), is("simpleProject"));
		assertThat("credential projectDomain is mapped correctly", credential.getProjectDomain(), is("everSimplerDomain"));
		assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("s3cr3tPass"));
	}

	@Issue("JENKINS-53105")
	@Test
	void canConvertAValidScopedSecret() throws Exception {
		Secret secret = getSecret("validScoped.yaml");
		OpenstackCredentialv3 credential = convertor.convert(secret);
		assertThat(credential, notNullValue());
		assertThat("credential id is mapped correctly", credential.getId(), is("test-openstack-credential-v3"));
		assertThat("credential description is mapped correctly", credential.getDescription(), is("openstack credentials for you Jenkins in Kubernetes!"));
		assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
		assertThat("credential userName is mapped correctly", credential.getUserName(), is("casualName"));
		assertThat("credential userDomain is mapped correctly", credential.getUserDomain(), is("meaningfulDomain"));
		assertThat("credential projectName is mapped correctly", credential.getProjectName(), is("simpleProject"));
		assertThat("credential projectDomain is mapped correctly", credential.getProjectDomain(), is("everSimplerDomain"));
		assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("s3cr3tPass"));
	}

	@Test
	void failsToConvertWhenUserNameMissing() throws Exception {
		testMissingField(convertor, "userName");
	}

	@Test
	void failsToConvertWhenUserDomainMissing() throws Exception {
		testMissingField(convertor, "userDomain");
	}

	@Test
	void failsToConvertWhenProjectNameMissing() throws Exception {
		testMissingField(convertor, "projectName");
	}

	@Test
	void failsToConvertWhenProjectDomainMissing() throws Exception {
		testMissingField(convertor, "projectDomain");
	}

	@Test
	void failsToConvertWhenPasswordMissing() throws Exception {
		testMissingField(convertor, "password");
	}

	@Test
	void failsToConvertWhenUserNameCorrupt() throws Exception {
		testCorruptField(convertor, "userName");
	}

	@Test
	void failsToConvertWhenUserDomainCorrupt() throws Exception {
		testCorruptField(convertor, "userDomain");
	}

	@Test
	void failsToConvertWhenProjectNameCorrupt() throws Exception {
		testCorruptField(convertor, "projectName");
	}

	@Test
	void failsToConvertWhenProjectDomainCorrupt() throws Exception {
		testCorruptField(convertor, "projectDomain");
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
