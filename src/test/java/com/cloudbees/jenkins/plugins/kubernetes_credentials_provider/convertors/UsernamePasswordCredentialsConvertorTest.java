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
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import io.fabric8.kubernetes.api.model.Secret;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;

/**
 * Tests UsernamePasswordCredentialConvertor
 */
class UsernamePasswordCredentialsConvertorTest extends AbstractConverterTest {

    private final UsernamePasswordCredentialsConvertor convertor = new UsernamePasswordCredentialsConvertor();

    @Test
    void canConvert() {
        assertThat("correct registration of valid type", convertor.canConvert("usernamePassword"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
    }

    @Test
    void canConvertAValidSecret() throws Exception {
        Secret secret = getSecret("valid.yaml");
        UsernamePasswordCredentialsImpl credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-usernamepass"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential username is mapped correctly", credential.getUsername(), is("myUsername"));
        assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("Pa$$word"));
    }

    @Test
    void canConvertAValidMappedSecret() throws Exception {
        Secret secret = getSecret("validMapped.yaml");
        UsernamePasswordCredentialsImpl credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-usernamepass"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential username is mapped correctly", credential.getUsername(), is("myUsername"));
        assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("Pa$$word"));
    }

    @Issue("JENKINS-54313")
    @Test
    void canConvertAValidSecretWithNoDescription() throws Exception {
        Secret secret = getSecret("valid-no-desc.yaml");
        UsernamePasswordCredentialsImpl credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-usernamepass"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is(emptyString()));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential username is mapped correctly", credential.getUsername(), is("myUsername"));
        assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("Pa$$word"));
    }

    @Issue("JENKINS-53105")
    @Test
    void canConvertAValidScopedSecret() throws Exception {
        Secret secret = getSecret("validScoped.yaml");
        UsernamePasswordCredentialsImpl credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-usernamepass"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
        assertThat("credential username is mapped correctly", credential.getUsername(), is("myUsername"));
        assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("Pa$$word"));
    }

    @Test
    void failsToConvertWhenUsernameMissing() throws Exception {
        testMissingField(convertor, "username");
    }

    @Test
    void failsToConvertWhenPasswordMissing() throws Exception {
        testMissingField(convertor, "password");
    }

    @Test
    void failsToConvertWhenUsernameCorrupt() throws Exception {
        testCorruptField(convertor, "username");
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