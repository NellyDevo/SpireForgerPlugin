package me.johnnydevo.plugins.spireforgerplugin.cleaner

import org.gradle.api.Project

class DirectoryCleaner {
    private final Project project
    private final List<File> filesToClean

    DirectoryCleaner(Project project) {
        this.project = project
        filesToClean = new ArrayList<>()
    }

    void clean() {
        println "cleaning temporary files..."
        for (File file : filesToClean) {
            delete(file)
        }
        println "...done"
    }

    static void delete(File toDelete) {
        long startTime = System.currentTimeMillis()
        while (!deleteDirectory(toDelete)) {
            if (System.currentTimeMillis() - startTime > 10000) {
                println "could not delete file at " + toDelete.absolutePath
                return
            }
        }
    }

    static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles()
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file)
            }
        }
        return directoryToBeDeleted.delete()
    }

    void markForDeletion(File file) {
        filesToClean.add(file)
    }
}
