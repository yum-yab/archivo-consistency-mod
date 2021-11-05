package org.dbpedia.consistencyChecks

import openllet.owlapi.OpenlletReasonerFactory
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OpenlletConsistencyCheck(reasoner: OWLReasoner): CallableConsistencyCheck(factory = OpenlletReasonerFactory(), reasoner = reasoner) {
    override val reasonerCheckID: String by lazy {
        "OpenlletReasoner"
    }

    override val logger: Logger by lazy {
        LoggerFactory.getLogger(OpenlletConsistencyCheck::class.java)
    }
}