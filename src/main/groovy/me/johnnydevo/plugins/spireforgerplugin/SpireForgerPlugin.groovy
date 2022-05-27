package me.johnnydevo.plugins.spireforgerplugin

import me.johnnydevo.plugins.spireforgerplugin.cleaner.DirectoryCleaner
import me.johnnydevo.plugins.spireforgerplugin.decompiler.SourcesGenerator
import me.johnnydevo.plugins.spireforgerplugin.dependencies.DependencyProcessor
import me.johnnydevo.plugins.spireforgerplugin.outjar.PatchedJarGenerator
import me.johnnydevo.plugins.spireforgerplugin.run.RunGenerator
import org.gradle.api.Plugin
import org.gradle.api.Project
import spoon.decompiler.CFRDecompiler
import spoon.decompiler.Decompiler
import spoon.decompiler.ProcyonDecompiler

class SpireForgerPlugin implements Plugin<Project> {
    private static final String DEPENDENCY_PATH = "build/generated/dependencies/"
    static final String SOURCES = "sources_configuration"
    static final String PACKAGE = "package_configuration"
    static final String SPIRE_JAR = "spirejar_configuration"
    static final String MOD_THE_SPIRE = "mod_the_spire_configuration"
    private Decompiler decompiler;
    private DirectoryCleaner cleaner;
    private SourcesGenerator sources;
    private PatchedJarGenerator patcher;
    private RunGenerator runs;
    private DependencyProcessor dependencies;

    @Override
    void apply(Project project) {

        project.getConfigurations().create(SOURCES)
        project.getConfigurations().create(PACKAGE)
        project.getConfigurations().create(SPIRE_JAR)
        project.getConfigurations().create(MOD_THE_SPIRE)

        decompiler = new ProcyonDecompiler()
        cleaner = new DirectoryCleaner(project)
        sources = new SourcesGenerator(project, decompiler, cleaner)
        patcher = new PatchedJarGenerator(project)
        runs = new RunGenerator(project)
        dependencies = new DependencyProcessor(project)
        SpireForgerExtension extension = project.getExtensions().create(SpireForgerExtension.NAME, SpireForgerExtension.class, project, sources)

        project.afterEvaluate(p -> {
            List<String> mods = new ArrayList<>()
            boolean printed = false
            project.getConfigurations().getByName(PACKAGE).forEach(file -> {
                if (!printed) {
                    printed = true
                    println "Package dependencies detected. Gathering data for package task..."
                }
                runs.captureModId(file, mods)
            })

            File slayTheSpire = null
            project.getConfigurations().getByName(SPIRE_JAR).forEach(file -> {
                if (slayTheSpire != null) {
                    throw new IllegalStateException("A Spire Forger project cannot have more than one jar marked as Slay the Spire")
                }
                slayTheSpire = file
                runs.captureWorkingDirectory(file)
            })
            if (slayTheSpire == null) {
                throw new IllegalStateException("A Spire Forger project must have exactly one jar marked as Slay the Spire")
            }

            File modTheSpire = null
            project.getConfigurations().getByName(MOD_THE_SPIRE).forEach(file -> {
                if (modTheSpire != null) {
                    throw new IllegalStateException("A Spire Forger project cannot have more than one jar marked as ModTheSpire")
                }
                modTheSpire = file
                runs.captureTargetFile(file)
            })
            if (modTheSpire == null) {
                throw new IllegalStateException("A Spire Forger project must have exactly one jar marked as ModTheSpire")
            }

            String spirePath = DEPENDENCY_PATH + removeDotJar(slayTheSpire.name)
            String spireTarget = spirePath + "/" + removeDotJar(slayTheSpire.name)
            dependencies.declareSpireRepository(spirePath)
            if (!patcher.exists(spireTarget)) {
                File patched = patcher.doPackageTask(mods, slayTheSpire, modTheSpire)
                File target = new File(spireTarget + ".jar")
                File directory = target.getParentFile()
                if (!directory.exists()) {
                    directory.mkdirs()
                }
                if (!target.exists()) {
                    target.createNewFile()
                }

                if (patched == slayTheSpire) {
                    if (!dependencies.upToDate(spireTarget, slayTheSpire)) {
                        dependencies.copyDependency(slayTheSpire, target)
                    }
                } else {
                    dependencies.copyDependency(patched, target)
                }
                if (!sources.upToDate(target)) {
                    sources.generateSources(patched, spireTarget)
                }
            }

            project.getConfigurations().getByName(PACKAGE).forEach(file -> {
                makePatchedSources(file, slayTheSpire)
            })
            project.getConfigurations().getByName(SOURCES).forEach(file -> {
                makeSources(file)
            })

            extension.cleanRepositories()

            runs.generateDebugRun()

            cleaner.clean()

        })

    }

    void makePatchedSources(File file, File slayTheSpire) {
        String basePath = DEPENDENCY_PATH + removeDotJar(file.name)
        String targetPath = basePath + "/" + removeDotJar(file.name)
        File target = new File(targetPath + ".jar")
        File directory = target.getParentFile()
        if (!directory.exists()) {
            directory.mkdirs()
        }
        if (!target.exists()) {
            target.createNewFile()
        }
        File patchedFile = new File(slayTheSpire.getParentFile().getPath() + "/package/" + removeDotJar(file.name) + "-modded.jar")
        if (!patcher.exists(targetPath)) {
            generateDependency(patchedFile, target, targetPath)
        }
        dependencies.declareSpireRepository(basePath)
        runs.captureModId(file)
    }

    void makeSources(File file) {
        String basePath = DEPENDENCY_PATH + removeDotJar(file.name)
        String targetPath = basePath + "/" + removeDotJar(file.name)
        File target = new File(targetPath + ".jar")
        File directory = target.getParentFile()
        if (!directory.exists()) {
            directory.mkdirs()
        }
        if (!target.exists()) {
            target.createNewFile()
        }
        if (!dependencies.upToDate(targetPath, file)) {
            generateDependency(file, target, targetPath)
        }
        dependencies.declareSpireRepository(basePath)
        runs.captureModId(file)
    }

    void generateDependency(File file, File target, String targetPath) {
        dependencies.copyDependency(file, target)
        sources.generateSources(file, targetPath)
    }

    static String removeDotJar(String fileName) {
        String retVal = fileName.replace(".jar", "")
        return retVal
    }
}
