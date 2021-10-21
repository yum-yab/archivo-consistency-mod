package org.dbpedia.models

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class DatabusIdentifier(val publisher: String, val group: String, val artifact: String, val version: String, val file: String) {

    fun toPath(): String {
        return "${publisher}/${group}/${artifact}/${version}/${file}"
    }

    fun getDatabusId(): String {
        return "https://databus.dbpedia.org/" + toPath()
    }

    fun getEncodedIdentifier(): String {
        return URLEncoder.encode(getDatabusId(), StandardCharsets.UTF_8)
    }
}