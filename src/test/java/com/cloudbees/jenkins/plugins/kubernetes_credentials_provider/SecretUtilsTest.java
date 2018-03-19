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
package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import org.junit.Test;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretUtils;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.text.StringContainsInOrder.stringContainsInOrder;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SecretUtilsTest {

    @Test
    public void base64DecodeToStringWithInvalidBase64() {
        // TODO check that error is logged
        assertThat(SecretUtils.base64DecodeToString("this_is_invalid_base64!"), nullValue());
    }

    @Test
    public void base64DecodeToStringWithInvalidUTF8() {
        // TODO check that the error is logged
        String invalidUTF8 = Base64.getEncoder().encodeToString(new byte[] { (byte)0xff, (byte)0xff, (byte)0xff});
        assertThat(SecretUtils.base64DecodeToString(invalidUTF8), nullValue());
    }

    @Test
    public void base64DecodeToStringWithValidInput() {
        assertThat(SecretUtils.base64DecodeToString("SGVsbG8sIFdvcmxk"), is("Hello, World"));
    }
    
    @Test
    public void base64DecodeWithInvalidInput() {
        // TODO check logging? 
        assertThat(SecretUtils.base64Decode("%"), nullValue());
    }

    @Test
    public void base64DecodeWithValidInput() {
        byte[] expected = "Hello".getBytes(StandardCharsets.UTF_8);
        assertThat(SecretUtils.base64Decode("SGVsbG8"), is(expected));
    }

    @Test
    public void getCredentialId() {
        final String testName = "a-test-name";
        Secret s = new SecretBuilder().withNewMetadata().withName(testName).endMetadata().build();
        assertThat(SecretUtils.getCredentialId(s), is(testName));
    }

    @Test
    public void getCredentialDescription() {
        final String testDescription = "a-test-description";
        Secret s = new SecretBuilder().withNewMetadata().
                withAnnotations(Collections.singletonMap("jenkins.io/credentials-description", testDescription)).endMetadata().
                build();
        assertThat(SecretUtils.getCredentialDescription(s), is(testDescription));
    }

    @Test
    public void getCredentialDescriptionWhenNotPresent() {
        Secret s = new SecretBuilder().withNewMetadata().endMetadata().build();
        assertThat(SecretUtils.getCredentialDescription(s), nullValue());
    }

    @Test
    public void requireNonNullOnNonNullValue() throws Exception {
        String it = "not null";
        assertThat(SecretUtils.requireNonNull(it, "whatever"), is(it));
    }

    @Test
    public void requireNonNullOnNullValue() {
        String message = "the message";
        try {
            SecretUtils.requireNonNull(null, message);
            fail("no exception thrown");
        } catch (CredentialsConvertionException cce) {
            assertThat(cce.getMessage(), is(message));
        } 
    }

    @Test
    public void requireNonNullOnNonNullValueWithMapping() throws Exception {
        String it = "not null";
        assertThat(SecretUtils.requireNonNull(it, "whatever", "custom-mapping"), is(it));
    }

    @Test
    public void requireNonNullOnNullValueWithMapping() {
        String message = "the message";
        String mapping = "someMapping";
        try {
            SecretUtils.requireNonNull(null, message, mapping);
            fail("no exception thrown");
        } catch (CredentialsConvertionException cce) {
            assertThat(cce.getMessage(), stringContainsInOrder(message, "mapped to", mapping));
        } 
    }

    @Test
    public void getKeyNameWithNoMapping() {
        String keyName = "theKey";
        Secret s = new SecretBuilder().withNewMetadata().endMetadata().build();
        assertThat(SecretUtils.getKeyName(s, keyName), is(keyName));
    }

    @Test
    public void getKeyNameWithMapping() {
        String keyName = "theKey";
        String mappedKeyName = "mappedKey";
        Secret s = new SecretBuilder().withNewMetadata().addToAnnotations("jenkins.io/credentials-keybinding-"+keyName, mappedKeyName).endMetadata().build();
        assertThat(SecretUtils.getKeyName(s, keyName), is(mappedKeyName));
    }

    @Test
    public void getKeyNameWithIncompleteMapping() {
        String keyName = "theKey";
        Secret s = new SecretBuilder().withNewMetadata().addToAnnotations("jenkins.io/credentials-keybinding-"+keyName, "").endMetadata().build();
        assertThat(SecretUtils.getKeyName(s, keyName), is(keyName));
    }

    @Test
    public void getNonNullSecretDataWithEntry() throws CredentialsConvertionException {
        String key = "a-key";
        String datum = "some-data";
        Secret s = new SecretBuilder().withNewMetadata().endMetadata().addToData(key, datum).build();
        assertThat(SecretUtils.getNonNullSecretData(s, key, "ignored"), is(datum));
    }

    @Test
    public void getNonNullSecretDataWithMappedEntry() throws CredentialsConvertionException {
        String key = "a-key";
        String map = "not-the-key";
        String datum = "some-data";
        Secret s = new SecretBuilder().withNewMetadata().addToAnnotations("jenkins.io/credentials-keybinding-"+key, map).endMetadata().addToData(map, datum).build();
        assertThat(SecretUtils.getNonNullSecretData(s, key, "ignored"), is(datum));
    }

    @Test
    public void getNonNullSecretDataWithNoData() {
        Secret s = new SecretBuilder().withNewMetadata().endMetadata().build();
        try {
            SecretUtils.getNonNullSecretData(s, "some-key", "oops");
            fail();
        } catch (CredentialsConvertionException cce) {
            assertThat(cce.getMessage(), is("oops"));
        } 
    }

    @Test
    public void getNonNullSecretDataWithNoMappedData() {
        String keyName = "bogus";
        String mappedName = "wibble";
        Secret s = new SecretBuilder().withNewMetadata().addToAnnotations("jenkins.io/credentials-keybinding-"+keyName, mappedName).endMetadata().build();
        try {
            SecretUtils.getNonNullSecretData(s, keyName, "oops");
            fail();
        } catch (CredentialsConvertionException cce) {
            assertThat(cce.getMessage(), stringContainsInOrder("oops", "mapped to", mappedName));
        } 
    }
}
