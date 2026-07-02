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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.lang.model.SourceVersion;

import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JFieldVar;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Parameter;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.AnnotationSource;
import org.jboss.forge.roaster.model.source.AnnotationTargetSource;
import org.jboss.forge.roaster.model.source.Import;
import org.jboss.forge.roaster.model.source.Importer;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;
import org.jboss.forge.roaster.model.source.MethodSource;
import org.jboss.forge.roaster.model.source.ParameterSource;
import org.jboss.forge.roaster.model.util.Types;
import org.jsonschema2pojo.Annotator;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.SchemaStore;
import org.jsonschema2pojo.rules.Rule;
import org.jsonschema2pojo.rules.RuleFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.codemodel.JClassContainer;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JType;

import io.apitomy.datamodels.Library;
import io.apitomy.datamodels.TraverserDirection;
import io.apitomy.datamodels.VisitorUtil;
import io.apitomy.datamodels.models.Document;
import io.apitomy.datamodels.models.Extensible;
import io.apitomy.datamodels.models.ModelType;
import io.apitomy.datamodels.models.openapi.OpenApiDocument;
import io.apitomy.hub.api.codegen.beans.CodegenBeanAnnotationDirective;
import io.apitomy.hub.api.codegen.beans.CodegenInfo;
import io.apitomy.hub.api.codegen.beans.CodegenJavaBean;
import io.apitomy.hub.api.codegen.beans.CodegenJavaInterface;
import io.apitomy.hub.api.codegen.beans.CodegenJavaSchema;
import io.apitomy.hub.api.codegen.jaxrs.CodegenTarget;
import io.apitomy.hub.api.codegen.jaxrs.InterfacesVisitor;
import io.apitomy.hub.api.codegen.jaxrs.OpenApi2CodegenVisitor;
import io.apitomy.hub.api.codegen.post.JavaBeanPostProcessor;
import io.apitomy.hub.api.codegen.pre.DocumentPreProcessor;
import io.apitomy.hub.api.codegen.util.CodegenUtil;
import io.apitomy.hub.api.codegen.util.IndexedCodeWriter;


/**
 * Class used to generate a simple JAX-RS project from an OpenAPI document.
 *
 * @author eric.wittmann@gmail.com
 */
public class OpenApi2JaxRs {

    static final Map<String, Type<?>> TYPE_CACHE = new ConcurrentHashMap<>();
    static final String OPENAPI_OPERATION_ANNOTATION = "org.eclipse.microprofile.openapi.annotations.Operation";
    private static final String MEDIA_TYPE_CONSTANT_PREFIX = "MediaType.";

    protected static ObjectMapper mapper = new ObjectMapper();
    protected static Charset utf8 = StandardCharsets.UTF_8;
    protected static final Type<?> VOID = parseType("java.lang.Void");

    protected static JavaBeanPostProcessor postProcessor = new JavaBeanPostProcessor();

    protected String openApiDoc;
    protected transient Document document;
    protected JaxRsProjectSettings settings;
    protected boolean updateOnly;

    private GenerationConfig config;

    /**
     * Constructor.
     */
    public OpenApi2JaxRs() {
        this.settings = new JaxRsProjectSettings();
        this.settings.artifactId = "generated-api";
        this.settings.groupId = "org.example.api";
        this.settings.javaPackage = "org.example.api";
    }

    /**
     * Configure the settings.
     *
     * @param settings
     */
    public void setSettings(JaxRsProjectSettings settings) {
        this.settings = settings;
    }

    /**
     * Sets the OpenAPI document.
     *
     * @param content
     * @throws IOException
     */
    public void setOpenApiDocument(String content) {
        this.openApiDoc = content;
    }

    /**
     * Sets the OpenAPI document via a URL to the content.
     *
     * @param url
     * @throws IOException
     */
    public void setOpenApiDocument(URL url) throws IOException {
        try (InputStream is = url.openStream()) {
            this.setOpenApiDocument(is);
        }
    }

    /**
     * Sets the OpenAPI document via an input stream.  The stream must be closed
     * by the caller.
     *
     * @param stream
     * @throws IOException
     */
    public void setOpenApiDocument(InputStream stream) throws IOException {
        this.openApiDoc = IOUtils.toString(stream, utf8);
    }

    protected GenerationConfig buildGenerationConfig() {
        return new DefaultGenerationConfig() {
            @Override
            public boolean isUsePrimitives() {
                return false;
            }

            @Override
            public boolean isIncludeHashcodeAndEquals() {
                return false;
            }

            @Override
            public boolean isIncludeAdditionalProperties() {
                return true;
            }

            @Override
            public boolean isIncludeToString() {
                return false;
            }

            @Override
            public boolean isFormatDateTimes() {
                return false;
            }

            @Override
            public boolean isIncludeJsr303Annotations() {
                return settings.useJsr303;
            }

            @Override
            public boolean isUseJakartaValidation() {
                // only allow if the settings say to use JSR-303 annotations
                return isIncludeJsr303Annotations();
            }

            @Override
            public boolean isGenerateBuilders() {
                return settings.generateBuilders;
            }
        };
    }

