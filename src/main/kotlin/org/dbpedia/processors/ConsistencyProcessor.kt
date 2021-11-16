package org.dbpedia.processors

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.ResourceFactory
import org.dbpedia.databus_mods.lib.util.UriUtil
import org.dbpedia.databus_mods.lib.worker.execution.Extension
import org.dbpedia.databus_mods.lib.worker.execution.ModProcessor
import org.dbpedia.helpers.HelperConstants
import org.dbpedia.models.ModResult
import org.dbpedia.models.ReasonerReport
import org.dbpedia.runnables.*
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.OWLOntology
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.lang.Exception
import java.net.URI
import java.util.concurrent.TimeUnit

@Component
class ConsistencyProcessor: ModProcessor {

    private val logger: Logger = LoggerFactory.getLogger(ConsistencyProcessor::class.java)
    init {
        logger.info("Started Successfully!!!!!!")
    }

    // load timeout from cfg
    @Value("\${reasoners.timeout}")
    private val timeOutCounter: Long = 1

    @Value("\${version.currentHash}")
    private val currentHash: String = ""

    @Value("\${reasoners.reprieve}")
    private val reprieveTime: Int = 10

    // default timeout in minutes
    private val timeOutUnit = TimeUnit.MINUTES

    override fun process(extension: Extension?) {
        // be null safe, maybe do something more clever here
        extension!!
        logger.info("Started process for file: ${extension.source()}")
        // set the values of the extension and add some prefixes
        extension.setType("http://mods.tools.dbpedia.org/ns/demo#ArchivoReasonerMod")
        HelperConstants.namespaces.nsPrefixMap.map { extension.addPrefix(it.key, it.value) }
        // Download File
        // load into owlapi
        val inputStream = UriUtil.openStream(URI(extension.source()))
        var owlapiLog = "Loading in OWLAPI successfull"
        val ont = try {
                inputStream.use {
                    val inputHandler = OWLManager.createOWLOntologyManager()
                    inputHandler.loadOntologyFromOntologyDocument(it)
                }
            } catch (ex: Exception) {
                owlapiLog = "ERROR during OWLAPI loading: " + ex.stackTraceToString()
                null
            }
        extension.model.add(
            ResourceFactory.createResource("metadata.ttl"),
            ResourceFactory.createProperty("http://dataid.dbpedia.org/ns/mods/core#version"),
            ResourceFactory.createTypedLiteral(currentHash)
        )
        val resultModel = ModelFactory.createDefaultModel()
        resultModel.add(
            ResourceFactory.createResource(extension.databusID()),
            ResourceFactory.createProperty("https://archivo.dbpedia.org/onto#log"),
            ResourceFactory.createTypedLiteral(owlapiLog)
        )
        if (ont != null) {
            generateRealReport(extension, ont, resultModel)
        }
    }

    private fun generateRealReport(extension: Extension, ont: OWLOntology, resultModel: Model) {
        logger.info("Started generating the Stats for the ontology...")
        val axiomCount = ont.axiomCount
        logger.debug("Axioms: $axiomCount")
        val classCount = ont.classesInSignature().count().toInt()
        logger.debug("Classes: $classCount")
        val propCount = (ont.dataPropertiesInSignature().count() + ont.objectPropertiesInSignature().count()).toInt()
        logger.debug("Properties: $propCount")
        val endtime = System.currentTimeMillis() + (timeOutCounter * 1000 * 60)
        val profiles = try {
            getProfiles(RunnableProfileCheck(ont), endtime)
        } catch (ex: Exception) {
            logger.error("problem during OWL2 profile calculation: " + ex.stackTraceToString())
            null
        }
        logger.debug("Profiles: $profiles")

        logger.info("Starting Consistency Checks...")

        // initiate checks

        val checks = listOf(HermiTConsistencyCheck::class, ELKConsistencyCheck::class, JFactConsistencyCheck::class, OpenlletConsistencyCheck::class).mapNotNull {
            try {
                // call constructor
                it.constructors.first().call(ont)
            } catch (ex: Exception) {
                logger.error("Problem constructing the Reasoner for $it: ${ex.stackTraceToString()}")
                null
            }
        }
        // run them after each other
        val reports = checks.map {
            getReport(it)
        }
        // set the current commit hash as version
        extension.model.add(ResourceFactory.createResource("metadata.ttl"),
            ResourceFactory.createProperty("http://dataid.dbpedia.org/ns/mods/core#version"),
            ResourceFactory.createTypedLiteral(currentHash)
        )

        // add the data to the extension
        val modResult = ModResult(extension.databusID(), axiomCount, classCount, propCount, profiles, reports)
        val consistencyModel = modResult.generateDataModel()
        consistencyModel.write(extension.createModResult("consistencyChecks.ttl","http://dataid.dbpedia.org/ns/mods#statisticsDerivedFrom"), "TURTLE")
    }

    private fun getReport(task: RunnableConsistencyCheck): ReasonerReport {
        timeOutCounter!!
        val t = Thread(task)
        val end = System.currentTimeMillis() + (timeOutCounter * 1000 * 60)
        t.start()

        while (task.reasonerReport == null && System.currentTimeMillis() < end) {
            Thread.sleep(100)
        }
        task.interrupt()
        t.interrupt()
        val endOfReprieve = System.currentTimeMillis() + (reprieveTime * 1000)
        while(t.isAlive && System.currentTimeMillis() < endOfReprieve) {
            Thread.sleep(100)
        }
        val report = task.reasonerReport
            ?: ReasonerReport(
                reasonerID = task.reasonerCheckID,
                isConsistent = null,
                inspectionTime = (timeOutCounter * 60 * 1000).toInt(),
                messageConsistency = "CONSITENCY LOG: Process got interrupted after timeout $timeOutCounter",
            )

        // this is deprecated but works, so its fine I guess?
        if (t.isAlive) {
            logger.info("Thread has to be stopped!")
            t.stop()
        }

        return report
    }

    private fun getProfiles(profileCheck: RunnableProfileCheck, endtime: Long): List<String> {
        timeOutCounter!!
        val t = Thread(profileCheck)
        t.start()

        while (!profileCheck.finished && System.currentTimeMillis() < endtime) {
            Thread.sleep(100)
        }
        profileCheck.interrupted = true
        t.interrupt()
        val endOfReprieve = System.currentTimeMillis() + (reprieveTime * 1000)
        while(t.isAlive && System.currentTimeMillis() < endOfReprieve) {
            Thread.sleep(100)
        }
        val profiles = profileCheck.getFinalProfiles()

        // this is deprecated but works, so its fine I guess?
        if (t.isAlive) {
            logger.info("Thread has to be stopped!")
            t.stop()
        }

        return profiles
    }


}