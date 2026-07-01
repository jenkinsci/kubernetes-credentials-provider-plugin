/*
 * The MIT License
 *
 * Copyright 2021 CloudBees, Inc.
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
import io.fabric8.kubernetes.client.utils.Serialization;
import org.jenkinsci.plugins.github_branch_source.GitHubAppCredentials;
import org.jenkinsci.plugins.github_branch_source.app_credentials.AccessSpecifiedRepositories;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Tests {@link GitHubAppCredentialsConvertor}
 */
class GitHubAppCredentialsConvertorTest extends AbstractConverterTest {

    private final GitHubAppCredentialsConvertor convertor = new GitHubAppCredentialsConvertor();

    @Test
    void canConvert() {
        assertThat("correct registration of valid type", convertor.canConvert("gitHubApp"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
    }

    @Test
    void canConvertAValidSecret() throws Exception {
        Secret secret = getSecret("valid.yaml");
        GitHubAppCredentials credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-githubapp"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential appID is mapped correctly", credential.getAppID(), is("1234"));
        assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), is(notNullValue()));
        assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), startsWith("-----BEGIN PRIVATE KEY-----"));
    }

    @Test
    void canConvertAValidSecretWithOwner() throws Exception {
        Secret secret = getSecret("validWithOwner.yaml");
        GitHubAppCredentials credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-githubapp"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential appID is mapped correctly", credential.getAppID(), is("1234"));
        assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), is(notNullValue()));
        assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), startsWith("-----BEGIN PRIVATE KEY-----"));
        assertThat("credential repository access strategy is mapped correctly", credential.getRepositoryAccessStrategy(), instanceOf(AccessSpecifiedRepositories.class));
        var strategy = (AccessSpecifiedRepositories) credential.getRepositoryAccessStrategy();
        assertThat("credential owner is mapped correctly", strategy.getOwner(), is("cookies"));
    }

    @Test
    @Issue("JENKINS-69128")
    void canConvertAValidSecretWithApiUri() throws Exception {
        Secret secret = getSecret("validWithApiUri.yaml");
        GitHubAppCredentials credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-githubapp"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential appID is mapped correctly", credential.getAppID(), is("1234"));
        assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), is(notNullValue()));
        assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), startsWith("-----BEGIN PRIVATE KEY-----"));
        assertThat("credential apiUri is mapped correctly", credential.getApiUri(), is("https://github.example.com/api/v3"));
    }

    @Test
    void canConvertAValidSecretWithNoDescription() throws Exception {
        Secret secret = getSecret("valid-no-desc.yaml");
        GitHubAppCredentials credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-githubapp"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is(emptyString()));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential appID is mapped correctly", credential.getAppID(), is("1234"));
        assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), is(notNullValue()));
        assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), startsWith("-----BEGIN PRIVATE KEY-----"));
    }

    @Test
    void canConvertAValidScopedSecret() throws Exception {
        Secret secret = getSecret("validScoped.yaml");
        GitHubAppCredentials credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-githubapp"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
        assertThat("credential appID is mapped correctly", credential.getAppID(), is("1234"));
        assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), is(notNullValue()));
        assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), startsWith("-----BEGIN PRIVATE KEY-----"));
    }

    @Test
    void failsToConvertWhenAppIdMissing() throws Exception {
        testMissingField(convertor, "appID");
    }

    @Test
    void failsToConvertWhenPrivateKeyMissing() throws Exception {
        testMissingField(convertor, "privateKey");
    }

    @Test
    void failsToConvertWhenAppIdCorrupt() throws Exception {
        testCorruptField(convertor, "appID");
    }

    @Test
    void failsToConvertWhenPrivateKeyCorrupt() throws Exception {
        testCorruptField(convertor, "privateKey");
    }

    @Test
    void failsToConvertWhenOwnerCorrupt() throws Exception {
        testCorruptField(convertor, "owner");
    }

    @Test
    @Issue("JENKINS-69128")
    void failsToConvertWhenApiUriCorrupt() throws Exception {
        testCorruptField(convertor, "apiUri");
    }

    @Test
    void failsToConvertWhenDataEmpty() throws Exception {
        testNoData(convertor);
    }
}