    /**
     * Generates a JaxRs project and streams the generated ZIP to the given
     * output stream.
     *
     * @param output
     * @throws IOException
     */
    public final void generate(OutputStream output) throws IOException {
        StringBuilder log = new StringBuilder();

        config = buildGenerationConfig();

        try (ZipOutputStream zos = new ZipOutputStream(output)) {
            try {
                CodegenInfo info = getInfoFromApiDoc();
                generateAll(info, log, zos);
                System.out.println(log);
            } catch (Exception e) {
                // If we get an error, put an PROJECT_GENERATION_ERROR file into the ZIP.
                zos.putNextEntry(new ZipEntry("PROJECT_GENERATION_FAILED.txt"));
                zos.write("An unexpected server error was encountered while generating the project.  See\r\n".getBytes());
                zos.write("the details of the error below.\r\n\r\n".getBytes());
                zos.write("Generation Log:\r\n\r\n".getBytes());
                zos.write(log.toString().getBytes(utf8));
                zos.write("\r\n\r\nServer Stack Trace:\r\n".getBytes());

                PrintWriter writer = new PrintWriter(zos);
                e.printStackTrace(writer);
                writer.flush();
                zos.closeEntry();
            }
        }
    }

    /**
     * Generates all of the content for storage in the ZIP.  Responsible for generating all classes
     * and other resources that make up the generated project.
     *
     * @param info
     * @param log
     * @param zipOutput
     * @throws IOException
     */
    protected void generateAll(CodegenInfo info, StringBuilder log, ZipOutputStream zipOutput) throws IOException {
        if (!this.updateOnly && !this.settings.codeOnly) {
            log.append("Generating pom.xml\r\n");
            String pomXml = generatePomXml(info);
            if (pomXml != null) {
                zipOutput.putNextEntry(new ZipEntry("pom.xml"));
                zipOutput.write(pomXml.getBytes(utf8));
                zipOutput.closeEntry();
            } else {
                throw new IllegalArgumentException("generated pom.xml content is null");
            }
        }

        if (this.settings.cliGenCI) {
            log.append("Generating .github/workflows/release_cli.yaml\r\n");
            zipOutput.putNextEntry(new ZipEntry(".github/workflows/release_cli.yaml"));
            zipOutput.write(IOUtils.toString(getCommonResource("release_cli.yaml"), StandardCharsets.UTF_8).getBytes(utf8));
            zipOutput.closeEntry();
        }

        if (settings.isIncludeSpec()) {
            String specPath = "META-INF/openapi.json";
            if (settings.isMavenFileStructure()) {
                specPath = "src/main/resources/" + specPath;
            }
            log.append("Generating " + specPath + "\r\n");
            zipOutput.putNextEntry(new ZipEntry(specPath));
            zipOutput.write(this.openApiDoc.getBytes(utf8));
            zipOutput.closeEntry();
        }

        if (!this.updateOnly) {
            String appFileName = javaPackageToZipPath(this.settings.javaPackage) + getMainClassName() + ".java";
            String jaxRsApp = generateJaxRsApplication(info);
            if (jaxRsApp != null) {
                log.append("Generating " + appFileName + "\r\n");
                zipOutput.putNextEntry(new ZipEntry(appFileName));
                zipOutput.write(jaxRsApp.getBytes(utf8));
                zipOutput.closeEntry();
            }
        }

        // Generate the java beans from data types
        IndexedCodeWriter codeWriter = new IndexedCodeWriter();
        for (CodegenJavaBean bean : info.getBeans()) {
            log.append("Generating Bean: " + bean.getPackage() + "." + CodegenUtil.toClassName(settings, bean.getName()) + "\r\n");
            generateJavaBean(bean, info, codeWriter);
        }
        // Post-process generated java bean classes
        for (String className : codeWriter.keys()) {
            ByteArrayOutputStream beanData = codeWriter.getContent(className);
            List<CodegenBeanAnnotationDirective> annotations = new ArrayList<>();
            annotations.addAll(info.getBeanAnnotations());
            CodegenJavaBean bean = codeWriter.getBean(className);
            if (bean != null && bean.getAnnotations() != null) {
                annotations.addAll(bean.getAnnotations());
            }

            ByteArrayOutputStream processedBeanData = postProcessor.process(className, annotations, beanData);
            if (beanData != processedBeanData) {
                codeWriter.set(className, processedBeanData);
            }
        }
        // Write all of the java beans classes to the ZIP file
        for (String key : codeWriter.keys()) {
            String javaClassFileName = javaClassToZipPath(key);
            log.append("Adding to zip: " + javaClassFileName + "\r\n");
            zipOutput.putNextEntry(new ZipEntry(javaClassFileName));
            zipOutput.write(codeWriter.getContent(key).toByteArray());
            zipOutput.closeEntry();
        }

        // Generate the JAX-RS interfaces
        for (CodegenJavaInterface iface : info.getInterfaces()) {
            String ifaceClassName = CodegenUtil.toClassName(settings, iface.getName());
            String ifacePackageName = iface.getPackage();
            String ifaceFQCN = ifacePackageName + "." + ifaceClassName;
            log.append("Generating Interface: " + ifaceFQCN + "\r\n");
            String javaInterface = generateJavaInterface(info, iface);
            String javaInterfaceFileName = javaPackageToZipPath(ifacePackageName) + ifaceClassName + ".java";
            log.append("Adding to zip: " + javaInterfaceFileName + "\r\n");
            zipOutput.putNextEntry(new ZipEntry(javaInterfaceFileName));
            zipOutput.write(javaInterface.getBytes(utf8));
            zipOutput.closeEntry();
        }

    }

