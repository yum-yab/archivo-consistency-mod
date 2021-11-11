package org.dbpedia.runnables

import openllet.owlapi.OpenlletReasonerFactory
import org.semanticweb.owlapi.model.OWLOntology
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OpenlletConsistencyCheck(ont: OWLOntology): RunnableConsistencyCheck(reasoner = OpenlletReasonerFactory().createReasoner(ont)) {
    override val reasonerCheckID: String by lazy {
        "OpenlletReasoner"
    }

    override val logger: Logger by lazy {
        LoggerFactory.getLogger(OpenlletConsistencyCheck::class.java)
    }
}