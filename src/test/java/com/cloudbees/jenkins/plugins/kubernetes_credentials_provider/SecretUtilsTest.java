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
import java.util.Map;
import java.util.Optional;

import com.cloudbees.plugins.credentials.CredentialsScope;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import org.junit.Test;

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
    public void getCredentialScopeWithValidScopeLabel() throws CredentialsConvertionException {
        Map<String, String> scopeLabel = Collections.singletonMap(SecretUtils.JENKINS_IO_CREDENTIALS_SCOPE_LABEL, "system");
        Secret s = new SecretBuilder().withNewMetadata().withLabels(scopeLabel).endMetadata().build();
        assertThat(SecretUtils.getCredentialScope(s), is(CredentialsScope.SYSTEM));
    }

    @Test(expected = CredentialsConvertionException.class)
    public void getCredentialScopeWithInvalidScopeThrowsAnException() throws CredentialsConvertionException {
        Map<String, String> invalidScope = Collections.singletonMap(SecretUtils.JENKINS_IO_CREDENTIALS_SCOPE_LABEL, "barf");
        Secret secret = new SecretBuilder().withNewMetadata().withLabels(invalidScope).endMetadata().build();
        SecretUtils.getCredentialScope(secret);
    }

    @Test
    public void getCredentialId() {
        final String testName = "a-test-name";
        Secret s = new SecretBuilder().withNewMetadata().withName(testName).endMetadata().build();
        assertThat(SecretUtils.getCredentialId(s), is(testName));
    }

    @Test
    public void getCredentialIdWithNameOverride() {
        final String secretName = "a-test-name";
        final String credentialName = "A_TEST_NAME";
        Secret s = new SecretBuilder()
                .withNewMetadata()
                    .withName(secretName)
                    .withLabels(Collections.singletonMap(SecretUtils.JENKINS_IO_CREDENTIALS_ID_LABEL, credentialName))
                .endMetadata().build();
        assertThat(SecretUtils.getCredentialId(s), is(credentialName));
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

    @Test
    public void getOptionalSecretDataWithEntry() throws CredentialsConvertionException {
        String key = "a-key";
        String datum = "some-data";
        Optional<String> optdatum = Optional.of(datum);
        Secret s = new SecretBuilder().withNewMetadata().endMetadata().addToData(key, datum).build();
        assertThat(SecretUtils.getOptionalSecretData(s, key, "ignored"), is(optdatum));
    }

    @Test
    public void getOptionalSecretDataWithMappedEntry() throws CredentialsConvertionException {
        String key = "a-key";
        String map = "not-the-key";
        String datum = "some-data";
        Optional<String> optdatum = Optional.of(datum);
        Secret s = new SecretBuilder().withNewMetadata().addToAnnotations("jenkins.io/credentials-keybinding-"+key, map).endMetadata().addToData(map, datum).build();
        assertThat(SecretUtils.getOptionalSecretData(s, key, "ignored"), is(optdatum));
    }

    @Test
    public void getOptionalSecretDataWithMissingKey() throws CredentialsConvertionException {
        String keyexists = "a-key-that-exists";
        String keydoesnotexist = "a-key-that-does-not-exist";
        String datum = "some-data";
        Optional<String> emptyOpt = Optional.empty();
        Secret s = new SecretBuilder().withNewMetadata().endMetadata().addToData(keyexists, datum).build();
        assertThat(SecretUtils.getOptionalSecretData(s, keydoesnotexist, "ignored"), is(emptyOpt));
    }

    @Test
    public void getOptionalSecretDataWithAnnotations() throws CredentialsConvertionException {
        String key = "a-key";
        String map = "not-the-key";
        String datum = "some-data";
        Optional<String> optdatum = Optional.empty();
        Secret s = new SecretBuilder().withNewMetadata().endMetadata().addToData(map, datum).build();
        s.getMetadata().setAnnotations(null);
        assertThat(SecretUtils.getOptionalSecretData(s, key, "no_error"), is(optdatum));
    }
}
