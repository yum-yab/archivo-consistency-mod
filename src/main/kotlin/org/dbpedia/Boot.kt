package org.dbpedia

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import openllet.jena.PelletInfGraph
import openllet.jena.PelletReasonerFactory
import org.apache.jena.Jena
import org.apache.jena.JenaRuntime
import org.apache.jena.ontology.OntModel
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.sparql.core.Var
import org.dbpedia.consistencyChecks.CallableConsistencyCheck
import org.dbpedia.consistencyChecks.ELKConsistencyCheck
import org.dbpedia.consistencyChecks.HermiTConsistencyCheck
import org.dbpedia.consistencyChecks.OpenlletConsistencyCheck
import org.dbpedia.models.ConsistencyReport
//import org.dbpedia.databus_mods.lib.worker.AsyncWorker
//import org.dbpedia.databus_mods.lib.worker.execution.Extension
//import org.dbpedia.databus_mods.lib.worker.execution.ModProcessor
import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import java.io.ByteArrayInputStream
//import org.springframework.boot.autoconfigure.SpringBootApplication
//import org.springframework.context.annotation.Import
//import org.springframework.stereotype.Component
import java.io.File
import java.io.FileWriter
import java.net.URI
import java.net.http.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

//@SpringBootApplication
//@Import(AsyncWorker::class)
//open class ConsistencyMod
//
//@Component
//class Processor : ModProcessor {
//    override fun process(extension: Extension) {
//        // TODO
//        val reasonerFactory = ElkReasonerFactory()
////        val inputHandler = OWLManager.createOWLOntologyManager()
////        val ouputHandler = OWLManager.createOWLOntologyManager()
////        val ont = inputHandler.loadOntologyFromOntologyDocument(File("./testont.owl"))
////        val reasoner = reasonerFactory.createReasoner(ont)
////        val isConsistent = reasoner.isConsistent
////        println("Ontology is consistent: $isConsistent")
////        extension.setType("http://my.domain/ns#DatabusMod")
////        // File resultFile = extension.createModResult();
//    }
//}

data class ConsistencyReportOld(@SerializedName("hermit_consistency") val hermitConsistency: Boolean?,
                                @SerializedName("elk_consistency") val elkConsistency: Boolean?)


data class ArchivoOntology(val databusFileID: String, val title: String, val dlURL: String)

data class OntologyReport(val ontology: ArchivoOntology,
                          val hermitReport: ConsistencyReport,
                          val openlletReport: ConsistencyReport,
                          val elkReport: ConsistencyReport,
                          val classCount: Int,
                          val propCount: Int,
                          val axiomCount: Int,
                          val byteSize: Int,
                          val tripleCount: Int) {

    fun toRow(): String {
        return "${ontology.databusFileID},${ontology.title},$byteSize,$tripleCount,$axiomCount,$classCount,$propCount,${hermitReport.timeUsed},${hermitReport.memoryUsage},${openlletReport.timeUsed},${openlletReport.memoryUsage},${elkReport.timeUsed},${elkReport.memoryUsage}"
    }
}

fun loadOntFromString(ntString: String, inputHandler: OWLOntologyManager): OWLOntology? {

    return try {
        val inputStream = ByteArrayInputStream(ntString.toByteArray())
        val ont = inputHandler.loadOntologyFromOntologyDocument(inputStream)
        ont
    } catch (ex: Exception) {
        null
    } catch (ex: java.lang.Exception) {
        null
    }
}

fun runTimeOutTask(check: CallableConsistencyCheck, timeOutCounter: Long, timeOutUnit: TimeUnit): ConsistencyReport {
    val service = Executors.newSingleThreadExecutor()
    try {
        val future = service.submit(check)
        val report = future.get(timeOutCounter, timeOutUnit)
        return report
    } catch (timeEx: TimeoutException) {
        println(timeEx.stackTraceToString())
        return ConsistencyReport(check.reasonerCheckID, null, "Timeout uring execution", -1, 0)
    } finally {
        service.shutdown()
    }
}

fun generateReportOfOntology(archivoOnt: ArchivoOntology, timeOutCounter: Long, timeOutUnit: TimeUnit = TimeUnit.MINUTES): OntologyReport {

    // Download File
    val client = HttpClient.newHttpClient()
    val req = HttpRequest.newBuilder().uri(URI.create(archivoOnt.dlURL)).build()
    val resp = client.send(req, HttpResponse.BodyHandlers.ofString())
    val ntString = resp.body()
    // determine byte size
    val byteSize = ntString.toByteArray().size

    // load into owlapi
    val inputHandler = OWLManager.createOWLOntologyManager()
    val ont = loadOntFromString(ntString, inputHandler) ?: return OntologyReport(archivoOnt, ConsistencyReport("ERROR", null, "ERROR DURING LOADING", -11, -11), ConsistencyReport("ERROR", null, "ERROR DURING LOADING", -11, -11), ConsistencyReport("ERROR", null, "ERROR DURING LOADING", -11, -11), -1, -1, -1, byteSize, -1)

    val axiomCount = ont.axiomCount
    val classCount = ont.classesInSignature().count().toInt()
    val propCount = (ont.dataPropertiesInSignature().count() + ont.objectPropertiesInSignature().count()).toInt()
    val triples = -1
    val hermitCheck = HermiTConsistencyCheck(ont, inputHandler)
    val elkCheck = ELKConsistencyCheck(ont, inputHandler)
    val openlletCheck = OpenlletConsistencyCheck(ont, inputHandler)


    val hermitReport = runTimeOutTask(hermitCheck, timeOutCounter, timeOutUnit)

    val elkReport = runTimeOutTask(elkCheck, timeOutCounter, timeOutUnit)

    val openlletReport = runTimeOutTask(openlletCheck, timeOutCounter, timeOutUnit)

    return OntologyReport(archivoOnt, hermitReport, openlletReport, elkReport, classCount, propCount, axiomCount, byteSize, triples)
}




