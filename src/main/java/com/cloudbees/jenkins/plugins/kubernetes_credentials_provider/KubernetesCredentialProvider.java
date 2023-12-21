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

import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.WatcherException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.ExtensionList;
import hudson.model.AdministrativeMonitor;
import hudson.model.Item;
import hudson.triggers.SafeTimerTask;
import hudson.util.AdministrativeError;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import jenkins.model.Jenkins;
import jenkins.util.Timer;
import org.acegisecurity.Authentication;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import hudson.Extension;
import hudson.init.InitMilestone;
import hudson.init.Initializer;
import hudson.init.TermMilestone;
import hudson.init.Terminator;
import hudson.model.ItemGroup;
import hudson.model.ModelObject;
import hudson.security.ACL;
import com.cloudbees.plugins.credentials.Credentials;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.DomainRequirement;

@Extension
public class KubernetesCredentialProvider extends CredentialsProvider implements Watcher<Secret> {

    private static final Logger LOG = Logger.getLogger(KubernetesCredentialProvider.class.getName());

    /** Map of {@link KubernetesSourcedCredential} keyed by their credential ID */
    private ConcurrentHashMap<String, KubernetesSourcedCredential> credentials = new ConcurrentHashMap<>();

    @CheckForNull
    private KubernetesClient client;
    @CheckForNull
    private Watch watch;
    /** Attempt to reconnect k8s client on exception */
    private boolean reconnectClientOnException = Boolean.parseBoolean(System.getProperty(KubernetesCredentialProvider.class.getName() + ".reconnectClientOnException", "true"));
    /** Delay in minutes before attempting to reconnect k8s client */
    private int reconnectClientDelayMins = Integer.getInteger(KubernetesCredentialProvider.class.getName() + ".reconnectClientDelayMins", 5);

    /** A map storing credential scores scoped to ModelObjects, each ModelObject has its own credential store */
    private final Map<ModelObject, KubernetesCredentialsStore> lazyStoreCache = new HashMap<>();

    /**
     * Kubernetes <a href="https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors">label selector</a> expression
     * for matching secrets to manage.
     */
    static final String LABEL_SELECTOR = KubernetesCredentialProvider.class.getName() + ".labelSelector";

    KubernetesClient getKubernetesClient() {
        if (client == null) {
            ConfigBuilder cb = new ConfigBuilder();
            Config config = cb.build();
            // TODO post 2.362 use jenkins.util.SetContextClassLoader
            try (WithContextClassLoader ignored = new WithContextClassLoader(getClass().getClassLoader())) {
                client = new KubernetesClientBuilder().withConfig(config).build();
            }
        }
        return client;
    }

