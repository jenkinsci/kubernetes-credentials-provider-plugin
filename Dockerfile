FROM jenkins/jenkins:lts

RUN /usr/local/bin/install-plugins.sh credentials:2.1.16 credentials-binding:1.15
COPY target/kubernetes-credential-provider.hpi /usr/share/jenkins/ref/plugins/kubernetes-credential-provider.jpi
