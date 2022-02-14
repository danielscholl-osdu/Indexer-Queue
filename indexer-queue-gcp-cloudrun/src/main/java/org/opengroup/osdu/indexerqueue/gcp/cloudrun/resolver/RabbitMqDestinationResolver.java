/*
 * Copyright 2020-2022 Google LLC
 * Copyright 2020-2022 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.indexerqueue.gcp.cloudrun.resolver;


import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.http.client.Client;
import com.rabbitmq.http.client.ClientParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opengroup.osdu.core.common.partition.IPartitionProvider;
import org.opengroup.osdu.core.common.partition.PartitionException;
import org.opengroup.osdu.core.common.partition.PartitionInfo;
import org.opengroup.osdu.core.common.partition.Property;
import org.opengroup.osdu.core.gcp.oqm.driver.OqmDriverRuntimeException;
import org.opengroup.osdu.core.gcp.oqm.driver.rabbitmq.MqOqmDestinationResolution;
import org.opengroup.osdu.core.gcp.oqm.driver.rabbitmq.MqOqmDestinationResolver;
import org.opengroup.osdu.core.gcp.oqm.model.OqmDestination;
import org.opengroup.osdu.indexerqueue.gcp.cloudrun.config.RabbitMqOqmConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_SINGLETON;


@Component
@Scope(SCOPE_SINGLETON)
@ConditionalOnProperty(name = "oqmDriver", havingValue = "rabbitmq")
@RequiredArgsConstructor
@Slf4j
public class RabbitMqDestinationResolver implements MqOqmDestinationResolver {

    private final RabbitMqOqmConfiguration properties;

    //Compose names to get configuration properties from Partition
    private static final String AMQP = ".amqp.";
    private static final String AMQP_HOST = AMQP.concat("host");
    private static final String AMQP_PORT = AMQP.concat("port");
    private static final String AMQP_PATH = AMQP.concat("path");
    private static final String AMQP_USERNAME = AMQP.concat("username");
    private static final String AMQP_PASSWORD = AMQP.concat("password");

    private static final String ADMIN = ".admin.";
    private static final String ADMIN_SCHEMA = ADMIN.concat("schema");
    private static final String ADMIN_HOST = ADMIN.concat("host");
    private static final String ADMIN_PORT = ADMIN.concat("port");
    private static final String ADMIN_PATH = ADMIN.concat("path");
    private static final String ADMIN_USERNAME = ADMIN.concat("username");
    private static final String ADMIN_PASSWORD = ADMIN.concat("password");

    private final IPartitionProvider partitionProvider;

    private final Map<String, ConnectionFactory> amqpConnectionFactoryCache = new HashMap<>();
    private final Map<String, Client> httpClientCache = new HashMap<>();

    @Override
    public MqOqmDestinationResolution resolve(OqmDestination destination) {

        String partitionId = destination.getPartitionId();


        String virtualHost = "/";

        ConnectionFactory amqpFactory = amqpConnectionFactoryCache.get(partitionId);
        Client httpClient = httpClientCache.get(partitionId);



        if (amqpFactory == null || httpClient == null) {

            Map<String, Property> partitionProperties = getPartitionProperties(destination, partitionId);

            synchronized (amqpConnectionFactoryCache){
                amqpFactory = amqpConnectionFactoryCache.get(partitionId);
                if (amqpFactory == null) {
                    amqpFactory = getAmqpFactory(partitionId, partitionProperties);
                }
            }
            synchronized (httpClientCache){
                httpClient = httpClientCache.get(partitionId);
                if (httpClient == null) {
                    httpClient = getHttpClient(partitionId, partitionProperties);
                }
            }
        }
        return MqOqmDestinationResolution.builder()
                .amqpFactory(amqpFactory)
                .adminClient(httpClient)
                .virtualHost(virtualHost)
                .build();
    }

    private Map<String, Property> getPartitionProperties(OqmDestination destination, String partitionId) {
        PartitionInfo partitionInfo;
        try {
            partitionInfo = partitionProvider.get(partitionId);
        } catch (PartitionException e) {
            throw new OqmDriverRuntimeException(e, "Partition '%s' destination resolution issue", destination.getPartitionId());
        }
        return partitionInfo.getProperties();
    }

    private Client getHttpClient(String partitionId, Map<String, Property> partitionProperties) {
        Client httpClient;
        String adminSchema = getPartitionProperty(partitionId, partitionProperties, ADMIN_SCHEMA);
        String adminHost = getPartitionProperty(partitionId, partitionProperties, ADMIN_HOST);
        String adminPort = getPartitionProperty(partitionId, partitionProperties, ADMIN_PORT);
        String adminPath = getPartitionProperty(partitionId, partitionProperties, ADMIN_PATH);
        String adminUser = getPartitionProperty(partitionId, partitionProperties, ADMIN_USERNAME);
        String adminPassword = getPartitionProperty(partitionId, partitionProperties, ADMIN_PASSWORD);

        try {
            URI httpUrl = new URI(adminSchema, null, adminHost, Integer.parseInt(adminPort), adminPath, null, null);
            ClientParameters clientParameters = new ClientParameters().url(httpUrl.toURL())
                    .username(adminUser).password(adminPassword);

            httpClient = new Client(clientParameters);
            httpClientCache.put(partitionId, httpClient);

        } catch (URISyntaxException | MalformedURLException e) {
            throw new OqmDriverRuntimeException("RabbitMQ http(api) URI and Client", e);
        }
        return httpClient;
    }

    private ConnectionFactory getAmqpFactory(String partitionId, Map<String, Property> partitionProperties) {
        ConnectionFactory amqpFactory;
        String amqpHost = getPartitionProperty(partitionId, partitionProperties, AMQP_HOST);
        String amqpPort = getPartitionProperty(partitionId, partitionProperties, AMQP_PORT);
        String amqpPath = getPartitionProperty(partitionId, partitionProperties, AMQP_PATH);
        String amqpUser = getPartitionProperty(partitionId, partitionProperties, AMQP_USERNAME);
        String amqpPass = getPartitionProperty(partitionId, partitionProperties, AMQP_PASSWORD);

        URI amqpUri;
        try {
            amqpUri = new URI("amqp", amqpUser + ":" + amqpPass, amqpHost, Integer.parseInt(amqpPort), amqpPath, null, null);
            amqpFactory = new ConnectionFactory();
            amqpFactory.setUri(amqpUri);
            amqpConnectionFactoryCache.put(partitionId, amqpFactory);

        } catch (URISyntaxException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new OqmDriverRuntimeException("RabbitMQ amqp URI and ConnectionFactory", e);
        }
        return amqpFactory;
    }

    private String getPartitionProperty(String partitionId, Map<String, Property> partitionProperties, String propertyName) {
        String fullName = properties.getPartitionPropertiesPrefix().concat(propertyName);
        return Optional.ofNullable(partitionProperties.get(fullName)).map(Property::getValue).map(Object::toString)
                .orElseThrow(() -> new OqmDriverRuntimeException(null,
                        "Partition '%s' RabbitMQ OQM destination resolution configuration issue. Property '%s' is not provided in PartitionInfo.",
                        partitionId, fullName));
    }

    @PreDestroy
    public void shutdown() {
        log.info("On pre-destroy.");
    }
}
