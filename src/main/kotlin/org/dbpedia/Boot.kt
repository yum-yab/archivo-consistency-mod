package org.dbpedia

import openllet.jena.PelletInfGraph
import openllet.jena.PelletReasonerFactory
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.ModelFactory
import org.dbpedia.databus_mods.lib.worker.AsyncWorker
import org.dbpedia.databus_mods.lib.worker.execution.Extension
import org.dbpedia.databus_mods.lib.worker.execution.ModProcessor
import org.semanticweb.HermiT.Reasoner
import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.IRI
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
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

fun calculateOpenlletConsistency(uri: String): Boolean {
    val model: OntModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC)
    model.read(uri)
    // load the model to the reasoner
    model.prepare()

    // Get the KnolwedgeBase object
    val kb = (model.graph as PelletInfGraph).kb

    // perform initial consistency check
    return kb.isConsistent
}

fun main(args: Array<String>) {
    //runApplication<ConsistencyMod>(*args)

    val testontURI = "https://archivo.dbpedia.org/download?o=http%3A//www.w3.org/ns/shacl%23"
    val reasonerFactory = ElkReasonerFactory()
    val inputHandler = OWLManager.createOWLOntologyManager()
    val ont = inputHandler.loadOntologyFromOntologyDocument(IRI.create(testontURI))
    val hermitReasoner = Reasoner(ont)
    val elkReasoner = reasonerFactory.createReasoner(ont)
    val elkConsistency = elkReasoner.isConsistent
    val hermitConsistency = hermitReasoner.isConsistent
    val openlletConsistency = calculateOpenlletConsistency(testontURI)
    println("Ontology consistency: \n" +
            "\tELK: $elkConsistency\n" +
            "\tOPENLLET: $openlletConsistency\n" +
            "\tHermiT: $hermitConsistency\n")
}

