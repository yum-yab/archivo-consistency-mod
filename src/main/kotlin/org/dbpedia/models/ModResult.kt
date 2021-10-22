package org.dbpedia.models

import org.apache.jena.datatypes.RDFDatatype
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.ResourceFactory
import org.dbpedia.helpers.HelperConstants
import java.time.Instant
import java.time.format.DateTimeFormatter




data class ModResult(val dbusId: String, val axiomCount: Int, val classCount: Int, val propertyCount: Int, val reasonerReports: List<ReasonerReport>) {

    fun generateDataModel(): Model {
        val model = ModelFactory.createDefaultModel()

        // add normal metadata (classcount etc)
        model.add(ResourceFactory.createResource(dbusId),
            ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#axiomCount"),
            ResourceFactory.createTypedLiteral(axiomCount)
        )
        model.add(ResourceFactory.createResource(dbusId),
            ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#classCount"),
            ResourceFactory.createTypedLiteral(classCount)
        )
        model.add(ResourceFactory.createResource(dbusId),
            ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#propertyCount"),
            ResourceFactory.createTypedLiteral(propertyCount)
        )
        // add models from the reasoner reports
        for (report in reasonerReports) {
            model.add(ResourceFactory.createResource(dbusId),
                ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#hasReport"),
                report.checkBlankNode
            )
            model.add(report.toModel())
        }
        model.setNsPrefixes(HelperConstants.namespaces)
        return model
    }
}