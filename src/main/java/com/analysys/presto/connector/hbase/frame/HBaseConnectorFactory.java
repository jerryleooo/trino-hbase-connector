/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.analysys.presto.connector.hbase.frame;

import com.google.inject.Injector;
import io.airlift.bootstrap.Bootstrap;
import io.airlift.log.Logger;
import io.trino.spi.NodeManager;
import io.trino.spi.connector.Connector;
import io.trino.spi.connector.ConnectorContext;
import io.trino.spi.connector.ConnectorFactory;
import io.trino.spi.connector.ConnectorHandleResolver;
import java.util.Map;
import java.util.Objects;

import static com.analysys.presto.connector.hbase.utils.Constant.CONNECTOR_NAME;

/**
 * HBase connector factory
 *
 * @author wupeng
 * @date 2019/01/29
 */
public class HBaseConnectorFactory implements ConnectorFactory {

    private static final Logger log = Logger.get(HBaseConnectorFactory.class);

    @Override
    public String getName() {
        return CONNECTOR_NAME;
    }

    @Override
    public ConnectorHandleResolver getHandleResolver() {
        return new HBaseHandleResolver();
    }

    @Override
    public Connector create(String connectorId, Map<String, String> requiredConfig, ConnectorContext context) {
        Objects.requireNonNull(requiredConfig, "requiredConfig is null");
        try {
            Bootstrap e = new Bootstrap(
                    binder -> binder.bind(NodeManager.class).toInstance(context.getNodeManager()),
                    new HBaseModule(connectorId, context.getTypeManager()));

            Injector injector = e.strictConfig()
                    .doNotInitializeLogging()
                    .setRequiredConfigurationProperties(requiredConfig)
                    .initialize();

            return injector.getInstance(HBaseConnector.class);
        } catch (Exception e) {
            log.error(e, e.getMessage());
            return null;
        }
    }
}
