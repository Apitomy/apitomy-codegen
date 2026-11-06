/*
 * Copyright 2021 JBoss Inc
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

package io.apitomy.hub.api.codegen.pre;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import io.apitomy.datamodels.models.Document;
import io.apitomy.datamodels.models.Schema;
import io.apitomy.datamodels.models.openapi.v3x.v31.OpenApi31Schema;
import io.apitomy.datamodels.util.NodeUtil;
import io.apitomy.hub.api.codegen.CodegenExtensions;
import io.apitomy.hub.api.codegen.JaxRsProjectSettings;
import io.apitomy.hub.api.codegen.jaxrs.TraversingOpenApi31VisitorAdapter;
import io.apitomy.hub.api.codegen.util.CodegenUtil;

/**
 * Converts schema definitions tagged with a map {@code x-codegen-type} ({@code StringMap},
 * {@code StringObjectMap} or {@code ArrayMap}) into a Java map type by recording it as the schema's
 * {@code existingJavaType} and removing {@code additionalProperties}. The recorded type is then used
 * both by jsonschema2pojo (bean properties) and by the JAX-RS interface generator (method arguments
 * and return types).
 *
 * @author eric.wittmann@gmail.com
 */
public class OpenApiMapDataTypeProcessor extends TraversingOpenApi31VisitorAdapter {

    private static final String STRING_MAP = "StringMap";
    private static final String STRING_OBJECT_MAP = "StringObjectMap";
    private static final String ARRAY_MAP = "ArrayMap";

    /** The set of {@code x-codegen-type} values that are mapped to a Java map type. */
    public static final Set<String> MAP_TYPES = Set.of(STRING_MAP, STRING_OBJECT_MAP, ARRAY_MAP);

    private final JaxRsProjectSettings settings;
    private final Document document;

    /**
     * Constructor.
     *
     * @param settings the project settings (used to compute bean class names)
     * @param document the document being processed (used to resolve $refs)
     */
    public OpenApiMapDataTypeProcessor(JaxRsProjectSettings settings, Document document) {
        this.settings = settings;
        this.document = document;
    }

    /**
     * @see io.apitomy.datamodels.models.openapi.v3x.v31.visitors.OpenApi31VisitorAdapter#visitSchema(io.apitomy.datamodels.models.Schema)
     */
    @Override
    public void visitSchema(Schema node) {
        if (!NodeUtil.isDefinition(node)) {
            return;
        }
        OpenApi31Schema schema = (OpenApi31Schema) node;
        JsonNode typeExtension = CodegenUtil.getExtension(schema, CodegenExtensions.TYPE);
        if (typeExtension == null || !typeExtension.isTextual() || !MAP_TYPES.contains(typeExtension.asText())) {
            return;
        }

        OpenApi31Schema additionalProperties = schema.getAdditionalProperties() != null
                && schema.getAdditionalProperties().isSchema()
                        ? (OpenApi31Schema) schema.getAdditionalProperties().asSchema()
                        : null;

        String javaType = buildJavaType(typeExtension.asText(), additionalProperties);
        schema.setAdditionalProperties(null);
        schema.addExtraProperty(CodegenUtil.EXISTING_JAVA_TYPE, factory.textNode(javaType));
    }

    /**
     * Builds the Java map type for the given map alias. For {@code ArrayMap} the value type is
     * derived from {@code additionalProperties}; if that is missing, {@code Object} is used.
     */
    private String buildJavaType(String alias, OpenApi31Schema additionalProperties) {
        switch (alias) {
            case STRING_MAP:
                return CodegenUtil.JAVA_MAP_TYPE_PREFIX + "String, String>";
            case ARRAY_MAP:
                String valueType = CodegenUtil.resolveJavaType(settings, document, additionalProperties,
                        settings.getJavaPackage() + ".beans");
                return CodegenUtil.JAVA_MAP_TYPE_PREFIX + "String, " + valueType + ">";
            case STRING_OBJECT_MAP:
            default:
                return CodegenUtil.JAVA_MAP_TYPE_PREFIX + "String, Object>";
        }
    }

}
