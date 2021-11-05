package org.dbpedia.consistencyChecks

import org.dbpedia.models.ReasonerReport
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.profiles.Profiles
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory
import org.slf4j.Logger
import java.time.Duration
import java.time.Instant
import java.util.concurrent.Callable
import java.util.concurrent.Executors

abstract class CallableConsistencyCheck(val reasoner: OWLReasoner, val factory: OWLReasonerFactory): Callable<ReasonerReport> {

    protected abstract val logger: Logger

    abstract val reasonerCheckID: String

    override fun call(): ReasonerReport {
        println("In check: ${reasoner.toString()}")
        var errorMessage = ""
        //Step 2: Determine Consistency
        val beforeConsistsency = Instant.now()
        val consistent = try {
            reasoner.isConsistent
        } catch (intEx: InterruptedException) {
            logger.warn("Process got interrupted!")
            errorMessage = "CONSISTENCY CHECK: Process got interrupted: " + intEx.stackTraceToString()
            null
        } catch (incnsEx: InconsistentOntologyException) {
            logger.debug(incnsEx.stackTraceToString())
            errorMessage = "CONSISTENCY CHECK: Process got interrupted: " + incnsEx.stackTraceToString()
            null
        } catch (ex: OutOfMemoryError) {
            logger.error("Out of Memory: " + ex.stackTraceToString())
            errorMessage = "CONSISTENCY CHECK: Process got interrupted: " + ex.stackTraceToString()
            null
        } catch (ex: Exception) {
            logger.error("Some Error during Consistency Check: " + ex.stackTraceToString())
            errorMessage = "CONSISTENCY CHECK: Process got interrupted: " + ex.stackTraceToString()
            null
        }
        val consistencyMesage = if (errorMessage == "") "CONSISTENCY CHECK: No Problems occurred" else errorMessage

        // Step 3: Determine Profile
        errorMessage = ""
        val profiles = try {
            determineProfile(reasoner.rootOntology)
        } catch (intEx: InterruptedException) {
            logger.warn("Process got interrupted!")
            errorMessage = "PROFILECHECK: Process got interrupted: " + intEx.stackTraceToString()
            null
        } catch (incnsEx: InconsistentOntologyException) {
            logger.debug(incnsEx.stackTraceToString())
            errorMessage = "PROFILECHECK: Process got interrupted: " + incnsEx.stackTraceToString()
            null
        } catch (ex: OutOfMemoryError) {
            logger.error("Out of Memory: " + ex.stackTraceToString())
            errorMessage = "PROFILECHECK: Process got interrupted: " + ex.stackTraceToString()
            null
        } catch (ex: Exception) {
            logger.error("Some Error during Consistency Check: " + ex.stackTraceToString())
            errorMessage = "PROFILECHECK: Process got interrupted: " + ex.stackTraceToString()
            null
        }
        val timeUsed = Duration.between(beforeConsistsency, Instant.now())
        val profileMessage = if (errorMessage == "") "PROFILECHECK: No Problems occurred" else errorMessage

        return ReasonerReport(reasonerCheckID, consistent, profiles, timeUsed, consistencyMesage, profileMessage)
    }

    private fun determineProfile(ont: OWLOntology): List<String> {
        val resultList = mutableListOf<String>()
        for (p in Profiles.values()) {
            val report = p.checkOntology(ont)
            if (report.isInProfile) {
                resultList.add(p.name)
            }
        }
        return resultList.toList()
    }


}