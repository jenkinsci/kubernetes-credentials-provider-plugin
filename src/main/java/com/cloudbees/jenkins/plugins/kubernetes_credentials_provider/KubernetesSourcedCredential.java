package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider;

import java.util.Set;

import javax.annotation.Nullable;

import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.IdCredentials;

/**
 * Stores a credential that was sourced from a kubernetes secret.
 * Metadata that was passed via labels should be stored here.
 */
public class KubernetesSourcedCredential {

    private final IdCredentials idCredentials;
    private final Set<String> itemGroups;

    public KubernetesSourcedCredential(IdCredentials idCredentials, Set<String> itemGroups) {
        this.idCredentials = idCredentials;
        this.itemGroups = itemGroups;
    }

    public IdCredentials getIdCredentials() {
        return idCredentials;
    }

    public Set<String> getItemGroups() {
        return itemGroups;
    }

    public String getId() {
        return idCredentials.getId();
    }

    @Nullable
    public CredentialsScope getScope() {
        return idCredentials.getScope();
    }

}
