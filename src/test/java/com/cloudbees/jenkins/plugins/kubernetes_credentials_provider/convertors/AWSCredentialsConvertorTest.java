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

import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl;
import com.cloudbees.plugins.credentials.CredentialsScope;
import io.fabric8.kubernetes.api.model.Secret;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;

/**
 * Tests AWSCredentialConvertor
 */
class AWSCredentialsConvertorTest extends AbstractConverterTest {

    private static final String ACCESS_KEY = "ABC123456";
    private static final String SECRET_KEY = "Pa$$word";
    private static final String IAM_ROLE_ARN = "ecr:eu-west-1:86c8f5ec-1ce1-4e94-80c2-18e23bbd724a";
    private static final String IAM_MFA_SERIAL_NUMBER = "GAHT12345678";

    private final AWSCredentialsConvertor convertor = new AWSCredentialsConvertor();

    @Test
    void canConvert() {
        assertThat("correct registration of valid type", convertor.canConvert("aws"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
    }

    @Test
    void canConvertAValidSecret() throws Exception {
        Secret secret = getSecret("valid.yaml");
        AWSCredentialsImpl credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-aws"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), is(ACCESS_KEY));
        assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), is(SECRET_KEY));
        assertThat("credential iamRoleArn is mapped correctly", credential.getIamRoleArn(), is(IAM_ROLE_ARN));
        assertThat("credential iamMfaSerialNumber is mapped correctly", credential.getIamMfaSerialNumber(), is(IAM_MFA_SERIAL_NUMBER));
    }

    @Test
    void canConvertAValidSecretWithNoIamRoleAndMfa() throws Exception {
        Secret secret = getSecret("validMissingIamRoleMfa.yaml");
        AWSCredentialsImpl credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-aws"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), is(ACCESS_KEY));
        assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), is(SECRET_KEY));
    }

    @Test
    void canConvertAValidMappedSecret() throws Exception {
        Secret secret = getSecret("validMapped.yaml");
        AWSCredentialsImpl credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-aws"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), is(ACCESS_KEY));
        assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), is(SECRET_KEY));
        assertThat("credential iamRoleArn is mapped correctly", credential.getIamRoleArn(), is(IAM_ROLE_ARN));
        assertThat("credential iamMfaSerialNumber is mapped correctly", credential.getIamMfaSerialNumber(), is(IAM_MFA_SERIAL_NUMBER));
    }

    @Test
    void canConvertAValidSecretWithNoDescription() throws Exception {
        Secret secret = getSecret("valid-no-desc.yaml");
        AWSCredentialsImpl credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-aws"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is(emptyString()));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), is(ACCESS_KEY));
        assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), is(SECRET_KEY));
        assertThat("credential iamRoleArn is mapped correctly", credential.getIamRoleArn(), is(IAM_ROLE_ARN));
        assertThat("credential iamMfaSerialNumber is mapped correctly", credential.getIamMfaSerialNumber(), is(IAM_MFA_SERIAL_NUMBER));
    }

    @Test
    void canConvertAValidSecretWithNoIamRole() throws Exception {
        Secret secret = getSecret("validMissingIamRole.yaml");
        AWSCredentialsImpl credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-aws"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is(emptyString()));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), is(ACCESS_KEY));
        assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), is(SECRET_KEY));
        assertThat("credential iamMfaSerialNumber is mapped correctly", credential.getIamMfaSerialNumber(), is(IAM_MFA_SERIAL_NUMBER));
    }

    @Test
    void canConvertAValidSecretWithNoIamMfa() throws Exception {
        Secret secret = getSecret("validMissingIamMfa.yaml");
        AWSCredentialsImpl credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-aws"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is(emptyString()));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), is(ACCESS_KEY));
        assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), is(SECRET_KEY));
        assertThat("credential iamRoleArn is mapped correctly", credential.getIamRoleArn(), is(IAM_ROLE_ARN));
    }

    @Issue("JENKINS-53105")
    @Test
    void canConvertAValidScopedSecret() throws Exception {
        Secret secret = getSecret("validScoped.yaml");
        AWSCredentialsImpl credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-aws"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.SYSTEM));
        assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), is(ACCESS_KEY));
        assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), is(SECRET_KEY));
        assertThat("credential iamRoleArn is mapped correctly", credential.getIamRoleArn(), is(IAM_ROLE_ARN));
        assertThat("credential iamMfaSerialNumber is mapped correctly", credential.getIamMfaSerialNumber(), is(IAM_MFA_SERIAL_NUMBER));
    }

    @Test
    void canConvertAValidSecretWithNoAccessKeyAndSecretKey() throws Exception {
        Secret secret = getSecret("validMissingAccessKeyAndSecretKey.yaml");
        AWSCredentialsImpl credential = convertor.convert(secret);
        assertThat(credential, notNullValue());
        assertThat("credential id is mapped correctly", credential.getId(), is("a-test-aws"));
        assertThat("credential description is mapped correctly", credential.getDescription(), is("credentials from Kubernetes"));
        assertThat("credential scope is mapped correctly", credential.getScope(), is(CredentialsScope.GLOBAL));
        assertThat("credential accessKey is mapped correctly", credential.getAccessKey(), emptyString());
        assertThat("credential secretKey is mapped correctly", credential.getSecretKey().getPlainText(), emptyString());
        assertThat("credential iamRoleArn is mapped correctly", credential.getIamRoleArn(), is(IAM_ROLE_ARN));
    }

    // BASE64 Corrupt
    @Test
    void failsToConvertWhenAccessKeyCorrupt() throws Exception {
        testCorruptField(convertor, "accessKey");
    }

    @Test
    void failsToConvertWhenSecretKeyCorrupt() throws Exception {
        testCorruptField(convertor, "secretKey");
    }

    @Test
    void failsToConvertWhenARNCorrupt() throws Exception {
        testCorruptField(convertor, "iamRoleArn");
    }

    @Test
    void failsToConvertWhenMFACorrupt() throws Exception {
        testCorruptField(convertor, "iamMfaSerialNumber");
    }

    @Test
    void failsToConvertWhenDataEmpty() throws Exception {
        testNoData(convertor);
    }
}
