package org.dbpedia.runnables

import org.semanticweb.owlapi.model.OWLOntology
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.ac.manchester.cs.jfact.JFactFactory

class JFactConsistencyCheck(ont: OWLOntology): RunnableConsistencyCheck(reasoner = JFactFactory().createReasoner(ont)) {

    override val reasonerCheckID: String by lazy {
        "JFact"
    }

    override val logger: Logger by lazy {
        LoggerFactory.getLogger(JFactConsistencyCheck::class.java)
    }
}