package me.johnnydevo.plugins.spireforgerplugin

import me.johnnydevo.plugins.spireforgerplugin.decompiler.SourcesGenerator
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository

class SpireForgerExtension extends GroovyObjectSupport {
    public static final String NAME = "SpireForger"
    private final Project project
    private final SourcesGenerator generator
    private final List<String> temporaryRepositories

    SpireForgerExtension(Project project, SourcesGenerator generator) {
        this.project = project
        this.generator = generator
        temporaryRepositories = new ArrayList<>()
    }

    /**
     * marks the declared repository to be removed after build resolution. Guarantees the build won't
     * find the original mod jars still located in the workshop, and removes the need to store meta information
     * needed to determine if the files were changed.
     */
    FlatDirectoryArtifactRepository modDir(FlatDirectoryArtifactRepository repository) {
        if (!temporaryRepositories.contains(repository.name)) {
            temporaryRepositories.add(repository.name)
        }
        return repository
    }

    /**
     * marks the declared dependency as the Slay the Spire jar. When resolving dependencies, this file will be combined
     * with the the output jar from outjar before being decompiled to a sources jar. The project must have exactly one
     * dependency marked spireJar or an exception will be thrown.
     */
    Dependency spireJar(Object dependency) {
        return spireJar(dependency, null)
    }

    Dependency spireJar(Object dependency, Closure<?> options) {
        Dependency baseDependency = project.getDependencies().create(dependency, options)
        addConfiguration(baseDependency, SpireForgerPlugin.SPIRE_JAR)
        return baseDependency
    }

    /**
     * marks the declared dependency as the ModTheSpire jar. When resolving dependencies, this file will be used to patch
     * the base game with all outjar dependencies. The project must have exactly one dependency marked modTheSpire or an
     * exception will be thrown.
     */
    Dependency modTheSpire(Object dependency) {
        return modTheSpire(dependency, null)
    }

    Dependency modTheSpire(Object dependency, Closure<?> options) {
        Dependency baseDependency = project.getDependencies().create(dependency, options)
        addConfiguration(baseDependency, SpireForgerPlugin.SOURCES)
        addConfiguration(baseDependency, SpireForgerPlugin.MOD_THE_SPIRE)
        return baseDependency
    }

    /**
     * marks the declared repository to be decompiled and have sources jars made for them.
     */
    Dependency sources(Object dependency) {
        return sources(dependency, null)
    }

    Dependency sources(Object dependency, Closure<?> options) {
        Dependency baseDependency = project.getDependencies().create(dependency, options)
        addConfiguration(baseDependency, SpireForgerPlugin.SOURCES)
        return baseDependency
    }

    /**
     * marks the declared repository to be included in the outjar task. Use for any files you wish to see the results
     * of their patches on the base game.
     */
    Dependency patch(Object dependency) {
        return patch(dependency, null)
    }

    Dependency patch(Object dependency, Closure<?> options) {
        Dependency baseDependency = project.getDependencies().create(dependency, options)
        addConfiguration(baseDependency, SpireForgerPlugin.PACKAGE)
        return baseDependency
    }

    private void addConfiguration(Dependency dependency, String name) {
        project.getConfigurations().getByName(name).getDependencies().add(dependency)
    }

    void cleanRepositories() {
        for (String name : temporaryRepositories) {
            project.getRepositories().remove(project.getRepositories().getByName(name))
        }
    }

}
