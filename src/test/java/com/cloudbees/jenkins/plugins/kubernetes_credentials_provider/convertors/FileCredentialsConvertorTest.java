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
import java.nio.charset.StandardCharsets;

import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.jenkinsci.plugins.plaincredentials.impl.FileCredentialsImpl;
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
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests FileCredentialsConvertor
 */
@RunWith(MockitoJUnitRunner.class)
public class FileCredentialsConvertorTest {

    private @Mock ConfidentialStore confidentialStore;
    private @Mock MockedStatic<ConfidentialStore> confidentialStoreMockedStatic;

    // return null rather than go looking up Jenkins.getInstance....
    private @Mock MockedStatic<HistoricalSecrets> historicalSecretsMockedStatic;

    @Before
    public void mockConfidentialStore() {
        confidentialStoreMockedStatic.when(ConfidentialStore::get).thenReturn(confidentialStore);
        Mockito.when(confidentialStore.randomBytes(ArgumentMatchers.anyInt())).thenAnswer( it -> new byte[ (Integer)(it.getArguments()[0])] );
    }

    @Test
    public void canConvert() throws Exception {
        FileCredentialsConvertor convertor = new FileCredentialsConvertor();
        assertThat("correct registration of valid type", convertor.canConvert("secretFile"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
    }

    @Test
    public void canConvertAValidSecret() throws Exception {
        FileCredentialsConvertor convertor = new FileCredentialsConvertor();

        try (InputStream is = get("valid.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            FileCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("another-test-file"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("secret file credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential username is mapped correctly", credential.getFileName(), is("mySecret.txt"));
            assertThat("credential password is mapped correctly", credential.getSecretBytes().getPlainData(), is("Hello World!".getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    public void canConvertAValidMappedSecret() throws Exception {
        FileCredentialsConvertor convertor = new FileCredentialsConvertor();

        try (InputStream is = get("validMapped.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            FileCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("another-test-file"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("secret file credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential username is mapped correctly", credential.getFileName(), is("mySecret.txt"));
            assertThat("credential password is mapped correctly", credential.getSecretBytes().getPlainData(), is("Hello World!".getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    public void canConvertAValidSecretWithSpecifiedCredentialsId() throws Exception {
        FileCredentialsConvertor convertor = new FileCredentialsConvertor();

        try (InputStream is = get("validCredentialsId.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            FileCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("ANOTHER_TEST_FILE"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("secret file credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential username is mapped correctly", credential.getFileName(), is("mySecret.txt"));
            assertThat("credential password is mapped correctly", credential.getSecretBytes().getPlainData(), is("Hello World!".getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Issue("JENKINS-53105")
    @Test
    public void canConvertAValidScopedSecret() throws Exception {
        FileCredentialsConvertor convertor = new FileCredentialsConvertor();

        try (InputStream is = get("validScoped.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            FileCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("another-test-file"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("secret file credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
            assertThat("credential username is mapped correctly", credential.getFileName(), is("mySecret.txt"));
            assertThat("credential password is mapped correctly", credential.getSecretBytes().getPlainData(), is("Hello World!".getBytes(StandardCharsets.UTF_8)));
        }
    }

    @Test
    public void failsToConvertWhenFilenameMissing() throws Exception {
        FileCredentialsConvertor convertor = new FileCredentialsConvertor();
        
        try (InputStream is = get("missingFilename.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the filename"));
        }
    }

    
    @Test
    public void failsToConvertWhenDataMissing() throws Exception {
        FileCredentialsConvertor convertor = new FileCredentialsConvertor();

        try (InputStream is = get("missingData.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the data"));
        }
    }

    @Test
    public void failsToConvertWhenFilenameCorrupt() throws Exception {
        FileCredentialsConvertor convertor = new FileCredentialsConvertor();
        
        try (InputStream is = get("corruptFilename.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid filename"));
        }
    }

    
    @Test
    public void failsToConvertWhenDataCorrupt() throws Exception {
        FileCredentialsConvertor convertor = new FileCredentialsConvertor();

        try (InputStream is = get("corruptData.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid data"));
        }
    }

    @Test
    public void failsToConvertWhenDataEmpty() throws Exception {
        FileCredentialsConvertor convertor = new FileCredentialsConvertor();
        
        try (InputStream is = get("void.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("contains no data"));
        }
    }

    private static final InputStream get(String resource) {
        InputStream is = FileCredentialsConvertorTest.class.getResourceAsStream("FileCredentialsConvertorTest/" + resource);
        if (is == null) {
            fail("failed to load resource " + resource);
        }
        return is;
    }
}