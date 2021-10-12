package org.dbpedia.consistencyChecks

import org.apache.jena.base.Sys
import org.dbpedia.models.ConsistencyReport
import org.dbpedia.models.ReasonerReport
import org.semanticweb.owl.explanation.api.ExplanationManager
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.profiles.Profiles
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory
import org.slf4j.Logger
import java.util.concurrent.Callable

abstract class CallableConsistencyCheck(private val owlOntology: OWLOntology, private val factory: OWLReasonerFactory, private val owlOntologyManager: OWLOntologyManager): Callable<ReasonerReport> {

    protected abstract val logger: Logger

    abstract val reasonerCheckID: String

    override fun call(): ReasonerReport {
        var errorMessage = ""

        // Step 1: Load the reasoner, return error report if it didnt work
        val reasoner = try {
            factory.createReasoner(owlOntology)
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
        } ?: return ReasonerReport(reasonerCheckID, null, null, -1, errorMessage, errorMessage)

        //Step 2: Determine Consistency
        errorMessage = ""
        val beforeConsistsency = System.currentTimeMillis()
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
            determineProfile(owlOntology)
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
        val timeUsed = System.currentTimeMillis() - beforeConsistsency
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