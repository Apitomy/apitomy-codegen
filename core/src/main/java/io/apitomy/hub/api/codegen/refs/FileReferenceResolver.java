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
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.apitomy.datamodels.Library;
import io.apitomy.datamodels.TraverserDirection;
import io.apitomy.datamodels.VisitorUtil;
import io.apitomy.datamodels.deref.AllReferenceableNodeVisitor;
import io.apitomy.datamodels.models.Document;
import io.apitomy.datamodels.models.Example;
import io.apitomy.datamodels.models.Link;
import io.apitomy.datamodels.models.ModelType;
import io.apitomy.datamodels.models.Node;
import io.apitomy.datamodels.models.Parameter;
import io.apitomy.datamodels.models.Referenceable;
import io.apitomy.datamodels.models.Schema;
import io.apitomy.datamodels.models.SecurityScheme;
import io.apitomy.datamodels.models.openapi.OpenApiCallback;
import io.apitomy.datamodels.models.openapi.OpenApiHeader;
import io.apitomy.datamodels.models.openapi.OpenApiPathItem;
import io.apitomy.datamodels.models.openapi.OpenApiRequestBody;
import io.apitomy.datamodels.models.openapi.OpenApiResponse;
import io.apitomy.datamodels.refs.IReferenceResolver;
import io.apitomy.datamodels.refs.LocalReferenceResolver;
import io.apitomy.datamodels.refs.Reference;
import io.apitomy.datamodels.refs.ResolvedReference;

/**
 * A reference resolver that resolves $refs pointing at other, local files (relative to the
 * entry document's location), so that a multi-file OpenAPI spec can be dereferenced into a
 * single, self-contained document before the rest of the codegen pipeline runs.
 *
 * <p>Supported:</p>
 * <ul>
 *   <li>JSON and YAML external files</li>
 *   <li>Files that are complete OpenAPI documents (any version, upgraded to 3.1) as well as
 *       headless fragment files that contain only e.g. {@code components: {...}}</li>
 *   <li>Whole-file refs (no {@code #/...} fragment) to a file that contains a single object,
 *       e.g. {@code $ref: ./schemas/Pet.yaml}</li>
 *   <li>Chains of relative refs across several files/directories (e.g. A refers to
 *       {@code ./common/types.yaml} which in turn refers to {@code ./more.yaml}, relative to
 *       its own directory).  When an external file is loaded, every relative file $ref in it
 *       is rewritten to be relative to the root document's directory, so the refs stay
 *       correct no matter how the data model later copies or re-parents the imported nodes.</li>
 * </ul>
 *
 * <p>Remote ({@code scheme://...}) refs are out of scope and are left unresolved.</p>
 *
 * <p>Any local file ref that cannot be resolved (missing file, unparseable content, missing
 * fragment, or a path outside the allowed root directory) fails fast with an
 * {@link ExternalReferenceException}.  By default the allowed root is the root document's
 * directory, so a spec cannot read arbitrary files on disk (e.g. {@code ../../../etc/passwd});
 * use {@link #FileReferenceResolver(Path, Path)} to allow a wider directory.</p>
 *
 * @author Matheus Cruz
 */
public class FileReferenceResolver implements IReferenceResolver {

    private static final Pattern REMOTE_REF = Pattern.compile("^[a-zA-Z][a-zA-Z0-9+.-]*://.*");
    private static final String SYNTHETIC_NAME = "__whole_file__";
    /**
     * Whole-file refs (e.g. "./schemas/Pet.yaml") are rewritten to "schemas/Pet.yaml#/__whole_file__/Pet":
     * the data model names imported components after the last segment of the $ref, so this
     * makes the component (and generated bean) "Pet" rather than "Pet.yaml".
     */
    private static final String WHOLE_FILE_FRAGMENT_PREFIX = "#/" + SYNTHETIC_NAME + "/";

    private final Path rootBaseDir;
    private final Path allowedRootDir;
    private final LocalReferenceResolver fragmentResolver = new LocalReferenceResolver();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final Map<Path, ObjectNode> fileCache = new HashMap<>();
    private int resolvedCount = 0;

    /**
     * Constructor.  Refs are only allowed to point at files inside {@code rootBaseDir}.
     *
     * @param rootBaseDir the directory containing the entry/root document, used to resolve
     *                    relative refs found directly in that document
     */
    public FileReferenceResolver(Path rootBaseDir) {
        this(rootBaseDir, rootBaseDir);
    }

