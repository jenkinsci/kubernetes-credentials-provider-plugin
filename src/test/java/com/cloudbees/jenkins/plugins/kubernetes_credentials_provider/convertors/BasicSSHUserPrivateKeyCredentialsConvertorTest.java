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

import java.io.InputStream;

import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.Issue;
import org.mockito.*;
import org.mockito.junit.MockitoJUnitRunner;
import hudson.util.HistoricalSecrets;
import jenkins.security.ConfidentialStore;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests BasicSSHUserPrivateKeyCredentialsConvertor
 */
@RunWith(MockitoJUnitRunner.class)
public class BasicSSHUserPrivateKeyCredentialsConvertorTest {

    private String testkey = String.join("\n"
        , "-----BEGIN RSA PRIVATE KEY-----"
        , "MIIEowIBAAKCAQEAngWMYnda9vD2utvbAdgCOLVNanA/MW50er5ROW21it/eph1u"
        , "6RCuZ0CiuYUE5Eb8kOOQP7MTL3Ixyv9GW6hmMZwjyvcCamKj7cYuEHBYkn0X2Jgw"
        , "syPGUWZwITgSxgb/VfjRKbAtUdvXNFjHxknUlaVd+G6gQpN5Lv3//O/EglmVqf1d"
        , "CM2xAy9Ixk9roMSmBpgwC7lCsi1W9IGdLrjLAC96BrJkHX1EDQDdB8tWg8qLjZfr"
        , "L1ioddG/NDH8lOUetWX9SB5WF4xi/oBRNvSCwmBAa8v2DvhS/TEwcWAsReclRCNW"
        , "5eGAqhbb0Kl8E0hYJdFlEKYjQH3y5cZtqMAiuwIDAQABAoIBAGQK2TThoYpjRaFJ"
        , "XZ8ONWHXjpqLU8akykOHR/8WsO+qCdibG8OcFv4xkpPnXhBzzKSiHYnmgofwQQvm"
        , "j5GpzIEt/A8cUMAvkN8RL8qihcDAR5+Nwo83X+/a7bRqPqB2f6LbMvi0nAyOJPH0"
        , "Hw4vYdIX7qVAzF855GfW0QE+fueSdtgWviJM8gZHdhCqe/zqYm016zNaavap530r"
        , "tJ/+vhUW8WYqJqBW8+58laW5vTBusNsVjeL40yJF8X/XQQcdZ4XmthNcegx79oim"
        , "j9ELzX0ttchiwAe/trLxTkdWb4rEFz+U50iAOMUdS8T0brb5bxhqNM/ByiqQ28W9"
        , "2NJCVEkCgYEA0phCE9iKVWNZnvWX6+fHgr2NO2ShPexPeRfFxr0ugXGTQvyT0HnM"
        , "/Q//V+LduPMX8b2AsOzI0rQh+4bjohOZvKmGKiuPv3eSvqpi/r6208ZVTBjjFvBO"
        , "UQhMbPUyR6vO1ryFDwBMwMqQ06ldkXArhB+SG0dYnOKb/6g0nO2BVFUCgYEAwBeH"
        , "HGNGuxwum63UAaqyX6lRSpGGm6XSCBhzvHUPnVphgq7nnZOGl0z3U49jreCvuuEc"
        , "fA9YqxJjzoZy5870KOXY2kltlq/U/4Lrb0k75ag6ZVbi0oemACN6KCHtE+Zm2dac"
        , "rW8oKWpRTbsvMOYUvSjF0u8BCrestpRUF977Ks8CgYEAicbLFCjK9+ozq+eJKPFO"
        , "eZ6BU6YWR2je5Z5D6i3CyzT+3whXvECzd6yLpXfrDyEbPTB5jUacbB0lTmWFb3fb"
        , "UK6n89bkCKO2Ab9/XKJxAkPzcgGmME+vLRx8w5v29STWAW78rj/H9ymPbqqTaJ82"
        , "GQ5+jBI1Sw6GeNAW+8P2pLECgYAs/dXBimcosCMih4ZelZKN4WSO6KL0ldQp3UBO"
        , "ZcSwgFjSeRD60XD2wyoywiUAtt2yEcPQMu/7saT63HbRYKHDaoJuLkCiyLBE4G8w"
        , "c6C527tBvSYHVYpGAgk8mSWkQZTZdPDhlmV7vdEpOayF8X3uCDy9eQlvbzHe2cMQ"
        , "jEOb9QKBgG3jSxGfqN/sD8W9BhpVrybCXh2RvhxOBJAFx58wSWTkRcYSwpdyvm7x"
        , "wlMtcEdQgaSBeuBU3HPUdYE07bQNAlYO0p9MQnsLHzd2V9yiCX1Sq5iB6dQpHxyi"
        , "sDZLY2Mym1nUJWfE47GAcxFZtrVh9ojKcmgiHo8qPTkWjFGY7xe/"
        , "-----END RSA PRIVATE KEY-----"
        , ""
    );


    private @Mock ConfidentialStore confidentialStore;
    private @Mock MockedStatic<ConfidentialStore> confidentialStoreMockedStatic;

    // return null rather than go looking up Jenkins.getInstance....
    private @Mock MockedStatic<HistoricalSecrets> historicalSecretsMockedStatic;

    @Before
    public void mockConfidentialStore() {
        confidentialStoreMockedStatic.when(ConfidentialStore::get).thenReturn(confidentialStore);
    }

