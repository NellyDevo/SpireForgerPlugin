package me.johnnydevo.plugins.spireforgerplugin.decompiler

import me.johnnydevo.plugins.spireforgerplugin.cleaner.DirectoryCleaner
import org.gradle.api.Project
import spoon.decompiler.Decompiler

import java.util.jar.JarEntry
import java.util.jar.JarOutputStream

class SourcesGenerator {
    private final Project project
    private final Decompiler decompiler
    private final DirectoryCleaner cleaner

    SourcesGenerator(Project project, Decompiler decompiler, DirectoryCleaner cleaner) {
        this.project = project
        this.decompiler = decompiler
        this.cleaner = cleaner
    }

    void generateSources(File source, String targetPath) {
        File temporaryDirectory = new File(targetPath)
        if (!temporaryDirectory.exists()) {
            temporaryDirectory.mkdirs()
        }
        doDecompile(targetPath, source)
        doPackage(targetPath, temporaryDirectory)
        cleaner.markForDeletion(temporaryDirectory)
    }

    void doDecompile(String targetPath, File dependency) {
        println "decompiling file " + dependency.name + "..."
        int fileCount = 0

        PrintStream spamCull = new PrintStream(System.out) {
            //@Override
            //PrintStream printf(String format, Object... args) {
            //    synchronized (this) {
            //        if (fileCount % 250 == 0) {
            //            super.println("    " + fileCount + " files...")
            //        }
            //    }
            //    return this
            //}

            @Override
            void println(String s) {
                ++fileCount
                if (fileCount % 250 == 0) {
                    super.println("    " + fileCount + " files...")
                }
            }
        }
        PrintStream current = System.out
        System.setOut(spamCull)
        decompiler.decompile(dependency.getAbsolutePath(), targetPath)
        System.setOut(current)

        println "...done"
    }

    void doPackage(String targetPath, File temporaryDir) {
        println "packaging sources jar..."

        File jarFile = new File(targetPath + "-sources.jar")
        File directory = jarFile.getParentFile()
        if (!directory.exists()) {
            directory.mkdirs()
        }
        if (!jarFile.exists()) {
            jarFile.createNewFile()
        }
        JarOutputStream target = new JarOutputStream(new FileOutputStream(jarFile))
        for (File file : temporaryDir.listFiles()) {
            addFolder(file, target, "")
        }
        target.close()

        println "...done"
    }

    void addFolder(File source, JarOutputStream target, String path) {
        String name = path + source.getName()
        if (source.isDirectory()) {
            if (!name.endsWith("/")) {
                name += "/"
            }
            JarEntry entry = new JarEntry(name)
            entry.setTime(source.lastModified())
            target.putNextEntry(entry)
            target.closeEntry()
            path = name
            for (File nestedFile : source.listFiles()) {
                addFolder(nestedFile, target, path)
            }
        } else {
            JarEntry entry = new JarEntry(name)
            entry.setTime(source.lastModified())
            target.putNextEntry(entry)
            try {
                BufferedInputStream input = new BufferedInputStream(new FileInputStream(source))
                byte[] buffer = new byte[1024]
                while (true) {
                    int count = input.read(buffer)
                    if (count == -1)
                        break
                    target.write(buffer, 0, count)
                }
                target.closeEntry()
            } catch (Exception e) {
                project.logger.warn("unable to package File into jar:")
                e.printStackTrace()
            }
        }
    }

    boolean upToDate(File generated) {
        File sourcesJar = new File(generated.getAbsoluteFile().getParentFile().getName().replace(".jar", "-sources.jar"))
        if (sourcesJar.exists()) {
            if (sourcesJar.lastModified() == generated.lastModified()) {
                println "sources jar " + sourcesJar.name + " is up to date."
                return true
            }
        }
        return false
    }
}
