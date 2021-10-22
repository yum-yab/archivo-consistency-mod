package org.dbpedia

import org.springframework.boot.SpringApplication
import org.dbpedia.databus_mods.lib.worker.AsyncWorker
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@Import(AsyncWorker::class)
open class Worker


val logger = LoggerFactory.getLogger("MAINSCRIPT")

fun main(args: Array<String>) {
    SpringApplication.run(Worker::class.java)
}

