# kubernetes-credentials-provider-plugin
Credentials provider for Kubernetes

## Pre-requisites

- Jenkins must be running in a kubernetes cluster
- The pod running Jenkins must have a service account with a role that sets the following:
  - get/watch/list permissions for *configmaps* and *secrets*

## Example kubernetes credentials configuration files
There are examples of the supported types in [docs/examples](docs/examples)

## Using kubernetes credentials in a Jenkins pipeline

Both examples use a the following credentials configuration file https://github.com/jenkinsci/kubernetes-credentials-provider-plugin/blob/master/docs/examples/username-pass.yaml

    ```withCredentials([usernamePassword(credentialsId: 'another-test-usernamepass', passwordVariable: 'bar', usernameVariable: 'foo')]) {
        // username: env.foo
        // password: echo env.bar
    }```
    
or
    
    ```git credentialsId: 'another-test-usernamepass', url: '<repository here>'```
