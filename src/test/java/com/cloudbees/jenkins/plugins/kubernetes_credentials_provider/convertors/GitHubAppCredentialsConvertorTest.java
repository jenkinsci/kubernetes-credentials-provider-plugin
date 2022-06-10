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

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import java.io.InputStream;
import org.jenkinsci.plugins.github_branch_source.GitHubAppCredentials;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests {@link GitHubAppCredentialsConvertor}
 */
public class GitHubAppCredentialsConvertorTest {


    @Test
    public void canConvert() throws Exception {
        GitHubAppCredentialsConvertor convertor = new GitHubAppCredentialsConvertor();
        assertThat("correct registration of valid type", convertor.canConvert("gitHubApp"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
    }

    @Test
    public void canConvertAValidSecret() throws Exception {
        GitHubAppCredentialsConvertor convertor = new GitHubAppCredentialsConvertor();

        try (InputStream is = get("valid.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            GitHubAppCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-githubapp"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential appID is mapped correctly", credential.getAppID(), is("1234"));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), is(notNullValue()));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), startsWith("-----BEGIN PRIVATE KEY-----"));
        }
    }

    @Test
    public void canConvertAValidSecretWithOwner() throws Exception {
        GitHubAppCredentialsConvertor convertor = new GitHubAppCredentialsConvertor();

        try (InputStream is = get("validWithOwner.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            GitHubAppCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-githubapp"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential appID is mapped correctly", credential.getAppID(), is("1234"));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), is(notNullValue()));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), startsWith("-----BEGIN PRIVATE KEY-----"));
            assertThat("credential owner is mapped correctly", credential.getOwner(), is("cookies"));
        }
    }

    @Test
    public void canConvertAValidSecretWithSpecifiedCredentialsId() throws Exception {
        GitHubAppCredentialsConvertor convertor = new GitHubAppCredentialsConvertor();

        try (InputStream is = get("validCredentialsId.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            GitHubAppCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("A_TEST_GITHUBAPP"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential appID is mapped correctly", credential.getAppID(), is("1234"));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), is(notNullValue()));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), startsWith("-----BEGIN PRIVATE KEY-----"));
        }
    }

    @Test
    public void canConvertAValidSecretWithNoDescription() throws Exception {
        GitHubAppCredentialsConvertor convertor = new GitHubAppCredentialsConvertor();

        try (InputStream is = get("valid-no-desc.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            GitHubAppCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-githubapp"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is(emptyString()));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential appID is mapped correctly", credential.getAppID(), is("1234"));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), is(notNullValue()));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), startsWith("-----BEGIN PRIVATE KEY-----"));
        }
    }

    @Test
    public void canConvertAValidScopedSecret() throws Exception {
        GitHubAppCredentialsConvertor convertor = new GitHubAppCredentialsConvertor();

        try (InputStream is = get("validScoped.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            GitHubAppCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-githubapp"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
            assertThat("credential appID is mapped correctly", credential.getAppID(), is("1234"));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), is(notNullValue()));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey().getPlainText(), startsWith("-----BEGIN PRIVATE KEY-----"));
        }
    }

    @Test
    public void failsToConvertWhenAppIdMissing() throws Exception {
        GitHubAppCredentialsConvertor convertor = new GitHubAppCredentialsConvertor();
        
        try (InputStream is = get("missingAppId.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the appID"));
        }
    }

    
    @Test
    public void failsToConvertWhenPrivateKeyMissing() throws Exception {
        GitHubAppCredentialsConvertor convertor = new GitHubAppCredentialsConvertor();

        try (InputStream is = get("missingPrivateKey.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the privateKey"));
        }
    }

    @Test
    public void failsToConvertWhenAppIdCorrupt() throws Exception {
        GitHubAppCredentialsConvertor convertor = new GitHubAppCredentialsConvertor();
        
        try (InputStream is = get("corruptAppId.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid appID"));
        }
    }


    @Test
    public void failsToConvertWhenPrivateKeyCorrupt() throws Exception {
        GitHubAppCredentialsConvertor convertor = new GitHubAppCredentialsConvertor();

        try (InputStream is = get("corruptPrivateKey.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid privateKey"));
        }
    }

    @Test
    public void failsToConvertWhenOwnerCorrupt() throws Exception {
        GitHubAppCredentialsConvertor convertor = new GitHubAppCredentialsConvertor();

        try (InputStream is = get("corruptOwner.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid owner"));
        }
    }

    @Test
    public void failsToConvertWhenDataEmpty() throws Exception {
        GitHubAppCredentialsConvertor convertor = new GitHubAppCredentialsConvertor();
        
        try (InputStream is = get("void.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("contains no data"));
        }
    }


    private static final InputStream get(String resource) {
        InputStream is = GitHubAppCredentialsConvertorTest.class.getResourceAsStream("GitHubAppCredentialsConvertorTest/" + resource);
        if (is == null) {
            fail("failed to load resource " + resource);
        }
        return is;
    }
}
