package org.dbpedia.processors

import org.dbpedia.consistencyChecks.CallableConsistencyCheck
import org.dbpedia.consistencyChecks.ELKConsistencyCheck
import org.dbpedia.consistencyChecks.HermiTConsistencyCheck
import org.dbpedia.consistencyChecks.JFactConsistencyCheck
import org.dbpedia.databus_mods.lib.util.UriUtil
import org.dbpedia.databus_mods.lib.worker.execution.Extension
import org.dbpedia.databus_mods.lib.worker.execution.ModProcessor
import org.dbpedia.helpers.HelperConstants
import org.dbpedia.models.ModResult
import org.dbpedia.models.ReasonerReport
import org.semanticweb.owlapi.apibinding.OWLManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

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
        val inputStream = UriUtil.openStream(URI(extension.source()))
        inputStream.use {
            // load into owlapi
            val inputHandler = OWLManager.createOWLOntologyManager()
            val ont = inputHandler.loadOntologyFromOntologyDocument(it)
            logger.info("Started generating the Stats for the ontology...")
            val axiomCount = ont.axiomCount
            val classCount = ont.classesInSignature().count().toInt()
            val propCount = (ont.dataPropertiesInSignature().count() + ont.objectPropertiesInSignature().count()).toInt()
            logger.info("Starting Consistency Checks...")
            val hermitCheck = HermiTConsistencyCheck(ont, inputHandler)
            val elkCheck = ELKConsistencyCheck(ont, inputHandler)
            val jfactCheck = JFactConsistencyCheck(ont, inputHandler)


            val hermitReport = runTimeOutTask(hermitCheck, timeOutCounter, timeOutUnit)
            val elkReport = runTimeOutTask(elkCheck, timeOutCounter, timeOutUnit)
            val jfactReport = runTimeOutTask(jfactCheck, timeOutCounter, timeOutUnit)

            val modResult = ModResult(extension.source(), axiomCount, classCount, propCount, listOf(hermitReport, elkReport, jfactReport))
            val consistencyModel = modResult.generateDataModel()
            consistencyModel.write(extension.createModResult("consistencyChecks.ttl","http://dataid.dbpedia.org/ns/mods#statisticsDerivedFrom"), "TURTLE")
        }
    }

    private fun runTimeOutTask(check: CallableConsistencyCheck, timeOutCounter: Long, timeOutUnit: TimeUnit): ReasonerReport {
        logger.info("Running ${check.reasonerCheckID}...")
        val service = Executors.newSingleThreadExecutor()
        val future = service.submit(check)
        return try {
            val report = future.get(timeOutCounter, timeOutUnit)
            logger.info("Finished Report of ${check.reasonerCheckID}: $report")
            report
        } catch (timeEx: TimeoutException) {
            logger.error(timeEx.stackTraceToString())
            //service.shutdownNow()
            ReasonerReport(check.reasonerCheckID, null, null, null, "Timeout during execution", "Timeout during Execution")
        } catch (intEx: InterruptedException) {
            logger.error(intEx.stackTraceToString())
            //service.shutdownNow()
            ReasonerReport(check.reasonerCheckID, null, null, null, "Timeout during execution", "Timeout during Execution")
        } finally {
            service.shutdown()
            try {
                if (!service.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    service.shutdownNow()
                }
            } catch (e: InterruptedException) {
                service.shutdownNow()
                logger.error(e.stackTraceToString())
            }
            if (!service.isTerminated) {
                logger.error("Service wasn't terminated for ${check.reasonerCheckID}")
            }
        }
    }
}