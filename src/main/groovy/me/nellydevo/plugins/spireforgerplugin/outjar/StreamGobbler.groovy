package me.johnnydevo.plugins.spireforgerplugin.outjar

import java.util.function.Consumer

class StreamGobbler implements Runnable {
    private InputStream inputStream
    private Consumer<String> consumeInputLine

    StreamGobbler(InputStream inputStream, Consumer<String> consumeInputLine) {
        this.inputStream = inputStream
        this.consumeInputLine = consumeInputLine
    }

    void run() {
        new BufferedReader(new InputStreamReader(inputStream)).lines().forEach(consumeInputLine)
    }
}
