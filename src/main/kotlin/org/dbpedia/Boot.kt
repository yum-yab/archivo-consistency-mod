package org.dbpedia

import org.dbpedia.databus_mods.lib.worker.AsyncWorker
import org.dbpedia.databus_mods.lib.worker.execution.Extension
import org.dbpedia.databus_mods.lib.worker.execution.ModProcessor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component

@SpringBootApplication
@Import(AsyncWorker::class)
open class ConsistencyMod

@Component
class Processor : ModProcessor {
    override fun process(extension: Extension) {
        // TODO
        extension.setType("http://my.domain/ns#DatabusMod")
        // File resultFile = extension.createModResult();
    }
}

fun main(args: Array<String>) {
    runApplication<ConsistencyMod>(*args)
}

