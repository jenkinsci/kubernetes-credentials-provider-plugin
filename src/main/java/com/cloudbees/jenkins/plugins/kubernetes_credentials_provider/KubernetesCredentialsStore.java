package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider;

import java.io.IOException;
import java.util.List;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.acegisecurity.Authentication;
import org.apache.commons.lang.NotImplementedException;
import hudson.model.ModelObject;
import hudson.security.ACL;
import hudson.security.Permission;
import jenkins.model.Jenkins;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;

public class KubernetesCredentialsStore extends CredentialsStore {

    private final KubernetesCredentialProvider provider;

    public KubernetesCredentialsStore(KubernetesCredentialProvider provider) {
        super(KubernetesCredentialProvider.class);
        this.provider = provider;
    }

    @NonNull
    @Override
    public ModelObject getContext() {
        return Jenkins.getInstance();
    }

    @Override
    public boolean hasPermission(@NonNull Authentication authentication, @NonNull Permission permission) {
        return Jenkins.getInstance().getACL().hasPermission(authentication, permission);
    }

    @NonNull
    @Override
    public List<Credentials> getCredentials(@NonNull Domain domain) {
        // TODO: Filter by domain - how do I do this?
        return  provider.getCredentials(Credentials.class, Jenkins.getInstance(), ACL.SYSTEM);
    }

    @Override
    public boolean addCredentials(@NonNull Domain domain, @NonNull Credentials credentials) {
        throw new NotImplementedException();
    }

    @Override
    public boolean removeCredentials(@NonNull Domain domain, @NonNull Credentials credentials) {
        throw new NotImplementedException();
    }

    @Override
    public boolean updateCredentials(@NonNull Domain domain, @NonNull Credentials current,
                                     @NonNull Credentials replacement) {
        throw new NotImplementedException();
    }
}
