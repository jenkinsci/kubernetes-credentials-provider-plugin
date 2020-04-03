---
layout: default
title:  "Kubernetes Credentials Provider Plugin : Examples"
permalink: /examples/
---

# Credential Examples

Credentials are added and updated by adding/updating them as secrets to Kubernetes.
The format of the Secret is different depending on the type of credential you wish to expose, but will all have several things in common:

- the label  `"jenkins.io/credentials-type"` with a type that is known to the plugin (e.g. `certificate`, `secretFile`, `secretText`, `usernamePassword`, `basicSSHUserPrivateKey`, `aws`, 'openstackCredentialv3', 'kubeconfig')
- an annotation for the credential description: `"jenkins.io/credentials-description" : "certificate credential from Kubernetes"`

To add or update a Credential just execute the command `kubectl apply -f <nameOfFile.yaml>`

The raw yaml for the following examples can be found in the GitHub [repository](https://github.com/jenkinsci/kubernetes-credentials-provider-plugin/tree/master/docs/examples)

Where Strings are encoded using base64 the bytes encoded should be from the UTF-8 representation of the String.

## UserName / Password credentials

The UserName password credentials are probably the most commonly uses.

{% highlight yaml linenos %}
{% include_relative username-pass.yaml %}
{% endhighlight %}

## Secret Text

{% highlight yaml linenos %}
{% include_relative secretText.yaml %}
{% endhighlight %}

## Secret File

{% highlight yaml linenos %}
{% include_relative secretFile.yaml %}
{% endhighlight %}

## Certificates

{% highlight yaml linenos %}
{% include_relative certificate.yaml %}
{% endhighlight %}

## Basic SSH Private Key

Without passphrase:
{% highlight yaml linenos %}
{% include_relative basic-ssh-username-private-key.yaml %}
{% endhighlight %}

With passphrase:
{% highlight yaml linenos %}
{% include_relative basic-ssh-username-private-key-passphrase.yaml %}
{% endhighlight %}

## AWS Credentials

Only AWS AccessKey and SecretKey:
{% highlight yaml linenos %}
{% include_relative aws-credentials-access-keys.yaml %}
{% endhighlight %}

## Openstack Credential v3

{% highlight yaml linenos %}
{% include_relative openstack-credential-v3.yaml %}
{% endhighlight %}

## Kubeconfig Crendentials

{% highlight yaml linenos %}
{% include_relative kubeconfig.yaml %}
{% endhighlight %}

# Custom field mapping

Sometimes you may want the secret to be able to be consumed by another tool as well that has a different requirement for the data fields.
In order to facilitate this the plugin supports the remapping fields.
In order to achieve this you add an attribute begining with `jenkins.io/credentials-keybinding-` and ending with the normal field name and having the value of the new field name.
The following example remaps the `username` and `password` fields to `user` and `pass`:
{% highlight yaml linenos %}
{% include_relative username-pass-with-custom-mapping.yaml %}
{% endhighlight %}
