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
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests UsernamePasswordCredentialConvertor
 */
public class UsernamePasswordCredentialsConvertorTest {


    @Test
    public void canConvert() throws Exception {
        UsernamePasswordCredentialsConvertor convertor = new UsernamePasswordCredentialsConvertor();
        assertThat("correct registration of valid type", convertor.canConvert("usernamePassword"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
    }

    @Test
    public void canConvertAValidSecret() throws Exception {
        UsernamePasswordCredentialsConvertor convertor = new UsernamePasswordCredentialsConvertor();

        try (InputStream is = get("valid.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            UsernamePasswordCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-usernamepass"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential username is mapped correctly", credential.getUsername(), is("myUsername"));
            assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("Pa$$word"));
        }
    }

    @Test
    public void canConvertAValidMappedSecret() throws Exception {
        UsernamePasswordCredentialsConvertor convertor = new UsernamePasswordCredentialsConvertor();

        try (InputStream is = get("valid.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            UsernamePasswordCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-usernamepass"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential username is mapped correctly", credential.getUsername(), is("myUsername"));
            assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("Pa$$word"));
        }
    }

    @Test
    public void canConvertAValidSecretWithSpecifiedCredentialsId() throws Exception {
        UsernamePasswordCredentialsConvertor convertor = new UsernamePasswordCredentialsConvertor();

        try (InputStream is = get("validCredentialsId.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            UsernamePasswordCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("A_TEST_USERNAMEPASS"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential username is mapped correctly", credential.getUsername(), is("myUsername"));
            assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("Pa$$word"));
        }
    }

    @Issue("JENKINS-54313")
    @Test
    public void canConvertAValidSecretWithNoDescription() throws Exception {
        UsernamePasswordCredentialsConvertor convertor = new UsernamePasswordCredentialsConvertor();

        try (InputStream is = get("valid-no-desc.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            UsernamePasswordCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-usernamepass"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is(emptyString()));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential username is mapped correctly", credential.getUsername(), is("myUsername"));
            assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("Pa$$word"));
        }
    }

    @Issue("JENKINS-53105")
    @Test
    public void canConvertAValidScopedSecret() throws Exception {
        UsernamePasswordCredentialsConvertor convertor = new UsernamePasswordCredentialsConvertor();

        try (InputStream is = get("validScoped.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            UsernamePasswordCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-usernamepass"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
            assertThat("credential username is mapped correctly", credential.getUsername(), is("myUsername"));
            assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("Pa$$word"));
        }
    }

    @Test
    public void failsToConvertWhenUsernameMissing() throws Exception {
        UsernamePasswordCredentialsConvertor convertor = new UsernamePasswordCredentialsConvertor();
        
        try (InputStream is = get("missingUsername.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the username"));
        }
    }

    
    @Test
    public void failsToConvertWhenPasswordMissing() throws Exception {
        UsernamePasswordCredentialsConvertor convertor = new UsernamePasswordCredentialsConvertor();

        try (InputStream is = get("missingPassword.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the password"));
        }
    }

    @Test
    public void failsToConvertWhenUsernameCorrupt() throws Exception {
        UsernamePasswordCredentialsConvertor convertor = new UsernamePasswordCredentialsConvertor();
        
        try (InputStream is = get("corruptUsername.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid username"));
        }
    }


    @Test
    public void failsToConvertWhenPasswordCorrupt() throws Exception {
        UsernamePasswordCredentialsConvertor convertor = new UsernamePasswordCredentialsConvertor();

        try (InputStream is = get("corruptPassword.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid password"));
        }
    }


    @Test
    public void failsToConvertWhenDataEmpty() throws Exception {
        UsernamePasswordCredentialsConvertor convertor = new UsernamePasswordCredentialsConvertor();
        
        try (InputStream is = get("void.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("contains no data"));
        }
    }


    private static final InputStream get(String resource) {
        InputStream is = UsernamePasswordCredentialsConvertorTest.class.getResourceAsStream("UsernamePasswordCredentialsConvertorTest/" + resource);
        if (is == null) {
            fail("failed to load resource " + resource);
        }
        return is;
    }
}