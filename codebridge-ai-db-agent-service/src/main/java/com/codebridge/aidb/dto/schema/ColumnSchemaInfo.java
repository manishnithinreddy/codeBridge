package com.codebridge.aidb.dto.schema;

import java.io.Serializable;

public class ColumnSchemaInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String dataType; // e.g., from java.sql.Types mapped to string or rsMetaData.getColumnTypeName()
    private boolean isNullable;
    private Integer columnSize; // Optional
    private Integer decimalDigits; // Optional, for numeric types
    // Future: private boolean isPrimaryKey;
    // Future: private boolean isForeignKey;

    public ColumnSchemaInfo() {}

    public ColumnSchemaInfo(String name, String dataType, boolean isNullable, Integer columnSize, Integer decimalDigits) {
        this.name = name;
        this.dataType = dataType;
        this.isNullable = isNullable;
        this.columnSize = columnSize;
        this.decimalDigits = decimalDigits;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDataType() {
        return dataType;
    }

    public void setDataType(String dataType) {
        this.dataType = dataType;
    }

    public boolean isNullable() {
        return isNullable;
    }

    public void setNullable(boolean nullable) {
        isNullable = nullable;
    }

    public Integer getColumnSize() {
        return columnSize;
    }

    public void setColumnSize(Integer columnSize) {
        this.columnSize = columnSize;
    }

    public Integer getDecimalDigits() {
        return decimalDigits;
    }

    public void setDecimalDigits(Integer decimalDigits) {
        this.decimalDigits = decimalDigits;
    }
}

