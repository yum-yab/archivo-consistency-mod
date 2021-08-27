package org.dbpedia.consistencyChecks

import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ELKConsistencyCheck(owlOntology: OWLOntology, m: OWLOntologyManager): CallableConsistencyCheck(factory = ElkReasonerFactory(), owlOntology = owlOntology, owlOntologyManager = m) {

    override val reasonerCheckID: String by lazy {
        "ELKConsistencyCheck"
    }

    override val logger: Logger by lazy {
        LoggerFactory.getLogger(ELKConsistencyCheck::class.java)
    }
}