package org.dbpedia.processors

import org.dbpedia.databus_mods.lib.util.UriUtil
import org.dbpedia.databus_mods.lib.worker.execution.Extension
import org.dbpedia.databus_mods.lib.worker.execution.ModProcessor
import org.dbpedia.helpers.HelperConstants
import org.dbpedia.models.ModResult
import org.dbpedia.models.ReasonerReport
import org.dbpedia.runnables.*
import org.semanticweb.owlapi.apibinding.OWLManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.lang.Exception
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeUnit

@Component
class ConsistencyProcessor: ModProcessor {

    private val logger: Logger = LoggerFactory.getLogger(ConsistencyProcessor::class.java)
    init {
        logger.info("Started Successfully!!!!!!")
    }

    // load timeout from cfg
    @Value("\${reasoners.timeout}")
    private val timeOutCounter: Long? = null

    // default timeout in minutes
    private val timeOutUnit = TimeUnit.MINUTES

    override fun process(extension: Extension?) {
        // be null safe, maybe do something more clever here
        timeOutCounter!!
        extension!!
        logger.info("Started process for file: ${extension.source()}")
        // set the values of the extension and add some prefixes
        extension.setType("http://mods.tools.dbpedia.org/ns/demo#ArchivoReasonerMod")
        HelperConstants.namespaces.nsPrefixMap.map { extension.addPrefix(it.key, it.value) }
        // Download File
        // load into owlapi
        val inputStream = UriUtil.openStream(URI(extension.source()))
        val ont = inputStream.use {
            val inputHandler = OWLManager.createOWLOntologyManager()
            inputHandler.loadOntologyFromOntologyDocument(it)
        }
        logger.info("Started generating the Stats for the ontology...")
        val axiomCount = ont.axiomCount
        val classCount = ont.classesInSignature().count().toInt()
        val propCount = (ont.dataPropertiesInSignature().count() + ont.objectPropertiesInSignature().count()).toInt()
        logger.info("Starting Consistency Checks...")

        // initiate checks

        val checks = listOf(HermiTConsistencyCheck::class, ELKConsistencyCheck::class, JFactConsistencyCheck::class).mapNotNull {
            try {
                // call constructor
                it.constructors.first().call(ont)
            } catch (ex: Exception) {
                logger.error("Problem constructing the Reasoner for $it: ${ex.stackTraceToString()}")
                null
            }
        }
        // run them after each other
        val reports = checks.map {
            getReport(it)
        }
        val modResult = ModResult(extension.databusID(), axiomCount, classCount, propCount, reports)
        val consistencyModel = modResult.generateDataModel()
        consistencyModel.write(extension.createModResult("consistencyChecks.ttl","http://dataid.dbpedia.org/ns/mods#statisticsDerivedFrom"), "TURTLE")
    }

    private fun getReport(task: RunnableConsistencyCheck): ReasonerReport {
        timeOutCounter!!
        val t = Thread(task)
        t.start()
        val end = System.currentTimeMillis() + (timeOutCounter * 1000 * 30)

        while (task.reasonerReport == null && System.currentTimeMillis() < end) {
            Thread.sleep(100)
        }
        task.interrupted = true
        val report = task.reasonerReport
            ?: ReasonerReport(
                reasonerID = task.reasonerCheckID,
                isConsistent = null,
                owlProfiles = emptyList(),
                inspectionTime = Duration.ofMinutes(timeOutCounter),
                messageConsistency = "CONSITENCY LOG: Process got interrupted after timeout $timeOutCounter",
                messageProfiles = "CONSITENCY LOG: Process got interrupted after timeout $timeOutCounter"
            )

        // this is deprecated but works, so its fine I guess?
        t.stop()

        return report
    }


}