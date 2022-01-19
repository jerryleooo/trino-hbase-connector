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
package com.analysys.presto.connector.hbase.query;

import com.analysys.presto.connector.hbase.connection.HBaseClientManager;
import com.analysys.presto.connector.hbase.meta.HBaseInsertTableHandle;
import com.analysys.presto.connector.hbase.utils.Utils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import io.airlift.log.Logger;
import io.airlift.slice.Slice;
import io.trino.spi.Page;
import io.trino.spi.block.Block;
import io.trino.spi.block.DictionaryBlock;
import io.trino.spi.block.VariableWidthBlock;
import io.trino.spi.connector.ConnectorPageSink;
import io.trino.spi.type.DecimalType;
import io.trino.spi.type.SqlDecimal;
import io.trino.spi.type.StandardTypes;
import io.trino.spi.type.Type;
import io.trino.spi.type.VarcharType;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.analysys.presto.connector.hbase.utils.Constant.ARRAY_STRING_SPLITTER;
import static com.analysys.presto.connector.hbase.utils.Constant.SYSTEMOUT_INTERVAL;
import static io.trino.spi.type.BigintType.BIGINT;
import static io.trino.spi.type.BooleanType.BOOLEAN;
import static io.trino.spi.type.DoubleType.DOUBLE;
import static io.trino.spi.type.IntegerType.INTEGER;
import static io.trino.spi.type.TimestampType.TIMESTAMP;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * write data to HBase
 *
 * @author wupeng
 * @date 2018/4/25.
 */
public class HBasePageSink implements ConnectorPageSink {

    private static final Logger log = Logger.get(HBasePageSink.class);

    private final List<Type> columnTypes;
    private final List<String> columnNames;
    private String schemaName = null;
    private String tableName = null;
    private HBaseClientManager clientManager;
    private final int rowKeyColumnChannel;
    private final Map<String, String> colNameAndFamilyNameMap;

    public HBasePageSink(HBaseClientManager clientManager,
                         HBaseInsertTableHandle insertTableHandle) {
        requireNonNull(clientManager, "clientManager is null");
        this.columnTypes = insertTableHandle.getColumnTypes();
        this.columnNames = insertTableHandle.getColumnNames();

        this.clientManager = clientManager;
        this.rowKeyColumnChannel = insertTableHandle.getRowKeyColumnChannel();
        this.colNameAndFamilyNameMap = insertTableHandle.getColNameAndFamilyNameMap();

        try {
            this.tableName = insertTableHandle.getSchemaTableName().getTableName();
            this.schemaName = insertTableHandle.getSchemaTableName().getSchemaName();
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    @Override
    public CompletableFuture<?> appendPage(Page page) {
        long startTime = System.currentTimeMillis();
        List<Put> puts = new ArrayList<>(10000);
        String rowKey = null;
        try (Connection connection = this.clientManager.createConnection();
             Table table = connection.getTable(TableName.valueOf(schemaName + ":" + tableName))) {

            for (int position = 0; position < page.getPositionCount(); position++) {
                rowKey = getRowKeyByChannel(page, this.rowKeyColumnChannel, position);
                Put put = new Put(Bytes.toBytes(rowKey));
                for (int channel = 0; channel < page.getChannelCount(); channel++) {
                    // The value of rowKey has been planted in object Put already,
                    // so we don't need to append it here.
                    if (channel == rowKeyColumnChannel) {
                        continue;
                    }
                    appendColumnValue(put, page, position, channel, channel);
                }
                puts.add(put);

                if (puts.size() >= 10000) {
                    table.put(puts);
                    puts.clear();
                }
            }

            table.put(puts);

            if (System.currentTimeMillis() % SYSTEMOUT_INTERVAL == 0)
                log.info("INSERT DATA. StartTime=" + new Date(startTime).toString()
                        + ", used " + (System.currentTimeMillis() - startTime)
                        + " million seconds, pageCount=" + page.getPositionCount() + ", rowKey=" + rowKey
                        + ", table=" + schemaName + ":" + tableName);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return NOT_BLOCKED;
    }

    /**
     * Find the value of RowKey column by channel.
     *
     * @param page     page
     * @param channel  rowKey channel
     * @param position position
     * @return rowKey
     */
    private String getRowKeyByChannel(Page page, int channel, int position) {
        Preconditions.checkState(channel >= 0,
                "You must specify ROW_KEY column for Table %s.%s in your .json file.",
                schemaName, tableName);
        Block block = page.getBlock(channel);
        return columnTypes.get(channel).getSlice(block, position).toStringUtf8();
    }

    private void appendColumnValue(Put put, Page page, int position, int channel, int destChannel) {
        Block block = page.getBlock(channel);
        Type type = columnTypes.get(destChannel);
        String columnName = columnNames.get(destChannel);

        String columnFamilyName = this.colNameAndFamilyNameMap.get(columnNames.get(destChannel));

        // get value, add to Put
        if (block.isNull(position)) {
            // row.setNull(destChannel);
            return;
        } else if (TIMESTAMP.equals(type)) {
            put.addColumn(Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName),
                    Bytes.toBytes(type.getLong(block, position)));
        } else if (BIGINT.equals(type)) {
            put.addColumn(Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName),
                    Bytes.toBytes(type.getLong(block, position)));
        } else if (INTEGER.equals(type)) {
            int intValue = ((Long) type.getLong(block, position)).intValue();
            put.addColumn(Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName),
                    Bytes.toBytes(intValue));
        } else if (BOOLEAN.equals(type)) {
            int intValue = ((Long) type.getLong(block, position)).intValue();
            put.addColumn(Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName),
                    Bytes.toBytes(intValue));
        } else if (DOUBLE.equals(type)) {
            put.addColumn(Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName),
                    Bytes.toBytes(type.getDouble(block, position)));
        } else if (type.getClass().getSuperclass().equals(DecimalType.class)) {
            BigDecimal value = ((SqlDecimal) type.getObjectValue(null, block, position))
                    .toBigDecimal();

            put.addColumn(Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName),
                    Bytes.toBytes(value));
        } else if (type instanceof VarcharType) { // TODO: correct way to check if it is VarcharType?
            put.addColumn(Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName),
                    Bytes.toBytes(type.getSlice(block, position).toStringUtf8()));
        }
        // We only support Array<String>
        else if (type.getTypeSignature().getBase().equals(StandardTypes.ARRAY)) {
            Object obj = type.getObject(block, position);
            Block vBlock;
            if (obj instanceof VariableWidthBlock)
                vBlock = (VariableWidthBlock) obj;
            else
                vBlock = (DictionaryBlock) obj;
            StringBuilder buff = new StringBuilder();
            for (int i = 0; i < vBlock.getPositionCount(); i++) {
                Slice slice = vBlock.getSlice(i, 0, vBlock.getSliceLength(i));
                String value = slice.toStringUtf8();
                if (i > 0)
                    buff.append(ARRAY_STRING_SPLITTER);
                buff.append(Utils.removeExtraSpaceInArrayString(value));
            }
            put.addColumn(Bytes.toBytes(columnFamilyName), Bytes.toBytes(columnName),
                    Bytes.toBytes(buff.toString()));
        } else {
            throw new UnsupportedOperationException("Type is not supported: " + type);
        }
    }

    @Override
    public CompletableFuture<Collection<Slice>> finish() {
        closeSession();
        // the committer does not need any additional info.
        return completedFuture(ImmutableList.of());
    }

    @Override
    public void abort() {
        closeSession();
    }

    private void closeSession() {
    }

}
