package org.dbpedia

//import org.dbpedia.databus_mods.lib.worker.AsyncWorker
//import org.dbpedia.databus_mods.lib.worker.execution.Extension
//import org.dbpedia.databus_mods.lib.worker.execution.ModProcessor
//import org.springframework.boot.autoconfigure.SpringBootApplication
//import org.springframework.context.annotation.Import
//import org.springframework.stereotype.Component
import com.google.gson.annotations.SerializedName
import org.apache.jena.JenaRuntime
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.sparql.core.Var
import org.dbpedia.consistencyChecks.CallableConsistencyCheck
import org.dbpedia.consistencyChecks.ELKConsistencyCheck
import org.dbpedia.consistencyChecks.HermiTConsistencyCheck
import org.dbpedia.consistencyChecks.OpenlletConsistencyCheck
import org.dbpedia.helpers.HelperFunctions
import org.dbpedia.models.ConsistencyReport
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.OWLOntologyManager
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.File
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


val logger = LoggerFactory.getLogger("MAINSCRIPT")

data class ConsistencyReportOld(@SerializedName("hermit_consistency") val hermitConsistency: Boolean?,
                                @SerializedName("elk_consistency") val elkConsistency: Boolean?)


data class ArchivoOntology(val databusFileID: String, val title: String, val dlURL: String)

data class OntologyReport(val ontology: ArchivoOntology,
                          val hermitReport: ConsistencyReport,
                          val elkReport: ConsistencyReport,
                          val classCount: Int,
                          val propCount: Int,
                          val axiomCount: Int,
                          val byteSize: Int,
                          val tripleCount: Int) {

    fun toRow(): String {
        return "${ontology.databusFileID},${HelperFunctions.sanitizeForCSV(ontology.title)},$byteSize,$tripleCount,$axiomCount,$classCount,$propCount,${hermitReport.timeUsed},${hermitReport.memoryUsage},${HelperFunctions.sanitizeForCSV(hermitReport.message)},${elkReport.timeUsed},${elkReport.memoryUsage},${HelperFunctions.sanitizeForCSV(elkReport.message)}"
    }
}

fun loadOntFromString(ntString: String, inputHandler: OWLOntologyManager): OWLOntology? {
    return try {
        val ont = ByteArrayInputStream(ntString.toByteArray()).use {
                input -> inputHandler.loadOntologyFromOntologyDocument(input)
        }
        ont
    } catch (ex: Exception) {
        logger.error("Exception during loading of Ontology: " + ex.stackTraceToString())
        null
    } catch (ex: java.lang.Exception) {
        logger.error("Java Exception during loading of Ontology: " + ex.stackTraceToString())
        null
    }
}

fun runTimeOutTask(check: CallableConsistencyCheck, timeOutCounter: Long, timeOutUnit: TimeUnit): ConsistencyReport {
    logger.info("Running ${check.reasonerCheckID}...")
    val service = Executors.newSingleThreadExecutor()
    val future = service.submit(check)
    return try {
        val report = future.get(timeOutCounter, timeOutUnit)
        logger.info("Finished Report of ${check.reasonerCheckID}: $report")
        report
    } catch (timeEx: TimeoutException) {
        logger.error(timeEx.stackTraceToString())
        //service.shutdownNow()
        ConsistencyReport(check.reasonerCheckID, null, "Timeout during execution", -1, 0)
    } catch (intEx: InterruptedException) {
        logger.error(intEx.stackTraceToString())
        //service.shutdownNow()
        ConsistencyReport(check.reasonerCheckID, null, "Timeout during execution", -1, 0)
    } finally {
        service.shutdown()
        try {
            if (!service.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                service.shutdownNow()
                logger.debug("Service is terminated: ${service.isTerminated}")
            }
        } catch (e: InterruptedException) {
            service.shutdownNow()
            logger.error(e.stackTraceToString())
            logger.debug("Service is terminated: ${service.isTerminated}")
        }
    }
}

