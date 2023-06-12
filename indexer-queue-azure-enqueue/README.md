# os-indexer-queue-azure

os-indexer-queue-azure is a spring boot service that is woken up in response to messages emitted by `os-storage-azure` onto Service Bus. It is responsible for calling the `os-indexer` to trigger re-indexing events.

## Running Locally

### Requirements

In order to run this service locally, you will need the following:

- [Maven 3.6.0+](https://maven.apache.org/download.cgi)
- [AdoptOpenJDK8](https://adoptopenjdk.net/)

### General Tips

**Environment Variable Management**

The following tools make environment variable configuration simpler


* [direnv](https://direnv.net/) - for a shell/terminal environment
* [EnvFile](https://plugins.jetbrains.com/plugin/7861-envfile) - for [Intellij IDEA](https://www.jetbrains.com/idea/)


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
| `azure_servicebus_topic_name` | `recordstopic` | Service Bus topic to listen on | no | output of infrastructure deployment |
| `azure_servicebus_topic_subscription` | `recordstopicsubscription` | Service Bus subscription to listen from | no | output of infrastructure deployment |
| `azure_reindex_topic_name` | `reindextopic` | Re-index topic to listen on | no | output of infrastructure deployment |
| `azure_reindex_topic_subscription` | `reindextopicsubscription` | Re-index subscription to listen from | no | output of infrastructure deployment |
| `indexer_worker_url` | ex `https://indexer.azurewebsites.net/api/indexer/v2/_dps/task-handlers/index-worker` | Indexer endpoint | no | output of infrastructure deployment |
| `azure_application_insights_instrumentation_key` | `********` | Instrumentation key of the associated application insights resource | yes | output of infrastructure deployment |
| `AZURE_TENANT_ID` | `*******` | AD tenant to authenticate users from | yes | keyvault secret: `$KEYVAULT_URI/secrets/app-dev-sp-tenant-id` |
| `AZURE_CLIENT_ID` | `******` | Identity to run the service locally. This enables access to Azure resources. You only need this if running locally | yes | keyvault secret: `$KEYVAULT_URI/secrets/app-dev-sp-username` |
| `AZURE_CLIENT_SECRET` | `******` | Secret for `$AZURE_CLIENT_ID` | yes | keyvault secret: `$KEYVAULT_URI/secrets/app-dev-sp-password` |
| `KEYVAULT_URI` | ex `https://foo-keyvault.vault.azure.net/` | URI of KeyVault that holds application secrets | no | output of infrastructure deployment |
| `AZURE_APP_RESOURCE_ID` | `******` | AAD client application ID | yes | output of infrastructure deployment |
| `aad_client_id` | `*****` | | yes | output of infrastructure deployment |
| `partition_api` | ex `https://partition.azurewebsites.net/api/partition/v1` | partition service endpoint | no | |
| `executor_n_threads` | 32 | Max no of threads used concurrently  | no | | 
| `max_concurrent_calls` | 32 | Max no of concurrent calls to service bus | no | | 
| `max_lock_renew_duration_seconds` | 600 | Message lock will be released after this duration | no | | 
| `max_delivery_count` | 5 | Man no of times service bus re-tries a message before dead-lettering it | no | | 
| `azure_istioauth_enabled` | `true` (depends on if service is running in Kubernetes environment with Istio installed) | Configuring use of Istio | no | Set to false if running locally | 
| `server_port` | 8080 | | | |

**Required to run integration test**

| name | value | description | 
| ---  | ---   | ---         | 
| `STORAGE_URL` | ex: `https://storage.azurewebsites.net/api/storage/v2`| storage service endpoint | 
| `LEGAL_URL` | ex: `https://legal.azurewebsites.net/api/legal/v1/` | | | |
| `SEARCH_URL` | ex: `https://search.azurewebsites.net/api/search/v2/` | | | |
| `TENANT_NAME` | ex: `opendes` | OSDU tenant used for testing |
| `AZURE_AD_TENANT_ID` | `*****` | AD tenant to authenticate users from |
| `INTEGRATION_TESTER` | `*****`| System identity to assume for API calls. Note: this user must have entitlements configured already |
| `TESTER_SERVICEPRINCIPAL_SECRET` | `*****` | Secret for `$INTEGRATION_TESTER` |
| `AZURE_AD_APP_RESOURCE_ID` | `*****` | AAD client application ID |
| `NO_DATA_ACCESS_TESTER` | `*****` | Service principal ID of a service principal without entitlements |
| `NO_DATA_ACCESS_TESTER_SERVICEPRINCIPAL_SECRET` | `*****` | Secret for `$NO_DATA_ACCESS_TESTER` |
| `DOMAIN` | ex: `contoso.com` | domain name |

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

Build and run the application
After configuring your environment as specified above, you can follow these steps to build and run the application. These steps should be invoked from the repository root.

# build + test + package azure service code
$ cd indexer-queue-azure-enqueue && mvn clean package

# run service
#
# Note: this assumes that the environment variables for running the service as outlined
#       above are already exported in your environment.
$ mvn spring-boot:run -Dspring-boot.run.profiles=local   
# or directly run the jar file  
$ cd indexer-queue-azure-enqueue && java -jar target\indexer-queue-azure-enqueue-1.0.0-spring-boot.jar

### Test the application
```# build + install integration test core
   $ (cd testing/indexer-queue-azure-enqueue/ && mvn clean install)
   
   # build + run Azure integration tests.
   #
   # Note: this assumes that the environment variables for integration tests as outlined above are already exported in your environment.
   $ (cd testing/indexer-queue-azure-enqueue/ && mvn clean test)
```
## Debugging

Jet Brains - the authors of Intellij IDEA, have written an [excellent guide](https://www.jetbrains.com/help/idea/debugging-your-first-java-application.html) on how to debug java programs.

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
