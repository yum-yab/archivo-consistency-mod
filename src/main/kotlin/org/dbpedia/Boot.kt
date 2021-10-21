package org.dbpedia

import org.dbpedia.databus_mods.lib.worker.AsyncWorker
import org.dbpedia.databus_mods.lib.worker.execution.Extension
import org.dbpedia.databus_mods.lib.worker.execution.ModProcessor
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import
import org.springframework.stereotype.Component
import org.apache.jena.JenaRuntime
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFWriter
import org.dbpedia.consistencyChecks.*
import org.dbpedia.models.ModResult
import org.dbpedia.models.ReasonerReport
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import java.io.ByteArrayInputStream
import java.net.URI
import java.net.http.*
import java.time.Instant
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@SpringBootApplication
@Import(AsyncWorker::class)
open class ConsistencyMod

@Component
class Processor : ModProcessor {
    override fun process(extension: Extension) {
        // TODO
        //val modResult = generateModResult(extension.databusID(), )
    }
}


val logger = LoggerFactory.getLogger("MAINSCRIPT")

fun loadOntFromString(ntString: String, inputHandler: OWLOntologyManager): OWLOntology? {
    return try {
        val ont = ByteArrayInputStream(ntString.toByteArray()).use {
                input -> inputHandler.loadOntologyFromOntologyDocument(input)
        }
        ont
    } catch (ex: Exception) {
        logger.error("Exception during loading of Ontology: " + ex.stackTraceToString())
        null
    } catch (ex: java.lang.Exception) {
        logger.error("Java Exception during loading of Ontology: " + ex.stackTraceToString())
        null
    }
}

fun runTimeOutTask(check: CallableConsistencyCheck, timeOutCounter: Long, timeOutUnit: TimeUnit): ReasonerReport {
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
        ReasonerReport(check.reasonerCheckID, null, null, -2, "Timeout during execution", "Timeout during Execution")
    } catch (intEx: InterruptedException) {
        logger.error(intEx.stackTraceToString())
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
            logger.error(e.stackTraceToString())
        }
        if (!service.isTerminated) {
            logger.error("Service wasn't terminated for ${check.reasonerCheckID}")
        }
    }
}

@Value("\${reasoner.timeout}")
fun generateModResult(databusID: String, timeOutCounter: Long, timeOutUnit: TimeUnit = TimeUnit.MINUTES): ModResult {

    // Download File
    val startedAt = Instant.now()
    logger.info("Started process for file: $databusID")
    val client = HttpClient.newHttpClient()
    val req = HttpRequest.newBuilder().uri(URI.create(databusID)).build()
    val resp = client.send(req, HttpResponse.BodyHandlers.ofString())
    val ntString = resp.body()

    // load into owlapi
    val inputHandler = OWLManager.createOWLOntologyManager()
    val ont = loadOntFromString(ntString, inputHandler)!!
    logger.info("Started generating the Stats for the ontology...")
    val axiomCount = ont.axiomCount
    val classCount = ont.classesInSignature().count().toInt()
    val propCount = (ont.dataPropertiesInSignature().count() + ont.objectPropertiesInSignature().count()).toInt()
    logger.info("Starting Consistency Checks...")
    val hermitCheck = HermiTConsistencyCheck(ont, inputHandler)
    val elkCheck = ELKConsistencyCheck(ont, inputHandler)
    val jfactCheck = JFactConsistencyCheck(ont, inputHandler)


    val hermitReport = runTimeOutTask(hermitCheck, timeOutCounter, timeOutUnit)
    println(hermitReport)
    val elkReport = runTimeOutTask(elkCheck, timeOutCounter, timeOutUnit)
    println(elkReport)
    val jfactReport = runTimeOutTask(jfactCheck, timeOutCounter, timeOutUnit)

    val modResult = ModResult(databusID, axiomCount, classCount, propCount, listOf(hermitReport, elkReport, jfactReport), startedAt)
    println(RDFWriter.create().source(modResult.generateDataModel()).lang(Lang.TTL).asString())
    println(RDFWriter.create().source(modResult.generateActivityModel()).lang(Lang.TTL).asString())
    return modResult
}

fun main(args: Array<String>) {
    org.apache.jena.query.ARQ.init()
    JenaRuntime.isRDF11 = false
    val exampleID = "https://databus.dbpedia.org/ontologies/dbpedia.org/ontology--DEV/2021.10.02-164001/ontology--DEV_type=parsed.ttl"
//    val archivoOnt = ArchivoOntology("https://databus.dbpedia.org/ontologies/purl.allotrope.org/voc--afo--REC--2021--03--afo/2021.07.04-010558/voc--afo--REC--2021--03--afo_type=parsed.nt",
//        "AFO REC", "https://akswnc7.informatik.uni-leipzig.de/dstreitmatter/archivo/purl.allotrope.org/voc--afo--REC--2021--03--afo/2021.07.04-010558/voc--afo--REC--2021--03--afo_type=parsed.nt")
    val report = generateModResult(exampleID, 10)
    logger.info("Report: $report")
}

