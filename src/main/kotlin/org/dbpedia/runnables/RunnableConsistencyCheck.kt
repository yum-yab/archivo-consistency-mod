package org.dbpedia.runnables

import org.dbpedia.models.ReasonerReport
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.slf4j.Logger

abstract class RunnableConsistencyCheck(val reasoner: OWLReasoner) : Runnable {
    protected abstract val logger: Logger

    @Volatile
    var reasonerReport: ReasonerReport? = null

    abstract val reasonerCheckID: String

    override fun run() {
        logger.debug("Started running the check $reasonerCheckID")

        try {

            var errorMessage = ""
            val starttime = System.currentTimeMillis()
            val consistent = try {
                reasoner.isConsistent
            } catch (incnsEx: InconsistentOntologyException) {
                logger.debug(incnsEx.stackTraceToString())
                errorMessage = "CONSISTENCY CHECK: Process got interrupted: " + incnsEx.stackTraceToString()
                null
            } catch (ex: OutOfMemoryError) {
                logger.error("Out of Memory: " + ex.stackTraceToString())
                errorMessage = "CONSISTENCY CHECK: Process got interrupted: " + ex.stackTraceToString()
                null
            }
            val consistencyMessage = if (errorMessage == "") "CONSISTENCY CHECK: No Problems occurred" else errorMessage


            val timeUsed = System.currentTimeMillis() - starttime

            reasonerReport = ReasonerReport(reasonerCheckID, consistent, timeUsed.toInt(), consistencyMessage)
        } catch (intEx: InterruptedException) {
            logger.error("Thread got interrupted for check of $reasonerCheckID")
        }

    }

    fun interrupt() {
        reasoner.interrupt()
    }

}