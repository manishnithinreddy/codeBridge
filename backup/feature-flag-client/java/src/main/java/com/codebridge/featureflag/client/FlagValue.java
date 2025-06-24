package com.codebridge.featureflag.client;

/**
 * Represents a feature flag value with its type.
 */
public class FlagValue {
    
    private final FlagValueType type;
    private final Object value;
    
    /**
     * Creates a new flag value.
     * 
     * @param type the value type
     * @param value the value
     */
    public FlagValue(FlagValueType type, Object value) {
        this.type = type;
        this.value = value;
    }
    
    /**
     * Creates a boolean flag value.
     * 
     * @param value the boolean value
     * @return a new FlagValue instance
     */
    public static FlagValue ofBoolean(boolean value) {
        return new FlagValue(FlagValueType.BOOLEAN, value);
    }
    
    /**
     * Creates a string flag value.
     * 
     * @param value the string value
     * @return a new FlagValue instance
     */
    public static FlagValue ofString(String value) {
        return new FlagValue(FlagValueType.STRING, value);
    }
    
    /**
     * Creates an integer flag value.
     * 
     * @param value the integer value
     * @return a new FlagValue instance
     */
    public static FlagValue ofInteger(long value) {
        return new FlagValue(FlagValueType.INTEGER, value);
    }
    
    /**
     * Creates a double flag value.
     * 
     * @param value the double value
     * @return a new FlagValue instance
     */
    public static FlagValue ofDouble(double value) {
        return new FlagValue(FlagValueType.DOUBLE, value);
    }
    
    /**
     * Creates a JSON flag value.
     * 
     * @param value the JSON string
     * @return a new FlagValue instance
     */
    public static FlagValue ofJson(String value) {
        return new FlagValue(FlagValueType.JSON, value);
    }
    
    /**
     * Gets the value type.
     * 
     * @return the value type
     */
    public FlagValueType getType() {
        return type;
    }
    
    /**
     * Gets the raw value.
     * 
     * @return the raw value
     */
    public Object getValue() {
        return value;
    }
    
    /**
     * Gets the value as a boolean.
     * 
     * @return the boolean value
     * @throws ClassCastException if the value is not a boolean
     */
    public boolean getBooleanValue() {
        if (type != FlagValueType.BOOLEAN) {
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
    public String getStringValue() {
        if (type != FlagValueType.STRING) {
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
    public long getIntegerValue() {
        if (type != FlagValueType.INTEGER) {
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
    public double getDoubleValue() {
        if (type != FlagValueType.DOUBLE) {
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
    public String getJsonValue() {
        if (type != FlagValueType.JSON) {
            throw new ClassCastException("Flag value is not a JSON string");
        }
        return (String) value;
    }
    
    @Override
    public String toString() {
        return "FlagValue{" +
                "type=" + type +
                ", value=" + value +
                '}';
    }
}

