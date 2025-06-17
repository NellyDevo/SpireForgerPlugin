package me.johnnydevo.plugins.spireforgerplugin.run

import groovy.json.JsonSlurper
import groovy.xml.MarkupBuilder
import org.gradle.api.Project

import java.util.jar.JarEntry
import java.util.jar.JarFile

class RunGenerator {
    private final Project project
    private final List<String> modNames
    private String pathToModTheSpire
    private String pathToSpireFolder

    RunGenerator(Project project) {
        this.project = project
        modNames = new ArrayList<>()
    }

    void captureModId(File file) {
        captureModId(file, modNames)
    }

    static void captureModId(File file, List<String> store) {
        JarFile jar = new JarFile(file)
        JarEntry entry = jar.getJarEntry("ModTheSpire.json")
        if (entry != null) {
            println "capturing modid from ModTheSpire.json..."
            InputStream input = jar.getInputStream(entry)
            InputStreamReader reader = new InputStreamReader(input)
            BufferedReader buffer = new BufferedReader(reader)
            String line
            String json = ""
            while ((line = buffer.readLine()) != null) {
                json += line
            }
            JsonSlurper parser = new JsonSlurper()
            Map<String, Object> map = parser.parseText(json) as Map<String, Object>
            String modid = map.get("modid") as String
            if (modid != "modthespire") {
                store.add(modid)
                println modid + " detected"
            } else {
                println "not adding " + modid + " to debug task"
            }
        }
        jar.close()
    }

    void generateDebugRun() {
        if (pathToModTheSpire == null) {
            project.logger.warn("path to mod the spire not found, cannot generate debug run")
            return
        }
        if (pathToSpireFolder == null) {
            project.logger.warn("path to slay the spire not found, cannot generate debug run")
            return
        }
        String programArguments = "--mods ${project.mod_id},"
        modNames.forEach(s -> {
            programArguments += s + ","
        })

        String path = ".idea/runConfigurations/"
        File directory = new File(project.relativePath(path))
        File target = new File(path + "/Play.xml")
        if (!directory.exists()) {
            directory.mkdirs()
        }
        target.createNewFile()
        target.withWriter(writer -> {
            MarkupBuilder xml = new MarkupBuilder(new IndentPrinter(writer, "  ", true))
            xml.doubleQuotes = true

            xml.component('name': 'ProjectRunConfigurationManager') {
                configuration('default': 'false', 'name': 'Play', 'type': 'JarApplication', 'nameIsGenerated': 'true') {
                    option('name': 'JAR_PATH', 'value': pathToModTheSpire)
                    option('name': 'PROGRAM_PARAMETERS', 'value': programArguments)
                    option('name': 'WORKING_DIRECTORY', 'value': pathToSpireFolder)
                    option('name': 'ALTERNATIVE_JRE_PATH')
                    method('v': '2') {
                        option('name':'Gradle.BeforeRunTask', 'enabled':'true', 'tasks':'build', 'externalProjectPath':'$PROJECT_DIR$', 'vmOptions':'', 'scriptParameters':'')
                    }
                }
                // Leave file comment
                mkp.yield('\n    ')
                mkp.comment("AUTO-GENERATED FILE. MODIFICATIONS WILL BE LOST ON GRADLE REFRESH")
            }
        })
        println "successfully generated debug run"
    }

    void captureWorkingDirectory(File file) {
        pathToSpireFolder = file.getParentFile().getAbsolutePath()
    }

    void captureTargetFile(File file) {
        pathToModTheSpire = file.getAbsolutePath()
    }

}
