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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.apitomy.datamodels.Library;
import io.apitomy.datamodels.TraverserDirection;
import io.apitomy.datamodels.VisitorUtil;
import io.apitomy.datamodels.models.Document;
import io.apitomy.datamodels.models.ModelType;
import io.apitomy.datamodels.models.Node;
import io.apitomy.datamodels.models.visitors.AllNodeVisitor;
import io.apitomy.datamodels.refs.IReferenceResolver;
import io.apitomy.datamodels.refs.LocalReferenceResolver;
import io.apitomy.datamodels.refs.Reference;
import io.apitomy.datamodels.refs.ResolvedReference;

/**
 * A reference resolver that resolves $refs pointing at other, local files (relative to the
 * entry document's location), so that a multi-file OpenAPI spec can be dereferenced into a
 * single, self-contained document before the rest of the codegen pipeline runs.
 *
 * Supports chains of relative refs across several files/directories (e.g. A refers to
 * "./common/types.yaml" which in turn refers to "./more.yaml" relative to its own directory)
 * by tagging every node loaded from an external file with that file's own directory; the tag
 * survives being imported/attached into the main document, so nested refs are always resolved
 * relative to the file they actually came from.
 *
 * Remote (http(s)://) refs are out of scope and are left unresolved.
 *
 * @author eric.wittmann@gmail.com
 */
public class FileReferenceResolver implements IReferenceResolver {

    private static final String ATTR_BASE_DIR = "io.apitomy.codegen.refs.baseDir";

    private final Path rootBaseDir;
    private final LocalReferenceResolver fragmentResolver = new LocalReferenceResolver();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final Map<Path, Document> documentCache = new HashMap<>();

    /**
     * Constructor.
     *
     * @param rootBaseDir the directory containing the entry/root document, used to resolve
     *                     relative refs found directly in that document
     */
    public FileReferenceResolver(Path rootBaseDir) {
        this.rootBaseDir = rootBaseDir;
    }

    @Override
    public ResolvedReference resolveRef(String ref, Node from) {
        Reference reference = new Reference(ref);
        String filePart = reference.getAbsPart();
        if (filePart == null || isRemote(filePart)) {
            // Pure same-document fragment (already handled by the framework) or an out-of-scope
            // remote reference.
            return null;
        }

        Path baseDir = currentBaseDir(from);
        Path targetFile = baseDir.resolve(filePart).normalize();

        Document externalDoc = documentCache.computeIfAbsent(targetFile, this::loadDocument);
        if (externalDoc == null) {
            return null;
        }

        String fragment = reference.getRelPart();
        if (fragment == null) {
            // Whole-file reference (no "#/..." fragment), return the document itself.
            return ResolvedReference.fromNode(externalDoc);
        }
        return fragmentResolver.resolveRef(fragment, externalDoc);
    }

    /**
     * Determines the directory that a relative file reference found on the given node should be
     * resolved against: the directory of the external file that node was itself loaded from, or
     * the root document's directory if the node hasn't been tagged (i.e. it belongs to the
     * original/root document).
     *
     */
    private Path currentBaseDir(Node from) {
        Object attr = from.getNodeAttribute(ATTR_BASE_DIR);
        return attr != null ? (Path) attr : rootBaseDir;
    }

    private static boolean isRemote(String filePart) {
        return filePart.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");
    }

    /**
     * Loads and parses an external file (JSON or YAML) into a Document, tagging every node in it
     * with the file's own directory so that nested relative refs resolve correctly.
     *
     */
    private Document loadDocument(Path file) {
        try {
            if (!Files.isRegularFile(file)) {
                return null;
            }
            ObjectNode json = (ObjectNode) yamlMapper.readTree(file.toFile());
            if (!json.has("openapi") && !json.has("asyncapi") && !json.has("swagger")) {
                // Headless fragment file (e.g. just "components: {...}"), treat it as an
                // OpenAPI 3.1 fragment so it can be read by the data model.
                json.put("openapi", "3.1.0");
            }
            Document externalDoc = Library.readDocument(json);
            externalDoc = Library.transformDocument(externalDoc, ModelType.OPENAPI31);
            tagBaseDir(externalDoc, file.getParent());
            return externalDoc;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static void tagBaseDir(Document document, Path baseDir) {
        VisitorUtil.visitTree(document, new AllNodeVisitor() {
            @Override
            protected void visitNode(Node node) {
                node.setNodeAttribute(ATTR_BASE_DIR, baseDir);
            }
        }, TraverserDirection.down);
    }

}
