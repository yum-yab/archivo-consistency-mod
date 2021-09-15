package org.dbpedia.helpers

import com.google.gson.Gson
import openllet.jena.PelletInfGraph
import openllet.jena.PelletReasonerFactory
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.ModelFactory
import org.dbpedia.ConsistencyReportOld
import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.OWLClass
import org.semanticweb.owlapi.model.OWLOntology
import java.io.File
import java.io.FileWriter

object HelperFunctions {
    fun calculateELKConsistency(owlOntology: OWLOntology): Boolean? {
        return try {
            val elkFactory = ElkReasonerFactory()
            val elkReasoner = elkFactory.createReasoner(owlOntology)
            elkReasoner.isConsistent
        } catch (e: java.lang.Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    fun testOntologiesInPath(path: String) {
        val inputHandler = OWLManager.createOWLOntologyManager()

        val files = File("/home/denis/Workspace/Job/onto_mini_dump").walk().toList().filter { it.isFile }
        val reports: List<ConsistencyReportOld?> = files.map {
            println("Handling file ${it.path}")
            try {
                val ont = inputHandler.loadOntologyFromOntologyDocument(it)
                val hermitConsistency = calculateHermitConsistency(ont)
                val elkConsistency = calculateELKConsistency(ont)
                val c = ConsistencyReportOld(hermitConsistency, elkConsistency)
                println(c)
                c
            } catch (ex: java.lang.Exception) {
                null
            } catch (e: Exception) {
                null
            } catch (e: OutOfMemoryError) {
                null
            }
        }
        val gson = Gson()
        gson.toJson(reports, FileWriter("./report.json"))

        var elkConsistent = 0
        var elkInconsistent = 0
        var elkError = 0

        var hermitConsistent = 0
        var hermitInconsistent = 0
        var hermitError = 0

        println("Started evaluation of the reports...")
        for (report in reports.filterNotNull()) {
            when(report.elkConsistency) {
                true -> elkConsistent++
                false -> elkInconsistent++
                null -> elkError++
            }
            when(report.elkConsistency) {
                true -> hermitConsistent++
                false -> hermitInconsistent++
                null -> hermitError++
            }
        }

        val failedLoadingSize = reports.filter { it == null }.size
        println("Report:\n" +
                "Failed Loading: $failedLoadingSize\n" +
                "Hermit Reasoner: $hermitConsistent consistent, $hermitInconsistent inconsistent, $hermitError had an error\n" +
                "ELK Reasoner: $elkConsistent consistent, $elkInconsistent inconsistent, $elkError had an error\n")
    }

    fun calculateHermitConsistency(owlOntology: OWLOntology): Boolean? {
        return try {
            val reasonerFactory = org.semanticweb.HermiT.ReasonerFactory()
            val hermitReasoner = reasonerFactory.createReasoner(owlOntology)
            hermitReasoner.isConsistent
        } catch (e: java.lang.Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    fun calculateOpenlletConsistency(uri: String): Boolean {
        val model: OntModel = ModelFactory.createOntologyModel(PelletReasonerFactory.THE_SPEC)
        model.read(uri)
        // load the model to the reasoner
        model.prepare()
        // Get the KnolwedgeBase object
        val kb = (model.graph as PelletInfGraph).kb

        // perform initial consistency check

        return kb.isConsistent
    }
}