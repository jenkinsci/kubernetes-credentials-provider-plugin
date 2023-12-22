package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.convertors;


import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.microsoft.azure.util.AzureCredentials;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;

public class AzureServicePrincipalCedentialsConvertorTest {

	@Test
    public void canConvert() throws Exception {
		AzureServicePrincipalCedentialsConvertor convertor = new AzureServicePrincipalCedentialsConvertor();
        
        assertThat("correct registration of valid type", convertor.canConvert("azureServicePrincipal"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
    }
	
	@Test
    public void canConvertAValidSecret() throws Exception {
		AzureServicePrincipalCedentialsConvertor convertor = new AzureServicePrincipalCedentialsConvertor();

        try (InputStream is = get("valid.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AzureCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            
            assertThat("credentials is using SYSTEM scope", credential.getScope(), is(CredentialsScope.SYSTEM));
            
            assertThat("credential id is mapped correctly", credential.getId(), is("azure-service-principal"));
            assertThat("credential description is mapped correctly", credential.getDescription(), is("Azure service principal"));
            assertThat("credential text is mapped correctly", credential.getSubscriptionId(), is("1234456457"));
            assertThat("credential text is mapped correctly", credential.getClientId(), is("client-id"));
            assertThat("credential text is mapped correctly", credential.getTenant(), is("tenant-id"));
            assertThat("credential text is mapped correctly", credential.getPlainClientSecret(), is("client-secret-secret-string"));
            
            assertThat("Azure environment are defined", credential.getAzureEnvionmentName(), is("Azure"));
        }
    }
	
	@Test
    public void nonDefaultAzureEnv() throws Exception {
		AzureServicePrincipalCedentialsConvertor convertor = new AzureServicePrincipalCedentialsConvertor();

        try (InputStream is = get("nonDefaultAzureEnvironment.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AzureCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            
            assertThat("Azure environment are defined", credential.getAzureEnvionmentName(), is("Azure China"));
        }
    }
	
	@Test
    public void failOnInvalidDataString() throws Exception {
		AzureServicePrincipalCedentialsConvertor convertor = new AzureServicePrincipalCedentialsConvertor();

        try (InputStream is = get("curruptDataFields.yaml")) {
            Object secrets = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            
            for (Secret secret : (ArrayList<Secret>) secrets) {
            	try {
            		AzureCredentials credential = convertor.convert(secret);
            		assert false;
            	} catch (CredentialsConvertionException convertionException) {
            		assert true;
            	} catch (Exception e) {
            		assert false;
            	}
            }
        }
    }
	
	@Test
    public void defaultsToGlobalOnMissingScope() throws Exception {
		AzureServicePrincipalCedentialsConvertor convertor = new AzureServicePrincipalCedentialsConvertor();

        try (InputStream is = get("missingScope.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AzureCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            
            assertThat("credentials is using SYSTEM scope", credential.getScope(), is(CredentialsScope.GLOBAL));
        }
    }
	
	@Test
    public void failOnMissingDataFields() throws Exception {
		AzureServicePrincipalCedentialsConvertor convertor = new AzureServicePrincipalCedentialsConvertor();

        try (InputStream is = get("missingDataFields.yaml")) {
            Object secrets = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            for (Secret secret : (ArrayList<Secret>) secrets) {
            	try {
            		AzureCredentials credential = convertor.convert(secret);
            		assert false;
            	} catch (CredentialsConvertionException convertException) {
            		assert true;
            	} 
            }
        } catch (Exception e) {
        	assert false;
        }
    }
	
	
    private static final InputStream get(String resource) {
        InputStream is = AzureServicePrincipalCedentialsConvertorTest.class.getResourceAsStream("AzureServicePrincipalCedentialsConvertorTest/" + resource);
        if (is == null) {
            fail("failed to load resource " + resource);
        }
        return is;
    }

}