    @Initializer(after=InitMilestone.PLUGINS_PREPARED, fatal=false)
    @Restricted(NoExternalUse.class) // only for callbacks from Jenkins
    public void startWatchingForSecrets() {
        final String initAdminMonitorId = getClass().getName() + ".initialize";
        final String labelSelectorAdminMonitorId = getClass().getName() + ".labelSelector";
        String labelSelector = System.getProperty(LABEL_SELECTOR);
        try {
            KubernetesClient _client = getKubernetesClient();
            LOG.log(Level.FINER, "Using namespace: {0}", String.valueOf(_client.getNamespace()));
            LabelSelector selector = LabelSelectorExpressions.parse(labelSelector);
            LOG.log(Level.INFO, "retrieving secrets with selector: {0}, {1}", new String[]{SecretUtils.JENKINS_IO_CREDENTIALS_TYPE_LABEL, Objects.toString(selector)});

            // load current set of secrets into provider
            LOG.log(Level.FINER, "retrieving secrets");
            SecretList list = _client.secrets().withLabelSelector(selector).withLabel(SecretUtils.JENKINS_IO_CREDENTIALS_TYPE_LABEL).list();
            ConcurrentHashMap<String, KubernetesSourcedCredential> _credentials = new  ConcurrentHashMap<>();
            List<Secret> secretList = list.getItems();
            for (Secret s : secretList) {
                LOG.log(Level.FINE, "Secret Added - {0}", SecretUtils.getCredentialId(s));
                addSecret(s, _credentials);
            }
            credentials = _credentials;

            // start watching new secrets before we list the current set of secrets so we don't miss any events
            LOG.log(Level.FINER, "registering watch");
            // XXX https://github.com/fabric8io/kubernetes-client/issues/1014
            // watch(resourceVersion, watcher) is deprecated but there is nothing to say why?
            watch = _client.secrets().withLabelSelector(selector).withLabel(SecretUtils.JENKINS_IO_CREDENTIALS_TYPE_LABEL).watch(list.getMetadata().getResourceVersion(), this);
            LOG.log(Level.FINER, "registered watch, retrieving secrets");

            // successfully initialized, clear any previous monitors
            clearAdminMonitors(initAdminMonitorId, labelSelectorAdminMonitorId);
        } catch (KubernetesClientException kex) {
            LOG.log(Level.SEVERE, "Failed to initialise k8s secret provider, secrets from Kubernetes will not be available", kex);
            if (reconnectClientOnException) {
                reconnectLater();
            }
            // Only report the latest failure
            clearAdminMonitors(initAdminMonitorId);
            new AdministrativeError(initAdminMonitorId,
                    "Failed to initialize Kubernetes secret provider",
                    "Credentials from Kubernetes Secrets will not be available.", kex);
        } catch (LabelSelectorParseException lex) {
            LOG.log(Level.SEVERE, "Failed to initialise k8s secret provider, secrets from Kubernetes will not be available", lex);
            // Only report the latest failure
            clearAdminMonitors(labelSelectorAdminMonitorId);
            new AdministrativeError(labelSelectorAdminMonitorId,
                    "Failed to parse Kubernetes secret label selector",
                    "Failed to parse Kubernetes secret <a href=\"https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors\" _target=\"blank\">label selector</a> " +
                            "expression \"<code>" + labelSelector + "</code>\". Secrets from Kubernetes will not be available. ", lex);
        }
    }

    /**
     * Schedule a future task to attempt to reconnect to the kubernetes client.
     * @see #startWatchingForSecrets()
     * @see Timer
     */
    private void reconnectLater() {
        LOG.log(Level.INFO, "Attempting to reconnect Kubernetes client in {0} mins", reconnectClientDelayMins);
        Timer.get().schedule(new SafeTimerTask() {
            @Override
            protected void doRun() throws Exception {
                KubernetesCredentialProvider.this.startWatchingForSecrets();
            }
        }, reconnectClientDelayMins, TimeUnit.MINUTES);
    }

    private void clearAdminMonitors(String... ids) {
        Collection<String> monitorIds = Arrays.asList(ids);
        ExtensionList<AdministrativeMonitor> all = AdministrativeMonitor.all();
        List<AdministrativeMonitor> toRemove = all.stream().filter(am -> monitorIds.contains(am.id)).collect(Collectors.toList());
        all.removeAll(toRemove);
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
        LOG.log(Level.FINEST, "getCredentials called with type {0}, itemgroup {1} and authentication {2}", new Object[] {type.getName(), itemGroup, authentication});
        if (ACL.SYSTEM.equals(authentication)) {
            ArrayList<C> list = new ArrayList<>();
            for (KubernetesSourcedCredential credential : credentials.values()) {
                // Parent group of item can be null
                if(itemGroup == null && !credential.getItemGroups().isEmpty()) {
                    continue;
                }
                if(itemGroup != null) {
                    String itemGroupPath = itemGroup.getFullName();
                    Collection<String> itemGroups = credential.getItemGroups();
                    LOG.log(Level.FINEST, "getCredentials checking if itemGroupPath {0} is in itemGroups of {1} ({2})", new Object[] { itemGroupPath, credential.getId(), itemGroups });
                    if (!itemGroups.isEmpty() && itemGroups.stream().noneMatch(itemGroupPath::equals)) {
                        LOG.log(Level.FINEST, "getCredentials itemGroupPath not found in: {0}", itemGroups);
                        continue;
                    }
                }

                // is s a type of type then populate the list...
                LOG.log(Level.FINEST, "getCredentials {0} is a possible candidate", credential.getId());
                if (CredentialsScope.SYSTEM == credential.getScope() && !(itemGroup instanceof Jenkins)) {
                    LOG.log(Level.FINEST, "getCredentials {0} has SYSTEM scope, but the context is not Jenkins, ignoring", credential.getId());
                } else if (type.isAssignableFrom(credential.getIdCredentials().getClass())) {
                    LOG.log(Level.FINEST, "getCredentials {0} matches, adding to list", credential.getId());
                    // cast to keep generics happy even though we are assignable..
                    list.add(type.cast(credential.getIdCredentials()));
                } else {
                    LOG.log(Level.FINEST, "getCredentials {0} does not match", credential.getId());
                }
            }
            return list;
        }
        return emptyList();
    }

