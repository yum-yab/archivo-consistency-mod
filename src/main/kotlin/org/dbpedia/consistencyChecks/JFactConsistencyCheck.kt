package org.dbpedia.consistencyChecks

import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.semanticweb.owlapi.reasoner.OWLReasoner
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import uk.ac.manchester.cs.jfact.JFactFactory

class JFactConsistencyCheck(reasoner: OWLReasoner): CallableConsistencyCheck(factory = JFactFactory(), reasoner = reasoner) {

    override val reasonerCheckID: String by lazy {
        "JFactConsistencyCheck"
    }

    override val logger: Logger by lazy {
        LoggerFactory.getLogger(JFactConsistencyCheck::class.java)
    }
}