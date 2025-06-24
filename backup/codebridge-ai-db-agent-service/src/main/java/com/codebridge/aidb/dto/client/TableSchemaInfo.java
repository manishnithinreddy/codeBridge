package com.codebridge.aidb.dto.client; // Changed package to client

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// STUB of com.codebridge.session.dto.schema.TableSchemaInfo
public class TableSchemaInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tableName;
    private String tableType;
    private String remarks;
    private List<ColumnSchemaInfo> columns;

    public TableSchemaInfo() {
        this.columns = new ArrayList<>();
    }

    // Getters and Setters
    public String getTableName() { return tableName; }
    public void setTableName(String tableName) { this.tableName = tableName; }
    public String getTableType() { return tableType; }
    public void setTableType(String tableType) { this.tableType = tableType; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public List<ColumnSchemaInfo> getColumns() { return columns; }
    public void setColumns(List<ColumnSchemaInfo> columns) { this.columns = columns; }
    public void addColumn(ColumnSchemaInfo column) { this.columns.add(column); }
}
