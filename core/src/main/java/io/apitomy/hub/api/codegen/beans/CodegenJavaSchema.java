package io.apitomy.hub.api.codegen.beans;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class CodegenJavaSchema {

    private String collection;
    private List<String> type;
    private String format;
    private String constant;
    private Number maximum;
    private boolean exclusiveMaximum;
    private Number minimum;
    private boolean exclusiveMinimum;
    private Long maxLength;
    private Long minLength;
    private String pattern;
    private Long maxItems;
    private Long minItems;
    private Boolean uniqueItems;
    private Long maxProperties;
    private Long minProperties;
    private String defaultValue;
    private String existingJavaType;

    public void setType(String type) {
        this.type = Collections.singletonList(type);
    }

    public boolean isNullable() {
        return Optional.ofNullable(type).map(t -> t.contains("null")).orElse(false);
    }

    @Override
    public int hashCode() {
        return Objects.hash(collection, constant, defaultValue, exclusiveMaximum, exclusiveMinimum, format, maxItems,
                maxLength, maxProperties, maximum, minItems, minLength, minProperties, minimum, pattern,
                existingJavaType, type, uniqueItems);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CodegenJavaSchema other = (CodegenJavaSchema) obj;
        return Objects.equals(collection, other.collection) && Objects.equals(constant, other.constant)
                && Objects.equals(defaultValue, other.defaultValue)
                && Objects.equals(exclusiveMaximum, other.exclusiveMaximum)
                && Objects.equals(exclusiveMinimum, other.exclusiveMinimum) && Objects.equals(format, other.format)
                && Objects.equals(maxItems, other.maxItems) && Objects.equals(maxLength, other.maxLength)
                && Objects.equals(maxProperties, other.maxProperties) && Objects.equals(maximum, other.maximum)
                && Objects.equals(minItems, other.minItems) && Objects.equals(minLength, other.minLength)
                && Objects.equals(minProperties, other.minProperties) && Objects.equals(minimum, other.minimum)
                && Objects.equals(existingJavaType, other.existingJavaType) && Objects.equals(type, other.type)
                && Objects.equals(uniqueItems, other.uniqueItems);
    }

    /**
     * @return the collection
     */
    public String getCollection() {
        return collection;
    }

    /**
     * @param collection the collection to set
     */
    public void setCollection(String collection) {
        this.collection = collection;
    }

    /**
     * @return the type
     */
    public List<String> getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(List<String> type) {
        this.type = type;
    }

    /**
     * @return the format
     */
    public String getFormat() {
        return format;
    }

    /**
     * @param format the format to set
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * @return the constant
     */
    public String getConstant() {
        return constant;
    }

    /**
     * @param constant the constant to set
     */
    public void setConstant(String constant) {
        this.constant = constant;
    }

    /**
     * @return the maximum
     */
    public Number getMaximum() {
        return maximum;
    }

    /**
     * @param maximum the maximum to set
     */
    public void setMaximum(Number maximum) {
        this.maximum = maximum;
    }

    /**
     * @return the exclusiveMaximum
     */
    public boolean isExclusiveMaximum() {
        return exclusiveMaximum;
    }

    /**
     * @param exclusiveMaximum the exclusiveMaximum to set
     */
    public void setExclusiveMaximum(boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    /**
     * @return the minimum
     */
    public Number getMinimum() {
        return minimum;
    }

    /**
     * @param minimum the minimum to set
     */
    public void setMinimum(Number minimum) {
        this.minimum = minimum;
    }

    /**
     * @return the exclusiveMinimum
     */
    public boolean isExclusiveMinimum() {
        return exclusiveMinimum;
    }

    /**
     * @param exclusiveMinimum the exclusiveMinimum to set
     */
    public void setExclusiveMinimum(boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    /**
     * @return the maxLength
     */
    public Long getMaxLength() {
        return maxLength;
    }

    /**
     * @param maxLength the maxLength to set
     */
    public void setMaxLength(Long maxLength) {
        this.maxLength = maxLength;
    }

    /**
     * @return the minLength
     */
    public Long getMinLength() {
        return minLength;
    }

    /**
     * @param minLength the minLength to set
     */
    public void setMinLength(Long minLength) {
        this.minLength = minLength;
    }

    /**
     * @return the pattern
     */
    public String getPattern() {
        return pattern;
    }

    /**
     * @param pattern the pattern to set
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    /**
     * @return the maxItems
     */
    public Long getMaxItems() {
        return maxItems;
    }

    /**
     * @param maxItems the maxItems to set
     */
    public void setMaxItems(Long maxItems) {
        this.maxItems = maxItems;
    }

    /**
     * @return the minItems
     */
    public Long getMinItems() {
        return minItems;
    }

    /**
     * @param minItems the minItems to set
     */
    public void setMinItems(Long minItems) {
        this.minItems = minItems;
    }

    /**
     * @return the uniqueItems
     */
    public Boolean getUniqueItems() {
        return uniqueItems;
    }

    /**
     * @param uniqueItems the uniqueItems to set
     */
    public void setUniqueItems(Boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
    }

    /**
     * @return the maxProperties
     */
    public Long getMaxProperties() {
        return maxProperties;
    }

    /**
     * @param maxProperties the maxProperties to set
     */
    public void setMaxProperties(Long maxProperties) {
        this.maxProperties = maxProperties;
    }

    /**
     * @return the minProperties
     */
    public Long getMinProperties() {
        return minProperties;
    }

    /**
     * @param minProperties the minProperties to set
     */
    public void setMinProperties(Long minProperties) {
        this.minProperties = minProperties;
    }


    /**
     * @return the defaultValue
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @param defaultValue the defaultValue to set
     */
    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    /**
     * @return the existingJavaType
     */
    public String getExistingJavaType() {
        return existingJavaType;
    }

    /**
     * @param existingJavaType the existingJavaType to set
     */
    public void setExistingJavaType(String existingJavaType) {
        this.existingJavaType = existingJavaType;
    }

}
