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

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.datapipe.jenkins.vault.credentials.VaultTokenCredential;
import io.fabric8.kubernetes.api.model.Secret;
import org.junit.Before;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

/**
 * Tests {@link VaultTokenCredentialsConvertor}
 */
public class VaultTokenCredentialsConvertorTest extends AbstractConverterTest {

    private VaultTokenCredentialsConvertor convertor;

    @Before
    public void setup() {
        convertor = new VaultTokenCredentialsConvertor();
    }

    @Test
    public void canConvert() throws Exception {
        assertThat("correct registration of valid type", convertor.canConvert("vaultToken"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
    }

    @Test
    public void canConvertAValidSecret() throws Exception {
        Secret secret = getSecret("valid.yaml");
        assertThat("The Secret was loaded correctly from disk", notNullValue());
        VaultTokenCredential credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-vaulttoken"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential roleId is mapped correctly", credential.getToken().getPlainText(), is("-myToken-"));
    }

    @Test
    public void canConvertAValidMappedSecret() throws Exception {
        Secret secret = getSecret("validMapped.yaml");
        assertThat("The Secret was loaded correctly from disk", notNullValue());
        VaultTokenCredential credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-vaulttoken"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential roleId is mapped correctly", credential.getToken().getPlainText(), is("-myToken-"));
    }

    @Issue("JENKINS-54313")
    @Test
    public void canConvertAValidSecretWithNoDescription() throws Exception {
        Secret secret = getSecret("valid-no-desc.yaml");
        assertThat("The Secret was loaded correctly from disk", notNullValue());
        VaultTokenCredential credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-vaulttoken"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is(emptyString()));
    }

    @Issue("JENKINS-53105")
    @Test
    public void canConvertAValidScopedSecret() throws Exception {
        Secret secret = getSecret("validScoped.yaml");
        assertThat("The Secret was loaded correctly from disk", notNullValue());
        VaultTokenCredential credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-vaulttoken"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
    }

    @Test
    public void failsToConvertWhenTokenCorrupt() throws Exception {
        testCorruptField(convertor, "token");
    }

    @Test
    public void failsToConvertWhenDataEmpty() throws Exception {
        testNoData(convertor);
    }

}