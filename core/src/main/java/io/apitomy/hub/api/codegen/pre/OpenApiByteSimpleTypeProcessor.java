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

import static io.apitomy.hub.api.codegen.util.CodegenUtil.containsValue;

import io.apitomy.datamodels.models.openapi.v3x.v31.OpenApi31Schema;
import io.apitomy.datamodels.models.union.StringUnionValueImpl;
import io.apitomy.hub.api.codegen.jaxrs.TraversingOpenApi31VisitorAdapter;
import io.apitomy.hub.api.codegen.util.CodegenUtil;

/**
 * @author eric.wittmann@gmail.com
 */
public class OpenApiByteSimpleTypeProcessor extends TraversingOpenApi31VisitorAdapter {

    /**
     * @see io.apitomy.datamodels.models.openapi.v3x.v31.visitors.OpenApi31VisitorAdapter#visitSchema(io.apitomy.datamodels.models.Schema)
     */
    @Override
    public void visitSchema(io.apitomy.datamodels.models.Schema node) {
        OpenApi31Schema schema = (OpenApi31Schema) node;
        if (containsValue(schema.getType(), "string") && "byte".equals(schema.getFormat())) {
            schema.setType(new StringUnionValueImpl("object"));
            // workaround for a jsonschema2pojo limitation
            schema.addExtraProperty(CodegenUtil.EXISTING_JAVA_TYPE, factory.textNode(CodegenUtil.BYTE_ARRAY_REPRESENTATION));
        }
    }

}
