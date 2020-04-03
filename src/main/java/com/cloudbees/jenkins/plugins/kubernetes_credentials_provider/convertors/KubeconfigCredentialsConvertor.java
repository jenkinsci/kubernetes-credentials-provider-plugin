package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.convertors;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.CredentialsConvertionException;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretToCredentialConverter;
import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.SecretUtils;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.microsoft.jenkins.kubernetes.credentials.KubeconfigCredentials;
import io.fabric8.kubernetes.api.model.Secret;
import org.jenkinsci.plugins.variant.OptionalExtension;

import javax.annotation.Nonnull;

/**
 * SecretToCredentialConvertor that converts {@link KubeconfigCredentials}.
 */

@OptionalExtension(requirePlugins = {"kubernetes-cd"})
public class KubeconfigCredentialsConvertor extends SecretToCredentialConverter {
	@Override
	public boolean canConvert(String type) {
		return "kubeconfig".equals(type);
	}

	@Override
	public KubeconfigCredentials convert(Secret secret) throws CredentialsConvertionException {
		String textBase64 = SecretUtils.getNonNullSecretData(secret, "kubeconfig", "kubeconfig is empty");
		String secretText = SecretUtils.requireNonNull(SecretUtils.base64DecodeToString(textBase64), "kubeconfig credential has an invalid text (must be base64 encoded UTF-8)");
		return new KubeconfigCredentials(CredentialsScope.GLOBAL, SecretUtils.getCredentialId(secret), SecretUtils.getCredentialDescription(secret), new KubeconfigCredentials.KubeconfigSource() {
			@Nonnull
			@Override
			public String getContent() {
				return secretText;
			}
		});
	}


}
