# Helpful settings for testing

The plugin will run if you have configured kubectl locally (and have the permissions required to watch/list/read secrets).  
For full integration testing then this directory contains some files useful for *developer* testing the plugin inside a Jenkins running inside a kubernetes (tested with GKE).

They may require some small tweaks for your environment (as it will be different to mine), but if so please don't attempt to commit them back :-)

## Testing

### Initial setup...

1. Create the testing namespace  `kubectl apply -f testing-namespace.yaml`
2. create a service user to run Jenkins  `kubectl apply -f service-account.yaml`
3. Create a role to allow secret reading `kubectl apply -f secret-reader-role.yaml`
4. Create a role binding to bind the role to the service user. `kubectl apply -f secret-reader-role-binding.yaml`

### Deploying / upgrading Jenkins / plugin

1. Build the plugin (mvn verify)
2. build the docker image  (`docker build .. -f Dockerfile -t jenkins-k8s-creds:latest`)
3. push the docker image to the docker repo (specified in the app yaml)
   ```
      docker tag [SOURCE_IMAGE] [HOSTNAME]/[PROJECT-ID]/[IMAGE][:TAG]
      docker push [HOSTNAME]/[PROJECT-ID]/[IMAGE][:TAG]
   ```
   e.g
   ```
      docker tag jenkins-k8s-creds:latest eu.gcr.io/myproject/jenkins-k8s-creds:latest
      docker push eu.gcr.io/myproject/jenkins-k8s-creds:latest
      
   ```
4. deploy the application  `kubectl apply -f jenkins-kube-creds.yaml`
5. deploy service so that Jenkins is exposed (optional and one time only)  `kubectl apply -f service.yaml`

Note: [this page](https://cloud.google.com/container-registry/docs/pushing-and-pulling) is useful for setting up auth to push to GKE