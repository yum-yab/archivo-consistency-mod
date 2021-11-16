package org.dbpedia.runnables

import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.owlapi.model.OWLOntology
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ELKConsistencyCheck(ont: OWLOntology): RunnableConsistencyCheck(ElkReasonerFactory().createReasoner(ont)) {

    override val reasonerCheckID: String by lazy {
        "ELK"
    }

    override val logger: Logger by lazy {
        LoggerFactory.getLogger(ELKConsistencyCheck::class.java)
    }
}