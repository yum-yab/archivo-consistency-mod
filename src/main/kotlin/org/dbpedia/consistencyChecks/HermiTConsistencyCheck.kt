package org.dbpedia.consistencyChecks

import org.semanticweb.HermiT.ReasonerFactory
import org.semanticweb.owlapi.model.OWLOntology
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HermiTConsistencyCheck(owlOntology: OWLOntology): RunnableConsistencyCheck(factory = ReasonerFactory(), owlOntology = owlOntology) {

    override val reasonerCheckID: String by lazy {
        "HermiTConsistencyCheck"
    }

    override val logger: Logger by lazy {
        LoggerFactory.getLogger(HermiTConsistencyCheck::class.java)
    }
}