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

    public static String resolveJavaType(JaxRsProjectSettings settings, Document document, OpenApi31Schema schema, String defaultPackage) {
        if (schema == null) {
            return "java.lang.Object";
        }

        if (schema.get$ref() != null && !schema.get$ref().isBlank()) {
            return schemaRefToFQCN(settings, document, schema.get$ref(), defaultPackage);
        }

        if (containsValue(schema.getType(), "array")) {
            String itemType = "java.lang.Object";
            if (schema.getItems() != null) {
                itemType = resolveJavaType(settings, document, (OpenApi31Schema) schema.getItems(), defaultPackage);
            }
            return "java.util.List<" + itemType + ">";
        }

        if (containsValue(schema.getType(), "string")) {
            return "java.lang.String";
        }
        if (containsValue(schema.getType(), "integer")) {
            return "java.lang.Integer";
        }
        if (containsValue(schema.getType(), "number")) {
            return "java.lang.Double";
        }
        if (containsValue(schema.getType(), "boolean")) {
            return "java.lang.Boolean";
        }
        if (containsValue(schema.getType(), "object")) {
            if (schema.getAdditionalProperties() != null && schema.getAdditionalProperties().isSchema()) {
                String valueType = resolveJavaType(settings, document, (OpenApi31Schema) schema.getAdditionalProperties().asSchema(), defaultPackage);
                return "java.util.Map<String, " + valueType + ">";
            }
            return "java.lang.Object";
        }

        return "java.lang.Object";
    }
}
