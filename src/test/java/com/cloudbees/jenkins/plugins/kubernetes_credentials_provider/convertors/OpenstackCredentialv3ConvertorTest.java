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

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.plugins.credentials.CredentialsScope;

import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.util.HistoricalSecrets;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.emptyString;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;

import jenkins.plugins.openstack.compute.auth.OpenstackCredentialv3;
import jenkins.security.ConfidentialStore;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.jvnet.hudson.test.Issue;
import org.mockito.*;

import org.mockito.junit.MockitoJUnitRunner;


/**
 * Tests OpenstackCredentialv3Convertor
 */
@RunWith(MockitoJUnitRunner.class)
public class OpenstackCredentialv3ConvertorTest {

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
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();
        assertThat("correct registration of valid type", convertor.canConvert("openstackCredentialv3"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
    }

    @Test
    public void canConvertAValidSecret() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("valid.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            OpenstackCredentialv3 credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("test-openstack-credential-v3"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("openstack credentials for you Jenkins in Kubernetes!"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential userName is mapped correctly", credential.getUserName(), is("casualName"));
            assertThat("credential userDomain is mapped correctly", credential.getUserDomain(), is("meaningfulDomain"));
            assertThat("credential projectName is mapped correctly", credential.getProjectName(), is("simpleProject"));
            assertThat("credential projectDomain is mapped correctly", credential.getProjectDomain(), is("everSimplerDomain"));
            assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("s3cr3tPass"));
        }
    }

    @Test
    public void canConvertAValidMappedSecret() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("validMapped.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            OpenstackCredentialv3 credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("test-openstack-credential-v3"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("openstack credentials for you Jenkins in Kubernetes!"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential userName is mapped correctly", credential.getUserName(), is("casualName"));
            assertThat("credential userDomain is mapped correctly", credential.getUserDomain(), is("meaningfulDomain"));
            assertThat("credential projectName is mapped correctly", credential.getProjectName(), is("simpleProject"));
            assertThat("credential projectDomain is mapped correctly", credential.getProjectDomain(), is("everSimplerDomain"));
            assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("s3cr3tPass"));
        }
    }

    @Test
    public void canConvertAValidSecretWithSpecifiedCredentialsId() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("validCredentialsId.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            OpenstackCredentialv3 credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("TEST_OPENSTACK_CREDENTIAL_V3"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("openstack credentials for you Jenkins in Kubernetes!"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential userName is mapped correctly", credential.getUserName(), is("casualName"));
            assertThat("credential userDomain is mapped correctly", credential.getUserDomain(), is("meaningfulDomain"));
            assertThat("credential projectName is mapped correctly", credential.getProjectName(), is("simpleProject"));
            assertThat("credential projectDomain is mapped correctly", credential.getProjectDomain(), is("everSimplerDomain"));
            assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("s3cr3tPass"));
        }
    }

    @Test
    public void canConvertAValidSecretWithNoDescription() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("valid-no-desc.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            OpenstackCredentialv3 credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("test-openstack-credential-v3"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is(emptyString()));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential userName is mapped correctly", credential.getUserName(), is("casualName"));
            assertThat("credential userDomain is mapped correctly", credential.getUserDomain(), is("meaningfulDomain"));
            assertThat("credential projectName is mapped correctly", credential.getProjectName(), is("simpleProject"));
            assertThat("credential projectDomain is mapped correctly", credential.getProjectDomain(), is("everSimplerDomain"));
            assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("s3cr3tPass"));
        }
    }

    @Issue("JENKINS-53105")
    @Test
    public void canConvertAValidScopedSecret() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("validScoped.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            OpenstackCredentialv3 credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("test-openstack-credential-v3"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("openstack credentials for you Jenkins in Kubernetes!"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
            assertThat("credential userName is mapped correctly", credential.getUserName(), is("casualName"));
            assertThat("credential userDomain is mapped correctly", credential.getUserDomain(), is("meaningfulDomain"));
            assertThat("credential projectName is mapped correctly", credential.getProjectName(), is("simpleProject"));
            assertThat("credential projectDomain is mapped correctly", credential.getProjectDomain(), is("everSimplerDomain"));
            assertThat("credential password is mapped correctly", credential.getPassword().getPlainText(), is("s3cr3tPass"));
        }
    }

    @Test
    public void failsToConvertWhenUserNameMissing() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("missingUserName.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the userName"));
        }
    }

    @Test
    public void failsToConvertWhenUserDomainMissing() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("missingUserDomain.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the userDomain"));
        }
    }

    @Test
    public void failsToConvertWhenProjectNameMissing() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("missingProjectName.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the projectName"));
        }
    }

    @Test
    public void failsToConvertWhenProjectDomainMissing() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("missingProjectDomain.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the projectDomain"));
        }
    }

    @Test
    public void failsToConvertWhenPasswordMissing() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("missingPassword.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the password"));
        }
    }

    @Test
    public void failsToConvertWhenUserNameCorrupt() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("corruptUserName.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("credential has an invalid userName"));
        }
    }

    @Test
    public void failsToConvertWhenUserDomainCorrupt() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("corruptUserDomain.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("credential has an invalid userDomain"));
        }
    }

    @Test
    public void failsToConvertWhenProjectNameCorrupt() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("corruptProjectName.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("credential has an invalid projectName"));
        }
    }

    @Test
    public void failsToConvertWhenProjectDomainCorrupt() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("corruptProjectDomain.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("credential has an invalid projectDomain"));
        }
    }

    @Test
    public void failsToConvertWhenPasswordCorrupt() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("corruptPassword.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("credential has an invalid password"));
        }
    }

    @Test
    public void failsToConvertWhenDataEmpty() throws Exception {
        OpenstackCredentialv3Convertor convertor = new OpenstackCredentialv3Convertor();

        try (InputStream is = get("void.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("contains no data"));
        }
    }

    private static final InputStream get(String resource) {
        InputStream is = OpenstackCredentialv3ConvertorTest.class.getResourceAsStream("OpenstackCredentialv3ConvertorTest/" + resource);
        if (is == null) {
            fail("failed to load resource " + resource);
        }
        return is;
    }
}
