package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.convertors.azure;

public enum AzureEnvironments {
	AZURE("Azure"),
	AZURE_CHINA("Azure China"),
	AZURE_US_GOVERNMENT("Azure US Government");
	
	public final String label;
	
	private AzureEnvironments(String label) {
		this.label = label;
	}
	
	public static AzureEnvironments valueOfLabel(String label) {
		for (AzureEnvironments azureEnvironments : values()) {
			if (azureEnvironments.label.equals(label)) {
				return azureEnvironments;
			}
		}
		return null;
	}
}
