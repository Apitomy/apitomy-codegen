package io.apitomy.codegen.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import io.apitomy.hub.api.codegen.OpenApi2JaxRs;

/**
 * The main generate code mojo implementation.
 * 
 * @author eric.wittmann@gmail.com
 */
@Mojo(name = "generate", threadSafe = true)
public class GenerateCodeMojo extends AbstractMojo {

    /**
     * The current Maven project.
     */
    @Parameter(defaultValue = "${project}", readonly = true)
    protected MavenProject project;
    
    @Parameter(required = true)
    ProjectSettings projectSettings;

    @Parameter(required = true)
    File inputSpec;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/jaxrs")
    File outputDir;

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Generating JAX-RS interfaces and beans from: " + inputSpec);
        if (inputSpec == null || !inputSpec.isFile()) {
            throw new MojoFailureException("Input spec not found.");
        }

        if (outputDir.isFile()) {
            throw new MojoFailureException(
                    "Output directory is unexpectedly a file (should be a directory or non-existent).");
        }

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Add the output directory as a compile source.
        getLog().info("Generating code into: " + outputDir.getAbsolutePath());
        this.project.addCompileSourceRoot(outputDir.getAbsolutePath());

        // Generate code - output a ZIP file.
        File zipFile = new File(outputDir, "generated-code.zip");
        try (FileOutputStream fos = new FileOutputStream(zipFile)) {
            OpenApi2JaxRs generator = new OpenApi2JaxRs();
            generator.setSettings(projectSettings);
            generator.setUpdateOnly(true);
            generator.setOpenApiDocument(inputSpec.toURI().toURL());
            getLog().info("Generating code...");
            generator.generate(fos);
        } catch (Exception e) {
        	getLog().error(e);
            throw new MojoExecutionException(e);
        }

        // Unpack the temporary ZIP file
        getLog().info("Code generated, unpacking the output ZIP.");
        try {
            unzip(zipFile, outputDir);
        } catch (IOException e) {
        	getLog().error(e);
            throw new MojoExecutionException(e);
        } finally {
            // Delete the temporary ZIP file
            zipFile.delete();
        }
        
        getLog().info("Code successfully generated.");
    }

    private void unzip(File fromZipFile, File toOutputDir) throws ZipException, IOException {
        try (java.util.zip.ZipFile zipFile = new ZipFile(fromZipFile)) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                File entryDestination = new File(toOutputDir, entry.getName());
                if (entry.isDirectory()) {
                    entryDestination.mkdirs();
                } else {
                    entryDestination.getParentFile().mkdirs();
                    try (InputStream in = zipFile.getInputStream(entry);
                            OutputStream out = new FileOutputStream(entryDestination)) {
                        IOUtils.copy(in, out);
                    }
                }
            }
        }
    }

}