    @Override
    @NonNull
    public <C extends Credentials> List<C> getCredentials(@NonNull Class<C> type,
                                                          @NonNull Item item,
                                                          Authentication authentication) {
        return getCredentials(type, item.getParent(), authentication);
    }

    @Override
    public <C extends Credentials> List<C> getCredentials(@NonNull Class<C> type,
            @NonNull Item item,
            Authentication authentication,
            List<DomainRequirement> domainRequirements) {
        // we do not support domain requirements
        return getCredentials(type, item, authentication);
    }

    @SuppressWarnings("null")
    private final @NonNull <T> List<T> emptyList() {
        // just a separate method to avoid having to suppress "null" for the entirety of getCredentials
        return Collections.emptyList();
    }

    private void addSecret(Secret secret) {
        addSecret(secret, credentials);
    }

    private void addSecret(Secret secret, Map<String, KubernetesSourcedCredential> map) {
        KubernetesSourcedCredential cred = convertSecret(secret);
        String credentialId = SecretUtils.getCredentialId(secret);
        if (cred != null) {
            map.put(credentialId, cred);
        }
    }

    @Override
    public void eventReceived(Action action, Secret secret) {
        String credentialId = SecretUtils.getCredentialId(secret);
        switch (action) {
            case ADDED: {
                LOG.log(Level.FINE, "Secret Added - {0}", credentialId);
                addSecret(secret);
                break;
            }
            case MODIFIED: {
                LOG.log(Level.FINE, "Secret Modified - {0}", credentialId);
                addSecret(secret);
                break;
            }
            case DELETED: {
                LOG.log(Level.FINE, "Secret Deleted - {0}", credentialId);
                credentials.remove(credentialId);
                break;
            }
            case ERROR: {
                // XXX  ????
                LOG.log(Level.WARNING, "Action received of type Error. {0}", secret);
                break;
            }
            case BOOKMARK: {
                // TODO handle bookmarks for efficient reconnect
                break;
            }
        }
    }

    @Override
    public void onClose(WatcherException cause) {
        if (cause != null) {
            LOG.log(Level.WARNING, "Secrets watch stopped unexpectedly", cause);
            LOG.log(Level.INFO, "Restating secrets watcher");
            startWatchingForSecrets();
        } else {
            LOG.log(Level.INFO, "Secrets watcher stopped");
        }
    }


    @CheckForNull
    KubernetesSourcedCredential convertSecret(Secret s) {
        String type = s.getMetadata().getLabels().get(SecretUtils.JENKINS_IO_CREDENTIALS_TYPE_LABEL);

        SecretToCredentialConverter lookup = SecretToCredentialConverter.lookup(type);
        if (lookup != null) {
            try {
                return new KubernetesSourcedCredential(
                        lookup.convert(s),
                        SecretUtils.getCredentialItemScopes(s)
                );
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
        LOG.log(Level.WARNING, "No SecretToCredentialConverter found to convert secrets of type {0}", type);
        return null;
    }

    @Override
    public CredentialsStore getStore(ModelObject object) {
        if(object instanceof ItemGroup<?>) {
            lazyStoreCache.putIfAbsent(object, new KubernetesCredentialsStore(this, (ItemGroup<?>) object));
            return lazyStoreCache.get(object);
        }
        return null;
    }

    @Override
    public String getIconClassName() {
        return "icon-credentials-kubernetes-store";
    }

    private static class WithContextClassLoader implements AutoCloseable {

        private final ClassLoader previousClassLoader;

        public WithContextClassLoader(ClassLoader classLoader) {
            this.previousClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);
        }

        @Override
        public void close() {
            Thread.currentThread().setContextClassLoader(previousClassLoader);
        }
    }
}
