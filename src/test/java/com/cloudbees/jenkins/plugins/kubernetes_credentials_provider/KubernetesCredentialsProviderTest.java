package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.convertors.UsernamePasswordCredentialsConvertor;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.ExtensionList;
import hudson.model.AdministrativeMonitor;
import hudson.model.Item;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.SecretListBuilder;
import io.fabric8.kubernetes.api.model.WatchEvent;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.WatcherException;
import io.fabric8.kubernetes.client.server.mock.EnableKubernetesMockClient;
import io.fabric8.kubernetes.client.server.mock.KubernetesMockServer;
import io.fabric8.mockwebserver.http.RecordedRequest;
import jenkins.model.Jenkins;
import jenkins.util.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.jvnet.hudson.test.Issue;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@EnableKubernetesMockClient
class KubernetesCredentialsProviderTest {

    private static final Long EVENT_WAIT_PERIOD_MS = 10L;

    private KubernetesMockServer server;
    private KubernetesClient client;

    @Mock
    private ScheduledExecutorService jenkinsTimer;
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private MockedStatic<ExtensionList> extensionList;
    @Mock
    private MockedStatic<Timer> timer;

    @BeforeEach
    void setUp() {
        server = new KubernetesMockServer();
        server.init();
        server.clearExpectations();
        client = server.createClient();
        // mocked to validate add/remove of administrative errors
        ExtensionList<AdministrativeMonitor> monitors = ExtensionList.create((Jenkins) null, AdministrativeMonitor.class);
        // mocked to validate start watching for secrets
        ExtensionList<SecretToCredentialConverter> converters = ExtensionList.create((Jenkins) null, SecretToCredentialConverter.class);
        converters.add(new UsernamePasswordCredentialsConvertor());
        extensionList.when(() -> ExtensionList.lookup(AdministrativeMonitor.class)).thenReturn(monitors);
        extensionList.when(() -> ExtensionList.lookup(SecretToCredentialConverter.class)).thenReturn(converters);
        timer.when(Timer::get).thenReturn(jenkinsTimer);
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.destroy();
    }

    private void defaultMockKubernetesResponses() {
        mockKubernetesResponses("jenkins.io%2Fcredentials-type");
    }

    private void mockKubernetesResponses(String labelSelector) {
        // initial request without resourceVersion
        server.expect().withPath("/api/v1/namespaces/test/secrets?labelSelector=" + labelSelector)
                .andReturn(200, new SecretListBuilder()
                        .withNewMetadata()
                        .withResourceVersion("1")
                        .endMetadata()
                        .build()).always();
        // subsequent requests resourceVersion after the first list (we do not modify things, so return null)
        server.expect().withPath("/api/v1/namespaces/test/secrets?allowWatchBookmarks=true&labelSelector=" + labelSelector + "&resourceVersion=1&watch=true")
                .andReturn(200, null).always();
    }

    @Test
    void startWatchingForSecrets_Scoped() {
        Map<String, String> s4Annotations = new HashMap<>();
        s4Annotations.put(SecretUtils.JENKINS_IO_CREDENTIALS_ITEM_GROUP_ANNOTATION, "['my-item-group']");
        Secret s4 = createSecret("s4", Map.of(), s4Annotations);

        server.expect().withPath("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type")
                .andReturn(200, new SecretListBuilder()
                        .withNewMetadata()
                        .withResourceVersion("1")
                        .endMetadata()
                        .addToItems(s4)
                        .build())
                .once();

        // subsequent requests resourceVersion after the first list (we do not modify things, so return null)
        server.expect().withPath("/api/v1/namespaces/test/secrets?allowWatchBookmarks=true&labelSelector=jenkins.io%2Fcredentials-type&resourceVersion=1&watch=true")
                .andReturn(200, null).always();

        KubernetesCredentialProvider provider = new MockedKubernetesCredentialProvider();
        provider.startWatchingForSecrets();

        ItemGroup group = mock(ItemGroup.class);
        when(group.getFullName()).thenReturn("my-item-group");

        List<UsernamePasswordCredentials> credentials = provider.getCredentials(UsernamePasswordCredentials.class, group, ACL.SYSTEM);
        assertEquals(1, credentials.size(), "credentials");
        assertTrue(credentials.stream().anyMatch(c -> "s4".equals(((UsernamePasswordCredentialsImpl) c).getId())), "secret s4 exists");
    }

