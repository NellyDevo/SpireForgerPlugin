package me.johnnydevo.plugins.spireforgerplugin.dependencies

import org.gradle.api.Project
import org.gradle.api.artifacts.UnknownRepositoryException
import org.gradle.api.artifacts.repositories.FlatDirectoryArtifactRepository

class DependencyProcessor {
    private final Project project
    public static final String SPIRE_REPOSITORIES = "spire_repositories"

    DependencyProcessor(Project project) {
        this.project = project
    }

    void declareSpireRepository(String path) {
        FlatDirectoryArtifactRepository repository
        try {
            repository = project.getRepositories().getByName(SPIRE_REPOSITORIES) as FlatDirectoryArtifactRepository
        } catch (UnknownRepositoryException ignored) {
            repository = project.getRepositories().flatDir {
                setName SPIRE_REPOSITORIES
            }
        }
        repository.dir(path)
    }

    static void copyDependency(File dependency, File target) {
        println "copying original jar..."

        InputStream is = null
        OutputStream os = null
        File output = target
        try {
            is = new FileInputStream(dependency)
            os = new FileOutputStream(output)
            byte[] buffer = new byte[1024]
            int length
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length)
            }
            output.setLastModified(dependency.lastModified())
        } finally {
            if (is != null) {
                is.close()
            }
            if (os != null) {
                os.close()
            }
        }

        println "...done"
    }

    static boolean upToDate(String targetPath, File dependency) {
        File copiedJar = new File(targetPath + ".jar")
        if (copiedJar.exists()) {
            if (copiedJar.lastModified() == dependency.lastModified()) {
                println "dependency " + dependency.name + " is up to date."
                return true
            }
        }
        return false
    }
}
