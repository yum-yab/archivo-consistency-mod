package org.dbpedia.models

import org.apache.jena.datatypes.RDFDatatype
import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.ResourceFactory
import org.dbpedia.helpers.HelperConstants
import java.time.Instant
import java.time.format.DateTimeFormatter




data class ModResult(val dbusId: String, val axiomCount: Int, val classCount: Int, val propertyCount: Int, val reasonerReports: List<ReasonerReport>, val startedAt: Instant, val endedAt: Instant = Instant.now()) {

    /*
    Here the naming conventions for the files are defined:
        activity.ttl for the metafile
        reasoner-report.ttl for the actual generated graph
 */
    private val activityIdentity = "activity.ttl"
    private val dataIdentity = "reasoner-report.ttl"
    private val svgIdentity = "reasoner-report.svg"
    private val modIdentity = "ArchivoReasonerMod"

    fun generateActivityModel(): Model {
        val model = ModelFactory.createDefaultModel()

        model.add(ResourceFactory.createResource(dataIdentity),
            ResourceFactory.createProperty("http://dataid.dbpedia.org/ns/mods/core#wasDerivedFrom"),
            ResourceFactory.createResource(activityIdentity))

        model.add(ResourceFactory.createResource(svgIdentity),
            ResourceFactory.createProperty("http://dataid.dbpedia.org/ns/mods/core#svgDerivedFrom"),
            ResourceFactory.createResource(activityIdentity))

        // type it
        model.add(ResourceFactory.createResource(activityIdentity),
            ResourceFactory.createProperty("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"),
            ResourceFactory.createResource("http://mods.tools.dbpedia.org/ns/demo#$modIdentity"))
        // link graph data and SVG file
        model.add(ResourceFactory.createResource(activityIdentity),
            ResourceFactory.createProperty("http://www.w3.org/ns/prov#generated"),
            ResourceFactory.createResource(dataIdentity))
        model.add(ResourceFactory.createResource(activityIdentity),
            ResourceFactory.createProperty("http://www.w3.org/ns/prov#generated"),
            ResourceFactory.createResource(svgIdentity))
        // provide id of file
        model.add(ResourceFactory.createResource(activityIdentity),
            ResourceFactory.createProperty("http://www.w3.org/ns/prov#used"),
            ResourceFactory.createResource(dbusId))
        // provide time stamps
        model.add(ResourceFactory.createResource(activityIdentity),
            ResourceFactory.createProperty("http://www.w3.org/ns/prov#startedAtTime"),
            ResourceFactory.createTypedLiteral(DateTimeFormatter.ISO_INSTANT.format(startedAt), XSDDatatype.XSDdateTime))
        model.add(ResourceFactory.createResource(activityIdentity),
            ResourceFactory.createProperty("http://www.w3.org/ns/prov#endedAtTime"),
            ResourceFactory.createTypedLiteral(DateTimeFormatter.ISO_INSTANT.format(endedAt), XSDDatatype.XSDdateTime))

        return model.setNsPrefixes(HelperConstants.namespaces)
    }

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
        return model.setNsPrefixes(HelperConstants.namespaces)
    }
}