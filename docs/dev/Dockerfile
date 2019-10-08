FROM jenkins/jenkins:lts
RUN /usr/local/bin/install-plugins.sh credentials plain-credentials variant docker-slaves github-branch-source kubernetes-client-api
COPY target/kubernetes-credentials-provider.hpi /usr/share/jenkins/ref/plugins/kubernetes-credentials-provider.jpi
