package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import hudson.model.AdministrativeMonitor;
import io.fabric8.kubernetes.api.model.EventListBuilder;
import io.fabric8.kubernetes.api.model.SecretListBuilder;
import io.fabric8.kubernetes.api.model.StatusBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.server.mock.KubernetesServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

public class KubernetesCredentialsProviderTest {

    public @Rule JenkinsRule jenkins = new JenkinsRule();
    public @Rule KubernetesServer server = new KubernetesServer();

    private void defaultMockKubernetesResponses() {
        server.expect().withPath("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type")
                .andReturn(200, new SecretListBuilder().build()).always();
        server.expect().withPath("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type&watch=true")
                .andReturn(200, new EventListBuilder().build()).always();
    }

    @Test
    public void startWatchingForSecretsKubernetesClientException() throws IOException {
        // shutdown mock server to test client exception
       server.expect().withPath("/api/v1/namespaces/test/secrets?labelSelector=jenkins.io%2Fcredentials-type")
               .andReturn(403, new StatusBuilder()
                .withCode(403)
               .withMessage("Unauthorized")
               .build()).once();

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
