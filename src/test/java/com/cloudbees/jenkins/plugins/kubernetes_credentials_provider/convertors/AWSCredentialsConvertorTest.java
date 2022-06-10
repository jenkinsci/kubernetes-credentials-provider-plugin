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
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl;
import org.junit.Test;
import org.jvnet.hudson.test.Issue;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.emptyString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Tests AWSCredentialConvertor
 */
public class AWSCredentialsConvertorTest {

    String accessKey = "ABC123456";
    String secretKey= "Pa$$word";
    String iamRoleArn = "ecr:eu-west-1:86c8f5ec-1ce1-4e94-80c2-18e23bbd724a";
    String iamMfaSerialNumber = "GAHT12345678";

    @Test
    public void canConvert() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();
        assertThat("correct registration of valid type", convertor.canConvert("aws"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
    }

    @Test
    public void canConvertAValidSecret() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

        try (InputStream is = get("valid.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AWSCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-aws"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), is(accessKey));
            assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), is(secretKey));
            assertThat("credential iamRoleArn is mapped correctly", credential.getIamRoleArn(), is(iamRoleArn));
            assertThat("credential iamMfaSerialNumber is mapped correctly", credential.getIamMfaSerialNumber(), is(iamMfaSerialNumber));
        }
    }

    @Test
    public void canConvertAValidSecretWithNoIamRoleAndMfa() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

        try (InputStream is = get("validMissingIamRoleMfa.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AWSCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-aws"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), is(accessKey));
            assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), is(secretKey));
        }
    }

    @Test
    public void canConvertAValidMappedSecret() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

        try (InputStream is = get("validMapped.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AWSCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-aws"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), is(accessKey));
            assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), is(secretKey));
            assertThat("credential iamRoleArn is mapped correctly", credential.getIamRoleArn(), is(iamRoleArn));
            assertThat("credential iamMfaSerialNumber is mapped correctly", credential.getIamMfaSerialNumber(), is(iamMfaSerialNumber));
        }
    }

    @Test
    public void canConvertAValidSecretWithSpecifiedCredentialsId() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

        try (InputStream is = get("validCredentialsId.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AWSCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("A_TEST_AWS"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), is(accessKey));
            assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), is(secretKey));
            assertThat("credential iamRoleArn is mapped correctly", credential.getIamRoleArn(), is(iamRoleArn));
            assertThat("credential iamMfaSerialNumber is mapped correctly", credential.getIamMfaSerialNumber(), is(iamMfaSerialNumber));
        }
    }

    @Test
    public void canConvertAValidSecretWithNoDescription() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

        try (InputStream is = get("valid-no-desc.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AWSCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-aws"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is(emptyString()));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), is(accessKey));
            assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), is(secretKey));
            assertThat("credential iamRoleArn is mapped correctly", credential.getIamRoleArn(), is(iamRoleArn));
            assertThat("credential iamMfaSerialNumber is mapped correctly", credential.getIamMfaSerialNumber(), is(iamMfaSerialNumber));
        }
    }

    @Test
    public void canConvertAValidSecretWithNoIamRole() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

        try (InputStream is = get("validMissingIamRole.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AWSCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-aws"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is(emptyString()));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), is(accessKey));
            assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), is(secretKey));
            assertThat("credential iamMfaSerialNumber is mapped correctly", credential.getIamMfaSerialNumber(), is(iamMfaSerialNumber));
        }
    }

    @Test
    public void canConvertAValidSecretWithNoIamMfa() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

        try (InputStream is = get("validMissingIamMfa.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AWSCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-aws"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is(emptyString()));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
            assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), is(accessKey));
            assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), is(secretKey));
            assertThat("credential iamRoleArn is mapped correctly", credential.getIamRoleArn(), is(iamRoleArn));
        }
    }

    @Issue("JENKINS-53105")
    @Test
    public void canConvertAValidScopedSecret() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

        try (InputStream is = get("validScoped.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AWSCredentialsImpl credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            assertThat("credential id is mapped correctly", credential.getId(), is("a-test-aws"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
            assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
            assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), is(accessKey));
            assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), is(secretKey));
            assertThat("credential iamRoleArn is mapped correctly", credential.getIamRoleArn(), is(iamRoleArn));
            assertThat("credential iamMfaSerialNumber is mapped correctly", credential.getIamMfaSerialNumber(), is(iamMfaSerialNumber));
        }
    }

    @Test
    public void failsToConvertWhenAccessKeyMissing() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

        try (InputStream is = get("missingAccessKey.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the accessKey"));
        }
    }


    @Test
    public void failsToConvertWhenSecretKeyMissing() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

        try (InputStream is = get("missingSecretKey.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("missing the secretKey"));
        }
    }

    // BASE64 Corrupt
    @Test
    public void failsToConvertWhenAccessKeyCorrupt() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

        try (InputStream is = get("corruptAccessKey.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid accessKey"));
        }
    }


    @Test
    public void failsToConvertWhenSecretKeyCorrupt() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

        try (InputStream is = get("corruptSecretKey.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid secretKey"));
        }
    }

    @Test
    public void failsToConvertWhenARNCorrupt() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

        try (InputStream is = get("corruptIAMRoleARN.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid iamRoleArn"));
        }
    }

    @Test
    public void failsToConvertWhenMFACorrupt() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

        try (InputStream is = get("corruptMFASerialNumber.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("invalid iamMfaSerialNumber"));
        }
    }


    @Test
    public void failsToConvertWhenDataEmpty() throws Exception {
        AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

        try (InputStream is = get("void.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            convertor.convert(secret);
            fail("Exception should have been thrown");
        } catch (CredentialsConvertionException cex) {
            assertThat(cex.getMessage(), containsString("contains no data"));
        }
    }


    private static final InputStream get(String resource) {
        InputStream is = AWSCredentialsConvertorTest.class.getResourceAsStream("AWSCredentialsConvertorTest/" + resource);
        if (is == null) {
            fail("failed to load resource " + resource);
        }
        return is;
    }
}
