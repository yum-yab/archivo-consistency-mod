package org.dbpedia

import org.dbpedia.databus_mods.lib.worker.AsyncWorker
import org.dbpedia.databus_mods.lib.worker.execution.Extension
import org.dbpedia.databus_mods.lib.worker.execution.ModProcessor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import java.io.File


@SpringBootApplication
@Import(AsyncWorker::class)
open class ConsistencyMod

@Component
class Processor : ModProcessor {
    override fun process(extension: Extension) {
        // TODO
        val reasonerFactory = ElkReasonerFactory()
        val inputHandler = OWLManager.createOWLOntologyManager()
        val ouputHandler = OWLManager.createOWLOntologyManager()
        val ont = inputHandler.loadOntologyFromOntologyDocument(File("./testont.owl"))
        val reasoner = reasonerFactory.createReasoner(ont)
        val isConsistent = reasoner.isConsistent
        println("Ontology is consistent: $isConsistent")
        extension.setType("http://my.domain/ns#DatabusMod")
        // File resultFile = extension.createModResult();
    }
}

fun main(args: Array<String>) {
    runApplication<ConsistencyMod>(*args)
}

