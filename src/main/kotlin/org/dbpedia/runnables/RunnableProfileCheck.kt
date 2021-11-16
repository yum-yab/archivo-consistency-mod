package org.dbpedia.runnables

import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.profiles.Profiles

class RunnableProfileCheck(private val owlOntology: OWLOntology): Runnable {

    var interrupted = false

    var finished = false

    val profiles = mutableListOf<String>()

    override fun run() {
        for (p in Profiles.values()) {
            if (interrupted) {
                break
            }
            val report = p.checkOntology(owlOntology)
            if (report.isInProfile) {
                profiles.add(p.name)
            }
        }
        finished = true
    }

    fun getFinalProfiles(): List<String> {
        return profiles.toList()
    }
}