package org.dbpedia.runnables

import org.semanticweb.HermiT.ReasonerFactory
import org.semanticweb.owlapi.model.OWLOntology
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HermiTConsistencyCheck(ont: OWLOntology): RunnableConsistencyCheck(reasoner = ReasonerFactory().createReasoner(ont)) {

    override val reasonerCheckID: String by lazy {
        "HermiTConsistencyCheck"
    }

    override val logger: Logger by lazy {
        LoggerFactory.getLogger(HermiTConsistencyCheck::class.java)
    }
}