<!--- Deploy -->

# Deploy helm chart

## Introduction

This chart bootstraps a deployment on a [Kubernetes](https://kubernetes.io) cluster using [Helm](https://helm.sh) package manager.

## Prerequisites

The code was tested on **Kubernetes cluster** (v1.21.11) with **Istio** (1.12.6)
> It is possible to use other versions, but it hasn't been tested

### Operation system

The code works in Debian-based Linux (Debian 10 and Ubuntu 20.04) and Windows WSL 2. Also, it works but is not guaranteed in Google Cloud Shell. All other operating systems, including macOS, are not verified and supported.

### Packages

Packages are only needed for installation from a local computer.

* **HELM** (version: v3.7.1 or higher) [helm](https://helm.sh/docs/intro/install/)
* **Kubectl** (version: v1.21.0 or higher) [kubectl](https://kubernetes.io/docs/tasks/tools/#kubectl)

## Installation

Before installing deploy Helm chart you need to install [configmap Helm chart](../configmap).
First you need to set variables in **values.yaml** file using any code editor. Some of the values are prefilled, but you need to specify some values as well. You can find more information about them below.

### Configmap variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**data.logLevel** | logging level | string | INFO | yes
**data.entitlementsHost** | entitlements service host | string | "http://entitlements" | yes
**data.partitionHost** | partition service host | string | "http://partition" | yes
**data.springProfilesActive** | active spring profile | string | gcp | yes
**data.cloudTaskTargetHost** | your target host | string | - | yes
**data.googleAudiences** | your GCP [client ID](https://console.cloud.google.com/apis/credentials) | string | - | yes
**data.googleCloudProject** | your GCP project ID | string | - | yes
**data.googleCloudProjectRegion** | your GCP project region | string | - | yes
**data.indexerQueueIdentifier** | config for cloud tasks queue | string | - | yes
**data.indexerHost** | indexer service host | string | "http://indexer" | yes

### Deployment variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**data.requestsCpu** | amount of requested CPU | string | 0.1 | yes
**data.requestsMemory** | amount of requested memory| string | 384M | yes
**data.limitsCpu** | CPU limit | string | 1 | yes
**data.limitsMemory** | memory limit | string | 1G | yes
**data.image** | service image | string | - | yes
**data.imagePullPolicy** | when to pull image | string | IfNotPresent | yes
**data.serviceAccountName** | name of your service account | string | indexer-queue | yes

### Config variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**conf.appName** | name of the app | string | indexer-queue | yes
**conf.configmap** | configmap to be used | string | indexer-queue-config | yes
**conf.keycloakSecretName** | secret for keycloak | string | indexer-queue-keycloak-secret | yes
**conf.rabbitmqSecretName** | secret for rabbitmq | string | rabbitmq-secret | yes
**conf.onPremEnabled** | whether on-prem is enabled | boolean | false | yes
**conf.domain** | your domain | string | - | yes


### ISTIO variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**istio.proxyCPU** | CPU request for Envoy sidecars | string | 50m | yes
**istio.proxyCPULimit** | CPU limit for Envoy sidecars | string | 500m | yes
**istio.proxyMemory** | memory request for Envoy sidecars | string | 64Mi | yes
**istio.proxyMemoryLimit** | memory limit for Envoy sidecars | string | 512Mi | yes
**istio.bootstrapProxyCPU** | CPU request for Envoy sidecars | string | 10m | yes
**istio.bootstrapProxyCPULimit** | CPU limit for Envoy sidecars | string | 100m | yes

### Install the helm chart

Run this command from within this directory:

```console
helm install gcp-indexer-queue-deploy .
```

## Uninstalling the Chart

To uninstall the helm deployment:

```console
helm uninstall gcp-indexer-queue-deploy
```

[Move-to-Top](#deploy-helm-chart)
