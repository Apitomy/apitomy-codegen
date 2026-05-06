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
 * @author eric.wittmann@gmail.com
 */
public class OpenApiMapDataTypeProcessor extends TraversingOpenApi31VisitorAdapter {

    private static final Set<String> EXTENSION_NAMES = Set.of("StringMap", "StringObjectMap", "ArrayMap");
    private final JaxRsProjectSettings settings;
    private final Document document;

    public OpenApiMapDataTypeProcessor(JaxRsProjectSettings settings, Document document) {
        this.settings = settings;
        this.document = document;
    }

    /**
     * @see io.apitomy.datamodels.models.openapi.v3x.v31.visitors.OpenApi31VisitorAdapter#visitSchema(io.apitomy.datamodels.models.Schema)
     */
    @Override
    public void visitSchema(Schema node) {
        if (NodeUtil.isDefinition(node)) {
            OpenApi31Schema schema = (OpenApi31Schema) node;
            JsonNode typeExtension = CodegenUtil.getExtension(schema, CodegenExtensions.TYPE);
            if (typeExtension == null || !typeExtension.isTextual() || !EXTENSION_NAMES.contains(typeExtension.asText())) {
                return;
            }

            OpenApi31Schema additionalProperties = schema.getAdditionalProperties() != null
                    && schema.getAdditionalProperties().isSchema()
                            ? (OpenApi31Schema) schema.getAdditionalProperties().asSchema()
                            : null;

            schema.setAdditionalProperties(null);
            schema.addExtraProperty("existingJavaType", factory.textNode(buildJavaType(typeExtension.asText(), additionalProperties)));
        }
    }

    private String buildJavaType(String alias, OpenApi31Schema additionalProperties) {
        if ("StringMap".equals(alias)) {
            return "java.util.Map<String, String>";
        }

        if ("StringObjectMap".equals(alias)) {
            return "java.util.Map<String, Object>";
        }

        if ("ArrayMap".equals(alias)) {
            String valueType = "java.lang.Object";
            if (additionalProperties != null) {
                valueType = CodegenUtil.resolveJavaType(settings, document, additionalProperties, settings.getJavaPackage() + ".beans");
            }
            return "java.util.Map<String, " + valueType + ">";
        }

        return "java.util.Map<String, Object>";
    }

}
