package io.apitomy.hub.api.codegen;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class OpenApi2TestBase {

    protected void doFullTest(OpenApi2JaxRs generator, String expectedFilesPath, boolean debug) throws IOException {
        ByteArrayOutputStream outputStream = generator.generate();

        if (debug) {
            File tempFile = File.createTempFile("api", ".zip");
            FileUtils.writeByteArrayToFile(tempFile, outputStream.toByteArray());
            System.out.println("Generated ZIP (debug) can be found here: " + tempFile.getAbsolutePath());
        }

        if (Boolean.getBoolean("io.apitomy.codegen.dumptest")) {
            dumpOutput(outputStream.toByteArray(), new File("./target/generated-test-data/" + getClass().getSimpleName() + "/" + expectedFilesPath));
        }

        // Validate the result
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                if (!zipEntry.isDirectory()) {
                    String name = zipEntry.getName();
                    if (debug) {
                        System.out.println(name);
                    }
                    Assert.assertNotNull(name);

                    URL expectedFile = getClass().getClassLoader().getResource(getClass().getSimpleName() + "/" + expectedFilesPath + "/" + name);
                    if (expectedFile == null && "PROJECT_GENERATION_FAILED.txt".equals(name)) {
                        String errorLog = IOUtils.toString(zipInputStream, StandardCharsets.UTF_8);
                        System.out.println("----- UNEXPECTED ERROR LOG -----");
                        System.out.println(errorLog);
                        System.out.println("----- UNEXPECTED ERROR LOG -----");
                    }
                    Assert.assertNotNull("Could not find expected file for entry: " + name, expectedFile);
                    String expected = IOUtils.toString(expectedFile, StandardCharsets.UTF_8);
                    String actual = IOUtils.toString(zipInputStream, StandardCharsets.UTF_8);

                    if (debug) {
                        System.out.println("-----");
                        System.out.println(actual);
                        System.out.println("-----");
                    }

                    Assert.assertEquals("Expected vs. actual failed for entry: " + name,
                            normalizeQuarkusPlatformVersion(normalizeJdkVersion(normalizeLines(expected), name), name),
                            normalizeQuarkusPlatformVersion(normalizeLines(actual), name));
                }
                zipEntry = zipInputStream.getNextEntry();
            }
        }
    }

    private static String normalizeJdkVersion(String content, String fileName) {
        String jdkVersion = String.valueOf(Runtime.version().major());
        if (fileName.endsWith("pom.xml")) {
            content = content.replaceAll(
                    "(<maven\\.compiler\\.release>)\\d+(</maven\\.compiler\\.release>)",
                    "$1" + jdkVersion + "$2");
        }
        if (fileName.startsWith("src/main/docker/Dockerfile")) {
            content = content.replaceAll(
                    "openjdk-\\d+-runtime",
                    "openjdk-" + jdkVersion + "-runtime");
        }
        return content;
    }

    private static String normalizeQuarkusPlatformVersion(String content, String fileName) {
        if (fileName.endsWith("pom.xml")) {
            content = content.replaceAll(
                    "(<quarkus.platform.version>)[^<]+(</quarkus.platform.version>)",
                    "$1QUARKUS_VERSION$2");
        }
        return content;
    }

    private static String normalizeLines(String value) {
        return value.lines()
            .map(String::stripTrailing)
            .collect(Collectors.joining("\n"));
    }

    static void dumpOutput(byte[] rawArchive, File outputPath) throws IOException {
        byte[] buffer = new byte[1024];

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(rawArchive))) {
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                File newFile = newFile(outputPath, zipEntry);

                if (zipEntry.isDirectory()) {
                    if (!newFile.isDirectory() && !newFile.mkdirs()) {
                        throw new IOException("Failed to create directory " + newFile);
                    }
                } else {
                    // fix for Windows-created archives
                    File parent = newFile.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("Failed to create directory " + parent);
                    }

                    // write file content
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                    fos.close();
                }
                zipEntry = zis.getNextEntry();
            }
        }
    }

    static File newFile(File outputPath, ZipEntry zipEntry) throws IOException {
        File destFile = new File(outputPath, zipEntry.getName());

        String destDirPath = outputPath.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return destFile;
    }
}
