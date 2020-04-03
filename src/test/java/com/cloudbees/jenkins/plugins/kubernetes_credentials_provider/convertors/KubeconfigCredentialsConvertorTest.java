/*
 * The MIT License
 *
 * Copyright 2019 CloudBees, Inc., @hugomcfonseca
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
import com.microsoft.jenkins.kubernetes.credentials.KubeconfigCredentials;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests KubeconfigCredentialsConvertor
 */
public class KubeconfigCredentialsConvertorTest {

    String content = "Hello World!";

    @Test
    public void canConvert() throws Exception {
        KubeconfigCredentialsConvertor convertor = new KubeconfigCredentialsConvertor();
        assertThat("correct registration of valid type", convertor.canConvert("kubeconfig"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
    }

    @Test
    public void canConvertAValidSecret() throws Exception {
        KubeconfigCredentialsConvertor convertor = new KubeconfigCredentialsConvertor();

        try (InputStream is = get("valid.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            KubeconfigCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-kubeconfig"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("kubeconfig credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential content is mapped correctly", credential.getContent(), is(content));
        }
    }


    @Test
    public void canConvertAValidMappedSecret() throws Exception {
        KubeconfigCredentialsConvertor convertor = new KubeconfigCredentialsConvertor();

        try (InputStream is = get("validMapped.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            KubeconfigCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("another-test-kubeconfig"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("kubeconfig credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential content is mapped correctly", credential.getContent(), is(content));
        }
    }

    @Test
    public void canConvertAValidSecretWithNoDescription() throws Exception {
        KubeconfigCredentialsConvertor convertor = new KubeconfigCredentialsConvertor();

        try (InputStream is = get("valid-no-desc.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            KubeconfigCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-kubeconfig"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is(emptyString()));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential content is mapped correctly", credential.getContent(), is(content));
        }
    }



    @Test
    public void failsToConvertWhenDataKeyMissing() throws Exception {
        KubeconfigCredentialsConvertor convertor = new KubeconfigCredentialsConvertor();

        try (InputStream is = get("missingData.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("kubeconfig is empty"));
        }
    }




    // BASE64 Corrupt
    @Test
    public void failsToConvertWhenDataKeyCorrupt() throws Exception {
        KubeconfigCredentialsConvertor convertor = new KubeconfigCredentialsConvertor();

        try (InputStream is = get("corruptData.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("kubeconfig credential has an invalid text (must be base64 encoded UTF-8)"));
        }
    }





    @Test
    public void failsToConvertWhenDataEmpty() throws Exception {
        KubeconfigCredentialsConvertor convertor = new KubeconfigCredentialsConvertor();

        try (InputStream is = get("void.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("kubeconfig is empty"));
        }
    }


    private static final InputStream get(String resource) {
        InputStream is = KubeconfigCredentialsConvertorTest.class.getResourceAsStream("KubeconfigCredentialsConvertorTest/" + resource);
        if (is == null) {
            fail("failed to load resource " + resource);
        }
        return is;
    }
}
