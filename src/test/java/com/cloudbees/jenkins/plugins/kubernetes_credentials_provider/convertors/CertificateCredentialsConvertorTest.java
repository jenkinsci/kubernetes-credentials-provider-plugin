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
import java.security.KeyStore;
import java.security.cert.X509Certificate;

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
import com.cloudbees.plugins.credentials.impl.CertificateCredentialsImpl;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests CertificateCredentialsConvertor
 */
@RunWith(MockitoJUnitRunner.class)
public class CertificateCredentialsConvertorTest {

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
        CertificateCredentialsConvertor convertor = new CertificateCredentialsConvertor();
        assertThat("correct registration of valid type", convertor.canConvert("certificate"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
    }

    @Test
    public void canConvertAValidSecret() throws Exception {
        CertificateCredentialsConvertor convertor = new CertificateCredentialsConvertor();

        try (InputStream is = get("valid.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            CertificateCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-certificate"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("certificate credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("testPassword"));
            KeyStore ks = credential.getKeyStore();
            // credential.getKeyStore never returns null so we need to check the Keystore contains our certificate
            assertThat("credential certificate mapped correctly ", ks.containsAlias("myKey"), is(true));
            // TODO what can we check here to see this is valid
            X509Certificate cert  = (X509Certificate) ks.getCertificate("myKey");
            assertThat("Correct cert", cert.getSubjectDN().getName(), is("CN=A Test, OU=Dev, O=CloudBees, L=Around The World, ST=Cool, C=earth"));
        }
    }

    @Test
    public void canConvertAValidSecretWithMapping() throws Exception {
        CertificateCredentialsConvertor convertor = new CertificateCredentialsConvertor();

        try (InputStream is = get("validMapped.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            CertificateCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-certificate"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("certificate credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("testPassword"));
            KeyStore ks = credential.getKeyStore();
            // credential.getKeyStore never returns null so we need to check the Keystore contains our certificate
            assertThat("credential certificate mapped correctly ", ks.containsAlias("myKey"), is(true));
            // TODO what can we check here to see this is valid
            X509Certificate cert  = (X509Certificate) ks.getCertificate("myKey");
            assertThat("Correct cert", cert.getSubjectDN().getName(), is("CN=A Test, OU=Dev, O=CloudBees, L=Around The World, ST=Cool, C=earth"));
        }
    }

    @Test
    public void canConvertAValidSecretWithSpecifiedCredentialsId() throws Exception {
        CertificateCredentialsConvertor convertor = new CertificateCredentialsConvertor();

        try (InputStream is = get("validCredentialsId.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            CertificateCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("A_TEST_CERTIFICATE"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("certificate credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("testPassword"));
            KeyStore ks = credential.getKeyStore();
            // credential.getKeyStore never returns null so we need to check the Keystore contains our certificate
            assertThat("credential certificate mapped correctly ", ks.containsAlias("myKey"), is(true));
            // TODO what can we check here to see this is valid
            X509Certificate cert  = (X509Certificate) ks.getCertificate("myKey");
            assertThat("Correct cert", cert.getSubjectDN().getName(), is("CN=A Test, OU=Dev, O=CloudBees, L=Around The World, ST=Cool, C=earth"));
        }
    }

    @Issue("JENKINS-53105")
    @Test
    public void canConvertAValidScopedSecret() throws Exception {
        CertificateCredentialsConvertor convertor = new CertificateCredentialsConvertor();

        try (InputStream is = get("validScoped.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            CertificateCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-certificate"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("certificate credential from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
            assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("testPassword"));
            KeyStore ks = credential.getKeyStore();
            // credential.getKeyStore never returns null so we need to check the Keystore contains our certificate
            assertThat("credential certificate mapped correctly ", ks.containsAlias("myKey"), is(true));
            // TODO what can we check here to see this is valid
            X509Certificate cert  = (X509Certificate) ks.getCertificate("myKey");
            assertThat("Correct cert", cert.getSubjectDN().getName(), is("CN=A Test, OU=Dev, O=CloudBees, L=Around The World, ST=Cool, C=earth"));
        }
    }

    @Test
    public void failsToConvertWhenCertificateMissing() throws Exception {
        CertificateCredentialsConvertor convertor = new CertificateCredentialsConvertor();
        
        try (InputStream is = get("missingCertificate.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the certificate"));
        }
    }

    
    @Test
    public void failsToConvertWhenPasswordMissing() throws Exception {
        CertificateCredentialsConvertor convertor = new CertificateCredentialsConvertor();

        try (InputStream is = get("missingPassword.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the password"));
        }
    }

    /**
     * Tests when the certificate is not a valid certificate
     */
    @Test
    public void failsToConvertWhenCertificateCorruptPKCS12() throws Exception {
        CertificateCredentialsConvertor convertor = new CertificateCredentialsConvertor();
        
        try (InputStream is = get("corruptCertificatePKCS12.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), allOf(containsString("invalid certificate"), containsString("PKCS#12")));
        }
    }


    /**
     * Tests when the certificate is not valid base64
     */
    @Test
    public void failsToConvertWhenCertificateCorruptBase64() throws Exception {
        CertificateCredentialsConvertor convertor = new CertificateCredentialsConvertor();
        
        try (InputStream is = get("corruptBase64Certificate.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), allOf(containsString("invalid certificate"), containsString("base64")));
        }
    }

    @Test
    public void failsToConvertWhenPasswordCorrupt() throws Exception {
        CertificateCredentialsConvertor convertor = new CertificateCredentialsConvertor();

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
        CertificateCredentialsConvertor convertor = new CertificateCredentialsConvertor();
        
        try (InputStream is = get("void.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("contains no data"));
        }
    }

    private static final InputStream get(String resource) {
        InputStream is = CertificateCredentialsConvertorTest.class.getResourceAsStream("CertificateCredentialsConvertorTest/" + resource);
        if (is == null) {
            fail("failed to load resource " + resource);
        }
        return is;
    }
}