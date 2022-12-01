<!--- Configmap -->

# Configmap helm chart

## Introduction

This chart bootstraps a configmap deployment on a [Kubernetes](https://kubernetes.io) cluster using [Helm](https://helm.sh) package manager.

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

First you need to set variables in **values.yaml** file using any code editor. Some of the values are prefilled, but you need to specify some values as well. You can find more information about them below.

### Common variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**logLevel** | logging level | string | INFO | yes
**entitlementsHost** | entitlements service host | string | "http://entitlements" | yes
**partitionHost** | partition service host | string | "http://partition" | yes
**springProfilesActive** | active spring profile | string | gcp | yes

### GCP variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**cloudTaskTargetHost** | your target host | string | - | yes
**googleAudiences** | your GCP [client ID](https://console.cloud.google.com/apis/credentials) | string | - | yes
**googleCloudProject** | your GCP project ID | string | - | yes
**googleCloudProjectRegion** | your GCP project region | string | - | yes
**indexerQueueIdentifier** | config for cloud tasks queue | string | - | yes

### Anthos variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**indexerHost** | indexer service host | string | "http://indexer" | yes

### Config variables

| Name | Description | Type | Default |Required |
|------|-------------|------|---------|---------|
**appName** | name of the app | string | indexer-queue | yes
**configmap** | configmap to be used | string | indexer-queue-config | yes
**onPremEnabled** | whether on-prem is enabled | boolean | false | yes

### Install the helm chart

Run this command from within this directory:

```console
helm install gcp-indexer-queue-configmap .
```

## Uninstalling the Chart

To uninstall the helm deployment:

```console
helm uninstall gcp-indexer-queue-configmap
```

[Move-to-Top](#configmap-helm-chart)
