package org.dbpedia.consistencyChecks

import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.ac.manchester.cs.jfact.JFactFactory

class JFactConsistencyCheck(owlOntology: OWLOntology, m: OWLOntologyManager): CallableConsistencyCheck(factory = JFactFactory(), owlOntology = owlOntology, owlOntologyManager = m) {

    override val reasonerCheckID: String by lazy {
        "JFactConsistencyCheck"
    }

    override val logger: Logger by lazy {
        LoggerFactory.getLogger(JFactConsistencyCheck::class.java)
    }
}