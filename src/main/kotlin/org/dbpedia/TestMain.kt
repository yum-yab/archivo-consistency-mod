package org.dbpedia

import org.apache.jena.base.Sys
import org.dbpedia.consistencyChecks.*
import org.dbpedia.databus_mods.lib.util.UriUtil
import org.dbpedia.models.ReasonerReport
import org.dbpedia.runnables.JFactConsistencyCheck
import org.dbpedia.runnables.RunnableConsistencyCheck
import org.semanticweb.owlapi.apibinding.OWLManager
import org.slf4j.LoggerFactory
import uk.ac.manchester.cs.jfact.JFactFactory
import java.net.URI
import java.time.Duration
import java.time.temporal.TemporalUnit
import java.util.*
import java.util.concurrent.TimeUnit

class FakeProcessor {

    private val logger = LoggerFactory.getLogger(FakeProcessor::class.java)

    private val timeOutCounter: Long = 1

    // default timeout in minutes
    private val timeOutUnit = TimeUnit.SECONDS

    fun process(source: String) {
        // be null safe, maybe do something more clever here
        logger.info("Started process for file: $source")
        // set the values of the extension and add some prefixes
//        extension.setType("http://mods.tools.dbpedia.org/ns/demo#ArchivoReasonerMod")
//        HelperConstants.namespaces.nsPrefixMap.map { extension.addPrefix(it.key, it.value) }
        // Download File
        val inputStream = UriUtil.openStream(URI(source))
        val ont = inputStream.use {
            // load into owlapi
            val inputHandler = OWLManager.createOWLOntologyManager()
            val ont = inputHandler.loadOntologyFromOntologyDocument(it)
            ont
        }
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

    fun getReport(task: RunnableConsistencyCheck, minutesToRun: Long): ReasonerReport {
        val t = Thread(task)
        t.start()
        val end = System.currentTimeMillis() + (minutesToRun * 1000 * 30)

        while (task.reasonerReport == null && System.currentTimeMillis() < end) {
            Thread.sleep(100)
        }
        task.interrupted = true
        val report = task.reasonerReport
            ?: ReasonerReport(
                reasonerID = task.reasonerCheckID,
                isConsistent = null,
                owlProfiles = emptyList(),
                inspectionTime = Duration.ofMinutes(minutesToRun),
                messageConsistency = "CONSITENCY LOG: Process got interrupted after timeout $minutesToRun",
                messageProfiles = "CONSITENCY LOG: Process got interrupted after timeout $minutesToRun"
            )

        t.stop()

        return report
    }
}

internal class TimerWrapper(private val task: RunnableConsistencyCheck, val minutesToRun: Long) {


    private val timeUnit = TimeUnit.MINUTES

    fun getReport(): ReasonerReport {
        val t = Thread(task)
        t.start()
        val end = System.currentTimeMillis() + (minutesToRun * 1000 * 30)

        while (task.reasonerReport == null && System.currentTimeMillis() < end) {
            Thread.sleep(100)
        }
        task.interrupted = true
        val report = task.reasonerReport
            ?: ReasonerReport(
                reasonerID = task.reasonerCheckID,
                isConsistent = null,
                owlProfiles = emptyList(),
                inspectionTime = Duration.ofMinutes(minutesToRun),
                messageConsistency = "CONSITENCY LOG: Process got interrupted after timeout $minutesToRun",
                messageProfiles = "CONSITENCY LOG: Process got interrupted after timeout $minutesToRun"
            )

        t.stop()

        return report
    }
}


fun main(args: Array<String>) {
    val source = "https://databus.dbpedia.org/ontologies/dbpedia.org/ontology/2021.11.10-020002/ontology_type=parsed.nt"
//    val processor = FakeProcessor()
//    processor.process(source)

    val inputStream = UriUtil.openStream(URI(source))

    val ont =  inputStream.use {
        val inputHandler = OWLManager.createOWLOntologyManager()
        inputHandler.loadOntologyFromOntologyDocument(it)
    }


    val check = JFactConsistencyCheck(ont)
    val w = TimerWrapper(check, (2).toLong())

    val report = w.getReport()

    println(report)


//    val inputStream = UriUtil.openStream(URI(source))
//
//    val reasoner = inputStream.use {
//        val inputHandler = OWLManager.createOWLOntologyManager()
//        val ont = inputHandler.loadOntologyFromOntologyDocument(it)
//        ReasonerFactory().createReasoner(ont)
//    }
//
//    println(reasoner.isConsistent)
}
