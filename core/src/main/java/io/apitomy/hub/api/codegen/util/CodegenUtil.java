/*
 * Copyright 2019 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apitomy.hub.api.codegen.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.JsonNode;

import io.apitomy.datamodels.models.Document;
import io.apitomy.datamodels.models.Extensible;
import io.apitomy.datamodels.models.openapi.OpenApiSchema;
import io.apitomy.datamodels.models.openapi.v3x.v31.OpenApi31Document;
import io.apitomy.datamodels.models.openapi.v3x.v31.OpenApi31Schema;
import io.apitomy.datamodels.models.union.StringStringListUnion;
import io.apitomy.hub.api.codegen.CodegenExtensions;
import io.apitomy.hub.api.codegen.JaxRsProjectSettings;

public final class CodegenUtil {

    /**
     * Name of the (jsonschema2pojo-compatible) schema property used by the pre-processors to record
     * a pre-computed Java type for a schema.
     */
    public static final String EXISTING_JAVA_TYPE = "existingJavaType";

    /**
     * Placeholder used as the {@link #EXISTING_JAVA_TYPE} for "string/byte" schemas (a workaround
     * for a jsonschema2pojo limitation). It is replaced with {@code byte[]} when post-processing beans.
     */
    public static final String BYTE_ARRAY_REPRESENTATION = "APITOMY_CODEGEN_BYTE_ARRAY_REPRESENTATION";

    /**
     * Prefix shared by all Java map types recorded as {@link #EXISTING_JAVA_TYPE}.
     */
    public static final String JAVA_MAP_TYPE_PREFIX = "java.util.Map<";

    public static String schemaToPackageName(OpenApiSchema schema, String defaultPackage) {
        String pname = defaultPackage;

        if (schema != null) {
            JsonNode extension = getExtension((Extensible) schema, CodegenExtensions.PACKAGE);
            if (extension != null && !extension.isNull()) {
                String packageName = String.valueOf(extension.asText());
                if (!packageName.trim().isEmpty()) {
                    pname = packageName;
                }
            }
        }

        return pname;
    }

    public static String schemaRefToFQCN(JaxRsProjectSettings settings, Document document, String schemaRef,
          String defaultPackage) {
        String cname = "GeneratedClass_" + System.currentTimeMillis();
        String pname = defaultPackage;
        if (schemaRef.startsWith("#/components/schemas/")) {
            cname = schemaRef.substring(21);
            OpenApi31Document doc31 = (OpenApi31Document) document;
            if (doc31.getComponents() != null) {
                OpenApi31Schema definition = (OpenApi31Schema) doc31.getComponents().getSchemas().get(cname);
                pname = CodegenUtil.schemaToPackageName(definition, pname);
            }
        }
        cname = toClassName(settings, cname);
        return pname + "." + StringUtils.capitalize(cname);
    }

    public static String toClassName(JaxRsProjectSettings settings, String name) {
        return settings.getClassNamePrefix() + StringUtils.capitalize(sanitizeClassName(name)) + settings.getClassNameSuffix();
    }

    public static String sanitizeClassName(final String name) {
        return name.replaceAll("[^a-zA-Z0-9]", "");
    }

    public static JsonNode getExtension(Extensible node, String name) {
        if (node != null && node.getExtensions() != null && node.getExtensions().containsKey(name)) {
            return node.getExtensions().get(name);
        }
        return null;
    }

    public static List<String> toStringList(StringStringListUnion union) {
        return Optional.ofNullable(union)
                .filter(u -> Objects.nonNull(u.unionValue()))
                .map(u -> u.isStringList() ? u.asStringList() : new ArrayList<>(List.of(u.asString())))
                .orElse(null);
    }

    public static boolean containsValue(StringStringListUnion union, String... values) {
        if (union == null) {
            return false;
        }

        if (union.isStringList()) {
            return union.asStringList().stream().anyMatch(Arrays.asList(values)::contains);
        }

        return Arrays.asList(values).contains(union.asString());
    }

    /**
     * Returns the Java map type recorded (as {@link #EXISTING_JAVA_TYPE}) on the schema definition
     * referenced by the given $ref, or null if the referenced schema is not a map type. Such schemas
     * do not result in a generated bean class, so references to them must use the map type instead.
     *
     * @param document the document containing the schema definitions
     * @param schemaRef the $ref value
     * @return the Java map type, or null
     */
    public static String refToExistingMapType(Document document, String schemaRef) {
        if (schemaRef == null || !schemaRef.startsWith("#/components/schemas/")
                || !(document instanceof OpenApi31Document)) {
            return null;
        }
        OpenApi31Document doc31 = (OpenApi31Document) document;
        if (doc31.getComponents() == null || doc31.getComponents().getSchemas() == null) {
            return null;
        }
        OpenApi31Schema definition = (OpenApi31Schema) doc31.getComponents().getSchemas().get(schemaRef.substring(21));
        return getExistingMapType(definition);
    }

    /**
     * Returns the Java map type recorded (as {@link #EXISTING_JAVA_TYPE}) on the given schema, or
     * null if there is none.
     *
     * @param schema the schema (may be null)
     * @return the Java map type, or null
     */
    public static String getExistingMapType(OpenApi31Schema schema) {
        if (schema == null) {
            return null;
        }
        String existingJavaType = getExistingJavaType(schema);
        return existingJavaType != null && existingJavaType.startsWith(JAVA_MAP_TYPE_PREFIX) ? existingJavaType : null;
    }

    /**
     * Returns the {@link #EXISTING_JAVA_TYPE} (extra) property recorded on the given schema by the
     * pre-processors, or null if there is none.
     *
     * @param schema the schema (may be null)
     * @return the existing Java type, or null
     */
    public static String getExistingJavaType(OpenApi31Schema schema) {
        if (schema == null) {
            return null;
        }
        JsonNode existingJavaType = schema.getExtraProperty(EXISTING_JAVA_TYPE);
        return existingJavaType != null && existingJavaType.isTextual() ? existingJavaType.asText() : null;
    }

    /**
     * Resolves the fully qualified Java type for the given schema. This is the single place used to
     * compute the Java types of map values (see {@code OpenApiMapDataTypeProcessor}); its mapping of
     * OpenAPI types/formats to Java types intentionally mirrors {@code OpenApi2JaxRs#generateTypeName}.
     *
     * @param settings the project settings
     * @param document the document being processed (used to resolve $refs)
     * @param schema the schema to resolve (may be null)
     * @param defaultPackage the package for generated bean classes
     * @return the fully qualified Java type, never null
     */
    public static String resolveJavaType(JaxRsProjectSettings settings, Document document, OpenApi31Schema schema,
            String defaultPackage) {
        if (schema == null) {
            return "java.lang.Object";
        }

        if (schema.get$ref() != null && !schema.get$ref().isBlank()) {
            String mapType = refToExistingMapType(document, schema.get$ref());
            return mapType != null ? mapType : schemaRefToFQCN(settings, document, schema.get$ref(), defaultPackage);
        }

        String existingJavaType = getExistingJavaType(schema);
        if (existingJavaType != null) {
            return BYTE_ARRAY_REPRESENTATION.equals(existingJavaType) ? "byte[]" : existingJavaType;
        }

        String format = schema.getFormat();
        if (containsValue(schema.getType(), "array")) {
            String itemType = resolveJavaType(settings, document, (OpenApi31Schema) schema.getItems(), defaultPackage);
            String collection = Boolean.TRUE.equals(schema.isUniqueItems()) ? "java.util.Set<" : "java.util.List<";
            return collection + itemType + ">";
        }
        if (containsValue(schema.getType(), "string")) {
            if ("date".equals(format) || "date-time".equals(format)) {
                return "java.util.Date";
            }
            if ("byte".equals(format)) {
                return "byte[]";
            }
            return "java.lang.String";
        }
        if (containsValue(schema.getType(), "integer")) {
            if ("int64".equals(format) || "utc-millisec".equals(format)) {
                return "java.lang.Long";
            }
            if ("int32".equals(format)) {
                return "java.lang.Integer";
            }
            return "java.math.BigInteger";
        }
        if (containsValue(schema.getType(), "number")) {
            if ("float".equals(format)) {
                return "java.lang.Float";
            }
            if ("double".equals(format)) {
                return "java.lang.Double";
            }
            return "java.math.BigDecimal";
        }
        if (containsValue(schema.getType(), "boolean")) {
            return "java.lang.Boolean";
        }
        if (containsValue(schema.getType(), "object")
                && schema.getAdditionalProperties() != null && schema.getAdditionalProperties().isSchema()) {
            String valueType = resolveJavaType(settings, document,
                    (OpenApi31Schema) schema.getAdditionalProperties().asSchema(), defaultPackage);
            return JAVA_MAP_TYPE_PREFIX + "String, " + valueType + ">";
        }

        return "java.lang.Object";
    }
}
