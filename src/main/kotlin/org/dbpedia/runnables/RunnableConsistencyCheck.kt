package org.dbpedia.runnables

import org.dbpedia.models.ReasonerReport
import org.semanticweb.HermiT.ReasonerFactory
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.profiles.Profiles
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory
import org.slf4j.Logger
import java.time.Duration
import java.time.Instant

abstract class RunnableConsistencyCheck(val reasoner: OWLReasoner): Runnable {
    protected abstract val logger: Logger

    @Volatile
    var reasonerReport: ReasonerReport? = null

    abstract val reasonerCheckID: String

    var interrupted: Boolean = false

    override fun run() {
        while (!Thread.interrupted()) {
            try {
                var errorMessage = ""
                //Step 2: Determine Consistency
                val beforeConsistsency = Instant.now()
                val consistent = try {
                    reasoner.isConsistent
                }  catch (incnsEx: InconsistentOntologyException) {
                    logger.debug(incnsEx.stackTraceToString())
                    errorMessage = "CONSISTENCY CHECK: Process got interrupted: " + incnsEx.stackTraceToString()
                    null
                } catch (ex: OutOfMemoryError) {
                    logger.error("Out of Memory: " + ex.stackTraceToString())
                    errorMessage = "CONSISTENCY CHECK: Process got interrupted: " + ex.stackTraceToString()
                    null
                }
                val consistencyMesage = if (errorMessage == "") "CONSISTENCY CHECK: No Problems occurred" else errorMessage

                // Step 3: Determine Profile
                errorMessage = ""
                val profiles = try {
                    determineProfile(reasoner.rootOntology)
                }  catch (incnsEx: InconsistentOntologyException) {
                    logger.debug(incnsEx.stackTraceToString())
                    errorMessage = "PROFILECHECK: Process got interrupted: " + incnsEx.stackTraceToString()
                    null
                } catch (ex: OutOfMemoryError) {
                    logger.error("Out of Memory: " + ex.stackTraceToString())
                    errorMessage = "PROFILECHECK: Process got interrupted: " + ex.stackTraceToString()
                    null
                }
                //val profiles = emptyList<String>()
                val timeUsed = Duration.between(beforeConsistsency, Instant.now())
                val profileMessage = if (errorMessage == "") "PROFILECHECK: No Problems occurred" else errorMessage

                reasonerReport = ReasonerReport(reasonerCheckID, consistent, profiles, timeUsed, consistencyMesage, profileMessage)
            } catch (intEx: InterruptedException) {
                logger.error("Thread got interrupted for check of $reasonerCheckID")
            }
        }
    }

    private fun determineProfile(ont: OWLOntology): List<String> {
        val resultList = mutableListOf<String>()
        for (p in Profiles.values()) {
            if (interrupted) {
                return emptyList()
            }
            val report = p.checkOntology(ont)
            if (report.isInProfile) {
                resultList.add(p.name)
            }
        }
        return resultList.toList()
    }

}