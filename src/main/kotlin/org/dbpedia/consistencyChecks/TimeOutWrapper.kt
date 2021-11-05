package org.dbpedia.consistencyChecks

import org.dbpedia.models.ReasonerReport
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class TimeOutWrapper(val factory: OWLReasonerFactory, val ontology: OWLOntology) {

    private val logger = LoggerFactory.getLogger(TimeOutWrapper::class.java)

    private val timeOutUnit = TimeUnit.SECONDS

    private val timeOut: Long = 1

    fun runTimeOutTask(): ReasonerReport {
        var errorMessage = ""

        val reasoner = try {
            factory.createReasoner(ontology)
        } catch (ex: OutOfMemoryError) {
            logger.error("Out of Memory: " + ex.stackTraceToString())
            errorMessage = "Error during loading the Ontology: " + ex.stackTraceToString()
            null
        } catch (ex: Exception) {
            errorMessage = "Error during loading the Ontology: " + ex.stackTraceToString()
            null
        } catch (ex: java.lang.Exception) {
            logger.error("Java Exception: " + ex.stackTraceToString())
            errorMessage = "Error during loading the Ontology: " + ex.stackTraceToString()
            null
        } ?: return ReasonerReport("SomeSome", null, null, null, errorMessage, errorMessage)

        val jfactCheck = HermiTConsistencyCheck(reasoner)

        val executor = Executors.newSingleThreadExecutor()

        val future = executor.submit(jfactCheck)
        val report = try {
            future.get(timeOut, timeOutUnit)
        } catch (intEx: InterruptedException) {
            logger.info("Process timed out: " + intEx.stackTraceToString())
            reasoner.interrupt()
            ReasonerReport(jfactCheck.reasonerCheckID, null, null, null, "CONSISTENCY CHECK: Timeout during execution", "PROFILE CHECK: Timeout during Execution")
        } catch (timeEx: TimeoutException) {
            logger.info("Process timed out: " + timeEx.stackTraceToString())
            reasoner.interrupt()
            ReasonerReport(jfactCheck.reasonerCheckID, null, null, null, "CONSISTENCY CHECK: Timeout during execution", "PROFILE CHECK: Timeout during Execution")
        } finally {
            executor.shutdown()
            reasoner.interrupt()
            logger.info("Future Is cancelled: ${future.cancel(true)}")
            try {
                if (!executor.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                    executor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                executor.shutdownNow()
                logger.error(e.stackTraceToString())
            }
            if (!executor.isTerminated) {
                logger.error("Service wasn't shut down for ${jfactCheck.reasonerCheckID}")
            }
        }
        return report
    }
}