package org.dbpedia.helpers

import com.google.gson.Gson
import org.apache.jena.ontology.OntModel
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory
import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.OWLOntology
import java.io.File
import java.io.FileWriter

/*
An object with various different functions making the code cleaner and not fitting in the models
 */

object HelperFunctions