/*
 * The MIT License
 *
 * Copyright 2019 CloudBees, Inc., @hugomcfonseca
 * Copyright 2020 @luka5
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
import com.microsoft.azure.util.AzureCredentials;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.junit.Test;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests AzureCredentialsConvertor
 */
public class AzureCredentialsConvertorTest {

    String subscriptionId = "test-subscription-id";
    String clientId= "test-client-id";
    String clientSecret = "u.&%(Z>yEZ8(}:uoy";

    @Test
    public void canConvert() throws Exception {
        AzureCredentialsConvertor convertor = new AzureCredentialsConvertor();
        assertThat("correct registration of valid type", convertor.canConvert("azure"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
    }

    @Test
    public void canConvertAValidSecret() throws Exception {
        AzureCredentialsConvertor convertor = new AzureCredentialsConvertor();

        try (InputStream is = get("valid.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AzureCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-azure"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential subscriptionId is mapped correctly", credential.getSubscriptionId(), is(subscriptionId));
            assertThat("credential clientId is mapped correctly", credential.getClientId(), is(clientId));
            assertThat("credential clientSecret is mapped correctly", credential.getClientSecret(), is(clientSecret));
        }
    }

    @Test
    public void canConvertAValidMappedSecret() throws Exception {
        AzureCredentialsConvertor convertor = new AzureCredentialsConvertor();

        try (InputStream is = get("validMapped.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AzureCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-azure"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential subscriptionId is mapped correctly", credential.getSubscriptionId(), is(subscriptionId));
            assertThat("credential clientId is mapped correctly", credential.getClientId(), is(clientId));
            assertThat("credential clientSecret is mapped correctly", credential.getClientSecret(), is(clientSecret));
        }
    }

    @Test
    public void canConvertAValidSecretWithNoDescription() throws Exception {
        AzureCredentialsConvertor convertor = new AzureCredentialsConvertor();

        try (InputStream is = get("valid-no-desc.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AzureCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-azure"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is(emptyString()));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential subscriptionId is mapped correctly", credential.getSubscriptionId(), is(subscriptionId));
            assertThat("credential clientId is mapped correctly", credential.getClientId(), is(clientId));
            assertThat("credential clientSecret is mapped correctly", credential.getClientSecret(), is(clientSecret));
        }
    }

    @Test
    public void failsToConvertWhenSubscriptionIdMissing() throws Exception {
        AzureCredentialsConvertor convertor = new AzureCredentialsConvertor();

        try (InputStream is = get("missingSubscriptionId.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the subscriptionId"));
        }
    }

    @Test
    public void failsToConvertWhenClientIdMissing() throws Exception {
        AzureCredentialsConvertor convertor = new AzureCredentialsConvertor();

        try (InputStream is = get("missingClientId.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the clientId"));
        }
    }

    @Test
    public void failsToConvertWhenClientSecretMissing() throws Exception {
        AzureCredentialsConvertor convertor = new AzureCredentialsConvertor();

        try (InputStream is = get("missingClientSecret.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the clientSecret"));
        }
    }

    // BASE64 Corrupt
    @Test
    public void failsToConvertWhenSubscriptionIdCorrupt() throws Exception {
        AzureCredentialsConvertor convertor = new AzureCredentialsConvertor();

        try (InputStream is = get("corruptSubscriptionId.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid subscriptionId"));
        }
    }

    @Test
    public void failsToConvertWhenClientIdCorrupt() throws Exception {
        AzureCredentialsConvertor convertor = new AzureCredentialsConvertor();

        try (InputStream is = get("corruptClientId.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid clientId"));
        }
    }

    @Test
    public void failsToConvertWhenClientSecretCorrupt() throws Exception {
        AzureCredentialsConvertor convertor = new AzureCredentialsConvertor();

        try (InputStream is = get("corruptClientSecret.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid clientSecret"));
        }
    }


    @Test
    public void failsToConvertWhenDataEmpty() throws Exception {
        AzureCredentialsConvertor convertor = new AzureCredentialsConvertor();

        try (InputStream is = get("void.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("contains no data"));
        }
    }


    private static final InputStream get(String resource) {
        InputStream is = AzureCredentialsConvertorTest.class.getResourceAsStream("AzureCredentialsConvertorTest/" + resource);
        if (is == null) {
            fail("failed to load resource " + resource);
        }
        return is;
    }
}
