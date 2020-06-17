package org.opengroup.osdu.indexerqueue.gcp.di;

import org.opengroup.osdu.core.common.logging.ILogger;
import org.opengroup.osdu.core.gcp.logging.logger.appengine.AppEngineLoggingProvider;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@ConditionalOnProperty(name="disable.appengine.log.factory", havingValue = "false", matchIfMissing = true )
@Component
@Primary
@Lazy
public class AppengineLogFactory implements FactoryBean<ILogger> {

    private AppEngineLoggingProvider appEngineLoggingProvider = new AppEngineLoggingProvider();

    @Override
    public ILogger getObject() throws Exception {
        return appEngineLoggingProvider.getLogger();
    }

    @Override
    public Class<?> getObjectType() {
        return ILogger.class;
    }
}