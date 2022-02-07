package org.opengroup.osdu.indexerqueue.gcp.cloudrun.di;

import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.opengroup.osdu.core.common.partition.IPartitionFactory;
import org.opengroup.osdu.core.common.partition.IPartitionProvider;
import org.opengroup.osdu.core.gcp.googleidtoken.GcpServiceAccountJwtClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;
import static org.springframework.context.annotation.ScopedProxyMode.TARGET_CLASS;

@Configuration
public class PartitionProviderConfig {

    @Bean
    @Primary
    @Scope(value = SCOPE_PROTOTYPE, proxyMode = TARGET_CLASS)
    public IPartitionProvider partitionProvider(
            IPartitionFactory partitionFactory,
            GcpServiceAccountJwtClient jwtClient
    ) {
        DpsHeaders partitionHeaders = new DpsHeaders();
        String idToken = jwtClient.getDefaultOrInjectedServiceAccountIdToken();
        partitionHeaders.put("authorization", idToken);
        return partitionFactory.create(partitionHeaders);
    }
}