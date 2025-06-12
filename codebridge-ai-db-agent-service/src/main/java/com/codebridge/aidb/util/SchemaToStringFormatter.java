package com.codebridge.aidb.util;

import com.codebridge.aidb.dto.client.ColumnSchemaInfo;
import com.codebridge.aidb.dto.client.DbSchemaInfoResponse;
import com.codebridge.aidb.dto.client.TableSchemaInfo;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component // Make it a Spring bean for easier injection
public class SchemaToStringFormatter {

    public String formatSchema(DbSchemaInfoResponse schemaInfo, String dbType) {
        if (schemaInfo == null) {
            return "-- No schema information provided.\n";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("-- Database Type: ").append(dbType != null ? dbType : schemaInfo.getDatabaseProductName()).append("\n");
        if (StringUtils.hasText(schemaInfo.getDatabaseProductVersion())) {
            sb.append("-- Database Version: ").append(schemaInfo.getDatabaseProductVersion()).append("\n");
        }
        sb.append("\n");

        if (schemaInfo.getTables() == null || schemaInfo.getTables().isEmpty()) {
            sb.append("-- No tables found in the schema.\n");
            return sb.toString();
        }

        sb.append("-- Tables and their columns:\n");
        for (TableSchemaInfo table : schemaInfo.getTables()) {
            sb.append("-- Table: ").append(table.getTableName());
            if (StringUtils.hasText(table.getTableType())) {
                sb.append(" (Type: ").append(table.getTableType()).append(")");
            }
            sb.append("\n");

            if (StringUtils.hasText(table.getRemarks())) {
                sb.append("--   Description: ").append(table.getRemarks().replace("\n", " ")).append("\n");
            }

            if (table.getColumns() == null || table.getColumns().isEmpty()) {
                sb.append("--   (No column information available for this table)\n");
            } else {
                for (ColumnSchemaInfo column : table.getColumns()) {
                    sb.append("--   - ").append(column.getName()).append(" (Type: ").append(column.getDataType());

                    boolean hasSize = column.getColumnSize() != null && column.getColumnSize() > 0;
                    boolean hasDecimal = column.getDecimalDigits() != null && column.getDecimalDigits() > 0;

                    if (hasSize) {
                        sb.append("(").append(column.getColumnSize());
                        if (hasDecimal) {
                            sb.append(", ").append(column.getDecimalDigits());
                        }
                        sb.append(")");
                    }

                    if (!column.isNullable()) {
                        sb.append(" NOT NULL");
                    }
                    // Future: Add PK/FK info if available in ColumnSchemaInfo
                    // if (column.isPrimaryKey()) sb.append(" PRIMARY KEY");
                    sb.append(")\n");
                }
            }
            sb.append("\n"); // Blank line between tables
        }
        return sb.toString();
    }
}
