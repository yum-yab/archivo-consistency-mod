package org.dbpedia

import org.dbpedia.consistencyChecks.*
import org.dbpedia.databus_mods.lib.util.UriUtil
import org.dbpedia.models.ModResult
import org.dbpedia.models.ReasonerReport
import org.semanticweb.owlapi.apibinding.OWLManager
import org.slf4j.LoggerFactory
import uk.ac.manchester.cs.jfact.JFactFactory
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class FakeProcessor {

    private val logger = LoggerFactory.getLogger(FakeProcessor::class.java)

    private val timeOutCounter: Long = 1

    // default timeout in minutes
    private val timeOutUnit = TimeUnit.SECONDS

    fun process(source: String, dBusID: String) {
        // be null safe, maybe do something more clever here
        logger.info("Started process for file: $source")
        // set the values of the extension and add some prefixes
//        extension.setType("http://mods.tools.dbpedia.org/ns/demo#ArchivoReasonerMod")
//        HelperConstants.namespaces.nsPrefixMap.map { extension.addPrefix(it.key, it.value) }
        // Download File
        val inputStream = UriUtil.openStream(URI(source))
        inputStream.use {
            // load into owlapi
            val inputHandler = OWLManager.createOWLOntologyManager()
            val ont = inputHandler.loadOntologyFromOntologyDocument(it)
            logger.info("Started generating the Stats for the ontology...")
            val axiomCount = ont.axiomCount
            val classCount = ont.classesInSignature().count().toInt()
            val propCount = (ont.dataPropertiesInSignature().count() + ont.objectPropertiesInSignature().count()).toInt()
            logger.info("Starting Consistency Checks...")

            val wrapper = TimeOutWrapper(JFactFactory(), ont)

            val report = wrapper.runTimeOutTask()
            val consistencyModel = report.toModel()
            consistencyModel.write(System.out, "TURTLE")
        }
    }
}

class ConsistencyThread: Thread() {

    override fun run() {
        super.run()
    }
}

fun main(args: Array<String>) {
    val processor = FakeProcessor()
    println("Main Thread" + Thread.currentThread().name)
    processor.process("https://databus.dbpedia.org/ontologies/dbpedia.org/ontology/2021.10.20-180002/ontology_type=parsed.nt", "https://databus.dbpedia.org/ontologies/dbpedia.org/ontology/2021.10.20-180002/ontology_type=parsed.nt")
}
