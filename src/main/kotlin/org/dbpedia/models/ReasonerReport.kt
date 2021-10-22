package org.dbpedia.models

import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.rdf.model.*
import java.time.Duration

data class ReasonerReport(val reasonerID: String, val isConsistent: Boolean?, val owlProfiles: List<String>?, val inspectionTime: Duration?, val messageConsistency: String, val messageProfiles: String) {

    val checkBlankNode: Resource = ResourceFactory.createResource()

    fun toModel(): Model {
        val model = ModelFactory.createDefaultModel()
        // Add the type of report
        model.add(checkBlankNode, ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#checkID"), ResourceFactory.createTypedLiteral(reasonerID))
        // type it as a ReasonerCheck
        model.add(checkBlankNode, ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), ResourceFactory.createResource("https://archivo.dbpedia.org/onto#ReasonerReport"))
        // adds all the different OWL profiles
        owlProfiles?.map { model.add(checkBlankNode, ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#owlProfile"), ResourceFactory.createTypedLiteral(it)) }
        // adds if the ontology is consistent or not
        if (isConsistent != null) {
            model.add(checkBlankNode, ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#isConsistent"), ResourceFactory.createTypedLiteral(isConsistent))
        } else {
            model.add(checkBlankNode, ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#isConsistent"), ResourceFactory.createTypedLiteral("ERROR"))
        }
        // add time used
        if (inspectionTime != null) {
            model.add(checkBlankNode, ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#timeUsed"), ResourceFactory.createTypedLiteral(inspectionTime.toString(), XSDDatatype.XSDduration))
        }
        // add logs
        model.add(checkBlankNode, ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#log"), ResourceFactory.createTypedLiteral(messageConsistency))
        model.add(checkBlankNode, ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#log"), ResourceFactory.createTypedLiteral(messageProfiles))
        return model
    }
}