fun loadCollectionFromURI(collectionURI: String, endpoint: String = "https://databus.dbpedia.org/repo/sparql"): List<String> {
    val client = HttpClient.newHttpClient()
    // fetch correct sparql query
    val req = HttpRequest.newBuilder().uri(URI.create(collectionURI)).header("Accept", "text/sparql").build()
    val sparqlQueryString = client.send(req, HttpResponse.BodyHandlers.ofString()).body()

    return getFilesByQuery(sparqlQueryString)
}

fun getFilesByQuery(query: String, endpoint: String = "https://databus.dbpedia.org/repo/sparql"): List<String> {
    val sparqlQuery = QueryFactory.create(query)
    val qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery)
    val results = qexec.execSelect()

    val mutList = mutableListOf<String>()

    while (results.hasNext()) {
        val binding = results.nextBinding()
        val uri = binding.get(Var.alloc("file")).toString(false)
        mutList.add(uri)
    }
    return mutList.toList()
}

fun getArchivoOntsByQuery(query: String, endpoint: String = "https://databus.dbpedia.org/repo/sparql"): List<ArchivoOntology> {
    val sparqlQuery = QueryFactory.create(query)
    println(sparqlQuery.toString())
    try {
        val qexec = QueryExecutionFactory.sparqlService(endpoint, sparqlQuery)
        val results = qexec.execSelect()
        val mutList = mutableListOf<ArchivoOntology>()

        while (results.hasNext()) {
            val solution = results.next()
            val fileID = solution["file"].asResource().uri
            val dlURL = solution["dlURL"].asResource().uri
            val title = solution["title"].asLiteral().string
            mutList.add(ArchivoOntology(fileID, title, dlURL))
        }
        return mutList.toList()
    } catch (ex: Exception) {
        println("Error: ${ex.stackTraceToString()}")
        return listOf()
    }
}


fun main(args: Array<String>) {
    //runApplication<ConsistencyMod>(*args)
//    val inputHandler = OWLManager.createOWLOntologyManager()
//    val ont = inputHandler.resultList(File("./testont.owl"))
//    val hermitCheck = HermiTConsistencyCheck(ont, inputHandler)
//    val elkCheck = ELKConsistencyCheck(ont, inputHandler)
//
//    val service = Executors.newSingleThreadExecutor()
//
//    val future = service.submit(hermitCheck)
//
//    println(future.get(5, TimeUnit.MINUTES))
//
//    val elkFuture = service.submit(elkCheck)
//
//    println(elkFuture.get(5, TimeUnit.MINUTES))
//
//    service.shutdown()
    JenaRuntime.isRDF11 = false
    val sparql_string = """PREFIX dc: <http://purl.org/dc/elements/1.1/>
PREFIX data: <http://data.odw.tw/>
PREFIX da: <https://www.wowman.org/index.php?id=1&type=get#>
PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
PREFIX dcat: <http://www.w3.org/ns/dcat#>
PREFIX dct: <http://purl.org/dc/terms/>
PREFIX dataid-cv: <http://dataid.dbpedia.org/ns/cv#>
PREFIX dataid: <http://dataid.dbpedia.org/ns/core#>
PREFIX databus: <https://databus.dbpedia.org/>
SELECT DISTINCT ?file ?title ?dlURL WHERE { 
		  ?dataset dataid:account databus:ontologies .
		  ?dataset dataid:artifact ?art.
		  ?dataset dcat:distribution ?distribution .
  		  ?dataset dct:title ?title .
		  ?distribution dataid-cv:type 'parsed'^^xsd:string . 
		  ?distribution dataid:formatExtension 'nt'^^xsd:string . 
		  ?distribution dataid:file ?file .
  		  ?distribution dcat:downloadURL ?dlURL .
		  ?dataset dct:hasVersion ?latestVersion .
		  { 
		    # Selects the latest version
		    SELECT DISTINCT ?art (MAX(?v) as ?latestVersion) WHERE {
		      ?dataset dataid:account databus:ontologies .
			  ?dataset dataid:artifact ?art.
			  ?dataset dcat:distribution ?distribution .
			  ?dataset dct:hasVersion ?v .
			} GROUP BY ?art 
		  }
		  # Excludes dev versions
		  FILTER (!regex(?art, "--DEV"))
		  # Excludes sorted versions to prevent duplicates
		  MINUS { ?distribution dataid:contentVariant 'sorted'^^xsd:string . }
}  """
    val ontList = getArchivoOntsByQuery(sparql_string)
    println(ontList.size)
    for (ont in ontList) {
        val report = generateReportOfOntology(ont, 10)
        println(report)
        File("./output.csv").appendText(report.toRow() + "\n")
    }
//    val archivoOnt = ArchivoOntology("https://databus.dbpedia.org/ontologies/dbpedia.org/ontology/2021.09.15-100002/ontology_type=parsed.nt",
//        "The DBpedia Ontology", "https://akswnc7.informatik.uni-leipzig.de/dstreitmatter/archivo/dbpedia.org/ontology/2021.09.15-100002/ontology_type=parsed.nt")
//    val report = generateReportOfOntology(archivoOnt, 10)
//    println(report)
}

