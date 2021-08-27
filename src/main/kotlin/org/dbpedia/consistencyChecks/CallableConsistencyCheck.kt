package org.dbpedia.consistencyChecks

import org.dbpedia.models.ConsistencyReport
import org.semanticweb.owl.explanation.api.ExplanationManager
import org.semanticweb.owl.explanation.impl.blackbox.InitialEntailmentCheckStrategy
import org.semanticweb.owl.explanation.impl.blackbox.checker.InconsistentOntologyExplanationGeneratorFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory
import org.slf4j.Logger
import java.util.concurrent.Callable
import java.util.function.Supplier


abstract class CallableConsistencyCheck(private val owlOntology: OWLOntology, private val factory: OWLReasonerFactory, private val owlOntologyManager: OWLOntologyManager): Callable<ConsistencyReport> {

    protected abstract val logger: Logger

    protected abstract val reasonerCheckID: String

    override fun call(): ConsistencyReport {
        return try {
            val reasoner = factory.createReasoner(owlOntology)
            val consistent = reasoner.isConsistent

            val msg = if (consistent) {
                "Ontology is Consistent"
            } else {
                val explainator = ExplanationManager.createExplanationGeneratorFactory(factory) { owlOntologyManager }.createExplanationGenerator(owlOntology)
                val df = OWLManager.createOWLOntologyManager().owlDataFactory
                explainator.getExplanations(df.getOWLSubClassOfAxiom(df.owlThing, df.owlNothing)).map { it.toString() }.toList().joinToString()
            }
            ConsistencyReport(reasonerCheckID, consistent, msg)
        } catch (intEx: InterruptedException) {
            logger.warn("Process got interrupted!")
            ConsistencyReport(reasonerCheckID, null, intEx.stackTraceToString())
        } catch (incnsEx: InconsistentOntologyException) {
            logger.debug(incnsEx.stackTraceToString())
            ConsistencyReport(reasonerCheckID, false, incnsEx.stackTraceToString())
        } catch (ex: Exception) {
            logger.error("Some Error during Consistency Check: " + ex.localizedMessage)
            ConsistencyReport(reasonerCheckID, null, ex.stackTraceToString())
        }
    }
}