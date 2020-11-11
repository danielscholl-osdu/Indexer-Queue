# Indexer-queue-gcp

indexer-queue-gcp-cloudrunis a service that is woken up in response to messages emitted by os-storage onto Service Bus. It is responsible for calling the os-indexer to trigger re-indexing events.

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.
 
### Prerequisites
 
- [Maven 3.6.0+](https://maven.apache.org/download.cgi)
- [AdoptOpenJDK8](https://adoptopenjdk.net/)
- [Lombok 1.16 or later](https://projectlombok.org/setup/maven)
- [GCloud SDK with java (latest version)](https://cloud.google.com/sdk/docs/install)
 
### Installation

- Setup Apache Maven
- Setup AdoptOpenJDK
- Setup GCloud SDK
- Install Eclipse (or other IDE) to run applications
- Set up environment variables for Apache Maven and AdoptOpenJDK. For example M2_HOME, JAVA_HOME, etc.
- Add a configuration for build project in Eclipse(or other IDE)

### Run Locally

In order to run the service locally or remotely, you will need to have the following environment variables defined.

| name | value | description | sensitive? | source |
| ---  | ---   | ---         | ---        | ---    |
| `OSDU_ENTITLEMENTS_URL` | ex `https://entitlements.com/entitlements/v1` | Entitlements API endpoint | no | output of infrastructure deployment |
| `SERVICE_MAIL` | ex `service@osdu.iam.gserviceaccount.com` | Indexer Que Service mail with which token will be created for cloud tasks must match with variable in `INDEXER_QUE_SERVICE_MAIL` Indexer Service | no | - |
| `INDEXER_HOST` | ex `https://indexer.a.run.app` | Indexer Service host  | no | output of infrastructure deployment |
| `GOOGLE_CLOUD_PROJECT_REGION` | ex `us-central1` | Service deployment region | no | output of infrastructure deployment |
| `GOOGLE_CLOUD_PROJECT` | ex `opendes` | Google Cloud Project Id| no | output of infrastructure deployment |
| `GOOGLE_AUDIENCES` | ex `*****.apps.googleusercontent.com` | Client ID for getting access to cloud resources | yes | https://console.cloud.google.com/apis/credentials |
| `GOOGLE_APPLICATION_CREDENTIALS` | ex `/path/to/directory/service-key.json` | Service account credentials, you only need this if running locally | yes | https://console.cloud.google.com/iam-admin/serviceaccounts |

Check that maven is installed:

```bash
$ mvn --version
Apache Maven 3.6.0
Maven home: /usr/share/maven
Java version: 1.8.0_212, vendor: AdoptOpenJDK, runtime: /usr/lib/jvm/jdk8u212-b04/jre
...
```

You may need to configure access to the remote maven repository that holds the OSDU dependencies. This file should live within `~/.mvn/community-maven.settings.xml`:

```bash
$ cat ~/.m2/settings.xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>community-maven-via-private-token</id>
            <!-- Treat this auth token like a password. Do not share it with anyone, including Microsoft support. -->
            <!-- The generated token expires on or before 11/14/2019 -->
             <configuration>
              <httpHeaders>
                  <property>
                      <name>Private-Token</name>
                      <value>${env.COMMUNITY_MAVEN_TOKEN}</value>
                  </property>
              </httpHeaders>
             </configuration>
        </server>
    </servers>
</settings>
```

* Update the Google cloud SDK to the latest version:

```bash
gcloud components update
```
* Set Google Project Id:

```bash
gcloud config set project <YOUR-PROJECT-ID>
```

* Perform a basic authentication in the selected project:

```bash
gcloud auth application-default login
```

## Testing
* Navigate to Indexer-queue service's root folder and run:
 
```bash
mvn clean install   
```

* If you wish to see the coverage report then go to testing/target/site/jacoco-aggregate and open index.html

* If you wish to build the project without running tests

```bash
mvn clean install -DskipTests
```

After configuring your environment as specified above, you can follow these steps to build and run the application. These steps should be invoked from the *repository root.*

```bash
cd indexer-queue-gcp-cloudrun/ && mvn spring-boot:run
```

## Deployment

* Google Documentation: https://cloud.google.com/cloud-build/docs/deploying-builds/deploy-cloud-run

## PubSub
For authentication indexer-que-cloudrun rely on entitlements service, 
that mean Pubsub push subscription must be configured to use service account

**Entitlements configuration for pubsub account**

| Service account used in subscription | 
| ---  | 
| users<br/>service.entitlements.user<br/>service.search.admin|

Push endpoint must be service endpoint:

Push endpoint
https://indexer-queue-jvmvia5dea-uc.a.run.app/api/indexer/v1/_ah/push-handlers/enqueue

## Licence
Copyright © Google LLC
Copyright © EPAM Systems
 
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)
 
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.