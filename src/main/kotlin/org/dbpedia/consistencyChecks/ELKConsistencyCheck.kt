package org.dbpedia.consistencyChecks

import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ELKConsistencyCheck(reasoner: OWLReasoner): CallableConsistencyCheck(factory = ElkReasonerFactory(), reasoner = reasoner) {

    override val reasonerCheckID: String by lazy {
        "ELKConsistencyCheck"
    }

    override val logger: Logger by lazy {
        LoggerFactory.getLogger(ELKConsistencyCheck::class.java)
    }
}