    /**
     * Constructor.
     *
     * @param rootBaseDir    the directory containing the entry/root document, used to resolve
     *                       relative refs found directly in that document
     * @param allowedRootDir refs that resolve to a file outside this directory are rejected
     */
    public FileReferenceResolver(Path rootBaseDir, Path allowedRootDir) {
        this.rootBaseDir = rootBaseDir.toAbsolutePath().normalize();
        this.allowedRootDir = (allowedRootDir != null ? allowedRootDir : rootBaseDir).toAbsolutePath().normalize();
    }

    /**
     * Dereferences the given root document using this resolver: every resolvable local file
     * $ref is pulled into (a copy of) the document.
     *
     * The root document's own file refs are first normalized the same way refs in external
     * files are (e.g. "./a.json#/X" becomes "a.json#/X"), so that the data model, which caches
     * resolutions by ref string, recognizes two refs to the same target as the same ref.  This
     * avoids importing the same component twice when files refer back to each other.
     *
     * @param document the root document
     * @return the dereferenced copy of the document
     * @throws ExternalReferenceException if a local file $ref cannot be resolved
     */
    public Document dereference(Document document) {
        Document copy = Library.cloneDocument(document);
        VisitorUtil.visitTree(copy, new AllReferenceableNodeVisitor() {
            @Override
            protected void visitReferenceableNode(Referenceable refNode) {
                String ref = refNode.get$ref();
                if (ref != null && !ref.startsWith("#")) {
                    refNode.set$ref(rewriteRef(ref, rootBaseDir.resolve("_")));
                }
            }
        }, TraverserDirection.down);
        return Library.dereferenceDocument(copy, this, false);
    }

    /**
     * @return the number of external file refs this resolver has resolved so far
     */
    public int getResolvedCount() {
        return resolvedCount;
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

        // All refs we see are relative to the root dir: refs in the root document already are,
        // and refs in external files were rewritten to be when the file was loaded.
        Path targetFile = rootBaseDir.resolve(filePart).normalize();
        ObjectNode json = fileCache.computeIfAbsent(targetFile, f -> loadFile(f, ref));

        String fragment = reference.getRelPart();
        ResolvedReference resolved = fragment == null || fragment.startsWith(WHOLE_FILE_FRAGMENT_PREFIX)
                ? resolveWholeFile(json, ref, from)
                : resolveFragment(json, fragment, ref);
        resolvedCount++;
        return resolved;
    }

    private ResolvedReference resolveFragment(ObjectNode json, String fragment, String ref) {
        ObjectNode docJson = json.deepCopy();
        if (!isOpenApiDocument(docJson)) {
            // Headless fragment file (e.g. just "components: {...}"), treat it as an
            // OpenAPI 3.1 fragment so it can be read by the data model.
            docJson.put("openapi", "3.1.0");
        }
        // A fresh Document per resolution, so the importer can freely re-parent/copy its nodes.
        Document externalDoc = toOpenApi31(docJson);
        ResolvedReference resolved = fragmentResolver.resolveRef(fragment, externalDoc);
        if (resolved == null) {
            throw new ExternalReferenceException("Could not resolve $ref '" + ref + "': fragment '"
                    + fragment + "' was not found in the referenced file.");
        }
        return resolved;
    }

    /**
     * Resolves a ref with no fragment: the whole file is the referenced object (e.g. a schema).
     * It is wrapped into a synthetic document under the components section matching the type of
     * the node holding the $ref, so that the data model reads it as the right type of node.
     */
    private ResolvedReference resolveWholeFile(ObjectNode json, String ref, Node from) {
        if (isOpenApiDocument(json)) {
            throw new ExternalReferenceException("Could not resolve $ref '" + ref
                    + "': it points at a whole OpenAPI document; add a fragment such as '#/components/schemas/Name'.");
        }
        String componentType = componentTypeOf(from);
        if (componentType == null) {
            throw new ExternalReferenceException("Could not resolve $ref '" + ref
                    + "': whole-file refs are not supported for " + from.getClass().getSimpleName() + " nodes.");
        }
        ObjectNode docJson = yamlMapper.createObjectNode();
        docJson.put("openapi", "3.1.0");
        docJson.putObject("components").putObject(componentType).set(SYNTHETIC_NAME, json.deepCopy());
        Document externalDoc = toOpenApi31(docJson);
        return fragmentResolver.resolveRef("#/components/" + componentType + "/" + SYNTHETIC_NAME, externalDoc);
    }

