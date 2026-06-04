/*
 * Copyright 2022 Red Hat Inc
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

/**
 * Represents some basic meta information about the project being generated.
 *
 * @author eric.wittmann@gmail.com
 */
public class JaxRsProjectSettings {

    public boolean mavenFileStructure = true;
    public boolean includeSpec = true;
    public boolean codeOnly = false;
    public boolean reactive = false;
    public boolean cliGenCI = false;
    public String groupId = "org.example";
    public String artifactId = "example-api";
    public String javaPackage = "org.example.api";
    public String classNamePrefix = "";
    public String classNameSuffix = "";
    public String genericReturnType = null;
    protected boolean useJsr303 = false;
    protected boolean generateBuilders = false;
    protected boolean generatesOpenApi = true;
    protected boolean capitalizeEnumValues = true;

    /**
     * Constructor.
     */
    public JaxRsProjectSettings() {
    }

    /**
     * @return the codeOnly
     */
    public boolean isCodeOnly() {
        return codeOnly;
    }

    /**
     * @param codeOnly the codeOnly to set
     */
    public void setCodeOnly(boolean codeOnly) {
        this.codeOnly = codeOnly;
    }

    /**
     * @return the reactive
     */
    public boolean isReactive() {
        return reactive;
    }

    /**
     * @param reactive the reactive to set
     */
    public void setReactive(boolean reactive) {
        this.reactive = reactive;
    }

    /**
     * @return the cliGenCI
     */
    public boolean isCliGenCI() {
        return cliGenCI;
    }

    /**
     * @param cliGenCI the cliGenCI to set
     */
    public void setCliGenCI(boolean cliGenCI) {
        this.cliGenCI = cliGenCI;
    }

    /**
     * @return the groupId
     */
    public String getGroupId() {
        return groupId;
    }

    /**
     * @param groupId the groupId to set
     */
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    /**
     * @return the artifactId
     */
    public String getArtifactId() {
        return artifactId;
    }

    /**
     * @param artifactId the artifactId to set
     */
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    /**
     * @return the javaPackage
     */
    public String getJavaPackage() {
        return javaPackage;
    }

    /**
     * @param javaPackage the javaPackage to set
     */
    public void setJavaPackage(String javaPackage) {
        this.javaPackage = javaPackage;
    }

    /**
     * @return the mavenFileStructure
     */
    public boolean isMavenFileStructure() {
        return mavenFileStructure;
    }

    /**
     * @param mavenFileStructure the mavenFileStructure to set
     */
    public void setMavenFileStructure(boolean mavenFileStructure) {
        this.mavenFileStructure = mavenFileStructure;
    }

    /**
     * @return the includeSpec
     */
    public boolean isIncludeSpec() {
        return includeSpec;
    }

    /**
     * @param includeSpec the includeSpec to set
     */
    public void setIncludeSpec(boolean includeSpec) {
        this.includeSpec = includeSpec;
    }

    public String getClassNamePrefix() {
        return classNamePrefix;
    }

    public void setClassNamePrefix(String classNamePrefix) {
        this.classNamePrefix = classNamePrefix;
    }

    public String getClassNameSuffix() {
        return classNameSuffix;
    }

    public void setClassNameSuffix(String classNameSuffix) {
        this.classNameSuffix = classNameSuffix;
    }

    public String getGenericReturnType() {
        return genericReturnType;
    }

    public void setGenericReturnType(String genericReturnType) {
        this.genericReturnType = genericReturnType;
    }

    public boolean isUseJsr303() {
        return useJsr303;
    }

    public void setUseJsr303(boolean useJsr303) {
        this.useJsr303 = useJsr303;
    }

    public boolean isGenerateBuilders() {
        return generateBuilders;
    }

    public void setGenerateBuilders(boolean generateBuilders) {
        this.generateBuilders = generateBuilders;
    }

    /**
     * @return the capitalizeEnumValues
     */
    public boolean isCapitalizeEnumValues() {
        return capitalizeEnumValues;
    }

    /**
     * @param capitalizeEnumValues the capitalizeEnumValues to set
     */
    public void setCapitalizeEnumValues(boolean capitalizeEnumValues) {
        this.capitalizeEnumValues = capitalizeEnumValues;
    }
}