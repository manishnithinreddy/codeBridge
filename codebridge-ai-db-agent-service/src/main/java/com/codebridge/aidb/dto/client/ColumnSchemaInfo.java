package com.codebridge.aidb.dto.client; // Changed package to client

import java.io.Serializable;

// STUB of com.codebridge.session.dto.schema.ColumnSchemaInfo
public class ColumnSchemaInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String dataType;
    private boolean isNullable;
    private Integer columnSize;
    private Integer decimalDigits;

    public ColumnSchemaInfo() {}

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDataType() { return dataType; }
    public void setDataType(String dataType) { this.dataType = dataType; }
    public boolean isNullable() { return isNullable; }
    public void setNullable(boolean nullable) { isNullable = nullable; }
    public Integer getColumnSize() { return columnSize; }
    public void setColumnSize(Integer columnSize) { this.columnSize = columnSize; }
    public Integer getDecimalDigits() { return decimalDigits; }
    public void setDecimalDigits(Integer decimalDigits) { this.decimalDigits = decimalDigits; }
}
