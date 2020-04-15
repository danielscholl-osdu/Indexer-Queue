#  Copyright © Microsoft Corporation
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

FROM maven:3-jdk-8 AS installer-env
ARG VSTS_FEED_TOKEN

RUN mkdir -p /build/azure

# Copy the parent pom and settings file
COPY ./pom.xml /build
COPY ./indexer-queue-azure-enqueue/maven/docker_settings.xml /root/.m2/settings.xml
COPY ./indexer-queue-azure-enqueue/pom.xml /build/azure

# # Copy the azure provider pom and pull artifacts
WORKDIR /build/azure
RUN mvn -B dependency:go-offline dependency:resolve dependency:resolve-plugins

# # Copy the project source code and build it
COPY ./indexer-queue-azure-enqueue/host.json /build/azure/host.json
COPY ./indexer-queue-azure-enqueue/src /build/azure/src
RUN mvn clean package -DskipTests

FROM mcr.microsoft.com/azure-functions/java:2.0 as runtime
ENV AzureWebJobsScriptRoot=/home/site/wwwroot \
  AzureFunctionsJobHost__Logging__Console__IsEnabled=true

COPY --from=installer-env ["/build/azure/target/azure-functions/EnqueueFunction", "/home/site/wwwroot"]