    @Test
    public void canConvert() throws Exception {
        BasicSSHUserPrivateKeyCredentialsConvertor convertor = new BasicSSHUserPrivateKeyCredentialsConvertor();
        assertThat("correct registration of valid type", convertor.canConvert("basicSSHUserPrivateKey"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
    }

    @Test
    public void canConvertAValidSecretWithoutPassphrase() throws Exception {
        BasicSSHUserPrivateKeyCredentialsConvertor convertor = new BasicSSHUserPrivateKeyCredentialsConvertor();

        try (InputStream is = get("valid.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            BasicSSHUserPrivateKey credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("jenkins-key"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("basic user private key credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential username is mapped correctly", credential.getUsername(), is("jenkins"));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey(), is(this.testkey));
            assertThat("credential passphrase is mapped correctly", credential.getPassphrase(), nullValue());
        }
    }

    @Test
    public void canConvertAValidSecretWithPassphrase() throws Exception {
        BasicSSHUserPrivateKeyCredentialsConvertor convertor = new BasicSSHUserPrivateKeyCredentialsConvertor();

        try (InputStream is = get("validPassphrase.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            BasicSSHUserPrivateKey credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("jenkins-key"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("basic user private key credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential username is mapped correctly", credential.getUsername(), is("jenkins"));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey(), is(this.testkey));
            assertThat("credential passphrase is mapped correctly", credential.getPassphrase(), is(hudson.util.Secret.fromString("mypassphrase")));
        }
    }

    @Test
    public void canConvertAValidMappedSecretWithoutPassphrase() throws Exception {
        BasicSSHUserPrivateKeyCredentialsConvertor convertor = new BasicSSHUserPrivateKeyCredentialsConvertor();

        try (InputStream is = get("validMapped.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            BasicSSHUserPrivateKey credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("jenkins-key"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("basic user private key credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential username is mapped correctly", credential.getUsername(), is("jenkins"));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey(), is(this.testkey));
            assertThat("credential passphrase is mapped correctly", credential.getPassphrase(), nullValue());
        }
    }

    @Test
    public void canConvertAValidSecretWithSpecifiedCredentialsId() throws Exception {
        BasicSSHUserPrivateKeyCredentialsConvertor convertor = new BasicSSHUserPrivateKeyCredentialsConvertor();

        try (InputStream is = get("validCredentialsId.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            BasicSSHUserPrivateKey credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("JENKINS_KEY"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("basic user private key credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential username is mapped correctly", credential.getUsername(), is("jenkins"));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey(), is(this.testkey));
            assertThat("credential passphrase is mapped correctly", credential.getPassphrase(), nullValue());
        }
    }

    @Test
    public void canConvertAValidMappedSecretWithPassphrase() throws Exception {
        BasicSSHUserPrivateKeyCredentialsConvertor convertor = new BasicSSHUserPrivateKeyCredentialsConvertor();

        try (InputStream is = get("validPassphraseMapped.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            BasicSSHUserPrivateKey credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("jenkins-key"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("basic user private key credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential username is mapped correctly", credential.getUsername(), is("jenkins"));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey(), is(this.testkey));
            assertThat("credential passphrase is mapped correctly", credential.getPassphrase(), is(hudson.util.Secret.fromString("mypassphrase")));
        }
    }

    @Issue("JENKINS-53105")
    @Test
    public void canConvertAValidScopedSecret() throws Exception {
        BasicSSHUserPrivateKeyCredentialsConvertor convertor = new BasicSSHUserPrivateKeyCredentialsConvertor();

        try (InputStream is = get("validScoped.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            BasicSSHUserPrivateKey credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("jenkins-key"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("basic user private key credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
            assertThat("credential username is mapped correctly", credential.getUsername(), is("jenkins"));
            assertThat("credential privateKey is mapped correctly", credential.getPrivateKey(), is(this.testkey));
            assertThat("credential passphrase is mapped correctly", credential.getPassphrase(), nullValue());
        }
    }

    @Test
    public void failsToConvertWhenUsernameMissing() throws Exception {
        BasicSSHUserPrivateKeyCredentialsConvertor convertor = new BasicSSHUserPrivateKeyCredentialsConvertor();

        try (InputStream is = get("missingUsername.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the username"));
        }
    }

    @Test
    public void failsToConvertWhenPrivateKeyMissing() throws Exception {
        BasicSSHUserPrivateKeyCredentialsConvertor convertor = new BasicSSHUserPrivateKeyCredentialsConvertor();

        try (InputStream is = get("missingPrivateKey.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the privateKey"));
        }
    }

    @Test
    public void failsToConvertWhenUsernameCorrupt() throws Exception {
        BasicSSHUserPrivateKeyCredentialsConvertor convertor = new BasicSSHUserPrivateKeyCredentialsConvertor();

        try (InputStream is = get("corruptUsername.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid username"));
        }
    }

    @Test
    public void failsToConvertWhenPrivateKeyCorrupt() throws Exception {
        BasicSSHUserPrivateKeyCredentialsConvertor convertor = new BasicSSHUserPrivateKeyCredentialsConvertor();

        try (InputStream is = get("corruptPrivateKey.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid privateKey"));
        }
    }

    @Test
    public void failsToConvertWhenPassphraseCorrupt() throws Exception {
        BasicSSHUserPrivateKeyCredentialsConvertor convertor = new BasicSSHUserPrivateKeyCredentialsConvertor();

        try (InputStream is = get("corruptPassphrase.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid passphrase"));
        }
    }

    @Test
    public void failsToConvertWhenDataEmpty() throws Exception {
        BasicSSHUserPrivateKeyCredentialsConvertor convertor = new BasicSSHUserPrivateKeyCredentialsConvertor();
 
        try (InputStream is = get("void.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("contains no data"));
        }
    }

    private static final InputStream get(String resource) {
        InputStream is = BasicSSHUserPrivateKeyCredentialsConvertorTest.class.getResourceAsStream("BasicSSHUserPrivateKeyCredentialsConvertorTest/" + resource);
        if (is == null) {
            fail("failed to load resource " + resource);
        }
        return is;
    }
}
