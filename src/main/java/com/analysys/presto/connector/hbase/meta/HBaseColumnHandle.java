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
package com.analysys.presto.connector.hbase.meta;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.trino.spi.connector.ColumnHandle;
import io.trino.spi.connector.ColumnMetadata;
import io.trino.spi.type.Type;
import java.util.Objects;

/**
 * HBase column handle
 *
 * @author wupeng
 * @date 2019/01/29
 */
public final class HBaseColumnHandle implements ColumnHandle {

    private final String connectorId;
    private final String family;
    private final String columnName;
    private final Type columnType;
    private final boolean rowKey;

    /**
     * The index of a column in table, start from 0 to n-1(The table has n columns)
     */
    private final int ordinalPosition;

    @JsonCreator
    public HBaseColumnHandle(@JsonProperty("connectorId") String connectorId,
                             @JsonProperty("family") String family,
                             @JsonProperty("columnName") String columnName,
                             @JsonProperty("columnType") Type columnType,
                             @JsonProperty("ordinalPosition") int ordinalPosition,
                             @JsonProperty("rowKey") boolean rowKey) {
        this.connectorId = Objects.requireNonNull(connectorId, "connectorId is null");
        this.family = Objects.requireNonNull(family, "family is null");
        this.columnName = Objects.requireNonNull(columnName, "columnName is null");
        this.columnType = Objects.requireNonNull(columnType, "columnType is null");
        this.ordinalPosition = ordinalPosition;
        this.rowKey = rowKey;
    }

    @JsonProperty
    public String getConnectorId() {
        return this.connectorId;
    }

    @JsonProperty
    public String getColumnName() {
        return this.columnName;
    }

    @JsonProperty
    public Type getColumnType() {
        return this.columnType;
    }

    @JsonProperty
    public int getOrdinalPosition() {
        return this.ordinalPosition;
    }

    @JsonProperty
    public String getFamily() {
        return family;
    }

    @JsonProperty
    public boolean isRowKey() {
        return rowKey;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.connectorId, this.family, this.columnName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj != null && this.getClass() == obj.getClass()) {
            HBaseColumnHandle other = (HBaseColumnHandle) obj;
            return Objects.equals(this.connectorId, other.connectorId)
                    && Objects.equals(this.family, other.family)
                    && Objects.equals(this.columnName, other.columnName);
        } else {
            return false;
        }
    }

    ColumnMetadata toColumnMetadata() {
        return new ColumnMetadata(columnName, columnType);
    }

    @Override
    public String toString() {
        return "HBaseColumnHandle{" +
                "connectorId='" + connectorId + '\'' +
                ", family='" + family + '\'' +
                ", columnName='" + columnName + '\'' +
                ", columnType=" + columnType +
                ", rowKey=" + rowKey +
                ", ordinalPosition=" + ordinalPosition +
                '}';
    }
}
