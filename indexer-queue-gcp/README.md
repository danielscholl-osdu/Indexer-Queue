# Indexer-queue-gcp

os-indexer-queue-gcp is a service that is woken up in response to messages emitted by os-storage onto Service Bus. It is responsible for calling the os-indexer to trigger re-indexing events.

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
* Navigate to indexer-queue service's root folder and run:
 
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
cd indexer-queue-gcp/ && mvn spring-boot:run
```

## Testing

Navigate to indexer-queue service's root folder and run all the tests.

## Deployment

* Data-Lake Indexer-queue Google Cloud Endpoints on App Engine Flex environment
  * Deploy
    ```sh
    mvn appengine:deploy -pl org.opengroup.osdu.indexer-queue:indexer-queue -amd
    ```

  * If you wish to deploy the indexer-queue service without running tests
    ```sh
    mvn appengine:deploy -pl org.opengroup.osdu.indexer-queue:indexer-queue -amd -DskipTests
    ```

or
* Google Documentation: https://cloud.google.com/cloud-build/docs/deploying-builds/deploy-appengine

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