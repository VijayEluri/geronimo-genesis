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

package org.apache.geronimo.plugin;

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.model.Dependency;

/**
 * Support for Mojo implementations.
 *
 * @version $Rev$ $Date$
 */
public abstract class MojoSupport
    extends AbstractMojo
{
    /**
     * Instance logger.  This is initialized to the value of {@link #getLog}
     * on execution.
     */
    protected Log log;

    /**
     * Initializes logging.  Called by {@link #execute}.
     */
    protected void init() {
        log = getLog();
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

    /**
     * Get the Maven project.
     *
     * <p>
     * Sub-class must overridde to provide access.
     */
    protected MavenProject getProject() {
        throw new Error("Sub-class must override to provide access");
    }

    /**
     * Get the artifact factory..
     *
     * <p>
     * Sub-class must overridde to provide access.
     */
    protected ArtifactFactory getArtifactFactory() {
        throw new Error("Sub-class must override to provide access");
    }

    /**
     * Get the artifact resolver.
     *
     * <p>
     * Sub-class must overridde to provide access.
     */
    protected ArtifactResolver getArtifactResolver() {
        throw new Error("Sub-class must override to provide access");
    }

    /**
     * Get the artifact repository.
     *
     * <p>
     * Sub-class must overridde to provide access.
     */
    protected ArtifactRepository getArtifactRepository() {
        throw new Error("Sub-class must override to provide access");
    }

    /**
     * Create a new artifact. If no version is specified, it will be retrieved from the dependency
     * list or from the DependencyManagement section of the pom.
     */
    protected Artifact createArtifact(final ArtifactItem item) throws MojoExecutionException {
        Artifact artifact;

        if (item.getVersion() == null) {
            fillMissingArtifactVersion(item);

            if (item.getVersion() == null) {
                throw new MojoExecutionException("Unable to find artifact version of " + item.getGroupId()
                    + ":" + item.getArtifactId() + " in either dependency list or in project's dependency management.");
            }

        }

        String classifier = item.getClassifier();
        if (classifier == null || classifier.equals("")) {
            artifact = getArtifactFactory().createArtifact(
                    item.getGroupId(),
                    item.getArtifactId(),
                    item.getVersion(),
                    Artifact.SCOPE_PROVIDED,
                    item.getType());
        }
        else {
            artifact = getArtifactFactory().createArtifactWithClassifier(
                    item.getGroupId(),
                    item.getArtifactId(),
                    item.getVersion(),
                    item.getType(),
                    item.getClassifier());
        }

        return artifact;
    }

    /**
     * Resolves the Artifact from the remote repository if nessessary. If no version is specified, it will
     * be retrieved from the dependency list or from the DependencyManagement section of the pom.
     */
    protected Artifact getArtifact(final ArtifactItem item) throws MojoExecutionException {
        Artifact artifact = createArtifact(item);

        try {
            getArtifactResolver().resolve(artifact, getProject().getRemoteArtifactRepositories(), getArtifactRepository());
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
}