    /**
     * Generate the JaxRs project.
     *
     * @throws IOException
     */
    public ByteArrayOutputStream generate() throws IOException {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            this.generate(output);
            return output;
        }
    }

    private String javaClassToZipPath(String javaClass) {
        return (settings.isMavenFileStructure() ? "src/main/java/" : "") + javaClass.replace('.', '/') + ".java";
    }

    /**
     * Processes the OpenAPI document to produce a CodegenInfo object that contains everything
     * needed to generate appropriate Java class(es).
     */
    protected CodegenInfo getInfoFromApiDoc() throws IOException {
        document = Library.readDocumentFromJSONString(openApiDoc);

        // If the document is OpenAPI 2.0 or 3.0, upgrade/transform it to 3.1
        document = Library.transformDocument(document, ModelType.OPENAPI31);

        // Pre-process the document
        document = preProcess(document);

        // Figure out the breakdown of the interfaces.
        InterfacesVisitor iVisitor = new InterfacesVisitor();
        VisitorUtil.visitTree(document, iVisitor, TraverserDirection.down);

        // Then generate the CodegenInfo object.
        OpenApi2CodegenVisitor cgVisitor = new OpenApi2CodegenVisitor(this.settings, iVisitor.getInterfaces(), CodegenTarget.JAX_RS);
        VisitorUtil.visitTree(document, cgVisitor, TraverserDirection.down);

        // Now resolve any inline schemas/types
        CodegenInfo info = cgVisitor.getCodegenInfo();
        info.getInterfaces().forEach(iface -> {
            iface.getMethods().forEach(method -> {
                method.getArguments().forEach(arg -> {
                    String argTypeSig = arg.getTypeSignature();
                    CodegenJavaBean matchingBean = findMatchingBean(info, argTypeSig);
                    if (matchingBean != null) {
                        arg.setType(matchingBean.getPackage() + "." + StringUtils.capitalize(matchingBean.getName()));
                    }
                });
            });
        });
        String contextRoot = getContextRoot(document);
        if (contextRoot != null) {
            info.setContextRoot(contextRoot);
        }
        return info;
    }

    protected String getContextRoot(Document document) {
        OpenApiDocument oaiDoc = (OpenApiDocument) document;
        if (oaiDoc.getPaths() != null) {
            JsonNode extension = CodegenUtil.getExtension((Extensible) oaiDoc.getPaths(), CodegenExtensions.CONTEXT_ROOT);
            if (extension != null && !extension.isNull()) {
                return extension.asText();
            }
        }
        return null;
    }

    protected String getMainClassName() {
        return "JaxRsApplication";
    }

    protected JavaClassSource generateApplicationClassSource(String topLevelPackage, CodegenInfo info) {
      return Roaster.create(JavaClassSource.class)
                      .setPackage(this.settings.javaPackage)
                      .setPublic()
                      .setName(getMainClassName())
                      .setSuperType(String.format("%s.ws.rs.core.Application", topLevelPackage))
                      .getJavaDoc()
                      .setFullText("The JAX-RS application.")
                      .getOrigin()
                      .addAnnotation(String.format("%s.enterprise.context.ApplicationScoped", topLevelPackage))
                      .getOrigin()
                      .addAnnotation(String.format("%s.ws.rs.ApplicationPath", topLevelPackage))
                      .setStringValue("/")
                      .getOrigin();
    }

    /**
     * Pre-process the document to modify it in the following ways:
     *
     * 1) Inline any re-usable simple-type schemas
     * 2) Check for the "x-codegen-contextRoot" property in the Paths object and prepend its value to all paths
     *
     * @param document
     */
    protected Document preProcess(Document document) {
        DocumentPreProcessor preprocessor = new DocumentPreProcessor();
        preprocessor.process(document);

        if (Boolean.FALSE) {
            System.out.println("-------------------------");
            System.out.println(Library.writeDocumentToJSONString(document));
            System.out.println("-------------------------");
            System.exit(1);
        }

        return document;
    }

    /**
     * Find a bean that matches the schema signature.
     *
     * @param info
     * @param typeSignature
     */
    protected static CodegenJavaBean findMatchingBean(CodegenInfo info, String typeSignature) {
        if (typeSignature == null) {
            return null;
        }
        for (CodegenJavaBean bean : info.getBeans()) {
            if (typeSignature.equals(bean.getSignature())) {
                return bean;
            }
        }
        return null;
    }

    protected URL getPomXmlResource() {
        return getResource("pom.xml");
    }

    /**
     * Generates the pom.xml file.
     *
     * @param info
     */
    protected String generatePomXml(CodegenInfo info) throws IOException {
        String template = IOUtils.toString(getPomXmlResource(), StandardCharsets.UTF_8);

        return template.replace("__GROUP_ID__", this.settings.groupId)
                .replace("_" + getClass().getSimpleName(), this.settings.artifactId)
                .replace("__VERSION__", info.getVersion())
                .replace("__NAME__", info.getName())
                .replace("__DESCRIPTION__", info.getDescription());
    }

    /**
     * Generates the JaxRsApplication Java class.
     */
    protected String generateJaxRsApplication(String topLevelPackage, CodegenInfo info) {
        JavaClassSource jaxRsApp = generateApplicationClassSource(topLevelPackage, info);

        sortImports(jaxRsApp);

        return jaxRsApp + "\n";
    }

    /**
     * Generates the JaxRsApplication java class.
     */
    protected String generateJaxRsApplication(CodegenInfo info) {
        return generateJaxRsApplication("jakarta", info);
    }

    void sortImports(Importer<?> javaSource) {
        javaSource.getImports()
            .stream()
            .sorted(Comparator.comparing(Import::getQualifiedName))
            .forEach(i -> {
                javaSource.removeImport(i);
                javaSource.addImport(i);
            });
    }

    /**
     * Generates a Jax-rs interface from the given codegen information.
     * @param info
     * @param interfaceInfo
     */
    protected String generateJavaInterface(CodegenInfo info, CodegenJavaInterface interfaceInfo, String topLevelPackage) {
        String jaxRsPath = info.getContextRoot() + interfaceInfo.getPath();
        final Parser markdownParser = Parser.builder().build();
        final HtmlRenderer htmlRenderer = HtmlRenderer.builder().build();

        final String iClassName = CodegenUtil.toClassName(settings, interfaceInfo.getName());
        final String iPackageName = interfaceInfo.getPackage();

        JavaInterfaceSource resourceInterface = Roaster.create(JavaInterfaceSource.class)
                .setPackage(iPackageName)
                .setPublic()
                .setName(iClassName)
                .getJavaDoc()
                .setFullText("A JAX-RS interface. An implementation of this interface must be provided.")
                .getOrigin()
                .addAnnotation(String.format("%s.ws.rs.Path", topLevelPackage))
                .setStringValue(jaxRsPath)
                .getOrigin();

        interfaceInfo.getMethods().forEach(methodInfo -> {
            MethodSource<JavaInterfaceSource> operationMethod = resourceInterface.addMethod()
                    .setName(methodInfo.getName());

            Optional.ofNullable(methodInfo.getDescription()).ifPresent(description -> {
                operationMethod.getJavaDoc()
                    .setFullText(htmlRenderer.render(markdownParser.parse(description)));

                if (settings.generatesOpenApi) {
                    operationMethod.addAnnotation(OPENAPI_OPERATION_ANNOTATION)
                          .setStringValue("description", description);
                }

            });

            if (settings.generatesOpenApi) {
                Optional.ofNullable(methodInfo.getSummary()).ifPresent(summary -> {
                    AnnotationSource<JavaInterfaceSource> annotation = operationMethod.getAnnotation(
                          OPENAPI_OPERATION_ANNOTATION);
                    if (annotation == null) {
                        operationMethod.addAnnotation(OPENAPI_OPERATION_ANNOTATION).setStringValue("summary", summary);
                    } else {
                        annotation.setStringValue("summary", summary);
                    }
                });

                Optional.ofNullable(methodInfo.getOperationId()).ifPresent(operationId -> {
                    AnnotationSource<JavaInterfaceSource> annotation = operationMethod.getAnnotation(
                          OPENAPI_OPERATION_ANNOTATION);
                    if (annotation == null) {
                        operationMethod.addAnnotation(OPENAPI_OPERATION_ANNOTATION).setStringValue("operationId", operationId);
                    } else {
                        annotation.setStringValue("operationId", operationId);
                    }
                });
            }


            Optional.ofNullable(methodInfo.getPath()).ifPresent(path ->
            operationMethod.addAnnotation(String.format("%s.ws.rs.Path", topLevelPackage)).setStringValue(path));

            operationMethod.addAnnotation(String.format("%s.ws.rs.%s", topLevelPackage, methodInfo.getMethod().toUpperCase()));

            Optional.ofNullable(methodInfo.getProduces())
                .filter(Predicate.not(Collection::isEmpty))
                .map(OpenApi2JaxRs::toStringArrayLiteral)
                .ifPresent(produces ->
                    operationMethod.addAnnotation(String.format("%s.ws.rs.Produces", topLevelPackage)).setLiteralValue(produces));

            Optional.ofNullable(methodInfo.getConsumes())
                .filter(Predicate.not(Collection::isEmpty))
                .ifPresent(consumesSet -> {
                    // Add MediaType import if any consumes value uses MediaType constants
                    if (consumesSet.stream().anyMatch(consume -> consume.contains(MEDIA_TYPE_CONSTANT_PREFIX))) {
                        resourceInterface.addImport(String.format("%s.ws.rs.core.MediaType", topLevelPackage));
                    }
                    String consumesLiteral = toStringArrayLiteral(consumesSet);
                    operationMethod.addAnnotation(String.format("%s.ws.rs.Consumes", topLevelPackage)).setLiteralValue(consumesLiteral);
                });

            final boolean reactive;

            if (getSettings().isReactive()) {
                // Reactive mode but this operation is explicitly synchronous
                reactive = !Boolean.FALSE.equals(methodInfo.getAsync());
            } else {
                // Non-reactive mode but this operation is explicitly asynchronous
                reactive = Boolean.TRUE.equals(methodInfo.getAsync());
            }

            final String genericReturnType = getSettings().getGenericReturnType();

            Optional.ofNullable(methodInfo.getReturn())
                .map(rt -> generateTypeName(
                        rt,
                        true,
                        String.format("%s.ws.rs.core.Response", topLevelPackage)))
                .map(rt -> generateGenericReturnType(rt, genericReturnType))
                .map(rt -> reactive ? generateReactiveTypeName(rt) : rt)
                .map(Object::toString)
                .ifPresentOrElse(
                        operationMethod::setReturnType,
                        () -> setVoidReturnType(operationMethod, reactive, genericReturnType));

            Optional.ofNullable(methodInfo.getArguments())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .forEach(arg -> {
                    String methodArgName = paramNameToJavaArgName(arg.getName());
                    String defaultParamType = Object.class.getName();

                    if (arg.getIn().equals("body")) {
                        // Swagger 2.0?
                        defaultParamType = InputStream.class.getName();
                    } else if (arg.getIn().equals("form")
                            && arg.getType() != null
                            && !arg.getType().isEmpty()) {
                        defaultParamType = arg.getType().get(0);
                    }

                    Type<?> paramType = generateTypeName(arg, arg.getRequired(), defaultParamType);

                    if (arg.getTypeSignature() != null) {
                        // TODO try to find a re-usable data type that matches the type signature
                    }

                    resourceInterface.addImport(paramType);
                    var paramTypeName = Types.toSimpleName(paramType.getQualifiedNameWithGenerics());
                    ParameterSource<JavaInterfaceSource> param = operationMethod.addParameter(paramTypeName, methodArgName);

                    switch (arg.getIn()) {
                        case "path":
                            param.addAnnotation(String.format("%s.ws.rs.PathParam", topLevelPackage))
                            .setStringValue(arg.getName());
                            break;
                        case "query":
                            param.addAnnotation(String.format("%s.ws.rs.QueryParam", topLevelPackage))
                            .setStringValue(arg.getName());
                            break;
                        case "header":
                            param.addAnnotation(String.format("%s.ws.rs.HeaderParam", topLevelPackage))
                            .setStringValue(arg.getName());
                            break;
                        case "cookie":
                            param.addAnnotation(String.format("%s.ws.rs.CookieParam", topLevelPackage))
                            .setStringValue(arg.getName());
                            break;
                        case "form":
                            param.addAnnotation("org.jboss.resteasy.annotations.providers.multipart.RestForm")
                            .setStringValue(arg.getName());
                            resourceInterface.addImport("org.jboss.resteasy.annotations.providers.multipart.RestForm");
                            break;
                        default:
                            break;
                    }

                    boolean forbidNotNull =
                            paramType.isPrimitive() ||
                            "path".equals(arg.getIn()) ||
                            !arg.getRequired();

                    addValidationConstraints(param, arg, forbidNotNull, topLevelPackage);

                    Optional.ofNullable(arg.getAnnotations())
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .map(CodegenBeanAnnotationDirective::getAnnotation)
                    .map(this::parseParameterAnnotation)
                    .forEach(source -> {
                        AnnotationSource<?> target = param.addAnnotation(source.getQualifiedName());
                        source.getValues().forEach(value -> target.setLiteralValue(value.getName(), value.getLiteralValue()));
                    });
                });
            });

        sortImports(resourceInterface);

        return Roaster.format(getFormatterProperties(), resourceInterface.toUnformattedString());
    }

    protected String generateJavaInterface(CodegenInfo info, CodegenJavaInterface interfaceInfo) {
        return generateJavaInterface(info, interfaceInfo, "jakarta");
    }

    public static Properties getFormatterProperties() {
        Properties formattingProperties = new Properties();
        formattingProperties.setProperty("org.eclipse.jdt.core.formatter.indentation.size", "2");
        formattingProperties.setProperty("org.eclipse.jdt.core.formatter.tabulation.char", "space");
        formattingProperties.setProperty("org.eclipse.jdt.core.formatter.tabulation.size", "2");
        formattingProperties.setProperty("org.eclipse.jdt.core.formatter.insert_new_line_at_end_of_file_if_missing", "insert");
        formattingProperties.setProperty("org.eclipse.jdt.core.formatter.blank_lines_after_last_class_body_declaration", "0");
        formattingProperties.setProperty("org.eclipse.jdt.core.formatter.blank_lines_before_first_class_body_declaration", "0");
        formattingProperties.setProperty("org.eclipse.jdt.core.formatter.number_of_empty_lines_to_preserve", "0");
        formattingProperties.setProperty("org.eclipse.jdt.core.formatter.blank_lines_after_package", "1");
        formattingProperties.setProperty("org.eclipse.jdt.core.formatter.blank_lines_after_imports", "1");
        formattingProperties.setProperty("org.eclipse.jdt.core.formatter.comment.format_javadoc_comments", "true");
        formattingProperties.setProperty("org.eclipse.jdt.core.formatter.blank_lines_before_method", "1");
        return formattingProperties;
    }

    static Type<?> parseType(String value) {
        return TYPE_CACHE.computeIfAbsent(value, k -> {
            String stub = "public class Stub { public void method( " + value + " arg0 ) {} }";
            JavaClassSource temp = (JavaClassSource) Roaster.parse(stub);
            return temp.getMethods().get(0).getParameters().get(0).getType();
        });
    }

    AnnotationSource<?> parseParameterAnnotation(String annotation) {
        String stub = "public class Stub { public void method( " + annotation + " Object arg0 ) {} }";
        JavaClassSource temp = (JavaClassSource) Roaster.parse(stub);
        return temp.getMethods().get(0).getParameters().get(0).getAnnotations().get(0);
    }

    /**
     * Generates the java type name for a collection (optional) and type.  Examples include list/string,
     * null/org.example.Bean, list/org.example.OtherBean, etc.
     *
     * @param schema
     * @param required
     * @param defaultType
     */
    protected Type<?> generateTypeName(CodegenJavaSchema schema, Boolean required, String defaultType) {
        if (required == null) {
            required = Boolean.FALSE;
        }

        String collection = schema.getCollection();
        List<String> type = Optional.ofNullable(schema.getType()).orElseGet(Collections::emptyList);
        String format = schema.getFormat();

        boolean isCollection = Arrays.asList("list", "set").contains(collection);
        boolean usePrimitive = (required && !isCollection && format != null);
        Type<?> coreType = null;

        if (type.contains("string")) {
            coreType = parseType(String.class.getName());

            if (format != null) {
                if (format.equals("date") || format.equals("date-time")) {
                    coreType = parseType(Date.class.getName());
                }
                if (format.equals("binary") && collection == null) {
                    coreType = parseType(defaultType);
                }
                if (format.equals("byte")) {
                    coreType = parseType("byte[]");
                }
            }
        } else if (type.contains("integer")) {
            if (config.isUseLongIntegers() || "int64".equals(format) || "utc-millisec".equals(format)) {
                coreType = usePrimitive ? parseType("long") : parseType(Long.class.getName());
            } else if ("int32".equals(format)) {
                coreType = usePrimitive ? parseType("int") : parseType(Integer.class.getName());
            } else {
                coreType = parseType(BigInteger.class.getName());
            }
        } else if (type.contains("number")) {
            if ("float".equals(format)) {
                coreType = usePrimitive ? parseType("float") : parseType(Float.class.getName());
            } else if ("double".equals(format)) {
                coreType = usePrimitive ? parseType("double") : parseType(Double.class.getName());
            } else {
                coreType = parseType(BigDecimal.class.getName());
            }
        } else if (type.contains("boolean")) {
            coreType = parseType(Boolean.class.getName());
        } else {
            Optional<String> qualifiedType = type.stream()
                    .filter(Predicate.not("null"::equals))
                    .findFirst()
                    .filter(Types::isQualified);

            if (qualifiedType.isPresent()) {
                try {
                    coreType = parseType(qualifiedType.get());
                } catch (Exception e) {
                    return parseType(defaultType);
                }
            } else {
                return parseType(defaultType);
            }
        }

        if (collection == null) {
            return coreType;
        }

        if ("list".equals(collection)) {
            return parseType(String.format("java.util.List<%s>", coreType.toString()));
        } else if ("set".equals(collection)) {
            return parseType(String.format("java.util.Set<%s>", coreType.toString()));
        } else if ("map".equals(collection)) {
            return parseType(String.format("java.util.Map<String, %s>", coreType.toString()));
        }

        return parseType(defaultType);
    }

    protected void setVoidReturnType(MethodSource<JavaInterfaceSource> operationMethod, boolean reactive,
            String genericReturnType) {
        Optional.of(VOID)
            .map(type -> generateGenericReturnType(type, genericReturnType))
            .map(type -> reactive ? generateReactiveTypeName(type) : type)
            .ifPresent(type -> {
                if (type != VOID) {
                    operationMethod.setReturnType(type);
                } else {
                    operationMethod.setReturnTypeVoid();
                }
            });
    }

    /**
     * Generates the reactive java type name for a collection (optional) and type.  Examples include list/string,
     * null/org.example.Bean, list/org.example.OtherBean, etc.
     *
     * @param coreType
     */
    protected Type<?> generateReactiveTypeName(Type<?> coreType) {
        Type<?> currentType = Types.isPrimitive(coreType.toString()) ? getType(coreType) : coreType;
        return parseType("java.util.concurrent.CompletionStage<" + currentType + ">");
    }

    /**
     * Wraps the given type in a generic return type wrapper, if configured.
     *
     * @param coreType
     * @param genericReturnType
     */
    protected Type<?> generateGenericReturnType(Type<?> coreType, String genericReturnType) {
        if (genericReturnType == null) {
            return coreType;
        }
        return parseType(String.format("%s<%s>", genericReturnType, coreType.toString()));
    }

    public Type<?> getType(final Type<?> coreType) {
        Type<?> currentType;
        switch (coreType.toString()) {
            case "byte":
                currentType = parseType("java.lang.Byte");
                break;
            case "short":
                currentType = parseType("java.lang.Short");
                break;
            case "int":
                currentType = parseType("java.lang.Integer");
                break;
            case "long":
                currentType = parseType("java.lang.Long");
                break;
            case "float":
                currentType = parseType("java.lang.Float");
                break;
            case "double":
                currentType = parseType("java.lang.Double");
                break;
            case "char":
                currentType = parseType("java.lang.Character");
                break;
            case "boolean":
                currentType = parseType("java.lang.Boolean");
                break;
            default:
                currentType = coreType;
        }

        return currentType;
    }

    /**
     * Converts a set of strings into an array literal format.
     *
     * @param values
     */
    protected static String toStringArrayLiteral(Set<String> values) {
        StringBuilder builder = new StringBuilder();

        if (values.size() == 1) {
            String value = values.iterator().next();
            if (value.startsWith(MEDIA_TYPE_CONSTANT_PREFIX)) {
                // Don't quote MediaType constants
                builder.append(value);
            } else {
                builder.append("\"");
                builder.append(value.replace("\"", "\\\""));
                builder.append("\"");
            }
        } else {
            builder.append("{");
            boolean first = true;
            for (String value : values) {
                if (!first) {
                    builder.append(", ");
                }
                if (value.startsWith(MEDIA_TYPE_CONSTANT_PREFIX)) {
                    // Don't quote MediaType constants
                    builder.append(value);
                } else {
                    builder.append("\"");
                    builder.append(value.replace("\"", "\\\""));
                    builder.append("\"");
                }
                first = false;
            }
            builder.append("}");
        }
        return builder.toString();
    }

    void addValidationConstraints(AnnotationTargetSource<?, ?> target, CodegenJavaSchema schemaInfo, boolean forbidNotNull, String topLevelPackage) {
        final String constraintPkg = String.format("%s.validation.constraints", topLevelPackage);
        final String sizeConstraint = String.format("%s.Size", constraintPkg);

        if (settings.isUseJsr303() && isUserBean(target)) {
            target.addAnnotation(String.format("%s.validation.Valid", topLevelPackage));
        }

        if (!forbidNotNull && !schemaInfo.isNullable()) {
            // nullable is false by default
            target.addAnnotation(String.format("%s.validation.constraints.NotNull", topLevelPackage));
        }

        if (schemaInfo.getCollection() != null) {
            if ("map".equals(schemaInfo.getCollection())) {
                addConstraint(target, schemaInfo.getMaxProperties(), sizeConstraint, "max");
                addConstraint(target, schemaInfo.getMinProperties(), sizeConstraint, "min");
            } else {
                addConstraint(target, schemaInfo.getMaxItems(), sizeConstraint, "max");
                addConstraint(target, schemaInfo.getMinItems(), sizeConstraint, "min");
            }
            return;
        }

        if (schemaInfo.getMaximum() != null) {
            BigDecimal max = new BigDecimal(schemaInfo.getMaximum().toString());
            boolean inclusive = !schemaInfo.isExclusiveMaximum();
            target.addAnnotation(String.format("%s.validation.constraints.DecimalMax", topLevelPackage))
            .setStringValue(max.toPlainString())
            .setLiteralValue("inclusive", String.valueOf(inclusive));
        }

        if (schemaInfo.getMinimum() != null) {
            BigDecimal min = new BigDecimal(schemaInfo.getMinimum().toString());
            boolean inclusive = !schemaInfo.isExclusiveMinimum();
            target.addAnnotation(String.format("%s.validation.constraints.DecimalMin", topLevelPackage))
            .setStringValue(min.toPlainString())
            .setLiteralValue("inclusive", String.valueOf(inclusive));
        }

        addConstraint(target, schemaInfo.getMaxLength(), sizeConstraint, "max");
        addConstraint(target, schemaInfo.getMinLength(), sizeConstraint, "min");
        addConstraint(target, schemaInfo.getPattern(), String.format("%s.Pattern", constraintPkg), "regexp");
        addConstraint(target, schemaInfo.getDefaultValue(), String.format("%s.ws.rs.DefaultValue", topLevelPackage), "value");
    }

    private boolean isUserBean(AnnotationTargetSource<?, ?> target) {
        return !((Parameter) target).getType().isPrimitive() && !((Parameter) target).getType().isArray() && !((Parameter<?>) target).getType().isParameterized();
    }

    <C> void addConstraint(AnnotationTargetSource<?, ?> target, C constraintValue, String annotationName, String annotationProperty) {
        if (constraintValue instanceof String) {
            target.addAnnotation(annotationName).setStringValue(annotationProperty, constraintValue.toString());
        } else if (constraintValue != null) {
            target.addAnnotation(annotationName).setLiteralValue(annotationProperty, constraintValue.toString());
        }
    }

    /**
     * Generates a Java Bean class for the given bean info.  The bean info should
     * have a name, package, and JSON Schema.  This information will be used to
     * generate a POJO.
     *
     * @param bean
     * @param info
     * @param codeWriter
     * @throws IOException
     */
    private void generateJavaBean(CodegenJavaBean bean, CodegenInfo info, IndexedCodeWriter codeWriter) throws IOException {
        JCodeModel codeModel = new JCodeModel();
        String generatedBeanName = CodegenUtil.toClassName(settings, bean.getName());
        String generatedBeanPackage = bean.getPackage();
        String generatedBeanFQCN = generatedBeanPackage + "." + generatedBeanName;

        SchemaMapper schemaMapper = new SchemaMapper(
                new JaxRsRuleFactory(config, new JaxRsJackson2Annotator(info, config), new SchemaStore() {
                    @Override
                    public Schema create(Schema parent, String path, String refFragmentPathDelimiters) {
                        String beanClassname = schemaRefToFQCN(path);
                        for (CodegenJavaBean cgBean : info.getBeans()) {
                            String cgBeanFQCN = cgBean.getPackage() + "." + CodegenUtil.toClassName(settings, cgBean.getName());
                            if (beanClassname.equals(cgBeanFQCN)) {
                                Schema schema = new Schema(classnameToUri(beanClassname), cgBean.get$schema(), null);
                                JType jclass = codeModel._getClass(beanClassname);
                                if (jclass == null) {
                                    jclass = codeModel.directClass(beanClassname);
                                }
                                schema.setJavaType(jclass);
                                return schema;
                            }
                        }
                        // TODO if we get here, we probably want to return an empty schema
                        return super.create(parent, path, refFragmentPathDelimiters);
                    }
                }, settings),
                new SchemaGenerator());
        String source = mapper.writeValueAsString(bean.get$schema());
        schemaMapper.generate(codeModel, generatedBeanName, generatedBeanPackage, source);
        codeModel.build(codeWriter);

        String fqcn = generatedBeanFQCN;
        codeWriter.indexBean(fqcn, bean);
    }

    protected URL getCommonResource(String name) {
        return getClass().getResource("common/" + name);
    }

    protected URL getResource(String name) {
        return getClass().getResource(name);
    }

    protected URI classnameToUri(String path) {
        return URI.create(path.replace('.', '/') + ".java");
    }

    protected String schemaRefToFQCN(String path) {
        return CodegenUtil.schemaRefToFQCN(settings, document, path, this.settings.javaPackage + ".beans");
    }

    protected String javaPackageToZipPath(String javaPackage) {
        return (settings.isMavenFileStructure() ? "src/main/java/" : "") + javaPackageToPath(javaPackage);
    }

    private static String javaPackageToPath(String javaPackage) {
        return javaPackage.replaceAll("[^A-Za-z0-9.]", "").replace('.', '/') + "/";
    }

    protected static String paramNameToJavaArgName(String paramName) {
        if (paramName == null) {
            return null;
        }
        String[] split = paramName.replaceAll("[^a-zA-Z0-9_]", "_").split("_");
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (String term : split) {
            if (term.trim().length() == 0) {
                continue;
            }
            if (first) {
                builder.append(decapitalize(term));
                first = false;
            } else {
                builder.append(capitalize(term));
            }
        }
        String rval = builder.toString();
        if (!SourceVersion.isName(rval)) {
            rval = "_" + rval;
        }
        return rval;
    }

    private static String capitalize(String term) {
        if (term.length() == 1) {
            return term.toUpperCase();
        }
        return term.substring(0, 1).toUpperCase() + term.substring(1);
    }

    private static String decapitalize(String term) {
        if (term.length() == 1) {
            return term.toLowerCase();
        }
        return term.substring(0, 1).toLowerCase() + term.substring(1);
    }

    /**
     * @return the updateOnly
     */
    public boolean isUpdateOnly() {
        return updateOnly;
    }

    /**
     * @return the settings
     */
    public JaxRsProjectSettings getSettings() {
        return this.settings;
    }

    /**
     * @param updateOnly the updateOnly to set
     */
    public void setUpdateOnly(boolean updateOnly) {
        this.updateOnly = updateOnly;
    }

    public static class JaxRsRuleFactory extends RuleFactory {

        private final JaxRsProjectSettings settings;

        /**
         * Constructor.
         */
        public JaxRsRuleFactory(GenerationConfig generationConfig, Annotator annotator, SchemaStore schemaStore, JaxRsProjectSettings settings) {
            super(generationConfig, annotator, schemaStore);
            this.settings = settings;
        }

        /**
         * @see org.jsonschema2pojo.rules.RuleFactory#getEnumRule()
         */
        @Override
        public Rule<JClassContainer, JType> getEnumRule() {
            return new JaxRsEnumRule(this, settings);
        }
    }

    public static class JaxRsJackson2Annotator extends Jackson2Annotator {

        private final CodegenInfo codegenInfo;

        /**
         * Constructor.
         */
        public JaxRsJackson2Annotator(CodegenInfo codegenInfo, GenerationConfig generationConfig) {
            super(generationConfig);
            this.codegenInfo = codegenInfo;
        }

        @Override
        public void dateTimeField(JFieldVar field, JDefinedClass clazz, JsonNode node) {
            if (!codegenInfo.getSuppressDateTimeFormats()) {
                super.dateTimeField(field, clazz, node);
            }
        }
    }
}