    @Test
    void startWatchingForSecrets() {
        Secret s1 = createSecret("s1", (CredentialsScope) null);
        Secret s2 = createSecret("s2", (CredentialsScope) null);
        Secret s3 = createSecret("s3", (CredentialsScope) null);

        // returns s1 and s3, the credentials map should be reset to this list
        server.expect().withPath("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type")
                .andReturn(200, new SecretListBuilder()
                        .withNewMetadata()
                        .withResourceVersion("1")
                        .endMetadata()
                        .addToItems(s1, s3)
                        .build())
                .once();

        // expect the s2 will get dropped when the credentials map is reset to the full list
        server.expect().withPath("/api/v1/namespaces/test/secrets?allowWatchBookmarks=true&labelSelector=jenkins.io%2Fcredentials-type&resourceVersion=1&watch=true")
                .andUpgradeToWebSocket()
                .open()
                    .waitFor(EVENT_WAIT_PERIOD_MS)
                    .andEmit(new WatchEvent(s1, "ADDED"))
                    .waitFor(EVENT_WAIT_PERIOD_MS)
                    .andEmit(new WatchEvent(s2, "ADDED"))
                .done()
                .once();

        KubernetesCredentialProvider provider = new MockedKubernetesCredentialProvider();
        provider.startWatchingForSecrets();

        List<UsernamePasswordCredentials> credentials = provider.getCredentials(UsernamePasswordCredentials.class, (ItemGroup) null, ACL.SYSTEM);
        assertEquals(2, credentials.size(), "credentials");
        assertTrue(credentials.stream().anyMatch(c -> "s1".equals(((UsernamePasswordCredentialsImpl) c).getId())), "secret s1 exists");
        assertTrue(credentials.stream().anyMatch(c -> "s3".equals(((UsernamePasswordCredentialsImpl) c).getId())), "secret s3 exists");
    }

    @Issue("SECURITY-3022")
    @Test
    void credentialScope() {
        Secret s1 = createSecret("s1", CredentialsScope.GLOBAL);
        Secret s2 = createSecret("s2", CredentialsScope.SYSTEM);
        Secret s3 = createSecret("s3", CredentialsScope.GLOBAL);

        // returns s1 and s3, the credentials map should be reset to this list
        server.expect().withPath("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type")
                .andReturn(200, new SecretListBuilder()
                        .withNewMetadata()
                        .withResourceVersion("1")
                        .endMetadata()
                        .addToItems(s1, s2, s3)
                        .build())
                .once();

        // expect the s2 will get dropped when the credentials map is reset to the full list
        server.expect().withPath("/api/v1/namespaces/test/secrets?allowWatchBookmarks=true&labelSelector=jenkins.io%2Fcredentials-type&resourceVersion=1&watch=true")
                .andReturnChunked(200, new WatchEvent(s1, "ADDED"), new WatchEvent(s2, "ADDED"))
                .once();
        server.expect().withPath("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type&watch=true")
                .andReturn(200, null)
                .always();

        KubernetesCredentialProvider provider = new MockedKubernetesCredentialProvider();
        provider.startWatchingForSecrets();
        List<UsernamePasswordCredentials> credentials;

        credentials = provider.getCredentials(UsernamePasswordCredentials.class, (ItemGroup) null, ACL.SYSTEM);
        assertThat("null itemgroup so system scoped credentials (s2) are *not* available", credentials,
                containsInAnyOrder(hasProperty("id", is("s1")), hasProperty("id", is("s3"))));

        credentials = provider.getCredentials(UsernamePasswordCredentials.class, mock(Jenkins.class), ACL.SYSTEM);
        assertThat("itemgroup is Jenkins so system scoped credentials (s2) are available", credentials,
                containsInAnyOrder(hasProperty("id", is("s1")), hasProperty("id", is("s2")), hasProperty("id", is("s3"))));

        credentials = provider.getCredentials(UsernamePasswordCredentials.class, mock(ItemGroup.class), ACL.SYSTEM);
        assertThat("itemgroup is not Jenkins so system scoped credentials (s2) are *not* available", credentials,
                containsInAnyOrder(hasProperty("id", is("s1")), hasProperty("id", is("s3"))));

        credentials = provider.getCredentials(UsernamePasswordCredentials.class, mock(Item.class), ACL.SYSTEM);
        assertThat("items do not have access to system scoped credentials (s2) so should not be available", credentials,
                containsInAnyOrder(hasProperty("id", is("s1")), hasProperty("id", is("s3"))));

        credentials = provider.getCredentials(UsernamePasswordCredentials.class, mock(Item.class), ACL.SYSTEM, Collections.emptyList());
        assertThat("items do not have access to system scoped credentials (s2) so should not be available", credentials,
                containsInAnyOrder(hasProperty("id", is("s1")), hasProperty("id", is("s3"))));
    }

    private Secret createSecret(String name, CredentialsScope scope) {
        Map<String, String> labels = Map.of(
                "jenkins.io/credentials-scope", scope == null ? "global" : scope.name().toLowerCase(Locale.ROOT)
        );
        return createSecret(name, labels);
    }

    private Secret createSecret(String name, Map<String, String> labels) {
        return createSecret(name, labels, Map.of());
    }

    private Secret createSecret(String name, Map<String, String> labels, Map<String, String> annotations) {
        Map<String, String> labelsCopy = new HashMap<>(labels);
        labelsCopy.put("jenkins.io/credentials-type", "usernamePassword");

        return new SecretBuilder()
                .withNewMetadata()
                .withNamespace("test")
                .withName(name)
                .addToLabels(labelsCopy)
                .addToAnnotations(annotations)
                .endMetadata()
                .addToData("username", "bXlVc2VybmFtZQ==")
                .addToData("password", "UGEkJHdvcmQ=")
                .build();
    }

