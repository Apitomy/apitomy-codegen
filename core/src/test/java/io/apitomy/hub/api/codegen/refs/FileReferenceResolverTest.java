/*
 * Copyright 2026 JBoss Inc
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

package io.apitomy.hub.api.codegen.refs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.apitomy.datamodels.Library;
import io.apitomy.datamodels.models.Document;
import io.apitomy.datamodels.models.ModelType;
import io.apitomy.hub.api.codegen.JaxRsProjectSettings;
import io.apitomy.hub.api.codegen.OpenApi2JaxRs;

public class FileReferenceResolverTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    public void testYamlExternalFile() throws Exception {
        JsonNode result = dereference("yaml-api.json");
        assertNoExternalRefs(result);
        Assert.assertEquals("#/components/schemas/Pet", responseSchemaRef(result, "/pets"));
        Assert.assertEquals("string", schema(result, "Pet").at("/properties/name/type").asText());
    }

    /**
     * nested/a/types.json refers to "../b/item.yaml" (relative to its own directory, not the
     * root document's) and to "#/components/schemas/Status" (a same-file ref inside types.json).
     * OrderAlias is a $ref-only definition, which the data model inlines by *copying* the
     * external Order node into it (node attributes don't survive that), so this also checks the
     * nested refs still resolve relative to types.json after being copied into the main document.
     */
    @Test
    public void testNestedRelativeRefsFromSubdirectory() throws Exception {
        JsonNode result = dereference("nested-api.json");
        assertNoExternalRefs(result);
        Assert.assertEquals("#/components/schemas/OrderAlias", responseSchemaRef(result, "/orders"));

        Assert.assertEquals("string", schema(result, "Item").at("/properties/sku/type").asText());
        Assert.assertEquals("open", schema(result, "Status").at("/enum/0").asText());

        JsonNode alias = schema(result, "OrderAlias");
        Assert.assertFalse("OrderAlias should have been inlined", alias.has("$ref"));
        Assert.assertEquals("object", alias.path("type").asText());
        Assert.assertEquals("#/components/schemas/Item", alias.at("/properties/item/$ref").asText());
        Assert.assertEquals("#/components/schemas/Status", alias.at("/properties/status/$ref").asText());
    }

    @Test
    public void testFragmentOnlyFile() throws Exception {
        JsonNode result = dereference("fragment-api.json");
        assertNoExternalRefs(result);
        Assert.assertEquals("#/components/schemas/Tag", responseSchemaRef(result, "/tags"));
        Assert.assertEquals("string", schema(result, "Tag").at("/properties/label/type").asText());
    }

    @Test
    public void testWholeFileRef() throws Exception {
        JsonNode result = dereference("whole-api.json");
        assertNoExternalRefs(result);
        // Named after the file, without its extension.
        Assert.assertEquals("#/components/schemas/Owner", responseSchemaRef(result, "/owners"));
        Assert.assertEquals("string", schema(result, "Owner").at("/properties/name/type").asText());
    }

    @Test
    public void testMissingFileFailsFast() throws Exception {
        try {
            dereference("missing-api.json");
            Assert.fail("Expected an ExternalReferenceException");
        } catch (ExternalReferenceException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("does-not-exist.json"));
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("file not found"));
        }
    }

    /**
     * a.json and b.json reference each other; dereferencing must terminate.
     */
    @Test(timeout = 10_000)
    public void testFilesReferencingEachOther() throws Exception {
        JsonNode result = dereference("cycle-api.json");
        assertNoExternalRefs(result);
        Assert.assertEquals("#/components/schemas/A", responseSchemaRef(result, "/a"));
        Assert.assertEquals("#/components/schemas/B", schema(result, "A").at("/properties/b/$ref").asText());
        Assert.assertEquals("#/components/schemas/A", schema(result, "B").at("/properties/a/$ref").asText());
    }

    @Test
    public void testRefOutsideOfAllowedRootIsRejected() throws Exception {
        try {
            dereference("outside-api.json");
            Assert.fail("Expected an ExternalReferenceException");
        } catch (ExternalReferenceException e) {
            Assert.assertTrue(e.getMessage(), e.getMessage().contains("outside of the allowed directory"));
        }
    }

    @Test
    public void testRefOutsideOfDocDirAllowedWithWiderRoot() throws Exception {
        Path baseDir = resourceDir();
        JsonNode result = dereference("outside-api.json", new FileReferenceResolver(baseDir, baseDir.getParent()));
        assertNoExternalRefs(result);
        Assert.assertEquals("#/components/schemas/Pet", responseSchemaRef(result, "/pets"));
    }

    /**
     * The spec written into the generated project must be self-contained, since the referenced
     * files are not part of that project.
     */
    @Test
    public void testGeneratedSpecIsDereferenced() throws Exception {
        OpenApi2JaxRs generator = newGenerator();
        generator.setOpenApiDocument(resource("nested-api.json"));
        JsonNode spec = MAPPER.readTree(zipEntries(generator).get("src/main/resources/META-INF/openapi.json"));
        assertNoExternalRefs(spec);
        Assert.assertTrue(spec.at("/components/schemas/Item").isObject());
    }

    @Test
    public void testStringDocumentResetsBaseDirFromEarlierUrl() throws Exception {
        OpenApi2JaxRs generator = newGenerator();
        generator.setOpenApiDocument(resource("yaml-api.json"));
        String content = IOUtils.toString(resource("yaml-api.json"), StandardCharsets.UTF_8);
        generator.setOpenApiDocument(content);

        // No base dir any more, so the external ref is left alone and the spec is written as given.
        Map<String, String> entries = zipEntries(generator);
        Assert.assertEquals(content, entries.get("src/main/resources/META-INF/openapi.json"));
    }

    @Test
    public void testStringDocumentWithExplicitBaseDir() throws Exception {
        OpenApi2JaxRs generator = newGenerator();
        generator.setOpenApiDocument(new ByteArrayInputStream(
                IOUtils.toByteArray(resource("yaml-api.json"))));
        generator.setOpenApiDocumentBaseDir(resourceDir());

        Map<String, String> entries = zipEntries(generator);
        Assert.assertTrue(entries.containsKey("src/main/java/org/example/api/beans/Pet.java"));
        assertNoExternalRefs(MAPPER.readTree(entries.get("src/main/resources/META-INF/openapi.json")));
    }

    @Test
    public void testGenerationFailsWithMissingFile() throws Exception {
        OpenApi2JaxRs generator = newGenerator();
        generator.setOpenApiDocument(resource("missing-api.json"));
        Map<String, String> entries = zipEntries(generator);
        String failure = entries.get("PROJECT_GENERATION_FAILED.txt");
        Assert.assertNotNull(failure);
        Assert.assertTrue(failure, failure.contains("does-not-exist.json"));
    }

    private static JsonNode dereference(String name) throws Exception {
        return dereference(name, new FileReferenceResolver(resourceDir()));
    }

    private static JsonNode dereference(String name, FileReferenceResolver resolver) throws Exception {
        String content = IOUtils.toString(resource(name), StandardCharsets.UTF_8);
        Document doc = Library.transformDocument(Library.readDocumentFromJSONString(content), ModelType.OPENAPI31);
        Document result = resolver.dereference(doc);
        return MAPPER.readTree(Library.writeDocumentToJSONString(result));
    }

    private static String responseSchemaRef(JsonNode doc, String path) {
        return doc.path("paths").path(path).at("/get/responses/200/content/application~1json/schema/$ref").asText();
    }

    private static JsonNode schema(JsonNode doc, String name) {
        JsonNode schema = doc.at("/components/schemas/" + name);
        Assert.assertTrue("Missing schema " + name + " in " + doc, schema.isObject());
        return schema;
    }

    private static void assertNoExternalRefs(JsonNode doc) {
        List<String> refs = new ArrayList<>();
        collectRefs(doc, refs);
        for (String ref : refs) {
            Assert.assertTrue("Unresolved external $ref " + ref + " in " + doc, ref.startsWith("#/"));
        }
    }

    private static void collectRefs(JsonNode node, List<String> refs) {
        if (node.has("$ref")) {
            refs.add(node.get("$ref").asText());
        }
        node.forEach(child -> collectRefs(child, refs));
    }

    private static OpenApi2JaxRs newGenerator() {
        JaxRsProjectSettings settings = new JaxRsProjectSettings();
        settings.artifactId = "generated-api";
        settings.groupId = "org.example.api";
        settings.javaPackage = "org.example.api";
        OpenApi2JaxRs generator = new OpenApi2JaxRs();
        generator.setSettings(settings);
        return generator;
    }

    private static Map<String, String> zipEntries(OpenApi2JaxRs generator) throws IOException {
        Map<String, String> entries = new java.util.HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(generator.generate().toByteArray()))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entries.put(entry.getName(), IOUtils.toString(zis, StandardCharsets.UTF_8));
            }
        }
        return entries;
    }

    private static URL resource(String name) {
        URL url = FileReferenceResolverTest.class.getClassLoader().getResource("FileReferenceResolverTest/" + name);
        Assert.assertNotNull("Missing test resource " + name, url);
        return url;
    }

    private static Path resourceDir() throws URISyntaxException {
        Path dir = Paths.get(resource("yaml-api.json").toURI()).getParent();
        Assert.assertTrue(Files.isDirectory(dir));
        return dir;
    }

}
