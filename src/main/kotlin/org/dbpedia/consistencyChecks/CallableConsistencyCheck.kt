package org.dbpedia.consistencyChecks

import org.apache.jena.base.Sys
import org.dbpedia.models.ConsistencyReport
import org.semanticweb.owl.explanation.api.ExplanationManager
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory
import org.slf4j.Logger
import java.util.concurrent.Callable

abstract class CallableConsistencyCheck(private val owlOntology: OWLOntology, private val factory: OWLReasonerFactory, private val owlOntologyManager: OWLOntologyManager): Callable<ConsistencyReport> {

    protected abstract val logger: Logger

    abstract val reasonerCheckID: String

    override fun call(): ConsistencyReport {
        val before = System.currentTimeMillis();
        return try {
            val reasoner = factory.createReasoner(owlOntology)
            val consistent = reasoner.isConsistent
            val after = System.currentTimeMillis()
            val msg = if (consistent) {
                "Ontology is Consistent"
            } else {
//                val explainator = ExplanationManager.createExplanationGeneratorFactory(factory) { owlOntologyManager }.createExplanationGenerator(owlOntology)
//                val df = owlOntologyManager.owlDataFactory
//                explainator.getExplanations(df.getOWLSubClassOfAxiom(df.owlThing, df.owlNothing)).map { it.toString() }.toList().joinToString()
                "Ontology is not consistent"
            }
            ConsistencyReport(reasonerCheckID, consistent, msg, after - before, 1)
        } catch (intEx: InterruptedException) {
            logger.warn("Process got interrupted!")
            ConsistencyReport(reasonerCheckID, null, intEx.stackTraceToString(), -1, 0)
        } catch (incnsEx: InconsistentOntologyException) {
            logger.debug(incnsEx.stackTraceToString())
            ConsistencyReport(reasonerCheckID, false, incnsEx.stackTraceToString(), 0, 0)
        } catch (ex: OutOfMemoryError) {
            logger.error("Out of Memory for ")
            ConsistencyReport(reasonerCheckID, false, ex.stackTraceToString(), 0, -1)
        }
        catch (ex: Exception) {
            logger.error("Some Error during Consistency Check: " + ex.localizedMessage)
            ConsistencyReport(reasonerCheckID, null, ex.stackTraceToString(), -1, -1)
        }
    }
}