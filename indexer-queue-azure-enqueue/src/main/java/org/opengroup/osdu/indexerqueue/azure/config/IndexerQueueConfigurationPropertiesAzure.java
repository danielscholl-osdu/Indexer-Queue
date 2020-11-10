package org.opengroup.osdu.indexerqueue.azure.config;

import lombok.Getter;
import lombok.Setter;
import org.opengroup.osdu.core.common.model.search.DeploymentEnvironment;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties()
@Getter
@Setter
public class IndexerQueueConfigurationPropertiesAzure {

	private String deploymentEnvironment = DeploymentEnvironment.CLOUD.name();

	public DeploymentEnvironment getDeploymentEnvironment(){
		return DeploymentEnvironment.valueOf(deploymentEnvironment);
	}
}