    @Test
    void startWatchingForSecretsKubernetesClientException() {
        KubernetesCredentialProvider provider = new MockedKubernetesCredentialProvider();
        provider.startWatchingForSecrets();
        assertEquals(1, getInitAdministrativeMonitorCount(), "expect administrative error");
        provider.startWatchingForSecrets();
        assertEquals(1, getInitAdministrativeMonitorCount(), "expect at most 1 administrative error");

        // enable default responses
        defaultMockKubernetesResponses();
        // restart with success should clear errors
        provider.startWatchingForSecrets();
        // verify we schedule reconnect task
        ArgumentCaptor<Runnable> reconnectTask = ArgumentCaptor.forClass(Runnable.class);
        verify(jenkinsTimer, times(2)).schedule(reconnectTask.capture(), eq(5L), eq(TimeUnit.MINUTES));
        reconnectTask.getValue().run();
        assertEquals(0, getInitAdministrativeMonitorCount(), "expect administrative error to be cleared");
    }

    private long getInitAdministrativeMonitorCount() {
        return AdministrativeMonitor.all().stream()
                .filter(am -> am.id.equals(MockedKubernetesCredentialProvider.class.getName() + ".initialize"))
                .count();
    }

    @Test
    void restartWatchOnCloseException() throws Exception {
        defaultMockKubernetesResponses();
        KubernetesCredentialProvider provider = new MockedKubernetesCredentialProvider();
        provider.startWatchingForSecrets();
        provider.onClose(new WatcherException("test exception"));
        // expect 2 requests to list
        assertRequestCount("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type", 2);
    }

    @Test
    void noRestartWatchOnCloseNormal() throws Exception {
        defaultMockKubernetesResponses();
        KubernetesCredentialProvider provider = new MockedKubernetesCredentialProvider();
        provider.startWatchingForSecrets();

        provider.onClose(null);
        // expect 1 requests to list
        assertRequestCount("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type", 1);
    }

    @Test
    void startWatchingWithCustomLabelSelectors() throws InterruptedException {
        try {
            System.setProperty(KubernetesCredentialProvider.LABEL_SELECTOR, "env in (iat uat)");
            mockKubernetesResponses("jenkins.io%2Fcredentials-type%2Cenv%20in%20%28iat%20uat%29");
            KubernetesCredentialProvider provider = new MockedKubernetesCredentialProvider();
            provider.startWatchingForSecrets();

            assertRequestCount("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type%2Cenv%20in%20%28iat%20uat%29", 1);
            assertRequestCountAtLeast("/api/v1/namespaces/test/secrets?allowWatchBookmarks=true&labelSelector=jenkins.io%2Fcredentials-type%2Cenv%20in%20%28iat%20uat%29&resourceVersion=1&watch=true", 1);
        } finally {
            System.clearProperty(KubernetesCredentialProvider.LABEL_SELECTOR);
        }
    }

    @Test
    void startWatchingWithCustomLabelException() {
        try {
            System.setProperty(KubernetesCredentialProvider.LABEL_SELECTOR, "partition  in");
            KubernetesCredentialProvider provider = new MockedKubernetesCredentialProvider();
            provider.startWatchingForSecrets();
            assertEquals(1, getLabelSelectorAdministrativeMonitorCount(), "expect administrative error");

            System.setProperty(KubernetesCredentialProvider.LABEL_SELECTOR, "env in (iat uat)");
            // enable default responses
            mockKubernetesResponses("jenkins.io%2Fcredentials-type%2Cenv%20in%20%28iat%20uat%29");
            // restart with success should clear errors
            provider.startWatchingForSecrets();
            assertEquals(0, getLabelSelectorAdministrativeMonitorCount(), "expect administrative error to be cleared");
        } finally {
            System.clearProperty(KubernetesCredentialProvider.LABEL_SELECTOR);
        }
    }

    private static long getLabelSelectorAdministrativeMonitorCount() {
        return AdministrativeMonitor.all().stream()
                .filter(am -> am.id.equals(MockedKubernetesCredentialProvider.class.getName() + ".labelSelector"))
                .count();
    }

    private void assertRequestCount(String path, long count) throws InterruptedException {
        assertEquals(count, requestCount(path), path);
    }

    private void assertRequestCountAtLeast(String path, long count) throws InterruptedException {
        assertTrue(requestCount(path) >= count, path + " >= " + count);
    }

    private long requestCount(String path) throws InterruptedException {
        return getRequests().stream()
                .filter(r -> r.getPath().equals(path))
                .count();
    }

    private List<RecordedRequest> getRequests() throws InterruptedException {
        int count = server.getRequestCount();
        List<RecordedRequest> requests = new LinkedList<>();
        while (count-- > 0) {
            requests.add(server.takeRequest(30L, TimeUnit.SECONDS));
        }
        return requests;
    }

    private class MockedKubernetesCredentialProvider extends KubernetesCredentialProvider {
        @Override
        KubernetesClient getKubernetesClient() {
            return client;
        }
    }
}
