package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.convertors.UsernamePasswordCredentialsConvertor;
import com.cloudbees.plugins.credentials.common.UsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl;
import hudson.ExtensionList;
import hudson.model.AdministrativeMonitor;
import hudson.model.ItemGroup;
import hudson.security.ACL;
import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import jenkins.model.Jenkins;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ExtensionList.class})
@PowerMockIgnore({"okhttp3.*", "io.fabric8.*"})
public class KubernetesCredentialsProviderTest {

    public @Rule KubernetesServer server = new KubernetesServer();

    @Before
    public void setUp() {
        // mocked to validate add/remove of administrative errors
        ExtensionList<AdministrativeMonitor> monitors = ExtensionList.create((Jenkins) null, AdministrativeMonitor.class);
        // mocked to validate start watching for secrets
        ExtensionList<SecretToCredentialConverter> converters = ExtensionList.create((Jenkins) null, SecretToCredentialConverter.class);
        converters.addAll(Collections.singleton(new UsernamePasswordCredentialsConvertor()));
        PowerMockito.mockStatic(ExtensionList.class);
        PowerMockito.when(ExtensionList.lookup(AdministrativeMonitor.class)).thenReturn(monitors);
        PowerMockito.when(ExtensionList.lookup(SecretToCredentialConverter.class)).thenReturn(converters);
    }

    private void defaultMockKubernetesResponses() {
        server.expect().withPath("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type")
                .andReturn(200, new SecretListBuilder().build()).always();
        server.expect().withPath("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type&watch=true")
                .andReturn(200, new EventListBuilder().build()).always();
    }

    @Test
    public void startWatchingForSecrets() {
        Secret s1 = createSecret("s1");
        Secret s2 = createSecret("s2");
        Secret s3 = createSecret("s3");

        // returns s1 and s3, the credentials map should be reset to this list
        server.expect().withPath("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type")
                .andReturn(200, new SecretListBuilder()
                        .addToItems(s1, s3)
                        .build())
                .once();

        // expect the s2 will get dropped when the credentials map is reset to the full list
        server.expect().withPath("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type&watch=true")
                .andReturnChunked(200, new WatchEvent(s1, "ADDED"), new WatchEvent(s2, "ADDED"))
                .once();
        server.expect().withPath("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type&watch=true")
                .andReturn(200, null)
                .always();

        KubernetesCredentialProvider provider = new MockedKubernetesCredentialProvider();
        provider.startWatchingForSecrets();

        List<UsernamePasswordCredentials> credentials = provider.getCredentials(UsernamePasswordCredentials.class, (ItemGroup) null, ACL.SYSTEM);
        assertEquals("credentials", 2, credentials.size());
        assertTrue("secret s1 exists", credentials.stream().anyMatch(c -> "s1".equals(((UsernamePasswordCredentialsImpl) c).getId())));
        assertTrue("secret s3 exists", credentials.stream().anyMatch(c -> "s3".equals(((UsernamePasswordCredentialsImpl) c).getId())));
    }

    private Secret createSecret(String name) {
        return new SecretBuilder()
                .withNewMetadata()
                .withNamespace("test")
                .withName(name)
                .addToLabels("jenkins.io/credentials-type", "usernamePassword")
                .endMetadata()
                .addToData("username", "bXlVc2VybmFtZQ==")
                .addToData("password", "UGEkJHdvcmQ=")
                .build();
    }

    @Test
    public void startWatchingForSecretsKubernetesClientException() throws IOException {
        KubernetesCredentialProvider provider = new MockedKubernetesCredentialProvider();
        provider.startWatchingForSecrets();
        assertEquals("expect administrative error", 1, getInitAdministrativeMonitorCount());

        // enable default responses
        defaultMockKubernetesResponses();
        // restart with success should clear errors
        provider.startWatchingForSecrets();
        assertEquals("expect administrative error to be cleared", 0, getInitAdministrativeMonitorCount());
    }

    private long getInitAdministrativeMonitorCount() {
        return AdministrativeMonitor.all().stream()
                .filter(am -> am.id.equals(MockedKubernetesCredentialProvider.class.getName() + ".initialize"))
                .count();
    }

    @Test
    public void restartWatchOnCloseException() throws Exception {
        defaultMockKubernetesResponses();
        KubernetesCredentialProvider provider = new MockedKubernetesCredentialProvider();
        provider.startWatchingForSecrets();
        provider.onClose(new KubernetesClientException("test exception"));
        // expect 2 requests to list
        assertRequestCount("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type", 2);
    }

    @Test
    public void noRestartWatchOnCloseNormal() throws Exception {
        defaultMockKubernetesResponses();
        KubernetesCredentialProvider provider = new MockedKubernetesCredentialProvider();
        provider.startWatchingForSecrets();

        provider.onClose(null);
        // expect 1 requests to list
        assertRequestCount("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type", 1);
    }

    private void assertRequestCount(String path, long count) throws InterruptedException {
        long actual = getRequests().stream()
                .filter(r -> r.getPath().equals(path))
                .count();
        assertEquals(path, count, actual);
    }

    private List<RecordedRequest> getRequests() throws InterruptedException {
        int count = server.getMockServer().getRequestCount();
        List<RecordedRequest> requests = new LinkedList<>();
        while (count-- > 0) {
            requests.add(server.getMockServer().takeRequest());
        }
        return requests;
    }

    private class MockedKubernetesCredentialProvider extends KubernetesCredentialProvider {
        @Override
        KubernetesClient getKubernetesClient() {
            return server.getClient();
        }
    }
}
