package org.dbpedia.models

import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.rdf.model.*
import java.time.Duration

data class ReasonerReport(val reasonerID: String, val isConsistent: Boolean?, val inspectionTime: Int, val messageConsistency: String) {

    val checkBlankNode: Resource = ResourceFactory.createResource()

    fun toModel(): Model {
        val model = ModelFactory.createDefaultModel()
        // Add the type of report
        model.add(checkBlankNode, ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#checkID"), ResourceFactory.createTypedLiteral(reasonerID))
        // type it as a ReasonerCheck
        model.add(checkBlankNode, ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), ResourceFactory.createResource("https://archivo.dbpedia.org/onto#ReasonerReport"))
        // adds if the ontology is consistent or not
        if (isConsistent != null) {
            model.add(checkBlankNode, ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#isConsistent"), ResourceFactory.createTypedLiteral(isConsistent))
        } else {
            model.add(checkBlankNode, ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#isConsistent"), ResourceFactory.createTypedLiteral("ERROR"))
        }
        // add time used
        model.add(checkBlankNode, ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#timeUsed"), ResourceFactory.createTypedLiteral(inspectionTime))

        // add logs
        model.add(checkBlankNode, ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#log"), ResourceFactory.createTypedLiteral(messageConsistency))
        return model
    }
}