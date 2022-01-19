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

import com.analysys.presto.connector.hbase.meta.HBaseColumnHandle;
import com.analysys.presto.connector.hbase.meta.HBaseInsertTableHandle;
import com.analysys.presto.connector.hbase.meta.HBaseTableHandle;
import com.analysys.presto.connector.hbase.schedule.HBaseSplit;
import io.trino.spi.connector.*;

/**
 * HBase handle resolver
 *
 * @author wupeng
 * @date 2019/01/29
 */
public class HBaseHandleResolver implements ConnectorHandleResolver {

    @Override
    public Class<? extends ConnectorTableHandle> getTableHandleClass() {
        return HBaseTableHandle.class;
    }

    @Override
    public Class<? extends ColumnHandle> getColumnHandleClass() {
        return HBaseColumnHandle.class;
    }

    @Override
    public Class<? extends ConnectorSplit> getSplitClass() {
        return HBaseSplit.class;
    }

    @Override
    public Class<? extends ConnectorTransactionHandle> getTransactionHandleClass() {
        return HBaseTransactionHandle.class;
    }

    @Override
    public Class<? extends ConnectorInsertTableHandle> getInsertTableHandleClass() {
        return HBaseInsertTableHandle.class;
    }

}
