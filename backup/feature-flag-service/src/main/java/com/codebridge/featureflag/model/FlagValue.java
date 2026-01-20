package com.codebridge.featureflag.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Represents a feature flag value with its type.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlagValue implements Serializable {
    
    private ValueType type;
    private Object value;
    
    /**
     * Creates a boolean flag value.
     * 
     * @param value the boolean value
     * @return a new FlagValue instance
     */
    public static FlagValue ofBoolean(boolean value) {
        return new FlagValue(ValueType.BOOLEAN, value);
    }
    
    /**
     * Creates a string flag value.
     * 
     * @param value the string value
     * @return a new FlagValue instance
     */
    public static FlagValue ofString(String value) {
        return new FlagValue(ValueType.STRING, value);
    }
    
    /**
     * Creates an integer flag value.
     * 
     * @param value the integer value
     * @return a new FlagValue instance
     */
    public static FlagValue ofInteger(long value) {
        return new FlagValue(ValueType.INTEGER, value);
    }
    
    /**
     * Creates a double flag value.
     * 
     * @param value the double value
     * @return a new FlagValue instance
     */
    public static FlagValue ofDouble(double value) {
        return new FlagValue(ValueType.DOUBLE, value);
    }
    
    /**
     * Creates a JSON flag value.
     * 
     * @param value the JSON string
     * @return a new FlagValue instance
     */
    public static FlagValue ofJson(String value) {
        return new FlagValue(ValueType.JSON, value);
    }
    
    /**
     * Gets the value as a boolean.
     * 
     * @return the boolean value
     * @throws ClassCastException if the value is not a boolean
     */
    @JsonIgnore
    public boolean getBooleanValue() {
        if (type != ValueType.BOOLEAN) {
            throw new ClassCastException("Flag value is not a boolean");
        }
        return (Boolean) value;
    }
    
    /**
     * Gets the value as a string.
     * 
     * @return the string value
     * @throws ClassCastException if the value is not a string
     */
    @JsonIgnore
    public String getStringValue() {
        if (type != ValueType.STRING) {
            throw new ClassCastException("Flag value is not a string");
        }
        return (String) value;
    }
    
    /**
     * Gets the value as an integer.
     * 
     * @return the integer value
     * @throws ClassCastException if the value is not an integer
     */
    @JsonIgnore
    public long getIntegerValue() {
        if (type != ValueType.INTEGER) {
            throw new ClassCastException("Flag value is not an integer");
        }
        return ((Number) value).longValue();
    }
    
    /**
     * Gets the value as a double.
     * 
     * @return the double value
     * @throws ClassCastException if the value is not a double
     */
    @JsonIgnore
    public double getDoubleValue() {
        if (type != ValueType.DOUBLE) {
            throw new ClassCastException("Flag value is not a double");
        }
        return ((Number) value).doubleValue();
    }
    
    /**
     * Gets the value as a JSON string.
     * 
     * @return the JSON string
     * @throws ClassCastException if the value is not a JSON string
     */
    @JsonIgnore
    public String getJsonValue() {
        if (type != ValueType.JSON) {
            throw new ClassCastException("Flag value is not a JSON string");
        }
        return (String) value;
    }
}

