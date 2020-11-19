# os-indexer-queue-azure

os-indexer-queue-azure is an [Azure Function](https://docs.microsoft.com/en-us/azure/azure-functions/) service that is woken up in response to messages emitted by `os-storage-azure` onto Service Bus. It is responsible for calling the `os-indexer` to trigger re-indexing events.

## Running Locally

### Requirements

In order to run this service locally, you will need the following:

- [Maven 3.6.0+](https://maven.apache.org/download.cgi)
- [AdoptOpenJDK8](https://adoptopenjdk.net/)
- [Docker](https://www.docker.com/)
- Infrastructure dependencies, deployable through the relevant [infrastructure template](https://dev.azure.com/slb-des-ext-collaboration/open-data-ecosystem/_git/infrastructure-templates?path=%2Finfra&version=GBmaster&_a=contents)
- While not a strict dependency, example commands in this document use [bash](https://www.gnu.org/software/bash/)

### General Tips

**Environment Variable Management**

Because this service is Dockerized, you will want to keep all environment variables inside of a file called `.env`

**Lombok**

This project uses [Lombok](https://projectlombok.org/) for code generation. You may need to configure your IDE to take advantage of this tool.
 - [Intellij configuration](https://projectlombok.org/setup/intellij)
 - [VSCode configuration](https://projectlombok.org/setup/vscode)


### Environment Variables

In order to run the service locally, you will need to have the following environment variables defined in your `.env` file.

**Note** The following command can be useful to pull secrets from keyvault:
```bash
az keyvault secret show --vault-name $KEY_VAULT_NAME --name $KEY_VAULT_SECRET_NAME --query value -otsv
```

**Required to run service**

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `REGISTRY` | `localhost:5000` | Container registry endpoint. Localhost is fine for local development | no | - |
| `VSTS_FEED_TOKEN` | `********` | Access token that grants access to Maven repository | yes | Azure DevOps web UI |
| `SUBSCRIPTION_NAME` | ex `foosubscription` | Azure subscription name | no | - |
| `STORAGE_CONNECTION_STRING` | `********` | Azure storage connection string | yes | output of infrastructure deployment |
| `SERVICE_BUS_ENDPOINT` | `********` | Service Bus connection string | yes | output of infrastructure deployment |
| `TOPIC_NAME` | `recordstopic` | Service Bus topic to listen on | yes | output of infrastructure deployment |
| `INDEXER_WORKER_URL` | ex `https://indexer.azurewebsites.net` | Indexer endpoint | no | output of infrastructure deployment |
| `azure.application-insights.instrumentation-key` | `********` | Instrumentation key of the associated application insights resource | yes | output of infrastructure deployment | 

### Configure Maven

Check that maven is installed:
```bash
$ mvn --version
Apache Maven 3.6.0
Maven home: /usr/share/maven
Java version: 1.8.0_212, vendor: AdoptOpenJDK, runtime: /usr/lib/jvm/jdk8u212-b04/jre
...
```

You will need to configure access to the remote maven repository that holds the OSDU dependencies. This file should live within `~/.m2/settings.xml`:
```bash
$ cat ~/.m2/settings.xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>os-core</id>
            <username>mvn-pat</username>
            <!-- Treat this auth token like a password. Do not share it with anyone, including Microsoft support. -->
            <!-- The generated token expires on or before 11/14/2019 -->
            <password>$PERSONAL_ACCESS_TOKEN_GOES_HERE</password>
        </server>
    </servers>
</settings>
```

### Build and run the application

In order to build the application, you can run the following commands. These commands should be run from the same folder as this file.

> Note: `os-indexer-queue-azure` listens for messages on a Service Bus subscription. If you are running this service locally,
> be aware that there is likely an Azure deployed version of this listening on the same subscription. Take care to do one
> of the following when testing locally:
>   - Stop the `os-indexer-queue-azure` function in the Azure portal and run it locally. You'll need to remember to restart the Azure deployed `os-indexer-queue-azure` when you are finished testing
>   - Deploy your own infrastructure stack and configure all the services *except* `os-indexer-queue-azure`

```bash
cp .env.template .env
vim .env # configure as needed

# Build the docker image and start it
docker-compose build
docker-compose up -d

# Stop and Remove the image
docker-compose kill
docker-compose rm
```

### Test the application

As of today, this Azure Function has no integration tests.

## Debugging

Jet Brains - the authors of Intellij IDEA, have written an [excellent guide](https://www.jetbrains.com/help/idea/debugging-your-first-java-application.html) on how to debug java programs.

## Deploying service to Azure

Service deployments into Azure are standardized to make the process the same for all services if using ADO and are closely related to the infrastructure deployed. The steps to deploy into Azure can be [found here](https://github.com/azure/osdu-infrastructure)

Note: The pipeline for `os-indexer-queue-azure` is slightly different than noted above because it is an Azure Function.
The correct pipeline for Azure is [azure-pipeline.yml](./azure-pipeline.yml).

### Manual Deployment Steps

__Environment Settings__

The following environment variables are necessary to properly deploy a service to an Azure OSDU Environment.

```bash
# Group Level Variables
export AZURE_TENANT_ID=""
export AZURE_SUBSCRIPTION_ID=""
export AZURE_SUBSCRIPTION_NAME=""
export AZURE_PRINCIPAL_ID=""
export AZURE_PRINCIPAL_SECRET=""
export AZURE_APP_ID=""
export AZURE_BASENAME_21=""
export AZURE_BASENAME=""
export AZURE_BASE=""
export AZURE_STORAGE_ACCOUNT=""
export AZURE_NO_ACCESS_ID=""

# Required for Azure Deployment
export AZURE_CLIENT_ID="${AZURE_PRINCIPAL_ID}"
export AZURE_CLIENT_SECRET="${AZURE_PRINCIPAL_SECRET}"
export AZURE_RESOURCE_GROUP="${AZURE_BASENAME}-osdu-r2-app-rg"
export AZURE_FUNCTIONAPP_NAME="${AZURE_BASENAME}-enque"
export AZURE_CONTAINER_REGISTRY="${AZURE_BASE}cr"

```

__Azure Service Deployment__

1. Log in to the Azure CLI
  `az login --service-principal -u $AZURE_PRINCIPAL_ID -p $AZURE_PRINCIPAL_SECRET --tenant $AZURE_TENANT_ID`

2. Log in to the ACR Registry
  `az acr login -n $AZURE_CONTAINER_REGISTRY`

3. Build the Docker Image
  `docker-compose build`

4. Tag the Image to the Container Registry
  `docker tag indexer-enqueue:latest ${AZURE_CONTAINER_REGISTRY}.azurecr.io/indexer-enqueue:latest`

5. Push the Image to the Container Registry
  `docker push ${AZURE_CONTAINER_REGISTRY}.azurecr.io/indexer-enqueue:latest`

6. Deploy the Function App

```bash
az functionapp config container set \
        --docker-custom-image-name ${AZURE_CONTAINER_REGISTRY}.azurecr.io/indexer-enqueue:latest \
        --name $AZURE_FUNCTIONAPP_NAME \
        --resource-group $AZURE_RESOURCE_GROUP \
        -ojsonc
```

## License
Copyright © Microsoft Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
