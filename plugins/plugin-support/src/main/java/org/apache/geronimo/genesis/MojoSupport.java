/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.geronimo.genesis;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Collections;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.resolver.filter.ExcludesArtifactFilter;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Exclusion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

import org.apache.geronimo.genesis.dependency.DependencyHelper;
import org.apache.geronimo.genesis.util.ArtifactItem;
import org.apache.geronimo.genesis.util.MavenPluginLog;

/**
 * Support for Mojo implementations.
 *
 * @version $Rev$ $Date$
 */
public abstract class MojoSupport
    extends AbstractMojo
    implements Contextualizable
{
    static {
        //
        // NOTE: Force install our custom JCL Log bridge, and disable Geronimo's bootstrap logging
        //       in case any sub-clas ends up dependening on geronimo-kernel which will muck
        //       with logging in unexpected ways.
        //

        System.setProperty("org.apache.commons.logging.LogFactory", "org.apache.commons.logging.impl.LogFactoryImpl");

        //
        // NOTE: org.apache.commons.logging.Log is set in commons-logging.properties.  Hard-coding this here
        //       causes some other Maven plugins to have problems (like the site plugin when it runs checkstyle).
        //       Not sure that this will always get picked up though... :-(
        //
        // System.setProperty("org.apache.commons.logging.Log", "org.apache.geronimo.genesis.util.MavenPluginLog");

        System.setProperty("geronimo.bootstrap.logging.enabled", "false");
    }

    protected PlexusContainer container;

    /**
     * Instance logger.  This is initialized to the value of {@link #getLog}
     * on execution.
     */
    protected Log log;
    
    private DependencyHelper dependencyHelper;

    /**
     * Initializes logging.  Called by {@link #execute}.
     */
    protected void init() throws MojoExecutionException, MojoFailureException {
        this.log = getLog();

        // Install the bridge from JCL to this plugins Log
        MavenPluginLog.setLog(log);

        //
        // NOTE: Using direct lookup because this class may not have been directly configured
        //
        try {
            this.dependencyHelper = (DependencyHelper)container.lookup(DependencyHelper.class.getName());
        }
        catch (ComponentLookupException e) {
            throw new MojoExecutionException("Failed to lookup required components", e);
        }
    }

    public void contextualize(final Context context) throws ContextException {
        container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
    }

    /**
     * Main Mojo execution hook.  Sub-class should use {@link #doExecute} instead.
     *
     * @throws MojoExecutionException
     * @throws MojoFailureException
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        init();

        try {
            doExecute();
        }
        catch (Exception e) {
            //
            // NOTE: Wrap to avoid truncating the stacktrace
            //
            if (e instanceof MojoExecutionException) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
            else if (e instanceof MojoFailureException) {
                MojoFailureException x = new MojoFailureException(e.getMessage());
                x.initCause(e);
                throw x;
            }
            else {
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }
    }

    /**
     * Sub-class should override to provide custom execution logic.
     *
     * @throws Exception
     */
    protected void doExecute() throws Exception {
        // Empty
    }

    //
    // NOTE: These are not abstract because not all sub-classes will need this functionality
    //
    
    /**
     * Get the Maven project.
     *
     * <p>
     * Sub-class must overridde to provide access.
     */
    protected MavenProject getProject() {
        throw new Error("Sub-class must override to provide access to : " + MavenProject.class);
    }

    /**
     * Get the artifact repository.
     *
     * <p>
     * Sub-class must overridde to provide access.
     */
    protected ArtifactRepository getArtifactRepository() {
        throw new Error("Sub-class must override to provide access to: " + ArtifactRepository.class);
    }

    /**
     * Get the artifact factory.
     */
    protected final ArtifactFactory getArtifactFactory() {
        return dependencyHelper.getArtifactFactory();
    }

    /**
     * Get the artifact resolver.
     */
    protected final ArtifactResolver getArtifactResolver() {
        return dependencyHelper.getArtifactResolver();
    }

    /**
     * Create a new artifact. If no version is specified, it will be retrieved from the dependency
     * list or from the DependencyManagement section of the pom.
     */
    protected Artifact createArtifact(final ArtifactItem item) throws MojoExecutionException {
        if (item.getVersion() == null) {
            fillMissingArtifactVersion(item);

            if (item.getVersion() == null) {
                throw new MojoExecutionException("Unable to find artifact version of " + item.getGroupId()
                    + ":" + item.getArtifactId() + " in either dependency list or in project's dependency management.");
            }
        }

        // Convert the string version to a range
        VersionRange range;
        try {
            range = VersionRange.createFromVersionSpec(item.getVersion());
            if (log.isDebugEnabled()) {
                log.debug("Using version range: " + range);
            }
        }
        catch (InvalidVersionSpecificationException e) {
            throw new MojoExecutionException("Could not create range for version: " + item.getVersion(), e);
        }
        
        Artifact artifact = getArtifactFactory().createDependencyArtifact(
            item.getGroupId(),
            item.getArtifactId(),
            range,
            item.getType(),
            item.getClassifier(),
            Artifact.SCOPE_PROVIDED);

        return artifact;
    }

    /**
     * Resolves the Artifact from the remote repository if nessessary. If no version is specified, it will
     * be retrieved from the dependency list or from the DependencyManagement section of the pom.
     */
    protected Artifact getArtifact(final ArtifactItem item) throws MojoExecutionException {
        Artifact artifact = createArtifact(item);
        
        return resolveArtifact(artifact);
    }

    /**
     * Resolves the Artifact from the remote repository if nessessary. If no version is specified, it will
     * be retrieved from the dependency list or from the DependencyManagement section of the pom.
     */
    protected Artifact resolveArtifact(final Artifact artifact, final boolean transitive) throws MojoExecutionException {
        try {
            if (transitive) {
                getArtifactResolver().resolveTransitively(
                        Collections.singleton(artifact),
                        getProject().getArtifact(),
                        getProject().getRemoteArtifactRepositories(),
                        getArtifactRepository(),
                        dependencyHelper.getArtifactMetadataSource());
            }
            else {
                getArtifactResolver().resolve(
                        artifact,
                        getProject().getRepositories(),
                        getArtifactRepository());
            }
        }
        catch (ArtifactResolutionException e) {
            throw new MojoExecutionException("Unable to resolve artifact.", e);
        }
        catch (ArtifactNotFoundException e) {
            throw new MojoExecutionException("Unable to find artifact.", e);
        }

        return artifact;
    }

    /**
     * Resolves the Artifact from the remote repository if nessessary. If no version is specified, it will
     * be retrieved from the dependency list or from the DependencyManagement section of the pom.
     */
    protected Artifact resolveArtifact(final Artifact artifact) throws MojoExecutionException {
        return resolveArtifact(artifact, false);
    }

    /**
     * Tries to find missing version from dependancy list and dependency management.
     * If found, the artifact is updated with the correct version.
     */
    private void fillMissingArtifactVersion(final ArtifactItem item) {
        log.debug("Attempting to find missing version in " + item.getGroupId() + ":" + item.getArtifactId());

        List list = getProject().getDependencies();

        for (int i = 0; i < list.size(); ++i) {
            Dependency dependency = (Dependency) list.get(i);

            if (dependency.getGroupId().equals(item.getGroupId())
                && dependency.getArtifactId().equals(item.getArtifactId())
                && dependency.getType().equals(item.getType()))
            {
                log.debug("Found missing version: " + dependency.getVersion() + " in dependency list.");

                item.setVersion(dependency.getVersion());

                return;
            }
        }

        list = getProject().getDependencyManagement().getDependencies();

        for (int i = 0; i < list.size(); i++) {
            Dependency dependency = (Dependency) list.get(i);

            if (dependency.getGroupId().equals(item.getGroupId())
                && dependency.getArtifactId().equals(item.getArtifactId())
                && dependency.getType().equals(item.getType()))
            {
                log.debug("Found missing version: " + dependency.getVersion() + " in dependency management list");

                item.setVersion(dependency.getVersion());
            }
        }
    }
    
    //
    // Access to Project artifacts
    //

    protected Set getProjectArtifacts(final MavenProject project, final boolean resolve) throws MojoExecutionException {
        Set artifacts = new HashSet();

        Iterator dependencies = project.getDependencies().iterator();
        while (dependencies.hasNext()) {
            Dependency dep = (Dependency) dependencies.next();

            String groupId = dep.getGroupId();
            String artifactId = dep.getArtifactId();
            VersionRange versionRange = VersionRange.createFromVersion(dep.getVersion());
            String type = dep.getType();
            if (type == null) {
                type = "jar";
            }

            String classifier = dep.getClassifier();
            boolean optional = dep.isOptional();
            String scope = dep.getScope();
            if (scope == null) {
                scope = Artifact.SCOPE_COMPILE;
            }

            Artifact artifact = getArtifactFactory().createDependencyArtifact(
                groupId,
                artifactId,
                versionRange,
                type,
                classifier,
                scope,
                optional);

            if (scope.equalsIgnoreCase(Artifact.SCOPE_SYSTEM)) {
                artifact.setFile(new File(dep.getSystemPath()));
            }

            List exclusions = new ArrayList();
            for (Iterator j = dep.getExclusions().iterator(); j.hasNext();) {
                Exclusion e = (Exclusion) j.next();
                exclusions.add(e.getGroupId() + ":" + e.getArtifactId());
            }

            ArtifactFilter newFilter = new ExcludesArtifactFilter(exclusions);
            artifact.setDependencyFilter(newFilter);
            
            if (resolve && !artifact.isResolved()) {
                log.debug("Resolving artifact: " + artifact);
                artifact = resolveArtifact(artifact);
            }

            artifacts.add(artifact);
        }

        return artifacts;
    }

    protected Set getProjectArtifacts(final boolean resolve) throws MojoExecutionException {
        return getProjectArtifacts(getProject(), resolve);
    }

    protected Set getProjectArtifacts() throws MojoExecutionException {
        return getProjectArtifacts(false);
    }
}
