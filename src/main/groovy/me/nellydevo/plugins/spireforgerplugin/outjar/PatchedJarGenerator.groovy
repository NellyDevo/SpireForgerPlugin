package me.johnnydevo.plugins.spireforgerplugin.outjar

import org.gradle.api.Project

class PatchedJarGenerator {
    private final Project project

    PatchedJarGenerator(Project project) {
        this.project = project
    }

    File doPackageTask(List<String> mods, File slayTheSpire, File modTheSpire) {
        String arg = ""
        for (String s : mods) {
            arg += s + ","
        }
        ProcessBuilder builder = new ProcessBuilder("java", "-jar", modTheSpire.getAbsolutePath(), "--package", "--close-when-finished", "--mods", arg)
        File spireInstallation = slayTheSpire.getAbsoluteFile().getParentFile()
        builder.directory(spireInstallation)
        Process process = builder.start()

        StreamGobbler outputGobbler = new StreamGobbler(process.getInputStream(), System.out::println)
        StreamGobbler errorGobbler = new StreamGobbler(process.getErrorStream(), project.logger::error)

        new Thread(outputGobbler).start()
        new Thread(errorGobbler).start()

        process.waitFor()

        println "attempting to find desktop-1.0-modded.jar..."
        long startTime = System.currentTimeMillis()
        File patched = new File(spireInstallation.getAbsolutePath() + "/desktop-1.0-modded.jar")
        println "looking in " + patched.getAbsolutePath()
        while(!patched.exists()) {
            println System.currentTimeMillis() - startTime
            if (System.currentTimeMillis() - startTime > 10000) {
                println "waited 10 seconds for file to appear in directory, but failed to find it"
                return slayTheSpire
            }
        }

        return patched
    }

    boolean exists(String targetPath) {
        File copiedJar = new File(targetPath + ".jar")
        if (copiedJar.exists()) {
            println "A previously patched copy of " + copiedJar.name + " was found, and will not be re-generated. If you wish to generate it again, delete it from build/generated and refresh the project."
            //if (copiedJar.lastModified() == slayTheSpire.lastModified() && copiedJar.size() != slayTheSpire.size()) {
            //    println "desktop-1.0 was found to be modified, but has the same timestamp -- it is assumed this is the patched jar"
            //    return true
            //}
            return true
        }
        return false
    }

}