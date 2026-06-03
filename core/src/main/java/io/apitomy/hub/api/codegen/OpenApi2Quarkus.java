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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

import io.apitomy.hub.api.codegen.beans.CodegenInfo;
import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.Type;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.util.Types;


/**
 * Class used to generate a Quarkus JAX-RS project from an OpenAPI document.
 *
 * @author eric.wittmann@gmail.com
 */
public class OpenApi2Quarkus extends OpenApi2JaxRs {

    /**
     * Constructor.
     */
    public OpenApi2Quarkus() {
        super();
    }

    /**
     * @see io.apitomy.hub.api.codegen.OpenApi2JaxRs#generateAll(io.apitomy.hub.api.codegen.beans.CodegenInfo, java.lang.StringBuilder, java.util.zip.ZipOutputStream)
     */
    @Override
    protected void generateAll(CodegenInfo info, StringBuilder log, ZipOutputStream zipOutput)
            throws IOException {
        super.generateAll(info, log, zipOutput);

        if (!this.isUpdateOnly() && !this.settings.codeOnly) {
            log.append("Generating Dockerfiles\r\n");
            Stream.of("jvm", "legacy-jar", "native", "native-micro")
                .map("src/main/docker/Dockerfile."::concat)
                .forEach(path -> writeEntry(zipOutput, path));

            log.append("Generating application.properties\r\n");
            zipOutput.putNextEntry(new ZipEntry("src/main/resources/application.properties"));
            zipOutput.write(generateApplicationProperties().getBytes());
            zipOutput.closeEntry();

            log.append("Generating project files\r\n");
            writeEntry(zipOutput, ".mvn/wrapper/maven-wrapper.properties");
            writeEntry(zipOutput, ".dockerignore");
            writeEntry(zipOutput, ".gitignore");
            writeEntry(zipOutput, "mvnw");
            writeEntry(zipOutput, "mvnw.cmd");
            writeEntry(zipOutput, "README.md");
        }
    }

    @Override
    protected JavaClassSource generateApplicationClassSource(String topLevelPackage, CodegenInfo info) {

       return Roaster.create(JavaClassSource.class)
             .setName(getMainClassName())
             .getJavaDoc()
             .setFullText(info.getName() + " " + info.getVersion())
             .getOrigin()
             .setSuperType("jakarta.ws.rs.core.Application")
             .setPackage(this.settings.javaPackage)
             .setPublic()
             .addAnnotation("org.eclipse.microprofile.openapi.annotations.OpenApiDefinition")
             .setLiteralValue("info", String.format("@org.eclipse.microprofile.openapi.annotations.info.Info(title = \"%s\", version = \"%s\")", info.getName(), info.getVersion()))
             .getOrigin();
    }

    @Override
    protected URL getPomXmlResource() {
        return getResource("quarkus/pom.xml");
    }

    @Override
    protected String generatePomXml(CodegenInfo info) throws IOException {
        return super.generatePomXml(info).replace("<artifactId>quarkus</artifactId>",
                "<artifactId>" + this.settings.artifactId + "</artifactId>");
    }

    private void writeEntry(ZipOutputStream zipOutput, String sourcePath) {
        try {
            zipOutput.putNextEntry(new ZipEntry(sourcePath));
            zipOutput.write(IOUtils.toString(getQuarkusResource(sourcePath), StandardCharsets.UTF_8).getBytes());
            zipOutput.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Generates the openshift-template.yml file.
     */
    private String generateApplicationProperties() throws IOException {
        String template = IOUtils.toString(getQuarkusResource("src/main/resources/application.properties"), StandardCharsets.UTF_8);
        template += "\nmp.openapi.scan.disable=true\n";
        return template;
    }

    protected URL getQuarkusResource(String name) {
        return getClass().getResource("quarkus/" + name);
    }

    @Override
    protected String getMainClassName() {
        return "Application";
    }

    @Override
    protected Type<?> generateReactiveTypeName(Type<?> coreType) {
        Type<?> currentType = Types.isPrimitive(coreType.toString()) ? getType(coreType) : coreType;
        return parseType("io.smallrye.mutiny.Uni<" + currentType + ">");
    }
}
