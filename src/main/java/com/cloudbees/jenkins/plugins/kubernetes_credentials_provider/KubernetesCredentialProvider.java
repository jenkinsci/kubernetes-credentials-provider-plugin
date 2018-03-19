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
package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import org.acegisecurity.Authentication;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.init.TermMilestone;
import hudson.init.Terminator;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;

@Extension
public class KubernetesCredentialProvider extends CredentialsProvider implements Watcher<Secret> {

    private static final Logger LOG = Logger.getLogger(KubernetesCredentialProvider.class.getName());

    /** Map of Credentials keyed by their credential ID */
    private ConcurrentHashMap<String, IdCredentials> credentials = new ConcurrentHashMap<>();

    @CheckForNull
    private KubernetesClient client;
    @CheckForNull
    private Watch watch;

    @Initializer(after=InitMilestone.PLUGINS_PREPARED, fatal=false)
    @Restricted(NoExternalUse.class) // only for callbacks from Jenkins
    public void startWatchingForSecrets() {
        try {
            ConfigBuilder cb = new ConfigBuilder();
            Config config = cb.build();
            DefaultKubernetesClient _client = new DefaultKubernetesClient(config);
            LOG.log(Level.FINER, "Using namespace: {0}", _client.getNamespace());
            LOG.log(Level.FINER, "retreiving secrets");
            SecretList list = _client.secrets().withLabel(SecretUtils.JENKINS_IO_CREDENTIALS_TYPE_LABEL).list();

            List<Secret> secretList = list.getItems();
            ConcurrentHashMap<String, IdCredentials> _credentials = new  ConcurrentHashMap<>();
            for (Secret s : secretList) {
                LOG.log(Level.FINE, "Secret Added - {0}", SecretUtils.getCredentialId(s));
                IdCredentials cred = convertSecret(s);
                if (cred != null) {
                    _credentials.put(SecretUtils.getCredentialId(s), cred);
                }
            }
            credentials = _credentials;

            // XXX https://github.com/fabric8io/kubernetes-client/issues/1014
            // watch(resourceVersion, watcher) is deprecated but there is nothing to say why?
            client = _client;
            LOG.log(Level.FINER, "regestering watch");
            watch = _client.secrets().withLabel(SecretUtils.JENKINS_IO_CREDENTIALS_TYPE_LABEL).watch(list.getMetadata().getResourceVersion(), this);
            LOG.log(Level.FINER, "registered watch, retreiving secrets");
        } catch (KubernetesClientException kex) {
            LOG.log(Level.SEVERE, "Failed to initialise k8s secret provider, secrets from Kubernetes will not be available", kex);
            // TODO add an administrative warning to report this clearly to the admin
        }
    }
 

    @Terminator(after=TermMilestone.STARTED)
    @Restricted(NoExternalUse.class) // only for callbacks from Jenkins
    public void stopWatchingForSecrets() {
        if (watch != null) {
            watch.close();
            watch = null;
        }
        if (client != null) {
            client.close();
            client = null;
        }
    }

    @Override
    public <C extends Credentials> List<C> getCredentials(Class<C> type, ItemGroup itemGroup, Authentication authentication) {
        LOG.log(Level.FINEST, "getCredentials called with type {0} and authentication {1}", new Object[] {type.getName(), authentication});
        if (ACL.SYSTEM.equals(authentication)) {
            ArrayList<C> list = new ArrayList<>();
            for (IdCredentials credential : credentials.values()) {
                // is s a type of type then populate the list...
                LOG.log(Level.FINEST, "getCredentials {0} is a possible candidate", credential.getId());
                if (type.isAssignableFrom(credential.getClass())) {
                    LOG.log(Level.FINEST, "getCredentials {0} matches, adding to list", credential.getId());
                    // cast to keep generics happy even though we are assignable..
                    list.add(type.cast(credential));
                }
                LOG.log(Level.FINEST, "getCredentials {0} does not match", credential.getId());
            }
            return list;
        }
        return emptyList();
    }

    @SuppressWarnings("null")
    private final @NonNull <T> List<T> emptyList() {
        // just a separate method to avoid having to suppress "null" for the entirety of getCredentials
        return Collections.emptyList();
    }

    @Override
    public void eventReceived(Action action, Secret secret) {
        String credentialId = SecretUtils.getCredentialId(secret);
        switch (action) {
            case ADDED: {
                LOG.log(Level.FINE, "Secret Added - {0}", credentialId);
                IdCredentials cred = convertSecret(secret);
                if (cred != null) {
                    credentials.put(credentialId, cred);
                }
                break;
            }
            case MODIFIED: {
                LOG.log(Level.FINE, "Secret Modified - {0}", credentialId);
                IdCredentials cred = convertSecret(secret);
                if (cred != null) {
                    credentials.put(credentialId, cred);
                }
                break;
            }
            case DELETED: {
                LOG.log(Level.FINE, "Secret Deleted - {0}", credentialId);
                credentials.remove(credentialId);
                break;
            }
            case ERROR:
                // XXX  ????
                LOG.log(Level.WARNING, "Action received of type Error. {0}", secret);
        }
    }

    @Override
    public void onClose(KubernetesClientException cause) {
        // TODO reconnect?
        LOG.log(Level.INFO, "onClose.", cause);
    }


    @CheckForNull
    IdCredentials convertSecret(Secret s) {
        String type = s.getMetadata().getLabels().get(SecretUtils.JENKINS_IO_CREDENTIALS_TYPE_LABEL);

        SecretToCredentialConverter lookup = SecretToCredentialConverter.lookup(type);
        if (lookup != null) {
            try {
                return lookup.convert(s);
            } catch (CredentialsConvertionException ex) {
                // do not spam the logs with the stacktrace...
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.log(Level.FINE, "Failed to convert Secret '" + SecretUtils.getCredentialId(s) + "' of type " + type, ex);
                }
                else {
                    LOG.log(Level.WARNING, "Failed to convert Secret ''{0}'' of type {1} due to {2}", new Object[] {SecretUtils.getCredentialId(s), type, ex.getMessage()});
                }
                return null; 
            }
        }
        LOG.log(Level.WARNING, "No SecretToCredentialConveror found to convert secrets of type {0}", type);
        return null;
    }

}