    private static String componentTypeOf(Node node) {
        if (node instanceof Schema) {
            return "schemas";
        } else if (node instanceof OpenApiResponse) {
            return "responses";
        } else if (node instanceof Parameter) {
            return "parameters";
        } else if (node instanceof Example) {
            return "examples";
        } else if (node instanceof OpenApiRequestBody) {
            return "requestBodies";
        } else if (node instanceof OpenApiHeader) {
            return "headers";
        } else if (node instanceof SecurityScheme) {
            return "securitySchemes";
        } else if (node instanceof Link) {
            return "links";
        } else if (node instanceof OpenApiCallback) {
            return "callbacks";
        } else if (node instanceof OpenApiPathItem) {
            return "pathItems";
        }
        return null;
    }

    private static Document toOpenApi31(ObjectNode json) {
        return Library.transformDocument(Library.readDocument(json), ModelType.OPENAPI31);
    }

    private static boolean isOpenApiDocument(ObjectNode json) {
        return json.has("openapi") || json.has("swagger");
    }

    private static boolean isRemote(String filePart) {
        return REMOTE_REF.matcher(filePart).matches();
    }

    /**
     * Loads and parses an external file (JSON or YAML), failing with a descriptive error if
     * it can't be read.  Relative file refs inside it are rewritten relative to the root dir.
     */
    private ObjectNode loadFile(Path file, String ref) {
        checkAllowed(file, ref);
        if (!Files.isRegularFile(file)) {
            throw new ExternalReferenceException("Could not resolve $ref '" + ref + "': file not found: " + file);
        }
        JsonNode content;
        try {
            content = yamlMapper.readTree(file.toFile());
        } catch (IOException e) {
            throw new ExternalReferenceException("Could not resolve $ref '" + ref + "': failed to parse " + file
                    + ": " + e.getMessage(), e);
        }
        if (!(content instanceof ObjectNode)) {
            throw new ExternalReferenceException("Could not resolve $ref '" + ref + "': " + file
                    + " does not contain a JSON/YAML object.");
        }
        ObjectNode json = (ObjectNode) content;
        rewriteRefs(json, file);
        return json;
    }

    private void checkAllowed(Path file, String ref) {
        Path real = file;
        try {
            if (Files.exists(file)) {
                // Follow symlinks so that they can't be used to escape the allowed root either.
                real = file.toRealPath();
            }
        } catch (IOException e) {
            // Fall back to the normalized path.
        }
        Path allowed = allowedRootDir;
        try {
            allowed = allowedRootDir.toRealPath();
        } catch (IOException e) {
            // Fall back to the normalized path.
        }
        if (!real.startsWith(allowed)) {
            throw new ExternalReferenceException("Could not resolve $ref '" + ref + "': " + file
                    + " is outside of the allowed directory " + allowedRootDir);
        }
    }

    /**
     * Rewrites every "$ref" in the given (external) file's content:
     * <ul>
     *   <li>relative file refs are made relative to the root document's directory</li>
     *   <li>same-file refs ("#/...") get the file's own (root-relative) path prepended</li>
     * </ul>
     * This way every ref in the content means the same thing once it has been imported into
     * the root document, independently of which file it originally came from.
     */
    private void rewriteRefs(JsonNode node, Path file) {
        if (node.isObject()) {
            ObjectNode obj = (ObjectNode) node;
            JsonNode refNode = obj.get("$ref");
            if (refNode != null && refNode.isTextual()) {
                obj.put("$ref", rewriteRef(refNode.asText(), file));
            }
            for (Map.Entry<String, JsonNode> field : obj.properties()) {
                if (!"$ref".equals(field.getKey())) {
                    rewriteRefs(field.getValue(), file);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode item : node) {
                rewriteRefs(item, file);
            }
        }
    }

    private String rewriteRef(String ref, Path file) {
        Reference reference;
        try {
            reference = new Reference(ref);
        } catch (IllegalArgumentException e) {
            return ref;
        }
        String filePart = reference.getAbsPart();
        if (filePart != null && isRemote(filePart)) {
            return ref;
        }
        Path target = filePart == null ? file : file.getParent().resolve(filePart).normalize();
        String rootRelative = rootBaseDir.relativize(target).toString().replace('\\', '/');
        String fragment = reference.getRelPart();
        if (fragment == null) {
            String fileName = target.getFileName().toString();
            int dot = fileName.lastIndexOf('.');
            String name = dot > 0 ? fileName.substring(0, dot) : fileName;
            return rootRelative + WHOLE_FILE_FRAGMENT_PREFIX + name;
        }
        return rootRelative + fragment;
    }

}