fun generateReportOfOntology(archivoOnt: ArchivoOntology, timeOutCounter: Long, timeOutUnit: TimeUnit = TimeUnit.MINUTES): OntologyReport {

    // Download File

    logger.info("Started process for ontology: ${archivoOnt.databusFileID}\n\tDownloading File for ont \"${archivoOnt.title}\": ${archivoOnt.dlURL}")
    val client = HttpClient.newHttpClient()
    val req = HttpRequest.newBuilder().uri(URI.create(archivoOnt.dlURL)).build()
    val resp = client.send(req, HttpResponse.BodyHandlers.ofString())
    val ntString = resp.body()
    // determine byte size
    val byteSize = ntString.toByteArray().size

    // load into owlapi
    val inputHandler = OWLManager.createOWLOntologyManager()
    val ont = loadOntFromString(ntString, inputHandler) ?: return OntologyReport(archivoOnt, ConsistencyReport("ERROR", null, "ERROR DURING LOADING", -11, -11), ConsistencyReport("ERROR", null, "ERROR DURING LOADING", -11, -11), -1, -1, -1, byteSize, -1)
    logger.info("Started generating the Stats for the ontology...")
    val axiomCount = ont.axiomCount
    val classCount = ont.classesInSignature().count().toInt()
    val propCount = (ont.dataPropertiesInSignature().count() + ont.objectPropertiesInSignature().count()).toInt()
    val triples = -1
    logger.info("Starting Consistency Checks...")
    val hermitCheck = HermiTConsistencyCheck(ont, inputHandler)
    val elkCheck = ELKConsistencyCheck(ont, inputHandler)
    //val openlletCheck = OpenlletConsistencyCheck(ont, inputHandler)


    val hermitReport = runTimeOutTask(hermitCheck, timeOutCounter, timeOutUnit)

    val elkReport = runTimeOutTask(elkCheck, timeOutCounter, timeOutUnit)

    //val openlletReport = runTimeOutTask(openlletCheck, timeOutCounter, timeOutUnit)

    return OntologyReport(archivoOnt, hermitReport, elkReport, classCount, propCount, axiomCount, byteSize, triples)
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
    logger.info(sparqlQuery.toString())
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
        logger.error("Error: ${ex.stackTraceToString()}")
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
    org.apache.jena.query.ARQ.init()
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
} ORDER BY ?file  """

    val skiplist = listOf("https://databus.dbpedia.org/ontologies/purl.allotrope.org/voc--afo--REC--2021--03--afo/2021.07.04-010558/voc--afo--REC--2021--03--afo_type=parsed.nt",
    "https://databus.dbpedia.org/ontologies/purl.allotrope.org/voc--afo--REC--2021--06--afo/2021.08.04-200617/voc--afo--REC--2021--06--afo_type=parsed.nt",
    "https://databus.dbpedia.org/ontologies/purl.allotrope.org/voc--afo--REC--2020--12--curation/2021.07.26-152230/vocafo--REC--2020--12--curation_type=parsed.nt")

    val lastStop = "https://databus.dbpedia.org/ontologies/purl.allotrope.org/voc--afo--REC--2020--12--curation/2021.07.26-152230/voc--afo--REC--2020--12--curation_type=parsed.nt"
    var stopReached = false
    var stopCounter = 0
    val ontList = getArchivoOntsByQuery(sparql_string)
    logger.info("Found ontologies: ${ontList.size}")
    if (lastStop == "") {
        stopReached = true
    }
    for (ont in ontList) {
        if (!stopReached && ont.databusFileID != lastStop) {
            stopCounter++
            continue
        } else if (!stopReached && ont.databusFileID == lastStop) {
            stopReached = true
            logger.info("Skipped $stopCounter Ontologies, found the last executed one")
            continue
        }
        if (skiplist.contains(ont.databusFileID)){
            logger.info("Skipped ontology ${ont.databusFileID}")
            continue
        }
        val report = generateReportOfOntology(ont, 10)
        logger.info(report.toString())
        File("./no_openllet_output.csv").appendText(report.toRow() + "\n")
    }
//    val archivoOnt = ArchivoOntology("https://databus.dbpedia.org/ontologies/purl.obolibrary.org/obo--ncbitaxon--owl/2021.06.16-143403/obo--ncbitaxon--owl_type=parsed.nt",
//        "ncbitaxon", "https://akswnc7.informatik.uni-leipzig.de/dstreitmatter/archivo/purl.obolibrary.org/obo--ncbitaxon--owl/2021.06.16-143403/obo--ncbitaxon--owl_type=parsed.nt")
//    val report = generateReportOfOntology(archivoOnt, 2)
//    logger.info("Report: $report")
}

