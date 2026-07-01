package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.convertors;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretToCredentialConverter;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Abstract credentials converter test.
 */
class AbstractConverterTest {

    /**
     * Test and validate secret with empty "data". Expects a "void.yaml" test resource.
     *
     * @param convertor converter to test
     * @throws Exception if unable to load "void.yaml" test resource
     */
    protected final void testNoData(SecretToCredentialConverter convertor) throws Exception {
        Secret secret = getSecret("void.yaml");
        CredentialsConvertionException cex = assertThrows(CredentialsConvertionException.class, () -> convertor.convert(secret));
        assertThat(cex.getMessage(), containsString("contains no data"));
    }

    /**
     * Test and validate missing field. Expects a test resource file named "missing<field>.yaml" where the {@code field}
     * name is capitalized.
     *
     * @param convertor converter to test
     * @param field     field name
     * @throws Exception if unable to load test resource
     * @see #testMissingField(SecretToCredentialConverter, String, String)
     */
    protected final void testMissingField(SecretToCredentialConverter convertor, String field) throws Exception {
        testMissingField(convertor, field, "missing" + StringUtils.capitalize(field) + ".yaml");
    }

    /**
     * Test and validate missing field.
     *
     * @param convertor converter to test
     * @param field     field name
     * @param resource  test resource file name
     * @throws Exception if unable to load test resource
     */
    protected final void testMissingField(SecretToCredentialConverter convertor, String field, String resource) throws Exception {
        Secret secret = getSecret(resource);
        CredentialsConvertionException cex = assertThrows(CredentialsConvertionException.class, () -> convertor.convert(secret));
        assertThat(cex.getMessage(), containsString("missing the " + field));
    }

    /**
     * Test and validate corrupt field. Expects a test resource file named "corrupt<field>.yaml" where the {@code field}
     * name is capitalized.
     *
     * @param convertor converter to test
     * @param field     field name
     * @throws Exception if unable to load test resource
     * @see #testCorruptField(SecretToCredentialConverter, String, String)
     */
    protected final void testCorruptField(SecretToCredentialConverter convertor, String field) throws Exception {
        testCorruptField(convertor, field, "corrupt" + StringUtils.capitalize(field) + ".yaml");
    }

    /**
     * Test and validate corrupt field.
     *
     * @param convertor converter to test
     * @param field     field name
     * @param resource  test resource file name
     * @throws Exception if unable to load test resource
     */
    protected final void testCorruptField(SecretToCredentialConverter convertor, String field, String resource) throws Exception {
        Secret secret = getSecret(resource);
        CredentialsConvertionException cex = assertThrows(CredentialsConvertionException.class, () -> convertor.convert(secret));
        assertThat(cex.getMessage(), containsString("invalid " + field));
    }

    /**
     * Load kubernetes secret test resource file.
     *
     * @param resource resource file name
     * @return kubernetes secret resource
     * @throws Exception if unable to load test resource
     */
    protected final Secret getSecret(String resource) throws Exception {
        try (InputStream is = getTestResource(resource)) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", secret, notNullValue());
            return secret;
        }
    }

    /**
     * Get test resource as input stream. Resources are expected to be in a resource folder under
     * {@code com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.convertors} and with the same
     * name as the test class.
     *
     * @param resource resource file name
     * @return resource input stream
     */
    protected final InputStream getTestResource(String resource) {
        String resourcePath = getClass().getSimpleName() + "/" + resource;
        InputStream is = getClass().getResourceAsStream(resourcePath);
        assertThat("failed to load resource " + resourcePath, is, notNullValue());
        return is;
    }
}
