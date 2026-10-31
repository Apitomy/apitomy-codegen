/*
 * Copyright 2018 JBoss Inc
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

package io.apitomy.hub.api.codegen;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author eric.wittmann@gmail.com
 */
public class OpenApi2JaxRsTest extends OpenApi2TestBase {

    private static enum UpdateOnly {
        yes, no
    }
    private static enum Reactive {
        yes, no
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFull() throws IOException {
        doFullTest("OpenApi2JaxRsTest/beer-api.json", UpdateOnly.no, Reactive.no, "_expected-full/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFullPrefixed() throws IOException {
        doFullTest("OpenApi2JaxRsTest/beer-api.json", UpdateOnly.no, Reactive.no, false,
                "_expected-full-prefixed/generated-api", "Test", "", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFullSuffixed() throws IOException {
        doFullTest("OpenApi2JaxRsTest/beer-api.json", UpdateOnly.no, Reactive.no, false,
                "_expected-full-suffixed/generated-api", "", "Test", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFull_GatewayApi() throws IOException {
        doFullTest("OpenApi2JaxRsTest/gateway-api.json", UpdateOnly.no, Reactive.no, "_expected-gatewayApi-full/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFull_RegistryApi() throws IOException {
        doFullTest("OpenApi2JaxRsTest/registry-api.json", UpdateOnly.no, Reactive.no, "_expected-registryApi-full/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFull_RegistryApiV2() throws IOException {
        doFullTest("OpenApi2JaxRsTest/registry-api-v2.json", UpdateOnly.yes, Reactive.no, "_expected-registry-api-v2/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateUpdateOnly() throws IOException {
        doFullTest("OpenApi2JaxRsTest/beer-api.json", UpdateOnly.yes, Reactive.no, "_expected-full/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFullReactive() throws IOException {
        doFullTest("OpenApi2JaxRsTest/beer-api.json", UpdateOnly.no, Reactive.yes, "_expected-reactive-full/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFullGenericReturnType() throws IOException {
        doFullTest("OpenApi2JaxRsTest/beer-api.json", UpdateOnly.no, Reactive.no, false,
                "_expected-genericReturnType-full/generated-api", "", "",
                "org.jboss.resteasy.reactive.RestResponse", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFullGenericReturnTypeReactive() throws IOException {
        doFullTest("OpenApi2JaxRsTest/beer-api.json", UpdateOnly.no, Reactive.yes, false,
                "_expected-genericReturnTypeReactive-full/generated-api", "", "",
                "org.jboss.resteasy.reactive.RestResponse", false);
    }

    @Test
    public void testGenerateFullReactiveWithPrimitives() throws IOException {
        doFullTest("OpenApi2JaxRsTest/reactive-with-primitives.json", UpdateOnly.no, Reactive.yes, "_expected-reactive-with-primitives/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateWithCLIGenCI() throws IOException {
        doFullTest("OpenApi2JaxRsTest/beer-api.json", UpdateOnly.no, Reactive.no, true,
                "_expected-full-with-ci/generated-api", "", "", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFull_Issue885Api() throws IOException {
        // Note: I can't seem to get this working in the maven build, but it works in Eclipse.
        doFullTest("OpenApi2JaxRsTest/issue-885-api.json", UpdateOnly.no, Reactive.no, "_expected-issue885Api-full/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFull_MultipleMediaTypes() throws IOException {
        doFullTest("OpenApi2JaxRsTest/mmt-api.json", UpdateOnly.no, Reactive.no, "_expected-mmt-full/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFull_ContextRoot() throws IOException {
        doFullTest("OpenApi2JaxRsTest/context-root.json", UpdateOnly.no, Reactive.no, "_expected-context-root/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFull_SimpleType() throws IOException {
        doFullTest("OpenApi2JaxRsTest/simple-type.json", UpdateOnly.no, Reactive.no, "_expected-simple-type/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFull_ArrayMapInline() throws IOException {
        doFullTest("OpenApi2JaxRsTest/array-map-inline.json", UpdateOnly.no, Reactive.no,
                "_expected-array-map-inline/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFull_BeanAnnotations() throws IOException {
        doFullTest("OpenApi2JaxRsTest/bean-annotations.json", UpdateOnly.no, Reactive.no, "_expected-bean-annotations/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFull_Inheritance() throws IOException {
        doFullTest("OpenApi2JaxRsTest/inheritance.json", UpdateOnly.yes, Reactive.no, "_expected-inheritance/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFull_NZDAX() throws IOException {
        doFullTest("OpenApi2JaxRsTest/nzdax.json", UpdateOnly.yes, Reactive.no, "_expected-nzdax/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateFull_CustomResponseType() throws IOException {
        doFullTest("OpenApi2JaxRsTest/custom-response-type.json", UpdateOnly.yes, Reactive.no, "_expected-custom-response-type/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testSchemaExtends() throws IOException {
        doFullTest("OpenApi2JaxRsTest/schema-extends.json", UpdateOnly.yes, Reactive.no, "_expected-schema-extends/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testMasStudio() throws IOException {
        doFullTest("OpenApi2JaxRsTest/mas-studio.json", UpdateOnly.no, Reactive.no, "_expected-mas-studio/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testPetStore() throws IOException {
        doFullTest("OpenApi2JaxRsTest/petstore-openapi.json", UpdateOnly.no, Reactive.no, "_expected-petstore/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testConstraintParameters() throws IOException {
        doFullTest("OpenApi2JaxRsTest/constrained-parameters.json", UpdateOnly.no, Reactive.no, "_expected-constrained-parameters/generated-api", false);
    }

    @Test
    public void testSchemaWithDash() throws IOException {
        doFullTest("OpenApi2JaxRsTest/schema-with-dash.json", UpdateOnly.no, Reactive.no, "_expected-schema-with-dash/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     * Tests that enum values with special characters are properly sanitized.
     */
    @Test
    public void testEnumWithSpecialChars() throws IOException {
        doFullTest("OpenApi2JaxRsTest/enum-with-special-chars.json", UpdateOnly.no, Reactive.no, "_expected-enum-with-special-chars/generated-api", false);
    }

    /**
     * Test method for {@link io.apitomy.hub.api.codegen.OpenApi2JaxRs#generate()}.
     */
    @Test
    public void testGenerateMultipartFormContent() throws IOException {
        doFullTest("OpenApi2JaxRsTest/multipart-form-content.json", UpdateOnly.no, Reactive.no,
                   "_expected-multipart-form-content.generated-api", false);
    }

    /**
     * Test method for comprehensive multipart form data type support.
     */
    @Test
    public void testGenerateMultipartComprehensive() throws IOException {
        doFullTest("OpenApi2JaxRsTest/multipart-comprehensive.json", UpdateOnly.no, Reactive.no,
                   "_expected-multipart-comprehensive.generated-api", false);
    }

    @Test
    public void testMultipartArrayOnlyRef() throws IOException {
        doFullTest("OpenApi2JaxRsTest/multipart-array-only-ref.json", UpdateOnly.no, Reactive.no,
                   "_expected-multipart-array-only-ref/generated-api", false);
    }

    @Test
    public void testMultipartOpenapiV300() throws IOException {
        doFullTest("OpenApi2JaxRsTest/multipart-openapi-v-3-0-0.json", UpdateOnly.no, Reactive.no,
                   "_expected-multipart-openapi-v-3-0-0/generated-api", false);
    }

    @Test
    public void testMultipartEdgeCases() throws IOException {
        doFullTest("OpenApi2JaxRsTest/multipart-edge-cases.json", UpdateOnly.no, Reactive.no,
                   "_expected-multipart-edge-cases/generated-api", false);
    }

    /**
     * Test that the generator detects and reports duplicate method names with a error.
     * Two operations on the same interface resolve to method name "items":
     *   - GET /products (operationId: "items")
     *   - GET /products/items (no operationId, falls back to last path segment "items")
     */
    @Test
    public void testDuplicateMethodNameDetection() throws IOException {
        JaxRsProjectSettings settings = new JaxRsProjectSettings();
        settings.codeOnly = false;
        settings.reactive = false;
        settings.artifactId = "generated-api";
        settings.groupId = "org.example.api";
        settings.javaPackage = "org.example.api";

        OpenApi2JaxRs generator = new OpenApi2JaxRs();
        generator.setSettings(settings);
        generator.setUpdateOnly(false);
        generator.setOpenApiDocument(getClass().getClassLoader().getResource(
                "OpenApi2JaxRsTest/duplicate-method-name.json"));

        ByteArrayOutputStream outputStream = generator.generate();

        String errorContent = null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if ("PROJECT_GENERATION_FAILED.txt".equals(entry.getName())) {
                    errorContent = IOUtils.toString(zis, StandardCharsets.UTF_8);
                    break;
                }
            }
        }
        Assert.assertNotNull( errorContent);
        Assert.assertTrue(errorContent.contains("Duplicate method name"));
        Assert.assertTrue(errorContent.contains("items"));
        Assert.assertTrue( errorContent.contains("ProductsResource"));
    }

    /**
     * Shared test method.
     */
    private void doFullTest(String apiDef, UpdateOnly updateOnly, Reactive reactive, String expectedFilesPath, boolean debug) throws IOException {
        doFullTest(apiDef, updateOnly, reactive, false, expectedFilesPath, "", "", null, debug);
    }

    /**
     * Shared test method.
     */
    private void doFullTest(String apiDef, UpdateOnly updateOnly, Reactive reactive, boolean generateCLiGenCI,
            String expectedFilesPath, String namePrefix, String nameSuffix, boolean debug) throws IOException {
        doFullTest(apiDef, updateOnly, reactive, generateCLiGenCI, expectedFilesPath, namePrefix, nameSuffix, null, debug);
    }

    /**
     * Shared test method.
     */
    private void doFullTest(String apiDef, UpdateOnly updateOnly, Reactive reactive, boolean generateCLiGenCI,
            String expectedFilesPath, String namePrefix, String nameSuffix, String genericReturnType,
            boolean debug) throws IOException {
        JaxRsProjectSettings settings = new JaxRsProjectSettings();
        settings.codeOnly = false;
        settings.reactive = reactive == Reactive.yes;
        settings.cliGenCI = generateCLiGenCI;
        settings.artifactId = "generated-api";
        settings.groupId = "org.example.api";
        settings.javaPackage = "org.example.api";
        settings.classNamePrefix = namePrefix;
        settings.classNameSuffix = nameSuffix;
        settings.genericReturnType = genericReturnType;

        OpenApi2JaxRs generator = new OpenApi2JaxRs();
        generator.setSettings(settings);
        generator.setUpdateOnly(updateOnly == UpdateOnly.yes);
        generator.setOpenApiDocument(getClass().getClassLoader().getResource(apiDef));

        super.doFullTest(generator, expectedFilesPath, debug);
    }

}
