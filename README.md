# kubernetes-credentials-provider-plugin

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/kubernetes-credentials-provider.svg)](https://plugins.jenkins.io/kubernetes-credentials-provider)
[![GitHub release](https://img.shields.io/github/release/jenkinsci/kubernetes-credentials-provider-plugin.svg?label=release)](https://github.com/jenkinsci/kubernetes-credentials-provider-plugin/releases/latest)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/kubernetes-credentials-provider.svg?color=blue)](https://plugins.jenkins.io/kubernetes-credentials-provider)

Complete documentation on GitHub page is [here](https://jenkinsci.github.io/kubernetes-credentials-provider-plugin/)

The *Kubernetes Credentials Provider* is a [Jenkins](https://jenkins.io) plugin to enable the retreival of [Credentials](https://plugins.jenkins.io/credentials) directly from Kubernetes.

The plugin supports most common credential types and defines an [`extension point`](https://jenkins.io/doc/developer/extensions/kubernetes-credentials-provider/) that can be implemented by other plugins to add support for custom Credential types. 

# Using

### Pre-requisites

- Jenkins must be running in a kubernetes cluster
- The pod running Jenkins must have a service account with a role that sets the following:
  - get/watch/list permissions for `secrets` [¹](#footnotes)


Because granting these permissions for secrets is not something that should be done lightly it is highly advised for security reasons that you both create a unique service account to run Jenkins as, and run Jenkins in a unique namespace.

## Managing credentials

### Adding credentials

Credentials are added by adding them as secrets to Kubernetes, this is covered in more detail in the [examples](https://jenkinsci.github.io/kubernetes-credentials-provider-plugin/examples) page.

To restrict the secrets added by this plugin use the system property `com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.KubernetesCredentialProvider.labelSelector`
to set the [Kubernetes Label selector](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors) expression.

```
-Dcom.cloudbees.jenkins.plugins.kubernetes_credentials_provider.KubernetesCredentialProvider.labelSelector="env in (iat uat)"
```

### Updating credentials

Credentials are updated automatically when changes are made to the Kubernetes secret.

### Deleting credentials

Credentials are deleted automatically when the secret is deleted from Kubernetes. 

### Viewing credentials

Once added the credentials will be visible in Jenkins under the `/credentials/` page.
Any credentials that are loaded from Kubernetes can be identified by the Kubernetes provider icon in the view.

## Using the credentials inside Jenkins

To use credentials in a pipeline you do not need to do anything special, you access them just as you would for credentials stored in Jenkins. 

for example, if you had the follwing Secret defined in Kubernetes:
<!-- docs/examples/username-pass.yaml -->
```yaml
apiVersion: v1
kind: Secret
metadata:
# this is the jenkins id.
  name: "another-test-usernamepass"
  labels:
# so we know what type it is.
    "jenkins.io/credentials-type": "usernamePassword"
  annotations:
# description - can not be a label as spaces are not allowed
    "jenkins.io/credentials-description" : "credentials from Kubernetes"
type: Opaque
stringData:
  username: myUsername
  password: 'Pa$$word'
```

you could use it via the [Credentials Binding plugin](https://plugins.jenkins.io/credentials-binding) 

```groovy
withCredentials([usernamePassword(credentialsId: 'another-test-usernamepass',
                                  usernameVariable: 'USER', 
                                  passwordVariable: 'PASS')]) {
  sh 'curl -u $USER:$PASS https://some-api/'
}
```

or by passing the credentialId directly to the step requiring a credential:

```groovy
git credentialsId: 'another-test-usernamepass', url: 'https://github.com/foo/bar'
```

# Issue reporting

Any issues should be reporting in the main [Jenkins JIRA tracker](https://issues.jenkins-ci.org).
The issue tracker is not a help forum, for help please use [IRC](https://jenkins.io/chat/) or the [user mailing list](https://groups.google.com/forum/#!forum/jenkinsci-users) 

# Releases and Change logs

The [release notes](https://github.com/jenkinsci/kubernetes-credentials-provider-plugin/releases) are managed in GitHub. 
The latest release will be visible in the Jenkins Update center approximatly 8 hours after a release.

# Developing

This [page](./docs/dev/) contains more information on a developer environment.

# Footnotes

1: it is reported that running in KOPS on AWS you will also need permissions to get/watch/list `configmaps`
