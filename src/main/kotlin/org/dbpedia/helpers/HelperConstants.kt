package org.dbpedia.helpers

import org.apache.jena.shared.PrefixMapping

object HelperConstants {

    val namespaces = PrefixMapping.Factory
        .create()
        .setNsPrefix("xsd", "http://www.w3.org/2001/XMLSchema#")
        .setNsPrefix("mods", "http://dataid.dbpedia.org/ns/mods/core#")
        .setNsPrefix("archivo", "https://archivo.dbpedia.org/onto#")
        .setNsPrefix("prov", "http://www.w3.org/ns/prov#")
}