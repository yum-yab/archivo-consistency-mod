package org.dbpedia.processors

import org.dbpedia.consistencyChecks.CallableConsistencyCheck
import org.dbpedia.consistencyChecks.ELKConsistencyCheck
import org.dbpedia.consistencyChecks.HermiTConsistencyCheck
import org.dbpedia.consistencyChecks.JFactConsistencyCheck
import org.dbpedia.databus_mods.lib.util.IORdfUtil
import org.dbpedia.databus_mods.lib.util.UriUtil
import org.dbpedia.databus_mods.lib.worker.execution.Extension
import org.dbpedia.databus_mods.lib.worker.execution.ModProcessor
import org.dbpedia.helpers.HelperConstants
import org.dbpedia.models.ModResult
import org.dbpedia.models.ReasonerReport
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Component
class ConsistencyProcessor: ModProcessor {

    private val logger: Logger = LoggerFactory.getLogger(ConsistencyProcessor::class.java)


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
            val ont = loadOntFromString(it, inputHandler)!!
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

    private fun loadOntFromString(inputStream: InputStream, inputHandler: OWLOntologyManager): OWLOntology? {
        return try {
            inputHandler.loadOntologyFromOntologyDocument(inputStream)
        } catch (ex: Exception) {
            org.dbpedia.logger.error("Exception during loading of Ontology: " + ex.stackTraceToString())
            null
        } catch (ex: java.lang.Exception) {
            org.dbpedia.logger.error("Java Exception during loading of Ontology: " + ex.stackTraceToString())
            null
        }
    }

    private fun runTimeOutTask(check: CallableConsistencyCheck, timeOutCounter: Long, timeOutUnit: TimeUnit): ReasonerReport {
        org.dbpedia.logger.info("Running ${check.reasonerCheckID}...")
        val service = Executors.newSingleThreadExecutor()
        val future = service.submit(check)
        return try {
            val report = future.get(timeOutCounter, timeOutUnit)
            org.dbpedia.logger.info("Finished Report of ${check.reasonerCheckID}: $report")
            report
        } catch (timeEx: TimeoutException) {
            org.dbpedia.logger.error(timeEx.stackTraceToString())
            //service.shutdownNow()
            ReasonerReport(check.reasonerCheckID, null, null, -2, "Timeout during execution", "Timeout during Execution")
        } catch (intEx: InterruptedException) {
            org.dbpedia.logger.error(intEx.stackTraceToString())
            //service.shutdownNow()
            ReasonerReport(check.reasonerCheckID, null, null, -2, "Timeout during execution", "Timeout during Execution")
        } finally {
            service.shutdown()
            try {
                if (!service.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    service.shutdownNow()
                }
            } catch (e: InterruptedException) {
                service.shutdownNow()
                org.dbpedia.logger.error(e.stackTraceToString())
            }
            if (!service.isTerminated) {
                org.dbpedia.logger.error("Service wasn't terminated for ${check.reasonerCheckID}")
            }
        }
    }
}