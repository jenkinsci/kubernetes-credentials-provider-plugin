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
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.convertors.azure.AzureEnvironments;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.microsoft.azure.util.AzureImdsCredentials;
import com.microsoft.azure.util.AzureImdsCredentials;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;

public class AzureManagedIdentityCedentialsConvertorTest {

	@Test
    public void canConvert() throws Exception {
		AzureManagedIdentityCedentialsConvertor convertor = new AzureManagedIdentityCedentialsConvertor();
        
        assertThat("correct registration of valid type", convertor.canConvert("azureManagedIdentity"), is(true));
        assertThat("incorrect type is rejected", convertor.canConvert("something"), is(false));
    }
	
	@Test
    public void canConvertAValidSecret() throws Exception {
		AzureManagedIdentityCedentialsConvertor convertor = new AzureManagedIdentityCedentialsConvertor();

        try (InputStream is = get("validManagedIdentity.yaml")) {
            Object secrets = Serialization.unmarshal(is, Secret.class);
            
            for (Secret secret : (ArrayList<Secret>)secrets) {
            	assertThat("The Secret was loaded correctly from disk", notNullValue());
            	AzureImdsCredentials credential = convertor.convert(secret);
            	assertThat(credential, notNullValue());
            	
            	assertThat("credentials is using SYSTEM scope", credential.getScope(), is(CredentialsScope.SYSTEM));
            	
            	assertThat("credential id is mapped correctly", credential.getId(), is("azure-managed-identity"));
            	assertThat("credential description is mapped correctly", credential.getDescription(), is("Azure managed identity"));
            	assertThat("credential text is mapped correctly", credential.getSubscriptionId(), is("1234456457"));
            	assertThat("credential text is mapped correctly", credential.getClientId(), is("client-id"));
            	
            	assertThat("Azure environment are defined", credential.getAzureEnvironmentName(), is(AzureEnvironments.AZURE.label));
            }
        }
    }
	
	@Test
    public void nonDefaultAzureEnv() throws Exception {
		AzureManagedIdentityCedentialsConvertor convertor = new AzureManagedIdentityCedentialsConvertor();

        try (InputStream is = get("nonDefaultAzureEnvironment.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AzureImdsCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            
            assertThat("Azure environment are defined", credential.getAzureEnvironmentName(), is(AzureEnvironments.AZURE_CHINA.label));
        }
    }
    
    /*
	
	@Test
    public void failOnInvalidDataString() throws Exception {
		AzureManagedIdentityCedentialsConvertor convertor = new AzureManagedIdentityCedentialsConvertor();

        try (InputStream is = get("curruptDataFields.yaml")) {
            Object secrets = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            
            for (Secret secret : (ArrayList<Secret>) secrets) {
            	try {
            		AzureImdsCredentials credential = convertor.convert(secret);
            		assert false;
            	} catch (NullPointerException nullPointerException) {
            		assert true;
            	} catch (Exception e) {
            		assert false;
            	}
            }
        }
    }
	
	@Test
    public void defaultsToGlobalOnMissingScope() throws Exception {
		AzureManagedIdentityCedentialsConvertor convertor = new AzureManagedIdentityCedentialsConvertor();

        try (InputStream is = get("missingScope.yaml")) {
            Secret secret = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            AzureImdsCredentials credential = convertor.convert(secret);
            assertThat(credential, notNullValue());
            
            assertThat("credentials is using SYSTEM scope", credential.getScope(), is(CredentialsScope.GLOBAL));
        }
    }
	
	@Test
    public void failOnMissingDataFields() throws Exception {
		AzureManagedIdentityCedentialsConvertor convertor = new AzureManagedIdentityCedentialsConvertor();

        try (InputStream is = get("missingDataFields.yaml")) {
            Object secrets = Serialization.unmarshal(is, Secret.class);
            assertThat("The Secret was loaded correctly from disk", notNullValue());
            for (Secret secret : (ArrayList<Secret>) secrets) {
            	try {
            		AzureImdsCredentials credential = convertor.convert(secret);
            		assert false;
            	} catch (CredentialsConvertionException convertException) {
            		assert true;
            	} 
            }
        } catch (Exception e) {
        	assert false;
        }
    }
	*/
	
    private static final InputStream get(String resource) {
        InputStream is = AzureManagedIdentityCedentialsConvertorTest.class.getResourceAsStream("AzureManagedIdentityCedentialsConvertorTest/" + resource);
        if (is == null) {
            fail("failed to load resource " + resource);
        }
        return is;
    }

}
