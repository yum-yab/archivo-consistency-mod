package org.dbpedia

import org.dbpedia.databus_mods.lib.worker.AsyncWorker
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Import

@SpringBootApplication
@Import(AsyncWorker::class)
open class Worker

fun main(args: Array<String>) {
    SpringApplication.run(Worker::class.java)
}

