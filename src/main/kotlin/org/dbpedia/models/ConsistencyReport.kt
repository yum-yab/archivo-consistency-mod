package org.dbpedia.models

data class ConsistencyReport(val testIdentifier: String, val isConsistent: Boolean?, val message: String, val timeUsed: Long, val memoryUsage: Long) {
}